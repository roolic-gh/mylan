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
package local.transport.netty.smb.protocol;

public enum ProtocolVersion {
    CIFS_SMB(0x424D53FF),
    SMB2(0x424D53FE);

    private final int code;

    ProtocolVersion(int code) {
        this.code = code;
    }

    public int code() {
        return code;
    }

    public static ProtocolVersion fromCode(int code) {
        for (ProtocolVersion pv : values()) {
            if (pv.code == code) {
                return pv;
            }
        }
        throw new IllegalArgumentException("Unknown protocol " + Integer.toHexString(code));
    }
}
