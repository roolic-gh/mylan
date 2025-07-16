/*
 * Copyright 2025 Ruslan Kashapov
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.nio.file.Path;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.io.TempDir;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class DefaultEncryptionServiceTest {
    
    private static final String TEXT = "text-data-to-encrypt-or-hash";
    private static final String PASSWORD = "Pa$$W0rd";

    private static String encryptedString1;
    private static String encryptedString2;
    private static String hashedString;

    @TempDir
    static Path tmpDir;

    @Test
    @Order(1)
    void initial() {
        final var encryptionService = new DefaultEncryptionService(tmpDir, tmpDir);
        final var encrypted1 = encryptionService.encrypt(TEXT);
        assertNotNull(encrypted1);
        assertEquals(TEXT, encryptionService.decrypt(encrypted1));
        final var hashed = encryptionService.buildHash(TEXT);
        assertNotNull(hashed);
        encryptedString1 = encrypted1;
        hashedString = hashed;
    }

    @Test
    @Order(2)
    void subsequent() {
        final var encryptionService = new DefaultEncryptionService(tmpDir, tmpDir);
        assertEquals(encryptedString1, encryptionService.encrypt(TEXT));
        assertEquals(TEXT, encryptionService.decrypt(encryptedString1));
        assertEquals(hashedString, encryptionService.buildHash(TEXT));
    }
}
