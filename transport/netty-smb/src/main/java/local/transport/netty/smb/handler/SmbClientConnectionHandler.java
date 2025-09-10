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
import java.util.concurrent.atomic.AtomicReference;
import local.transport.netty.smb.protocol.ClientFlow;
import local.transport.netty.smb.protocol.Flags;
import local.transport.netty.smb.protocol.SmbRequest;
import local.transport.netty.smb.protocol.SmbResponse;
import local.transport.netty.smb.protocol.details.ClientDetails;
import local.transport.netty.smb.protocol.details.ConnectionDetails;
import local.transport.netty.smb.protocol.flows.ClientNegotiationFlow;
import local.transport.netty.smb.protocol.smb2.Smb2Header;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SmbClientConnectionHandler extends ChannelDuplexHandler {
    private static final Logger LOG = LoggerFactory.getLogger(SmbClientConnectionHandler.class);

    private final ConnectionDetails connDetails;
    private final ClientFlow<Void> negotiationFlow;
    private final AtomicReference<ClientFlow<?>> currentFlow = new AtomicReference<>(null);

    public SmbClientConnectionHandler(final ClientDetails clientDetails, final ConnectionDetails connDetails) {
        this.connDetails = connDetails;
        negotiationFlow = new ClientNegotiationFlow(clientDetails, connDetails);
    }

    public ListenableFuture<Void> negotiationFuture() {
        return negotiationFlow.completeFuture();
    }

    @Override
    public void channelActive(final ChannelHandlerContext ctx) throws Exception {
        currentFlow.set(negotiationFlow);
        process(ctx, negotiationFlow.initialRequest());
    }

    @Override
    public void channelRead(final ChannelHandlerContext ctx, final Object msg) throws Exception {
        if (msg instanceof SmbResponse response) {
            process(response);
            final var curFlow = currentFlow.get();
            if (curFlow != null && !curFlow.isComplete()) {
                curFlow.handleResponse(response);
                if (curFlow.isComplete()) {
                    currentFlow.set(null);
                } else {
                    process(ctx, curFlow.nextRequest());
                }
                return;
            }
        }
        super.channelRead(ctx, msg);
    }

    private void process(final ChannelHandlerContext ctx, final SmbRequest request) {
        if (request.header() instanceof Smb2Header header) {
            if (header.command() == null) {
                header.setCommand(request.message().command());
            }
            if (header.flags() == null) {
                header.setFlags(new Flags<>());
            }
            if(header.signature() == null){
                header.setSignature(new byte[16]);
            }
            header.setCreditRequest(connDetails.defaultCreditsRequest());
            final var messageIdOpt = connDetails.sequenceWindow().nextMessageId();
            if (messageIdOpt.isEmpty()) {
                LOG.warn("Message {} wasn't sent and moved to pending state due to luck of credits", header.command());
                connDetails.pendingRequests().add(request); // TODO add timestamp
                return;
            }
            header.setMessageId(messageIdOpt.get());
        }
        ctx.writeAndFlush(request);
    }

    private SmbResponse process(SmbResponse response) {
        if (response.header() instanceof Smb2Header header) {
            connDetails.sequenceWindow().acceptGranted(header.creditResponse());
        }
        return response;
    }

    public ListenableFuture<Void> finish() {
        // TODO check pending actions
        return Futures.immediateFuture(null);
    }
}
