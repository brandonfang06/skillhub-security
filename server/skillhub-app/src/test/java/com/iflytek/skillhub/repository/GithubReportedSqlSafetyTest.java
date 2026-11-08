package com.iflytek.skillhub.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iflytek.skillhub.githubsync.GithubSystemCredential;
import com.iflytek.skillhub.infra.jpa.JdbcGithubDiscoveryEntryRepository;
import com.iflytek.skillhub.infra.jpa.JdbcGithubSystemCredentialRepository;
import java.time.Instant;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Testcontainers
class GithubReportedSqlSafetyTest {
    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    private JdbcTemplate jdbc;

    @BeforeEach
    void migrateIsolatedDatabase() {
        var dataSource = new DriverManagerDataSource(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
        Flyway.configure().dataSource(dataSource).locations("classpath:db/migration").load().migrate();
        jdbc = new JdbcTemplate(dataSource);
    }

    @Test
    void credentialQueriesKeepQuotedValuesAsDataAndEnforceTheResourceAllowlist() {
        var credentials = new JdbcGithubSystemCredentialRepository(jdbc);
        UUID id = UUID.randomUUID();
        Instant now = Instant.now();
        String label = "reader'); DROP TABLE github_system_credential; --";
        String fingerprint = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        credentials.create(new GithubSystemCredential(id, label, fingerprint, "v1", new byte[12],
                new byte[17], "ACTIVE", null, null, now, now));

        assertThat(credentials.find(id)).get().extracting(GithubSystemCredential::label).isEqualTo(label);
        assertThat(credentials.list()).extracting(GithubSystemCredential::id).contains(id);
        assertThat(credentials.find(UUID.randomUUID())).isEmpty();
        assertThat(credentials.availableForClaim(now, "CORE")).get()
                .extracting(GithubSystemCredential::id).isEqualTo(id);
        assertThatThrownBy(() -> credentials.availableForClaim(now, "CORE' OR TRUE --"))
                .isInstanceOf(IllegalArgumentException.class);

        String updated = "updated' /* still data */";
        assertThat(credentials.replace(id, updated, fingerprint, "v1", new byte[12], new byte[17])).isTrue();
        assertThat(credentials.find(id)).get().extracting(GithubSystemCredential::label).isEqualTo(updated);
        assertThat(credentials.setEnabled(id, false)).isTrue();
        assertThat(credentials.availableForClaim(now, "CORE")).isEmpty();
    }

    @Test
    void discoveryQueriesPreserveQuotedValuesAndNamespaceIsolation() {
        var entries = new JdbcGithubDiscoveryEntryRepository(jdbc, new ObjectMapper());
        String ownerId = UUID.randomUUID().toString();
        jdbc.update("INSERT INTO user_account (id, display_name) VALUES (?, ?)", ownerId, "SQL fixture");
        long team = jdbc.queryForObject("""
                INSERT INTO namespace (slug, display_name, type, status, created_by)
                VALUES (?, 'SQL fixture', 'TEAM', 'ACTIVE', ?) RETURNING id
                """, Long.class, "sql-" + UUID.randomUUID(), ownerId);
        String owner = "owner' OR TRUE --";
        String repository = "repo'); SELECT pg_sleep(10); --";
        var teamEntry = entries.createTeamRepository(team, owner, repository, "NAMESPACE_ONLY");

        assertThat(entries.findTeamRepository(team, teamEntry.id())).contains(teamEntry);
        assertThat(entries.findTeamRepository(team + 1, teamEntry.id())).isEmpty();
        assertThat(entries.findPlatformEntry(teamEntry.id())).isEmpty();
        assertThat(entries.listTeamRepositories(team)).singleElement().satisfies(entry -> {
            assertThat(entry.ownerLogin()).isEqualTo(owner);
            assertThat(entry.repositoryName()).isEqualTo(repository);
        });

        String title = "rule' UNION SELECT NULL --";
        String criteria = "{\"query\":\"'; DROP TABLE github_discovery_entry; --\"}";
        var rule = entries.createSearchRule(title, criteria, 86400);
        assertThat(entries.findPlatformEntry(rule.id())).get().satisfies(entry -> {
            assertThat(entry.title()).isEqualTo(title);
            assertThat(entry.criteriaJson()).contains("DROP TABLE github_discovery_entry");
        });
        assertThat(entries.listPlatformEntries()).extracting(entry -> entry.id()).contains(rule.id());
        assertThat(entries.setEnabled(rule.id(), true).enabled()).isTrue();
        assertThat(entries.findTeamRepository(team, rule.id())).isEmpty();
        assertThat(entries.updateSearchRule(rule.id(), title, criteria, 43200, false).enabled()).isFalse();
    }
}
