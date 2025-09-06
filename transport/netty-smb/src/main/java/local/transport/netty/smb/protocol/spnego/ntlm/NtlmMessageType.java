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
package local.transport.netty.smb.protocol.spnego.ntlm;

/**
 * NTLM Message type. Addresses MS-NLMP (#2.2 Message Syntax).
 */
public enum NtlmMessageType {
    NtLmNegotiate(0x00000001),
    NtLmChallenge(0x00000002),
    NtLmAuthenticate(0x00000003),
    Unidentified(0x00);

    private final int code;

    NtlmMessageType(final int code) {
        this.code = code;
    }

    public int code() {
        return code;
    }

    public static NtlmMessageType fromCode(final int code) {
        for (var mt : values()) {
            if (mt.code == code) {
                return mt;
            }
        }
        return Unidentified;
    }
}
