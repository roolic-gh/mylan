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

import javax.annotation.Nonnull;
import local.transport.netty.smb.protocol.Flags;
import local.transport.netty.smb.protocol.SmbError;
import local.transport.netty.smb.protocol.SmbException;
import local.transport.netty.smb.protocol.SmbRequest;
import local.transport.netty.smb.protocol.SmbResponse;
import local.transport.netty.smb.protocol.details.ConnectionDetails;
import local.transport.netty.smb.protocol.details.Session;
import local.transport.netty.smb.protocol.details.SessionDetails;
import local.transport.netty.smb.protocol.smb2.Smb2Header;
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
    protected SmbRequest initialRequest() {
        return sessionSetupRequest(authMech.init());
    }

    @Override
    public void handleResponse(@Nonnull final SmbResponse response) {
        if (response.header() instanceof Smb2Header head
            && response.message() instanceof Smb2SessionSetupResponse resp) {
            try {
                process(head, resp);
            } catch (Exception e) {
                completeFuture.setException(e);
            }
        }
    }

    protected void process(final Smb2Header header, final Smb2SessionSetupResponse message) {

        if (header.status() == SmbError.STATUS_SUCCESS && authMech.verify(message.token())) {
            connDetails.preauthSessions().remove(sessDetails.sessionId());
            connDetails.sessions().put(sessDetails.sessionId(), session);
            completeFuture.set(session);
            return;
        }
        if (header.status() == SmbError.STATUS_MORE_PROCESSING_REQUIRED) {
            sessDetails.setSessionId(header.sessionId());
            connDetails.preauthSessions().put(sessDetails.sessionId(), session);
            final var token = authMech.next(message.token());
            requestSender.send(sessionSetupRequest(token), this::handleResponse);
            return;
        }
        throw new SmbException("Error unexpected message status: " + header.status());
    }

    private SmbRequest sessionSetupRequest(NegToken token) {
        final var msg = new Smb2SessionSetupRequest();
        msg.setCapabilities(connDetails.clientCapabilities());
        msg.setSecurityMode(connDetails.clientSecurityMode());
//        if (token instanceof NegTokenResp ntr && ntr.mechListMIC() != null) {
//            msg.securityMode().set(Smb2NegotiateFlags.SMB2_NEGOTIATE_SIGNING_REQUIRED, true);
//        }
        msg.setSessionFlags(new Flags<Smb2SessionRequestFlags>());
//        if (sessDetails.sessionId() != null) {
//            msg.setPreviousSessionId(sessDetails.sessionId());
//            msg.sessionFlags().set(Smb2SessionRequestFlags.SMB2_SESSION_FLAG_BINDING, true);
//        }
        msg.setToken(token);
        return new SmbRequest(new Smb2Header(), msg);
    }
}
