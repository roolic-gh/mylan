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
package local.transport.netty.smb.protocol.flows;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import local.transport.netty.smb.protocol.ClientFlow;
import local.transport.netty.smb.protocol.ProtocolVersion;
import local.transport.netty.smb.protocol.SmbDialect;
import local.transport.netty.smb.protocol.SmbRequest;
import local.transport.netty.smb.protocol.SmbResponse;
import local.transport.netty.smb.protocol.cifs.CifsSmbHeader;
import local.transport.netty.smb.protocol.cifs.SmbComNegotiateRequest;
import local.transport.netty.smb.protocol.details.ClientDetails;
import local.transport.netty.smb.protocol.details.ConnectionDetails;
import local.transport.netty.smb.protocol.details.ServerDetails;
import local.transport.netty.smb.protocol.smb2.Smb2Header;
import local.transport.netty.smb.protocol.smb2.Smb2NegotiateRequest;
import local.transport.netty.smb.protocol.smb2.Smb2NegotiateResponse;

public class ClientNegotiationFlow implements ClientFlow<Void> {

    private final ClientDetails clientDetails;
    private final ConnectionDetails connDetails;
    private final SettableFuture<Void> completeFuture = SettableFuture.create();

    public ClientNegotiationFlow(final ClientDetails clientDetails, final ConnectionDetails connDetails) {
        this.clientDetails = clientDetails;
        this.connDetails = connDetails;
    }

    @Override
    public SmbRequest initialRequest() {
        final var minDialect = clientDetails.minDialect();
        final var maxDialect = clientDetails.maxDialect();
        if (minDialect.protocolVersion() == ProtocolVersion.CIFS_SMB) {
            // start with CIFS request
            final var req = new SmbComNegotiateRequest();
            req.setDialects(SmbDialect.smb1NegotiateDialects(maxDialect));
            return new SmbRequest(new CifsSmbHeader(), req);
        }
        // start from SMB2 request
        final var req = new Smb2NegotiateRequest();
        req.setDialects(SmbDialect.smb2NegotiateDialects(minDialect, maxDialect));
        req.setSecurityMode(connDetails.clientSecurityMode());
        req.setCapabilities(connDetails.clientCapabilities());
        req.setClientGuid(connDetails.clientGuid());
        return new SmbRequest(new Smb2Header(), req);
    }

    @Override
    public SmbRequest nextRequest() {
        return null;
    }

    @Override
    public void handleResponse(final SmbResponse response) {
        if (response.message() instanceof Smb2NegotiateResponse resp) {
            connDetails.setDialect(resp.dialectRevision());
            connDetails.setMaxTransactSize(resp.maxTransactSize());
            connDetails.setMaxReadSize(resp.maxReadSize());
            connDetails.setMaxWriteSize(resp.maxWriteSize());
            connDetails.setNegotiateToken(resp.token());

            final var serverGuid = resp.serverGuid();
            final var server = clientDetails.servers().computeIfAbsent(serverGuid, key -> new ServerDetails());
            server.setServerGuid(serverGuid);
            server.setSecurityMode(resp.securityMode());
            server.setCapabilities(resp.capabilities());
            connDetails.setServer(server);

            completeFuture.set(null);
        }
    }

    @Override
    public boolean isComplete() {
        return completeFuture.isDone();
    }

    @Override
    public ListenableFuture<Void> completeFuture() {
        return completeFuture;
    }
}
