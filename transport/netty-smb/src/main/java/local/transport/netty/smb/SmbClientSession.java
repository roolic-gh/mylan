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

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.SettableFuture;
import java.util.List;
import java.util.function.Consumer;
import local.transport.netty.smb.protocol.Smb2Request;
import local.transport.netty.smb.protocol.Smb2Response;
import local.transport.netty.smb.protocol.details.Session;
import local.transport.netty.smb.protocol.details.SessionDetails;
import local.transport.netty.smb.protocol.details.TreeConnect;
import local.transport.netty.smb.protocol.flows.AuthMechanism;
import local.transport.netty.smb.protocol.flows.ClientEnumerateSharesFlow;
import local.transport.netty.smb.protocol.flows.ClientLogoffFlow;
import local.transport.netty.smb.protocol.flows.ClientSessionSetupFlow;
import local.transport.netty.smb.protocol.flows.RequestSender;
import local.transport.netty.smb.protocol.srvs.SrvsShareInfo;
import local.transport.netty.smb.protocol.srvs.SrvsShareType;

public final class SmbClientSession implements Session, RequestSender {

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

    ListenableFuture<Session> setup(final AuthMechanism authMech) {
        final var setupFlow = new ClientSessionSetupFlow(this, this, authMech);
        setupFlow.start();
        return setupFlow.completeFuture();
    }

    @Override
    public void send(final Smb2Request request, final Consumer<Smb2Response> callback) {
        final var sessionId = sessDetails.sessionId();
        request.header().setSessionId(sessionId == null ? 0 : sessionId);
        requestSender.send(request, callback);
    }

    @Override
    public ListenableFuture<List<String>> fetchShareNames(final boolean omitCached) {
        final var result = SettableFuture.<List<String>>create();
        Futures.addCallback(connectShare("IPC$"), new FutureCallback<TreeConnect>() {
            @Override
            public void onSuccess(final TreeConnect treeConnect) {
                final var enumSharesFlow = new ClientEnumerateSharesFlow(
                    sessDetails.connection().details().serverName(), (RequestSender) treeConnect);
                Futures.addCallback(enumSharesFlow.completeFuture(), new FutureCallback<List<SrvsShareInfo>>() {
                    @Override
                    public void onSuccess(final List<SrvsShareInfo> infos) {
                        final var shareNames = infos.stream().filter(
                            info -> info.type().type() == SrvsShareType.SType.STYPE_DISKTREE
                                && !info.type().special() && !info.type().temporary()
                        ).map(SrvsShareInfo::netName).toList();
                        result.set(shareNames);
                        treeConnect.disconnect();
                    }

                    @Override
                    public void onFailure(final Throwable failure) {
                        result.setException(failure);
                    }
                }, MoreExecutors.directExecutor());
                enumSharesFlow.start();
            }

            @Override
            public void onFailure(final Throwable failure) {
                result.setException(failure);
            }
        }, MoreExecutors.directExecutor());
        return result;
    }

    @Override
    public ListenableFuture<TreeConnect> connectShare(final String shareName) {
        final var cached = sessDetails.treeConnects().get(shareName);
        if (cached != null) {
            return Futures.immediateFuture(cached);
        }
        return new SmbClientTreeConnect(shareName, this, this).connect();
    }

    @Override
    public ListenableFuture<Void> close() {
        if (sessDetails.sessionId() != null && sessDetails.connection() != null) {
            final var logoffFlow = new ClientLogoffFlow(sessDetails.sessionId(), sessDetails.connection().details(),
                this);
            logoffFlow.start();
            return logoffFlow.completeFuture();
        }
        return Futures.immediateFuture(null);
    }
}
