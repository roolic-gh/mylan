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

import com.google.common.base.Objects;
import java.util.List;
import java.util.UUID;
import local.transport.netty.smb.protocol.Flags;
import local.transport.netty.smb.protocol.SmbCommand;
import local.transport.netty.smb.protocol.SmbDialect;
import local.transport.netty.smb.protocol.SmbRequestMessage;

/**
 * SMB2 Negotiate request. Addresses MS-SMB2 (#2.2.3 SMB2 NEGOTIATE Request).
 */
public class Smb2NegotiateRequest implements SmbRequestMessage {

    private List<SmbDialect> dialects;
    private Flags<Smb2NegotiateFlags> securityMode;
    private Flags<Smb2CapabilitiesFlags> capabilities;
    private UUID clientGuid;
    private List<Smb2NegotiateContext> negotiateContexts;

    @Override
    public SmbCommand command() {
        return SmbCommand.SMB2_NEGOTIATE;
    }

    public List<SmbDialect> dialects() {
        return dialects;
    }

    public void setDialects(final List<SmbDialect> dialects) {
        this.dialects = dialects;
    }

    public Flags<Smb2NegotiateFlags> securityMode() {
        return securityMode;
    }

    public void setSecurityMode(
        final Flags<Smb2NegotiateFlags> securityMode) {
        this.securityMode = securityMode;
    }

    public Flags<Smb2CapabilitiesFlags> capabilities() {
        return capabilities;
    }

    public void setCapabilities(final Flags<Smb2CapabilitiesFlags> capabilities) {
        this.capabilities = capabilities;
    }

    public UUID clientGuid() {
        return clientGuid;
    }

    public void setClientGuid(final UUID clientGuid) {
        this.clientGuid = clientGuid;
    }

    public List<Smb2NegotiateContext> negotiateContexts() {
        return negotiateContexts;
    }

    public void setNegotiateContexts(final List<Smb2NegotiateContext> negotiateContexts) {
        this.negotiateContexts = negotiateContexts;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Smb2NegotiateRequest that)) {
            return false;
        }
        return Objects.equal(dialects,that.dialects) && Objects.equal(securityMode, that.securityMode)
            && Objects.equal(capabilities, that.capabilities) && Objects.equal(clientGuid, that.clientGuid)
            && Objects.equal(negotiateContexts, that.negotiateContexts);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(dialects, securityMode, capabilities, clientGuid, negotiateContexts);
    }
}
