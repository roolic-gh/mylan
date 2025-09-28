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
package local.transport.netty.smb.protocol.details;

import java.net.SocketAddress;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import local.transport.netty.smb.protocol.Flags;
import local.transport.netty.smb.protocol.SmbDialect;
import local.transport.netty.smb.protocol.smb2.Smb2CapabilitiesFlags;
import local.transport.netty.smb.protocol.smb2.Smb2NegotiateFlags;

/**
 * Server Details. Addresses MS-SMB2 (#3.2.1.9 Per Server)
 */
public class ServerDetails {
    private UUID serverGuid;
    private SmbDialect dialectRevision;
    private Flags<Smb2CapabilitiesFlags> capabilities;
    private Flags<Smb2NegotiateFlags> securityMode;
    private final Set<SocketAddress> addresses = ConcurrentHashMap.newKeySet();
    private String serverName;
    private String cipherId;
    // SMB 3.1.1 +
    private List<Object> rdmaTransformIds;
    private String signingAlgorithmId;

    public UUID serverGuid() {
        return serverGuid;
    }

    public void setServerGuid(final UUID serverGuid) {
        this.serverGuid = serverGuid;
    }

    public SmbDialect dialectRevision() {
        return dialectRevision;
    }

    public void setDialectRevision(final SmbDialect dialectRevision) {
        this.dialectRevision = dialectRevision;
    }

    public Flags<Smb2CapabilitiesFlags> capabilities() {
        return capabilities;
    }

    public void setCapabilities(final Flags<Smb2CapabilitiesFlags> capabilities) {
        this.capabilities = capabilities;
    }

    public Flags<Smb2NegotiateFlags> securityMode() {
        return securityMode;
    }

    public void setSecurityMode(final Flags<Smb2NegotiateFlags> securityMode) {
        this.securityMode = securityMode;
    }

    public Set<SocketAddress> addresses() {
        return addresses;
    }


    public String serverName() {
        return serverName;
    }

    public void setServerName(final String serverName) {
        this.serverName = serverName;
    }

    public String cipherId() {
        return cipherId;
    }

    public void setCipherId(final String cipherId) {
        this.cipherId = cipherId;
    }

    public List<Object> rdmaTransformIds() {
        return rdmaTransformIds;
    }

    public void setRdmaTransformIds(final List<Object> rdmaTransformIds) {
        this.rdmaTransformIds = rdmaTransformIds;
    }

    public String signingAlgorithmId() {
        return signingAlgorithmId;
    }

    public void setSigningAlgorithmId(final String signingAlgorithmId) {
        this.signingAlgorithmId = signingAlgorithmId;
    }
}
