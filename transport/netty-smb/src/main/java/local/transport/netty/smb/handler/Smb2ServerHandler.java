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

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.MoreExecutors;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import java.util.Objects;
import local.transport.netty.smb.protocol.Flags;
import local.transport.netty.smb.protocol.Smb2Request;
import local.transport.netty.smb.protocol.Smb2Response;
import local.transport.netty.smb.protocol.SmbError;
import local.transport.netty.smb.protocol.flows.ServerRequestDispatcher;
import local.transport.netty.smb.protocol.smb2.Smb2Flags;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Smb2ServerHandler extends ChannelInboundHandlerAdapter {

    private static final Logger LOG = LoggerFactory.getLogger(Smb2ServerHandler.class);
    private final ServerRequestDispatcher dispatcher;

    public Smb2ServerHandler(final ServerRequestDispatcher dispatcher) {
        this.dispatcher = Objects.requireNonNull(dispatcher);
    }

    @Override
    public void channelRead(final ChannelHandlerContext ctx, final Object msg) throws Exception {
        if (msg instanceof Smb2Request request) {
            Futures.addCallback(dispatcher.dispatch(request), new FutureCallback<Smb2Response>() {
                @Override
                public void onSuccess(final Smb2Response response) {
                    processOutbound(request, response);
                    ctx.writeAndFlush(response);
                }

                @Override
                public void onFailure(final Throwable throwable) {
                    LOG.error("Exception processing request {}", request, throwable);
                    // TODO send ErrorMessage
                }
            }, MoreExecutors.directExecutor());
        } else {
            super.channelRead(ctx, msg);
        }
    }

    private static void processOutbound(final Smb2Request request, final Smb2Response response) {
        if (response.header().status() == null) {
            response.header().setStatus(SmbError.STATUS_SUCCESS);
        }
        if (response.header().flags() == null) {
            response.header().setFlags(new Flags<>());
        }
        response.header().flags().set(Smb2Flags.SMB2_FLAGS_SERVER_TO_REDIR, true); // is response
        response.header().setMessageId(request.header().messageId());
        response.header().setAsyncId(request.header().asyncId());
        if (response.header().creditResponse() == 0) {
            response.header().setCreditResponse(request.header().creditRequest());
        }
        if (response.header().sessionId() == 0) {
            response.header().setSessionId(request.header().sessionId());
        }
        if (response.header().treeId() == 0) {
            response.header().setTreeId(request.header().treeId());
        }
        if (response.header().channelSequence() == 0) {
            response.header().setChannelSequence(request.header().channelSequence());
        }
    }
}
