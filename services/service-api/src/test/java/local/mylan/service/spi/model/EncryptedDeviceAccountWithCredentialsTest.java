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
package local.mylan.service.spi.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import local.mylan.service.api.model.DeviceAccountLockState;
import local.mylan.service.api.model.DeviceAccountWithCredentials;
import local.mylan.service.test.TestEncryptionService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class EncryptedDeviceAccountWithCredentialsTest {

    private static final String TEST = "Test";
    private static final String PASSWORD = "Pa$$W0rd";
    private static final String KEY = "correct-key";
    private static final String WRONG_KEY = "wrong-key";

    static EncryptedDeviceAccountWithCredentials.Encryptor encryptor;
    static EncryptedDeviceAccountWithCredentials.Decryptor decryptor;

    @BeforeAll
    static void beforeAll() {
        final var encService = new TestEncryptionService();
        encryptor = encService.credentialsEncryptor();
        decryptor = encService.credentialsDecryptor();

        // validate encryption/decryption works as expected

        final String encTest = encryptor.encrypt(TEST, null);
        assertNotNull(encTest);
        assertNotEquals(TEST, encTest);
        assertEquals(TEST, decryptor.decrypt(encTest, null));

        final String encTest2 = encryptor.encrypt(TEST, KEY);
        assertNotNull(encTest2);
        assertNotEquals(TEST, encTest2);
        assertEquals(TEST, decryptor.decrypt(encTest2, KEY));
    }

    @Test
    void lockable() {
        final var acc = new EncryptedDeviceAccountWithCredentials(encryptor, decryptor, true);
        acc.setPassword(encryptor.encrypt(PASSWORD, KEY));

        assertAccount(acc, DeviceAccountLockState.LOCKED, null);
        acc.unlock(null);
        assertAccount(acc, DeviceAccountLockState.LOCKED, null);
        acc.unlock(WRONG_KEY);
        assertAccount(acc, DeviceAccountLockState.LOCKED, null);
        acc.unlock(KEY);
        assertAccount(acc, DeviceAccountLockState.UNLOCKED, PASSWORD);
        acc.lock();
        assertAccount(acc, DeviceAccountLockState.LOCKED, null);
    }

    @Test
    void notLockable() {
        final var acc = new EncryptedDeviceAccountWithCredentials(encryptor, decryptor, false);
        acc.setPassword(encryptor.encrypt(PASSWORD, null));

        // lock state remain same even if lock/unlock invocked
        assertAccount(acc, DeviceAccountLockState.HAS_NO_LOCK, PASSWORD);
        acc.unlock(KEY);
        assertAccount(acc, DeviceAccountLockState.HAS_NO_LOCK, PASSWORD);
        acc.lock();
        assertAccount(acc, DeviceAccountLockState.HAS_NO_LOCK, PASSWORD);
    }

    private static void assertAccount(final DeviceAccountWithCredentials acc,
        final DeviceAccountLockState expectedLockState, final String expectedPassword) {

        assertEquals(expectedLockState, acc.getLockState());
        if (expectedPassword == null) {
            assertNull(acc.getPassword());
        } else {
            assertEquals(PASSWORD, acc.getPassword());
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void settersOverride(final boolean lockable){
        final var acc = new EncryptedDeviceAccountWithCredentials(encryptor, decryptor, lockable);

        assertUnsupported(()-> acc.setKey(KEY));
        assertUnsupported(()-> acc.getKey());
        assertUnsupported(()-> acc.setLockState(DeviceAccountLockState.HAS_NO_LOCK));
    }

    private static void assertUnsupported(final Executable executable) {
        assertThrows(UnsupportedOperationException.class, executable);
    }

}
