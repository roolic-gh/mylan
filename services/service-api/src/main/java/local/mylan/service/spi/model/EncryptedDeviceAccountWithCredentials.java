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

import java.security.SecureRandom;
import local.mylan.service.api.model.DeviceAccount;
import local.mylan.service.api.model.DeviceAccountLockState;
import local.mylan.service.api.model.DeviceAccountWithCredentials;

public class EncryptedDeviceAccountWithCredentials extends DeviceAccount implements DeviceAccountWithCredentials {
    private static final SecureRandom RANDOM = new SecureRandom();

    private final Encryptor encryptor;
    private final Decryptor decryptor;
    private final boolean lockable;

    private String keyKey;

    public EncryptedDeviceAccountWithCredentials(final Encryptor encryptor, final Decryptor decryptor,
        final boolean lockable) {

        this.encryptor = encryptor;
        this.decryptor = decryptor;
        this.lockable = lockable;
        super.setLockState(lockable ? DeviceAccountLockState.LOCKED : DeviceAccountLockState.HAS_NO_LOCK);
    }

    @Override
    public String getPassword() {
        if (lockable) {
            final var encKey = super.getKey();
            final var key = encKey == null || keyKey == null ? null : decryptor.decrypt(encKey, keyKey);
            return key == null ? null : decryptor.decrypt(super.getPassword(), key);
        }
        return decryptor.decrypt(super.getPassword(), null);
    }

    @Override
    public String getKey() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setKey(final String key) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setLockState(final DeviceAccountLockState lockState) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void unlock(final String key) {
        if (lockable) {
            try {
                // validate no exception will occur on getPassword()
                decryptor.decrypt(super.getPassword(), key);
                keyKey = Long.toHexString(RANDOM.nextLong());
                super.setKey(encryptor.encrypt(key, keyKey));
                super.setLockState(DeviceAccountLockState.UNLOCKED);
            } catch (Exception e) {
                // ignore
            }
        }
    }

    @Override
    public void lock() {
        if (lockable) {
            keyKey = null;
            super.setKey(null);
            super.setLockState(DeviceAccountLockState.LOCKED);
        }
    }

    @FunctionalInterface
    public interface Encryptor {
        String encrypt(String decrypted, String password);
    }

    @FunctionalInterface
    public interface Decryptor {
        String decrypt(String encrypted, String password);
    }

}
