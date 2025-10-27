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
package local.transport.netty.smb.handler;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;
import local.transport.netty.smb.protocol.ClientFlow;
import local.transport.netty.smb.protocol.Flags;
import local.transport.netty.smb.protocol.SmbCommand;
import local.transport.netty.smb.protocol.SmbDialect;
import local.transport.netty.smb.protocol.SmbRequest;
import local.transport.netty.smb.protocol.SmbResponse;
import local.transport.netty.smb.protocol.details.ClientDetails;
import local.transport.netty.smb.protocol.details.ConnectionDetails;
import local.transport.netty.smb.protocol.flows.ClientNegotiationFlow;
import local.transport.netty.smb.protocol.flows.RequestSender;
import local.transport.netty.smb.protocol.smb2.Smb2Header;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Smb2ClientHandler extends ChannelDuplexHandler implements RequestSender {
    private static final Logger LOG = LoggerFactory.getLogger(Smb2ClientHandler.class);

    private final ConnectionDetails connDetails;
    private final ClientFlow<Void> negotiationFlow;
    private final Map<Long, Consumer<SmbResponse>> callbacks = new ConcurrentHashMap<>();
    private final Queue<PendingRequest> pendingRequests = new ConcurrentLinkedQueue<>();
    private ChannelHandlerContext ctx;

    public Smb2ClientHandler(final ClientDetails clientDetails, final ConnectionDetails connDetails) {
        this.connDetails = connDetails;
        negotiationFlow = new ClientNegotiationFlow(clientDetails, connDetails, this);
    }

    public ListenableFuture<Void> negotiationFuture() {
        return negotiationFlow.completeFuture();
    }

    @Override
    public void channelActive(final ChannelHandlerContext ctx) throws Exception {
        this.ctx = ctx;
        negotiationFlow.start();
    }

    @Override
    public void channelRead(final ChannelHandlerContext ctx, final Object msg) throws Exception {
        if (msg instanceof SmbResponse response) {
            processInbound(response);
            if (response.header() instanceof Smb2Header header) {
                final var callback = callbacks.remove(header.messageId());
                if (callback != null) {
                    callback.accept(response);
                    return;
                }
            }
        }
        super.channelRead(ctx, msg);
    }

    @Override
    public void write(final ChannelHandlerContext ctx, final Object msg, final ChannelPromise promise)
        throws Exception {
        if (msg instanceof SmbRequest request) {
            processOutbound(ctx, request, null);
        } else {
            super.write(ctx, msg, promise);
        }
    }

    private void processOutbound(final ChannelHandlerContext ctx, final SmbRequest request,
        final Consumer<SmbResponse> callback) {

        if (request.header() instanceof Smb2Header header) {
            if (header.command() == null) {
                header.setCommand(request.message().command());
            }
            if (header.flags() == null) {
                header.setFlags(new Flags<>());
            }
            if (header.signature() == null) {
                header.setSignature(new byte[16]);
            }
            if (connDetails.dialect().sameOrAfter(SmbDialect.SMB2_1)) {
                header.setCreditRequest(header.command() == SmbCommand.SMB2_SESSION_SETUP
                    ? connDetails.setupCreditsRequest() : 1);
            }
            final var messageIdOpt = connDetails.sequenceWindow().nextMessageId();
            if (messageIdOpt.isEmpty()) {
                LOG.warn("Message {} wasn't sent and moved to pending state due to luck of credits", header.command());
                connDetails.pendingRequests().add(request);
                pendingRequests.add(new PendingRequest(request, callback, System.currentTimeMillis()));
                // TODO schedule processing pending requests
                return;
            }
            header.setMessageId(messageIdOpt.get());
            callbacks.put(header.messageId(), callback);
        }
        ctx.writeAndFlush(request);
    }

    private SmbResponse processInbound(final SmbResponse response) {
        if (response.header() instanceof Smb2Header header) {
            connDetails.sequenceWindow().acceptGranted(header.creditResponse());
            // TODO check pending
        }
        return response;
    }

    public ListenableFuture<Void> finish() {
        // TODO check pending actions
        return Futures.immediateFuture(null);
    }

    @Override
    public void send(final SmbRequest request, final Consumer<SmbResponse> callback) {
        processOutbound(ctx, request, callback);
    }

    private record PendingRequest(SmbRequest request, Consumer<SmbResponse> callback, long timestamp) {
    }
}
