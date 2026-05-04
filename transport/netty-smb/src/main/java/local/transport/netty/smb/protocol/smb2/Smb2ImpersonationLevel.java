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
 * Impersonation Level. Addresses MS-SMB2 (#2.2.13 SMB2 CREATE Request).
 */
public enum Smb2ImpersonationLevel {

    Anonymous(0x00000000),
    Identification(0x00000001),
    Impersonation(0x00000002),
    Delegate(0x00000003);

    private final int code;

    Smb2ImpersonationLevel(final int code) {
        this.code = code;
    }

    public int code() {
        return code;
    }

    public static Smb2ImpersonationLevel fromCode(final int code) {
        for (var opll : values()) {
            if (opll.code == code) {
                return opll;
            }
        }
        throw new IllegalArgumentException("Unknown ImpersonationLevel code " + code);
    }
}
