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
import local.transport.netty.smb.protocol.Smb2Request;
import local.transport.netty.smb.protocol.Smb2Response;
import local.transport.netty.smb.protocol.details.Session;
import local.transport.netty.smb.protocol.details.SessionDetails;
import local.transport.netty.smb.protocol.details.TreeConnect;
import local.transport.netty.smb.protocol.details.TreeConnectDetails;
import local.transport.netty.smb.protocol.flows.ClientTreeConnectFlow;
import local.transport.netty.smb.protocol.flows.ClientTreeDisconnectFlow;
import local.transport.netty.smb.protocol.flows.RequestSender;

public class SmbClientTreeConnect implements TreeConnect, RequestSender {

    private final TreeConnectDetails details;
    private final SessionDetails sessDetails;
    private final RequestSender sender;

    public SmbClientTreeConnect(final String shareName, final Session session, final RequestSender sender) {
        details = new TreeConnectDetails();
        details.setShareName(requireNonNull(shareName));
        details.setSession(requireNonNull(session));
        sessDetails = requireNonNull(session.details());
        requireNonNull(sessDetails.connection());
        requireNonNull(sessDetails.connection().details());
        details.setSharePath(
            "\\\\%s\\%s".formatted(requireNonNull(sessDetails.connection().details().serverName()), shareName));
        this.sender = sender;
    }

    @Override
    public TreeConnectDetails details() {
        return details;
    }

    ListenableFuture<TreeConnect> connect() {
        final var flow = new ClientTreeConnectFlow(this, this);
        flow.start();
        return flow.completeFuture();
    }

    @Override
    public ListenableFuture<Void> disconnect() {
        final var flow = new ClientTreeDisconnectFlow(this, this);
        flow.start();
        return flow.completeFuture();
    }

    @Override
    public void send(final Smb2Request request, final Consumer<Smb2Response> callback) {
        if (details.treeConnectId() != null) {
            request.header().setTreeId(details.treeConnectId());
        }
        sender.send(request, callback);
    }
}
