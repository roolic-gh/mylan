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
import local.transport.netty.smb.protocol.Smb2Dialect;
import local.transport.netty.smb.protocol.Smb2Request;
import local.transport.netty.smb.protocol.Smb2Response;
import local.transport.netty.smb.protocol.SmbError;
import local.transport.netty.smb.protocol.SmbException;
import local.transport.netty.smb.protocol.details.ClientDetails;
import local.transport.netty.smb.protocol.details.ConnectionDetails;
import local.transport.netty.smb.protocol.details.ServerDetails;
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
    protected Smb2Request initialRequest() {
        final var dialects = Smb2Dialect.negotiateDialects(clientDetails.minDialect(), clientDetails.maxDialect());
        connDetails.setOfferedDialects(dialects);
        final var request = new Smb2NegotiateRequest();
        request.setDialects(dialects);
        request.setSecurityMode(connDetails.clientSecurityMode());
        request.setCapabilities(connDetails.clientCapabilities());
        request.setClientGuid(connDetails.clientGuid());
        return request;
    }

    @Override
    public void handleResponse(@Nonnull final Smb2Response response) {
        try {
            if (response instanceof Smb2NegotiateResponse negotiateResponse) {
                process(negotiateResponse);
            } else {
                throw new SmbException("Unexpected negotiation response: " + response);
            }
        } catch (Exception e) {
            completeFuture.setException(e);
        }
    }

    /*
     * Addresses MS-SMB2 3.2.5.2 Receiving an SMB2 NEGOTIATE Response
     * */
    void process(final Smb2NegotiateResponse response) {
        final var status = response.header().status();
        if (status != SmbError.STATUS_SUCCESS) {
            throw new SmbException("Negotiate response with error status " + status);
        }

        final var maxTransactSize = response.maxTransactSize();
        final var maxReadSize = response.maxReadSize();
        final var maxWriteSize = response.maxWriteSize();
        if (maxTransactSize < SIZE_THRESHOLD || maxReadSize < SIZE_THRESHOLD || maxWriteSize < SIZE_THRESHOLD) {
            // terminate connection any of sizes is too small
            throw new SmbException("Negotiated size limits are too small: " +
                "maxTransactSize=%d, maxReadSize=%d, maxWriteSize=%d, expectedThreshold=%d"
                    .formatted(maxTransactSize, maxReadSize, maxWriteSize, SIZE_THRESHOLD));
        }

        connDetails.setMaxTransactSize(maxTransactSize);
        connDetails.setMaxReadSize(maxReadSize);
        connDetails.setMaxWriteSize(maxWriteSize);
        connDetails.setNegotiateToken(response.token());

        final var serverGuid = response.serverGuid();
        final var server = clientDetails.servers().computeIfAbsent(serverGuid, key -> new ServerDetails());
        server.setDialectRevision(response.dialectRevision());
        server.setServerGuid(serverGuid);
        server.setSecurityMode(response.securityMode());
        server.setCapabilities(response.capabilities());
        connDetails.setServer(server);

        completeFuture.set(null);
        LOG.debug("SMB Negotiation with server {} completed; {} dialect selected.",
            serverGuid, response.dialectRevision().identifier());
    }
}
