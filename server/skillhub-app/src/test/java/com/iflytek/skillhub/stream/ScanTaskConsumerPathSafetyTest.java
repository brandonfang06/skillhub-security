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
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.redisson.api.RedissonClient;

class ScanTaskConsumerPathSafetyTest {

    @Test
    @EnabledOnOs(OS.LINUX)
    void completionDoesNotDeleteOutsideFilesWhenAParentIsReplacedAfterValidation() throws Exception {
        Path root = Path.of("/tmp/skillhub-scans").toAbsolutePath().normalize();
        Files.createDirectories(root);
        Path parent = Files.createTempDirectory(root, "scan-cleanup-race-");
        Path moved = parent.resolveSibling(parent.getFileName() + "-moved");
        Path target = Files.createDirectory(parent.resolve("task"));
        Files.writeString(target.resolve("keep.txt"), "owned");
        Path outside = Files.createTempDirectory("scan-cleanup-race-outside-");
        Path sentinel = Files.writeString(Files.createDirectory(outside.resolve("task")).resolve("keep.txt"), "outside");
        var raceInjected = new java.util.concurrent.atomic.AtomicBoolean();
        var consumer = consumer();
        try {
            // Inject the same filesystem replacement at the first post-validation I/O boundary.
            try (var fileSystem = org.mockito.Mockito.mockStatic(Files.class, invocation -> {
                boolean pathnameOperation = invocation.getMethod().getName().equals("isDirectory")
                        && target.equals(invocation.getArgument(0));
                boolean handleOperation = invocation.getMethod().getName().equals("newDirectoryStream")
                        && target.getRoot().equals(invocation.getArgument(0));
                if ((pathnameOperation || handleOperation) && raceInjected.compareAndSet(false, true)) {
                    Files.move(parent, moved);
                    Files.createSymbolicLink(parent, outside);
                }
                return invocation.callRealMethod();
            })) {
                consumer.markCompleted(new ScanTaskConsumer.ScanTaskPayload(
                        "task-cleanup-race", 42L, target.toString(), null, ScannerType.SKILL_SCANNER));
            }
            assertThat(raceInjected.get()).isTrue();
            assertThat(Files.readString(sentinel)).isEqualTo("outside");
        } finally {
            if (Files.isSymbolicLink(parent)) {
                Files.delete(parent);
            }
            for (Path owned : java.util.List.of(parent, moved)) {
                Files.deleteIfExists(owned.resolve("task/keep.txt"));
                Files.deleteIfExists(owned.resolve("task"));
                Files.deleteIfExists(owned);
            }
            Files.deleteIfExists(sentinel);
            Files.deleteIfExists(outside.resolve("task"));
            Files.deleteIfExists(outside);
        }
    }

    @Test
    void completionRemovesOnlyItsNestedTreeAndNeverFollowsDescendantLinks() throws Exception {
        Path root = Path.of("/tmp/skillhub-scans").toAbsolutePath().normalize();
        Files.createDirectories(root);
        Path owned = Files.createTempDirectory(root, "scan-completion-");
        Path outside = Files.createTempDirectory("scan-completion-outside-");
        Path sentinel = Files.writeString(outside.resolve("keep.txt"), "keep");
        Path sibling = Files.createTempFile(root, "scan-other-task-", ".zip");
        Path nested = Files.createDirectories(owned.resolve("nested/deeper"));
        Path document = Files.writeString(nested.resolve("SKILL.md"), "valid nested package");
        Path link = owned.resolve("external");
        try {
            try {
                Files.createSymbolicLink(link, outside);
            } catch (UnsupportedOperationException | java.nio.file.FileSystemException exception) {
                org.junit.jupiter.api.Assumptions.abort("Symbolic links unavailable: " + exception.getClass().getSimpleName());
            }
            consumer().markCompleted(new ScanTaskConsumer.ScanTaskPayload(
                    "task-completion", 42L, owned.toString(), null, ScannerType.SKILL_SCANNER));
            assertThat(owned).doesNotExist();
            assertThat(Files.readString(sentinel)).isEqualTo("keep");
            assertThat(sibling).exists();
        } finally {
            Files.deleteIfExists(link);
            Files.deleteIfExists(document);
            Files.deleteIfExists(nested);
            Files.deleteIfExists(owned.resolve("nested"));
            Files.deleteIfExists(owned);
            Files.deleteIfExists(sentinel);
            Files.deleteIfExists(outside);
            Files.deleteIfExists(sibling);
        }
    }

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
