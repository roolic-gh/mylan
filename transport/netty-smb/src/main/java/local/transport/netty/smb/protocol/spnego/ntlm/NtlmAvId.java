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
 * AV Pair identifier. Addresses MS-NLMP (#2.2.2.1 AV_PAIR).
 */
public enum NtlmAvId {
    MsvAvEOL(0x0000),
    MsvAvNbComputerName(0x0001),
    MsvAvNbDomainName(0x0002),
    MsvAvDnsComputerName(0x0003),
    MsvAvDnsDomainName(0x0004),
    MsvAvDnsTreeName(0x0005),
    MsvAvFlags(0x0006),
    MsvAvTimestamp(0x0007),
    MsvAvSingleHost(0x0008),
    MsvAvTargetName(0x0009),
    MsvAvChannelBindings(0x000A);

    private final int code;

    NtlmAvId(final int code) {
        this.code = code;
    }
    public int code() {
        return code;
    }

    public static NtlmAvId fromCode(final int code) {
        for (var id : values()) {
            if (id.code == code) {
                return id;
            }
        }
        return null;
    }
}
