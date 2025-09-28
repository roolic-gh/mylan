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

import javax.annotation.Nonnull;
import local.transport.netty.smb.protocol.ProtocolVersion;
import local.transport.netty.smb.protocol.SmbDialect;
import local.transport.netty.smb.protocol.SmbError;
import local.transport.netty.smb.protocol.SmbException;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClientNegotiationFlow extends AbstractClientFlow<Void> {

    private static final Logger LOG = LoggerFactory.getLogger(ClientNegotiationFlow.class);
    private static final int SIZE_THRESHOLD = 65535;

    private final ClientDetails clientDetails;
    private final ConnectionDetails connDetails;

    public ClientNegotiationFlow(final ClientDetails clientDetails, final ConnectionDetails connDetails,
        final RequestSender requestSender) {

        super(requestSender);
        this.clientDetails = clientDetails;
        this.connDetails = connDetails;
    }

    @Override
    protected SmbRequest initialRequest() {
        final var minDialect = clientDetails.minDialect();
        final var maxDialect = clientDetails.maxDialect();
        if (minDialect.protocolVersion() == ProtocolVersion.CIFS_SMB) {
            // start with CIFS request
            final var req = new SmbComNegotiateRequest();
            req.setDialects(SmbDialect.smb1NegotiateDialects(maxDialect));
            return new SmbRequest(new CifsSmbHeader(), req);
        }
        // start from SMB2 request
        final var dialects = SmbDialect.smb2NegotiateDialects(minDialect, maxDialect);
        connDetails.setOfferedDialects(dialects);
        final var req = new Smb2NegotiateRequest();
        req.setDialects(dialects);
        req.setSecurityMode(connDetails.clientSecurityMode());
        req.setCapabilities(connDetails.clientCapabilities());
        req.setClientGuid(connDetails.clientGuid());
        return new SmbRequest(new Smb2Header(), req);
    }

    @Override
    public void handleResponse(@Nonnull final SmbResponse response) {
        if (response.header() instanceof Smb2Header head && response.message() instanceof Smb2NegotiateResponse resp){
            try {
                process(head, resp);
            } catch(Exception e){
                completeFuture.setException(e);
            }
        }
    }

    /*
     * Addresses MS-SMB2 3.2.5.2 Receiving an SMB2 NEGOTIATE Response
     * */
    void process(final Smb2Header head, final Smb2NegotiateResponse resp) {
        final var status = head.status();
        if (status != SmbError.STATUS_SUCCESS) {
            throw new SmbException("Negotiate response with error status " + status);
        }

        final var dialect = resp.dialectRevision();
        if (dialect == SmbDialect.SMB2_Wildcard) {
            // the respons was to CIFS negotiate request
            // negotiate again starting with 2.1 dialect
            final var dialects = SmbDialect.smb2NegotiateDialects(SmbDialect.SMB2_1, clientDetails.maxDialect());
            connDetails.setOfferedDialects(dialects);
            final var req = new Smb2NegotiateRequest();
            req.setDialects(dialects);
            req.setSecurityMode(connDetails.clientSecurityMode());
            req.setCapabilities(connDetails.clientCapabilities());
            req.setClientGuid(connDetails.clientGuid());
            sendRequest(new SmbRequest(new Smb2Header(), req));
            return;
        }

        final var maxTransactSize = resp.maxTransactSize();
        final var maxReadSize = resp.maxReadSize();
        final var maxWriteSize = resp.maxWriteSize();
        if (maxTransactSize < SIZE_THRESHOLD || maxReadSize < SIZE_THRESHOLD || maxWriteSize < SIZE_THRESHOLD) {
            // terminate connection any of sizes is too small
            throw new SmbException("Negotiated size limits are too small: " +
                "maxTransactSize=%d, maxReadSize=%d, maxWriteSize=%d, expectedThreshold=%d"
                    .formatted(maxTransactSize, maxReadSize, maxWriteSize, SIZE_THRESHOLD));
        }

        connDetails.setMaxTransactSize(maxTransactSize);
        connDetails.setMaxReadSize(maxReadSize);
        connDetails.setMaxWriteSize(maxWriteSize);
        connDetails.setNegotiateToken(resp.token());

        final var serverGuid = resp.serverGuid();
        final var server = clientDetails.servers().computeIfAbsent(serverGuid, key -> new ServerDetails());
        server.setDialectRevision(resp.dialectRevision());
        server.setServerGuid(serverGuid);
        server.setSecurityMode(resp.securityMode());
        server.setCapabilities(resp.capabilities());
        connDetails.setServer(server);

        completeFuture.set(null);
        LOG.debug("SMB Negotiation with server {} completed; {} dialect selected.",
            serverGuid, resp.dialectRevision().identifier());
    }
}
