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

public class ClientNegotiationFlow implements ClientFlow<Void> {

    private final SmbDialect minDialect;
    private final SmbDialect maxDialect;
    private final ConnectionDetails details;
    private final SettableFuture<Void> completeFuture = SettableFuture.create();

    public ClientNegotiationFlow(final ClientDetails clientDetails, final ConnectionDetails connDetails) {
        minDialect = clientDetails.minDialect();
        maxDialect = clientDetails.maxDialect();
        details = connDetails;
    }

    @Override
    public SmbRequest initialRequest() {
        if (minDialect.protocolVersion() == ProtocolVersion.CIFS_SMB) {
            // start with CIFS request
            final var req = new SmbComNegotiateRequest();
            req.setDialects(SmbDialect.smb1NegotiateDialects(maxDialect));
            return new SmbRequest(new CifsSmbHeader(), req);
        }
        // start from SMB2 request
        return null;
    }

    @Override
    public SmbRequest nextRequest() {
        return null;
    }

    @Override
    public void handleResponse(final SmbResponse response) {

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
