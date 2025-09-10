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

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.SettableFuture;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.MessageSizeEstimator;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollChannelOption;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioChannelOption;
import io.netty.channel.socket.nio.NioSocketChannel;
import java.io.IOException;
import java.net.InetAddress;
import java.util.Properties;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import jdk.net.ExtendedSocketOptions;
import local.mylan.common.utils.ConfUtils;
import local.transport.netty.smb.handler.SmbClientCodec;
import local.transport.netty.smb.handler.SmbClientConnectionHandler;
import local.transport.netty.smb.protocol.Flags;
import local.transport.netty.smb.protocol.details.ClientDetails;
import local.transport.netty.smb.protocol.details.Connection;
import local.transport.netty.smb.protocol.details.ConnectionDetails;
import local.transport.netty.smb.protocol.smb2.Smb2CapabilitiesFlags;
import local.transport.netty.smb.protocol.smb2.Smb2NegotiateFlags;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SmbClient {

    private static final Logger LOG = LoggerFactory.getLogger(SmbClient.class);

    private static final String DEFAULT_CONF = "/smb-client.conf";
    private final ClientDetails clientDetails = new ClientDetails();
    private final SmbClientConf clientConf;
    private final AtomicInteger nextConnectionId = new AtomicInteger(1);
    private Supplier<Bootstrap> bootstrapFactory;

    public SmbClient() {
        this(defaultConf());
    }

    public SmbClient(final SmbClientConf clientConf) {
        this.clientConf = clientConf;
        configure();
    }

    void setBootstrapFactory(final Supplier<Bootstrap> bootstrapFactory) {
        this.bootstrapFactory = bootstrapFactory;
    }

    private void configure() {
        clientDetails.setRequireMessageSigning(clientConf.requireMessageSigning());
        clientDetails.setEncryptionSupported(clientConf.encryptionSupported());
        clientDetails.setCompressionSupported(clientConf.compressionSupported());
        clientDetails.setChainedCompressionSupported(clientConf.chainedCompressionSupported());
        clientDetails.setRdmaTransformSupported(clientConf.rdmaTransformSupported());
        clientDetails.setDisableEncryptionOverSecureTransport(clientConf.disableEncryptionOverSecureTransport());
        clientDetails.setSigningCapabilitiesSupported(clientConf.signingCapabilitiesSupported());
        clientDetails.setTransportCapabilitiesSupported(clientConf.transportCapabilitiesSupported());
        clientDetails.setServerToClientNotificationsSupported(clientConf.serverToClientNotificationsSupported());

        clientDetails.setMinDialect(clientConf.smbDialectMin());
        clientDetails.setMaxDialect(clientConf.smbDialectMax());
    }

    public ListenableFuture<Connection> connect(final InetAddress address) {
        return connect(address, clientConf.smbServerDefaultPort());
    }

    public ListenableFuture<Connection> connect(final InetAddress address, int port) {
        // TODO check existing connection in connections map by server address
        return new ClientConnection().connect(address, port);
    }

    public class ClientConnection implements Connection {

        private final ConnectionDetails details;
        private final SmbClientConnectionHandler handler;
        private final SettableFuture<Void> closeFuture = SettableFuture.create();

        private EventLoopGroup group;
        private Channel nettyChannel;

        private ClientConnection() {
            details = new ConnectionDetails(clientDetails.clientGuid(), nextConnectionId.getAndIncrement());
            handler = new SmbClientConnectionHandler(clientDetails, details);
            configure();
        }

        private void configure() {
            details.setDefaultCreditsRequest(clientConf.defaultCreditsRequest());
            details.setClientSecurityMode(new Flags<Smb2NegotiateFlags>()
                .set(Smb2NegotiateFlags.SMB2_NEGOTIATE_SIGNING_ENABLED, clientConf.signingCapabilitiesSupported())
                .set(Smb2NegotiateFlags.SMB2_NEGOTIATE_SIGNING_REQUIRED, clientConf.requireMessageSigning())
            );
            details.setClientCapabilities(new Flags<Smb2CapabilitiesFlags>()
                // TODO use values from configuration
                .set(Smb2CapabilitiesFlags.SMB2_GLOBAL_CAP_DFS, false)
                .set(Smb2CapabilitiesFlags.SMB2_GLOBAL_CAP_LEASING, false)
                .set(Smb2CapabilitiesFlags.SMB2_GLOBAL_CAP_LARGE_MTU, false)
                .set(Smb2CapabilitiesFlags.SMB2_GLOBAL_CAP_MULTI_CHANNEL, false)
                .set(Smb2CapabilitiesFlags.SMB2_GLOBAL_CAP_PERSISTENT_HANDLES, false)
                .set(Smb2CapabilitiesFlags.SMB2_GLOBAL_CAP_DIRECTORY_LEASING, false)
                .set(Smb2CapabilitiesFlags.SMB2_GLOBAL_CAP_ENCRYPTION, clientConf.encryptionSupported())
                .set(Smb2CapabilitiesFlags.SMB2_GLOBAL_CAP_NOTIFICATIONS,
                    clientConf.serverToClientNotificationsSupported()));
        }

        private ListenableFuture<Connection> connect(InetAddress address, int port) {
            final var future = SettableFuture.<Connection>create();
            // on completion put active connection to map
            Futures.addCallback(future, new FutureCallback<Connection>() {
                @Override
                public void onSuccess(final Connection result) {
                    clientDetails.connections().put(details.connectionId(), result);
                }

                @Override
                public void onFailure(final Throwable t) {
                    onClose();
                }
            }, MoreExecutors.directExecutor());

            // complete connection only when negotiation is completed
            Futures.addCallback(handler.negotiationFuture(), new FutureCallback<Void>() {
                @Override
                public void onSuccess(final Void result) {
                    future.set(ClientConnection.this);
                }

                @Override
                public void onFailure(final Throwable cause) {
                    future.setException(cause);
                }
            }, MoreExecutors.directExecutor());

            final var bootstrap = bootstrapFactory == null ? newBootstrap() : bootstrapFactory.get();
            bootstrap.handler(new ChannelInitializer<>() {

                @Override
                protected void initChannel(final Channel channel) throws Exception {
                    channel.pipeline().addLast(new SmbClientCodec(details), handler);
                    channel.closeFuture().addListener(cf -> onClose());
                    channel.config().setMessageSizeEstimator(new MessageSizeEstimator() {
                        @Override
                        public Handle newHandle() {
                            return new Handle() {
                                @Override
                                public int size(final Object o) {
                                    return 0;
                                }
                            };
                        }
                    });
                    nettyChannel = channel;
                }
            });
            bootstrap.connect(address, port).addListener(result -> {
                if (result.cause() != null) {
                    future.setException(result.cause());
                }
            });
            return future;
        }

        private Bootstrap newBootstrap() {
            final var bootstrap = new Bootstrap();
            if (Epoll.isAvailable()) {
                group = new EpollEventLoopGroup(clientConf.groupThreads(), threadFactory(clientConf.groupName()));
                bootstrap.channel(EpollSocketChannel.class);
                if (clientConf.tcpKeepAliveEnabled()) {
                    bootstrap.option(ChannelOption.SO_KEEPALIVE, Boolean.TRUE);
                    bootstrap.option(EpollChannelOption.TCP_KEEPIDLE, clientConf.tcpKeepAliveIdleTime());
                    bootstrap.option(EpollChannelOption.TCP_KEEPCNT, clientConf.tcpKeepAliveRetransmissionCount());
                    bootstrap.option(EpollChannelOption.TCP_KEEPINTVL, clientConf.tcpKeepAliveRetransmissionInterval());
                }
            } else {
                final var group = new NioEventLoopGroup(clientConf.groupThreads(),
                    threadFactory(clientConf.groupName()));
                bootstrap.channel(NioSocketChannel.class);
                bootstrap.group(group);
                if (clientConf.tcpKeepAliveEnabled()) {
                    bootstrap.option(ChannelOption.SO_KEEPALIVE, Boolean.TRUE);
                    bootstrap.option(NioChannelOption.of(ExtendedSocketOptions.TCP_KEEPIDLE),
                        clientConf.tcpKeepAliveIdleTime());
                    bootstrap.option(NioChannelOption.of(ExtendedSocketOptions.TCP_KEEPIDLE),
                        clientConf.tcpKeepAliveRetransmissionCount());
                    bootstrap.option(NioChannelOption.of(ExtendedSocketOptions.TCP_KEEPIDLE),
                        clientConf.tcpKeepAliveRetransmissionInterval());
                }
            }
//            bootstrap.option(ChannelOption.SO_BACKLOG, clientConf.backlogSize());
            return bootstrap;
        }

        @Override
        public ConnectionDetails details() {
            return details;
        }

        @Override
        public boolean isActive() {
            return nettyChannel != null && nettyChannel.isActive();
        }

        @Override
        public ListenableFuture<Void> closeFuture() {
            return closeFuture;
        }

        @Override
        public ListenableFuture<Void> close() {
            handler.finish().addListener(nettyChannel::close, MoreExecutors.directExecutor());
            return closeFuture;
        }

        private void onClose() {
            if (group != null) {
                group.shutdownGracefully();
            }
            closeFuture.set(null);
        }
    }

    private static SmbClientConf defaultConf() {
        final var props = new Properties();
        try (var in = SmbClient.class.getResourceAsStream(DEFAULT_CONF)) {
            props.load(in);
        } catch (IOException e) {
            throw new ExceptionInInitializerError(e);
        }
        return ConfUtils.loadConfiguration(SmbClientConf.class, props);
    }

    private static ThreadFactory threadFactory(final String namePrefix) {
        return new ThreadFactoryBuilder().setNameFormat(namePrefix + "-%d").build();
    }

}
