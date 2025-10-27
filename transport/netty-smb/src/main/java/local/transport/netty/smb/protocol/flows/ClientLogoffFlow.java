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

import static java.util.Objects.requireNonNull;

import local.transport.netty.smb.protocol.SmbError;
import local.transport.netty.smb.protocol.SmbException;
import local.transport.netty.smb.protocol.SmbRequest;
import local.transport.netty.smb.protocol.SmbResponse;
import local.transport.netty.smb.protocol.details.ConnectionDetails;
import local.transport.netty.smb.protocol.smb2.Smb2Header;
import local.transport.netty.smb.protocol.smb2.Smb2LogoffRequest;
import local.transport.netty.smb.protocol.smb2.Smb2LogoffResponse;

public class ClientLogoffFlow extends AbstractClientFlow<Void> {

    private final ConnectionDetails connDetails;
    private final Long sessionId;

    public ClientLogoffFlow(final Long sessionId, final ConnectionDetails connDetails,
        final RequestSender requestSender) {

        super(requestSender);
        this.sessionId = requireNonNull(sessionId);
        this.connDetails = requireNonNull(connDetails);
    }

    @Override
    protected SmbRequest initialRequest() {
        return new SmbRequest(new Smb2Header(), new Smb2LogoffRequest());
    }

    @Override
    public void handleResponse(final SmbResponse response) {
        if (response.header() instanceof Smb2Header header
            && header.status() == SmbError.STATUS_SUCCESS
            && response.message() instanceof Smb2LogoffResponse) {

            connDetails.sessions().remove(sessionId);
            connDetails.preauthSessions().remove(sessionId);
            completeFuture.set(null);
            return;
        }
        throw new SmbException("Unexpected Logoff response" + response);
    }
}

