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
 * SMB2 Negotiate Context Type. Addresses MS-SMB2 (#2.2.3.1 SMB2 NEGOTIATE_CONTEXT Request Values)
 * and (#2.2.4.1 SMB2 NEGOTIATE_CONTEXT Response Values).
 */
public enum Smb2NegotiateContextType {
    SMB2_PREAUTH_INTEGRITY_CAPABILITIES(0x0001),
    SMB2_ENCRYPTION_CAPABILITIES(0x0002),
    SMB2_COMPRESSION_CAPABILITIES(0x0003),
    SMB2_NETNAME_NEGOTIATE_CONTEXT_ID(0x0005),
    SMB2_TRANSPORT_CAPABILITIES(0x0006),
    SMB2_RDMA_TRANSFORM_CAPABILITIES(0x0007),
    SMB2_SIGNING_CAPABILITIES(0x0008),
    SMB2_CONTEXTTYPE_RESERVED(0x0100);

    private final int code;

    Smb2NegotiateContextType(final int code) {
        this.code = code;
    }

    int code() {
        return code;
    }

    public static Smb2NegotiateContextType fromCode(int code) {
        for (var type : values()) {
            if (type.code == code) {
                return type;
            }
        }
        return null;
    }
}
