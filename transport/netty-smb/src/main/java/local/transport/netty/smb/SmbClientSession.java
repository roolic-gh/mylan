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
package local.transport.netty.smb;

import static java.util.Objects.requireNonNull;

import com.google.common.util.concurrent.ListenableFuture;
import java.util.function.Consumer;
import local.transport.netty.smb.protocol.SmbRequest;
import local.transport.netty.smb.protocol.SmbResponse;
import local.transport.netty.smb.protocol.details.Session;
import local.transport.netty.smb.protocol.details.SessionDetails;
import local.transport.netty.smb.protocol.flows.AuthMechanism;
import local.transport.netty.smb.protocol.flows.ClientSessionSetupFlow;
import local.transport.netty.smb.protocol.flows.RequestSender;
import local.transport.netty.smb.protocol.smb2.Smb2Header;

public class SmbClientSession implements Session, RequestSender {

    private final SessionDetails sessDetails;
    private final RequestSender requestSender;

    SmbClientSession(final SessionDetails sessDetails, final RequestSender requestSender) {
        this.sessDetails = requireNonNull(sessDetails);
        this.requestSender = requireNonNull(requestSender);
        requireNonNull(sessDetails.connection(), "missing connection in session details");
    }

    @Override
    public SessionDetails details() {
        return sessDetails;
    }

    public ListenableFuture<Session> setup(final AuthMechanism authMech) {
        final var setupFlow = new ClientSessionSetupFlow(this, this, authMech);
        setupFlow.start();
        return setupFlow.completeFuture();
    }

    @Override
    public void send(final SmbRequest request, final Consumer<SmbResponse> callback) {
        if (request.header() instanceof Smb2Header head) {
            final var sessionId = sessDetails.sessionId();
            head.setSessionId(sessionId == null ? 0 : sessionId);
        }
        requestSender.send(request, callback);
    }
}
