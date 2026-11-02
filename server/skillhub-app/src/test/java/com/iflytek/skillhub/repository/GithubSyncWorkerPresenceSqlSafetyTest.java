package com.iflytek.skillhub.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Array;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLSyntaxErrorException;
import java.util.List;
import javax.sql.DataSource;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.SQLExceptionSubclassTranslator;

class GithubSyncWorkerPresenceSqlSafetyTest {
    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void heartbeatBindsHostileValuesAndReleasesArrayEvenWhenDatabaseFails(boolean fail) throws Exception {
        DataSource source = mock(DataSource.class);
        Connection connection = mock(Connection.class);
        PreparedStatement statement = mock(PreparedStatement.class);
        Array array = mock(Array.class);
        when(source.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(statement);
        when(statement.getConnection()).thenReturn(connection);
        when(connection.createArrayOf(eq("text"), any(Object[].class))).thenReturn(array);
        if (fail) {
            when(statement.executeUpdate()).thenReturn(1).thenThrow(new SQLSyntaxErrorException("fixture failure"));
        } else {
            when(statement.executeUpdate()).thenReturn(1);
        }
        var jdbc = new JdbcTemplate(source);
        jdbc.setExceptionTranslator(new SQLExceptionSubclassTranslator());
        var repository = new GithubSyncWorkerPresenceRepository(jdbc);
        String worker = "worker'); DELETE FROM github_sync_task; --";
        List<String> capabilities = List.of("a', '{}'); SELECT pg_sleep(30); --", "normal");

        if (fail) {
            assertThatThrownBy(() -> repository.heartbeat(worker, 1, capabilities))
                    .isInstanceOf(DataAccessException.class);
        } else {
            repository.heartbeat(worker, 1, capabilities);
        }

        var sql = ArgumentCaptor.forClass(String.class);
        verify(connection, org.mockito.Mockito.times(2)).prepareStatement(sql.capture());
        assertThat(sql.getAllValues()).allSatisfy(value -> assertThat(value).doesNotContain(worker, capabilities.getFirst()));
        verify(connection, never()).createStatement();
        verify(statement).setString(1, worker);
        verify(connection).createArrayOf("text", capabilities.toArray());
        verify(statement).setArray(3, array);
        verify(array).free();
    }
}
