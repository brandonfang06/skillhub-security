package com.iflytek.skillhub.repository;

import com.iflytek.skillhub.domain.namespace.Namespace;
import com.iflytek.skillhub.domain.namespace.NamespaceStatus;
import com.iflytek.skillhub.domain.namespace.NamespaceType;
import com.iflytek.skillhub.domain.user.UserAccount;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(JpaAdminNamespaceQueryRepository.class)
@Testcontainers
class JpaAdminNamespaceQueryRepositoryTest {
    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.PostgreSQLDialect");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("spring.flyway.enabled", () -> true);
    }

    @Autowired EntityManager entities;
    @Autowired AdminNamespaceQueryRepository namespaces;

    @BeforeEach
    void owner() {
        entities.persist(new UserAccount("test-owner", "Test owner", null, null));
    }

    @Test
    void combinesFiltersAndCountsTheSameMatches() {
        Namespace expected = namespace("query-engineering", NamespaceStatus.ACTIVE, NamespaceType.TEAM);
        namespace("query-archived-engineering", NamespaceStatus.ARCHIVED, NamespaceType.TEAM);
        namespace("query-catalog-engineering", NamespaceStatus.ACTIVE, NamespaceType.CATALOG);
        entities.flush();

        var result = namespaces.search(" ENGINEERING ", NamespaceStatus.ACTIVE, NamespaceType.TEAM,
                PageRequest.of(0, 10));

        assertThat(result.getContent()).extracting(Namespace::getId).containsExactly(expected.getId());
        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    void treatsSqlControlCharactersAsSearchData() {
        namespace("query-safe", NamespaceStatus.ACTIVE, NamespaceType.TEAM);
        entities.flush();

        var result = namespaces.search("%' OR 1=1 --", null, null, PageRequest.of(0, 10));

        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isZero();
    }

    @Test
    void blankFiltersKeepPaginationAndTheFullCount() {
        namespace("query-first", NamespaceStatus.ACTIVE, NamespaceType.TEAM);
        namespace("query-second", NamespaceStatus.ARCHIVED, NamespaceType.CATALOG);
        entities.flush();
        long count = namespaces.search(null, null, null, PageRequest.of(0, 100)).getTotalElements();

        var result = namespaces.search("  ", null, null, PageRequest.of(0, 1));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getTotalElements()).isEqualTo(count);
    }

    private Namespace namespace(String slug, NamespaceStatus status, NamespaceType type) {
        Namespace namespace = new Namespace(slug, slug, "test-owner");
        namespace.setStatus(status);
        namespace.setType(type);
        entities.persist(namespace);
        return namespace;
    }
}
