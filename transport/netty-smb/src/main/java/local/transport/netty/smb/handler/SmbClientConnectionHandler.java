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

import com.google.common.util.concurrent.ListenableFuture;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import local.transport.netty.smb.protocol.ClientFlow;
import local.transport.netty.smb.protocol.SmbRequest;
import local.transport.netty.smb.protocol.SmbResponse;
import local.transport.netty.smb.protocol.details.ClientDetails;
import local.transport.netty.smb.protocol.details.ConnectionDetails;
import local.transport.netty.smb.protocol.flows.ClientNegotiationFlow;

public class SmbClientConnectionHandler extends ChannelDuplexHandler {

    private final ConnectionDetails details;
    private final ClientFlow<Void> negotiation;

    public SmbClientConnectionHandler(final ClientDetails clientDetails, final ConnectionDetails connDetails) {
        details = connDetails;
        negotiation = new ClientNegotiationFlow(clientDetails, connDetails);
    }

    public ListenableFuture<Void> negotiationFuture() {
        return negotiation.completeFuture();
    }

    @Override
    public void channelActive(final ChannelHandlerContext ctx) throws Exception {
        ctx.writeAndFlush(process(negotiation.initialRequest()));
    }

    @Override
    public void channelRead(final ChannelHandlerContext ctx, final Object msg) throws Exception {
        if (msg instanceof SmbResponse response) {
            process(response);
            if (!negotiation.isComplete()) {
                negotiation.handleResponse(response);
                if (!negotiation.isComplete()) {
                    ctx.writeAndFlush(process(negotiation.nextRequest()));
                }
                return;
            }
        }
        super.channelRead(ctx, msg);
    }

    private SmbRequest process(final SmbRequest request) {
        return request;
    }

    private SmbResponse process(SmbResponse response) {
        return response;
    }
}
