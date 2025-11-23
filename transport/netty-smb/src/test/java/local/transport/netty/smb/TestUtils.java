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

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.channel.embedded.EmbeddedChannel;
import local.transport.netty.smb.handler.Smb2ServerCodec;
import local.transport.netty.smb.handler.Smb2ServerHandler;
import local.transport.netty.smb.protocol.flows.ServerRequestDispatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class TestUtils {
    private static final Logger LOG = LoggerFactory.getLogger(TestUtils.class);

    private TestUtils() {
        // utility class;
    }

    static Channel channelToServer(final ServerRequestDispatcher dispatcher) {
        final var clientChannel = new EmbeddedChannel();
        final var serverChannel = new EmbeddedChannel();
        serverChannel.pipeline().addLast(new CrossChannelHandler("server", clientChannel),
            new Smb2ServerCodec(),
            new Smb2ServerHandler(dispatcher));
        clientChannel.pipeline().addLast(new CrossChannelHandler("client", serverChannel));
        return clientChannel;
    }

    /**
     * Takes outbound message from assigned channel and puts it as inbound to another one.
      */
    private static class CrossChannelHandler extends ChannelOutboundHandlerAdapter {
        private final String id;
        private final EmbeddedChannel otherChannel;

        CrossChannelHandler(final String id, final EmbeddedChannel otherChannel) {
            this.id = id;
            this.otherChannel = otherChannel;
        }

        @Override
        public void write(final ChannelHandlerContext ctx, final Object msg, final ChannelPromise promise)
            throws Exception {

            if (msg instanceof ByteBuf byteBuf) {
                final var packet = otherChannel.alloc().buffer(byteBuf.readableBytes());
                packet.writeBytes(byteBuf);
                otherChannel.writeInbound(packet);
                promise.setSuccess();
                LOG.debug("{} sent a packet ({} bytes)", id, packet.capacity());

            } else {
                super.write(ctx, msg, promise);
            }
        }
    }
}
