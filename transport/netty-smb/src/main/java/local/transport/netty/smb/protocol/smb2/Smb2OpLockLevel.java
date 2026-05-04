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
package local.transport.netty.smb.protocol.smb2;

/**
 * Opportunistic Lock (oplock) Level. Addresses MS-SMB2 (#2.2.13 SMB2 CREATE Request).
 */
public enum Smb2OpLockLevel {

    SMB2_OPLOCK_LEVEL_NONE((byte) 0x00),
    SMB2_OPLOCK_LEVEL_II((byte) 0x01),
    SMB2_OPLOCK_LEVEL_EXCLUSIVE((byte) 0x08),
    SMB2_OPLOCK_LEVEL_BATCH((byte) 0x09),
    SMB2_OPLOCK_LEVEL_LEASE((byte) 0xFF);

    private final byte code;

    Smb2OpLockLevel(final byte code) {
        this.code = code;
    }

    public byte code() {
        return code;
    }

    public static Smb2OpLockLevel fromCode(final byte code) {
        for (var opll : values()) {
            if (opll.code == code) {
                return opll;
            }
        }
        throw new IllegalArgumentException("Unknown OpLockLevel code " + code);
    }
}
