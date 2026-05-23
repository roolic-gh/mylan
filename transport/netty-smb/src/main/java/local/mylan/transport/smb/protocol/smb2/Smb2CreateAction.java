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
package local.mylan.transport.smb.protocol.smb2;

/**
 * Create Action. Addresses MS-SMB2 (#2.2.14 SMB2 CREATE Response).
 */
public enum Smb2CreateAction {

    FILE_SUPERSEDED(0x00000000),
    FILE_OPENED(0x00000001),
    FILE_CREATED(0x00000002),
    FILE_OVERWRITTEN(0x00000003);

    private final int code;

    Smb2CreateAction(final int code) {
        this.code = code;
    }

    public int code() {
        return code;
    }

    public static Smb2CreateAction fromCode(final int code) {
        for (var opll : values()) {
            if (opll.code == code) {
                return opll;
            }
        }
        throw new IllegalArgumentException("Unknown CreateAction code " + code);
    }
}
