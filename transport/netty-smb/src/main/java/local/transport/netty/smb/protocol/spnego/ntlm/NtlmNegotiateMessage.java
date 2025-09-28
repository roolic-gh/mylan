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

import local.transport.netty.smb.protocol.Flags;

/**
 * NTLM Negotiate Message. Addresses MS-NLMP (#2.2.1.1 NEGOTIATE_MESSAGE).
 */
public class NtlmNegotiateMessage implements NtlmMessage {

    private Flags<NtlmNegotiateFlags> negotiateFlags;
    private String domainName;
    private String workstationName;
    private NtlmVersion version;

    @Override
    public NtlmMessageType messageType() {
        return NtlmMessageType.NtLmNegotiate;
    }

    public Flags<NtlmNegotiateFlags> negotiateFlags() {
        return negotiateFlags;
    }

    public void setNegotiateFlags(
        final Flags<NtlmNegotiateFlags> negotiateFlags) {
        this.negotiateFlags = negotiateFlags;
    }

    public String domainName() {
        return domainName;
    }

    public void setDomainName(final String domainName) {
        this.domainName = domainName;
    }

    public String workstationName() {
        return workstationName;
    }

    public void setWorkstationName(final String workstationName) {
        this.workstationName = workstationName;
    }

    public NtlmVersion version() {
        return version;
    }

    public void setVersion(final NtlmVersion version) {
        this.version = version;
    }
}
