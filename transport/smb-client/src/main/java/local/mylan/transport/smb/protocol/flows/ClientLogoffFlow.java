/*
 * Copyright 2026 Ruslan Kashapov
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
package local.mylan.transport.smb.protocol.flows;

import static java.util.Objects.requireNonNull;

import local.mylan.transport.smb.exceptions.SmbException;
import local.mylan.transport.smb.protocol.Smb2Request;
import local.mylan.transport.smb.protocol.Smb2Response;
import local.mylan.transport.smb.protocol.SmbError;
import local.mylan.transport.smb.protocol.details.ConnectionDetails;
import local.mylan.transport.smb.protocol.smb2.Smb2LogoffRequest;
import local.mylan.transport.smb.protocol.smb2.Smb2LogoffResponse;

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
    protected Smb2Request initialRequest() {
        return new Smb2LogoffRequest();
    }

    @Override
    public void handleResponse(final Smb2Response response) {
        try {
            if (response instanceof Smb2LogoffResponse
                && response.header().status() == SmbError.STATUS_SUCCESS) {

                connDetails.sessions().remove(sessionId);
                connDetails.preauthSessions().remove(sessionId);
                completeFuture.set(null);
                return;

            }
            throw new SmbException("Unexpected Logoff response" + response);
        } catch (SmbException e) {
            completeFuture.setException(e);
        }
    }
}

