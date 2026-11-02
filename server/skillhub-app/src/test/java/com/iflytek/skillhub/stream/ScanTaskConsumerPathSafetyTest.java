package com.iflytek.skillhub.stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.iflytek.skillhub.domain.security.ScanTaskProducer;
import com.iflytek.skillhub.domain.security.SecurityScanService;
import com.iflytek.skillhub.domain.security.SecurityScanner;
import com.iflytek.skillhub.domain.security.ScannerType;
import com.iflytek.skillhub.domain.skill.SkillVersionRepository;
import com.iflytek.skillhub.observability.MessageObservationSupport;
import com.iflytek.skillhub.observability.RequestIdAccessor;
import com.iflytek.skillhub.storage.ObjectStorageService;
import io.micrometer.observation.ObservationRegistry;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.redisson.api.RedissonClient;

class ScanTaskConsumerPathSafetyTest {

    private ScanTaskConsumer consumer() {
        return new ScanTaskConsumer(
                org.mockito.Mockito.mock(RedissonClient.class), "scan-stream", "scan-group",
                org.mockito.Mockito.mock(SecurityScanner.class),
                org.mockito.Mockito.mock(SecurityScanService.class),
                org.mockito.Mockito.mock(SkillVersionRepository.class),
                org.mockito.Mockito.mock(ScanTaskProducer.class),
                org.mockito.Mockito.mock(ObjectStorageService.class),
                new MessageObservationSupport(ObservationRegistry.NOOP, new RequestIdAccessor()));
    }

    @Test
    void localScanRejectsPathsOutsideWorkspaceAndWorkspaceRoot() throws Exception {
        Path outside = Files.createTempDirectory("scan-outside-");
        Path root = Path.of("/tmp/skillhub-scans").toAbsolutePath().normalize();
        Method resolve = ScanTaskConsumer.class.getDeclaredMethod("resolveWorkingSkillPath",
                ScanTaskConsumer.ScanTaskPayload.class);
        resolve.setAccessible(true);
        try {
            for (String path : java.util.List.of(outside.toString(), root.toString(),
                    root.resolve("../secret").toString(), root + "-other/file")) {
                var payload = new ScanTaskConsumer.ScanTaskPayload(
                        "task-path", 42L, path, null, ScannerType.SKILL_SCANNER);
                assertThatThrownBy(() -> resolve.invoke(consumer(), payload))
                        .hasCauseInstanceOf(IllegalStateException.class);
                assertThat(payload.workingSkillPath()).isNull();
            }
        } finally {
            Files.delete(outside);
        }
    }

    @Test
    void localScanAndCleanupRejectSymbolicLinkAncestors() throws Exception {
        Path root = Path.of("/tmp/skillhub-scans").toAbsolutePath().normalize();
        Files.createDirectories(root);
        Path owned = Files.createTempDirectory(root, "scan-path-safety-");
        Path outside = Files.createTempDirectory("scan-outside-");
        Path link = owned.resolve("alias");
        Path sentinel = outside.resolve("keep.txt");
        Files.writeString(sentinel, "keep");
        try {
            try {
                Files.createSymbolicLink(link, outside);
            } catch (UnsupportedOperationException | java.nio.file.FileSystemException exception) {
                org.junit.jupiter.api.Assumptions.abort("Symbolic links unavailable: " + exception.getClass().getSimpleName());
            }
            var payload = new ScanTaskConsumer.ScanTaskPayload("task-link", 42L,
                    link.resolve("keep.txt").toString(), null, ScannerType.SKILL_SCANNER);
            Method resolve = ScanTaskConsumer.class.getDeclaredMethod("resolveWorkingSkillPath",
                    ScanTaskConsumer.ScanTaskPayload.class);
            resolve.setAccessible(true);
            assertThatThrownBy(() -> resolve.invoke(consumer(), payload))
                    .hasCauseInstanceOf(IllegalStateException.class);
            Method cleanup = ScanTaskConsumer.class.getDeclaredMethod("cleanupTempPath", String.class);
            cleanup.setAccessible(true);
            cleanup.invoke(consumer(), link.resolve("keep.txt").toString());
            assertThat(Files.readString(sentinel)).isEqualTo("keep");
        } finally {
            Files.deleteIfExists(link);
            Files.delete(owned);
            Files.delete(sentinel);
            Files.delete(outside);
        }
    }

    @Test
    void cleanupTempPath_ignoresPathsOutsideScanTempDirectory() throws Exception {
        ScanTaskConsumer consumer = new ScanTaskConsumer(
                org.mockito.Mockito.mock(RedissonClient.class),
                "scan-stream",
                "scan-group",
                org.mockito.Mockito.mock(SecurityScanner.class),
                org.mockito.Mockito.mock(SecurityScanService.class),
                org.mockito.Mockito.mock(SkillVersionRepository.class),
                org.mockito.Mockito.mock(ScanTaskProducer.class),
                org.mockito.Mockito.mock(ObjectStorageService.class),
                new MessageObservationSupport(ObservationRegistry.NOOP, new RequestIdAccessor())
        );
        Path outsideFile = Files.createTempFile("scan-cleanup-", ".txt");
        Files.writeString(outsideFile, "keep");

        Method cleanup = ScanTaskConsumer.class.getDeclaredMethod("cleanupTempPath", String.class);
        cleanup.setAccessible(true);
        cleanup.invoke(consumer, outsideFile.toString());

        assertThat(Files.exists(outsideFile)).isTrue();
        Files.deleteIfExists(outsideFile);
    }
}
