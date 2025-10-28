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

import local.transport.netty.smb.protocol.Flags;
import local.transport.netty.smb.protocol.Smb2Request;
import local.transport.netty.smb.protocol.Smb2Response;
import local.transport.netty.smb.protocol.SmbError;
import local.transport.netty.smb.protocol.SmbException;
import local.transport.netty.smb.protocol.details.ConnectionDetails;
import local.transport.netty.smb.protocol.details.Session;
import local.transport.netty.smb.protocol.details.SessionDetails;
import local.transport.netty.smb.protocol.smb2.Smb2SessionRequestFlags;
import local.transport.netty.smb.protocol.smb2.Smb2SessionSetupRequest;
import local.transport.netty.smb.protocol.smb2.Smb2SessionSetupResponse;
import local.transport.netty.smb.protocol.spnego.NegToken;
import local.transport.netty.smb.protocol.spnego.NegTokenInit;

public class ClientSessionSetupFlow extends AbstractClientFlow<Session> {
    private final Session session;
    private final SessionDetails sessDetails;
    private final ConnectionDetails connDetails;
    private final AuthMechanism authMech;

    public ClientSessionSetupFlow(final Session session, final RequestSender requestSender,
        final AuthMechanism authMech) {

        super(requestSender);
        this.session = requireNonNull(session);
        sessDetails = requireNonNull(session.details());
        requireNonNull(sessDetails.connection(), "connection is required");
        connDetails = requireNonNull(sessDetails.connection().details(), "connection details is missing");
        requireNonNull(connDetails.negotiateToken(), "negotiation token is missing");
        if (connDetails.negotiateToken() instanceof NegTokenInit negTokenInit
            && negTokenInit.mechTypes().contains(requireNonNull(authMech).mechType())) {
            this.authMech = authMech;
        } else {
            throw new SmbException("%s Authentication is not supported by server".formatted(authMech));
        }
    }

    @Override
    protected Smb2Request initialRequest() {
        return sessionSetupRequest(authMech.init());
    }

    @Override
    public void handleResponse(final Smb2Response response) {

        try {
            if (response instanceof Smb2SessionSetupResponse sessionSetupResponse) {
                process(sessionSetupResponse);
            } else {
                throw new SmbException("Unexpected Session Setup response: " + response);
            }
        } catch (Exception e) {
            completeFuture.setException(e);
        }

    }

    private void process(final Smb2SessionSetupResponse response) {

        if (response.header().status() == SmbError.STATUS_SUCCESS) {
            if (!authMech.verify(response.token())) {
                throw new SmbException("Session setup completed but token verification failed.");
            }
            connDetails.preauthSessions().remove(sessDetails.sessionId());
            connDetails.sessions().put(sessDetails.sessionId(), session);
            completeFuture.set(session);
            return;
        }
        if (response.header().status() == SmbError.STATUS_MORE_PROCESSING_REQUIRED) {
            sessDetails.setSessionId(response.header().sessionId());
            connDetails.preauthSessions().put(sessDetails.sessionId(), session);
            final var token = authMech.next(response.token());
            requestSender.send(sessionSetupRequest(token), this::handleResponse);
            return;
        }
        throw new SmbException("Error unexpected message status: " + response.header().status());
    }

    private Smb2Request sessionSetupRequest(final NegToken token) {
        final var request = new Smb2SessionSetupRequest();
        request.setCapabilities(connDetails.clientCapabilities());
        request.setSecurityMode(connDetails.clientSecurityMode());
        request.setSessionFlags(new Flags<Smb2SessionRequestFlags>());
//        if (sessDetails.sessionId() != null) {
//            request.setPreviousSessionId(sessDetails.sessionId());
//            request.sessionFlags().set(Smb2SessionRequestFlags.SMB2_SESSION_FLAG_BINDING, true);
//        }
        request.setToken(token);
        return request;
    }
}
