/*
 * Copyright 2026 Ruslan Kashapov
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package local.mylan.service.spi;

import static local.mylan.service.test.NavResourceTestUtils.assertList;
import static local.mylan.service.test.NavResourceTestUtils.assertNavDirectory;
import static local.mylan.service.test.NavResourceTestUtils.share;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import local.mylan.common.utils.ConfUtils;
import local.mylan.service.api.NavResourceService;
import local.mylan.service.api.model.NavDirectory;
import local.mylan.service.api.model.NavFile;
import local.mylan.service.api.model.NavResourceShare;
import local.mylan.service.api.model.ShareType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LocalFsAccessorTest {
    private static final String TEST_DIR_NAME = "test-dir";
    private static final String TEST_FILE_NAME = "test-file.txt";

    @TempDir
    static Path confDir;
    static Path sharesJsonFile;

    @Mock
    NavResourceService navResourceService;

    @Captor
    ArgumentCaptor<List<NavResourceShare>> shareListCaptor;

    @BeforeAll
    static void beforeAll() {
        sharesJsonFile = confDir.resolve(
            ConfUtils.loadConfiguration(LocalFsAccessorConf.class).localSharesJsonFilename());
    }

    @AfterEach
    @SuppressWarnings("ResultOfMethodCallIgnored")
    void afterEach() throws Exception {
        final var dir = confDir.toFile();
        for (var file : dir.listFiles()) {
            System.out.println("DELETE: " + file.getName());
            file.delete();
        }
    }

    @Test
    void nolocalShares() {
        new LocalFsAccessor(confDir, navResourceService);
        verify(navResourceService, never()).syncLocalSharedResources(any());
    }

    @Test
    void synclocalShares() throws Exception {
        final var dirPath = confDir.toString();
        final var filePath = sharesJsonFile.toString();
        final var json = """
            [
                {"name": "Local Dir", "path": "%s", "shareType": "REGISTERED"},
                {"name": "Local File", "path": "%s", "shareType": "ALL"},
                {"name": "Invalid", "path": "%s/invalid", "shareType": "ALL"}
            ]
            """.formatted(dirPath, filePath, dirPath);
        Files.writeString(sharesJsonFile, json);
        new LocalFsAccessor(confDir, navResourceService);
        verify(navResourceService, times(1))
            .syncLocalSharedResources(shareListCaptor.capture());

        assertList(
            List.of(
                share(null, null, "Local Dir", dirPath, false, ShareType.REGISTERED),
                share(null, null, "Local File", filePath, true, ShareType.ALL)),
            shareListCaptor.getValue(),
            NavResourceShare::getPath,
            (expected, actual) -> {
                assertNotNull(actual);
                assertNull(actual.getShareId());
                assertNull(actual.getAccountId());
                assertEquals(expected.getName(), actual.getName());
                assertEquals(expected.getPath(), actual.getPath());
                assertEquals(expected.isFile(), actual.isFile());
                assertEquals(expected.getShareType(), actual.getShareType());
            }
        );
    }

    @Test
    void listDirectory() throws Exception {
        final var testDir = confDir.resolve(TEST_DIR_NAME);
        Files.createDirectory(testDir);
        Files.createDirectory(confDir.resolve(".hidden-dir"));

        final var testFile = confDir.resolve(TEST_FILE_NAME);
        Files.writeString(testFile, "1", StandardCharsets.US_ASCII);
        Files.writeString(confDir.resolve(".hidden-file.txt"), "2");

        final var accessor = new LocalFsAccessor(confDir, navResourceService);

        final var expected = new NavDirectory(
            List.of(new NavDirectory(TEST_DIR_NAME)),
            List.of(new NavFile(TEST_FILE_NAME, 1, testFile.toFile().lastModified()))
        );
        final var result = accessor.listDirectory(null, null, confDir.toString());
        assertNavDirectory(expected, result);
    }
}
