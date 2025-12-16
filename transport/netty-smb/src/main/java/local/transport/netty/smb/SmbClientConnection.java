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
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
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
import java.net.SocketAddress;
import java.util.function.Consumer;
import jdk.net.ExtendedSocketOptions;
import local.transport.netty.smb.handler.Smb2ClientCodec;
import local.transport.netty.smb.handler.Smb2ClientHandler;
import local.transport.netty.smb.handler.codec.NtlmCodecUtils;
import local.transport.netty.smb.protocol.Flags;
import local.transport.netty.smb.protocol.details.Connection;
import local.transport.netty.smb.protocol.details.ConnectionDetails;
import local.transport.netty.smb.protocol.details.NtlmClientDetails;
import local.transport.netty.smb.protocol.details.Session;
import local.transport.netty.smb.protocol.details.SessionDetails;
import local.transport.netty.smb.protocol.details.UserCredentials;
import local.transport.netty.smb.protocol.flows.AuthMechanism;
import local.transport.netty.smb.protocol.flows.NtlmAuthMechanism;
import local.transport.netty.smb.protocol.smb2.Smb2CapabilitiesFlags;
import local.transport.netty.smb.protocol.smb2.Smb2NegotiateFlags;
import local.transport.netty.smb.protocol.spnego.ntlm.NtlmVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SmbClientConnection implements Connection {
    private static final Logger LOG = LoggerFactory.getLogger(SmbClientConnection.class);
    private final SmbClient client;
    private final ConnectionDetails connDetails;
    private final Smb2ClientHandler handler;
    private final SettableFuture<Void> closeFuture = SettableFuture.create();

    private EventLoopGroup group;
    private Channel nettyChannel;

    SmbClientConnection(final int connectionId, final SmbClient client) {
        this.client = client;
        connDetails = new ConnectionDetails(client.details().clientGuid(), connectionId);
        handler = new Smb2ClientHandler(client.details(), connDetails);
        configure();
    }

    private void configure() {
        connDetails.setSetupCreditsRequest(client.conf().setupCreditsRequest());
        connDetails.setClientSecurityMode(new Flags<Smb2NegotiateFlags>()
            .set(Smb2NegotiateFlags.SMB2_NEGOTIATE_SIGNING_ENABLED, client.details().signingCapabilitiesSupported())
            .set(Smb2NegotiateFlags.SMB2_NEGOTIATE_SIGNING_REQUIRED, client.details().requireMessageSigning())
        );
        connDetails.setClientCapabilities(new Flags<Smb2CapabilitiesFlags>()
            // TODO use values from configuration
            .set(Smb2CapabilitiesFlags.SMB2_GLOBAL_CAP_DFS, false)
            .set(Smb2CapabilitiesFlags.SMB2_GLOBAL_CAP_LEASING, false)
            .set(Smb2CapabilitiesFlags.SMB2_GLOBAL_CAP_LARGE_MTU, false)
            .set(Smb2CapabilitiesFlags.SMB2_GLOBAL_CAP_MULTI_CHANNEL, false)
            .set(Smb2CapabilitiesFlags.SMB2_GLOBAL_CAP_PERSISTENT_HANDLES, false)
            .set(Smb2CapabilitiesFlags.SMB2_GLOBAL_CAP_DIRECTORY_LEASING, false)
            .set(Smb2CapabilitiesFlags.SMB2_GLOBAL_CAP_ENCRYPTION, client.details().encryptionSupported())
            .set(Smb2CapabilitiesFlags.SMB2_GLOBAL_CAP_NOTIFICATIONS,
                client.details().serverToClientNotificationsSupported()));
    }

    ListenableFuture<Connection> connect(final Channel channel) {
        final var future = newConnectionFuture();
        channel.pipeline().addLast(newChannelInitializer());
        if (channel.isActive()) {
            // trigger channel activation related logic if it's already active (EmbeddedChannel case)
            channel.pipeline().fireChannelActive();
        }
        return future;
    }

    ListenableFuture<Connection> connect(final SocketAddress address) {
        final var future = newConnectionFuture();
        newBootstrap()
            .handler(newChannelInitializer())
            .connect(address).addListener(result -> {
                if (result.cause() != null) {
                    future.setException(result.cause());
                }
            });
        return future;
    }

    private SettableFuture<Connection> newConnectionFuture() {
        final var future = SettableFuture.<Connection>create();
        // on completion put active connection to map
        Futures.addCallback(future, new FutureCallback<Connection>() {
            @Override
            public void onSuccess(final Connection result) {
                client.details().connections().put(connDetails.connectionId(), result);
                connDetails.server().addresses().add(nettyChannel.remoteAddress());
            }

            @Override
            public void onFailure(final Throwable t) {
                close();
            }
        }, MoreExecutors.directExecutor());

        // complete connection only when negotiation is completed
        Futures.addCallback(handler.negotiationFuture(), new FutureCallback<Void>() {
            @Override
            public void onSuccess(final Void result) {
                future.set(SmbClientConnection.this);
            }

            @Override
            public void onFailure(final Throwable cause) {
                future.setException(cause);
            }
        }, MoreExecutors.directExecutor());
        return future;
    }

    private ChannelInitializer<Channel> newChannelInitializer() {
        final var errorHandler = new ChannelInboundHandlerAdapter() {
            @Override
            public void exceptionCaught(final ChannelHandlerContext ctx, final Throwable cause) throws Exception {
                super.exceptionCaught(ctx, cause);
                // TODO proper exception handler
                cause.printStackTrace(System.err);
            }
        };
        return new ChannelInitializer<>() {

            @Override
            protected void initChannel(final Channel channel) throws Exception {
                channel.pipeline().addLast(new Smb2ClientCodec(connDetails), handler, errorHandler);
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
                LOG.debug("Channel {} initialized", channel);
            }
        };
    }

    private Bootstrap newBootstrap() {
        final var bootstrap = new Bootstrap();
        final var threadFactory = new ThreadFactoryBuilder().setNameFormat(client.conf().groupName() + "-%d")
            .build();
        final var maxThreads = client.conf().groupThreads();
        if (Epoll.isAvailable()) {
            group = new EpollEventLoopGroup(maxThreads, threadFactory);
            bootstrap.channel(EpollSocketChannel.class);
            if (client.conf().tcpKeepAliveEnabled()) {
                bootstrap.option(ChannelOption.SO_KEEPALIVE, Boolean.TRUE);
                bootstrap.option(EpollChannelOption.TCP_KEEPIDLE, client.conf().tcpKeepAliveIdleTime());
                bootstrap.option(EpollChannelOption.TCP_KEEPCNT, client.conf().tcpKeepAliveRetransmissionCount());
                bootstrap.option(EpollChannelOption.TCP_KEEPINTVL, client.conf().tcpKeepAliveRetransmissionInterval());
            }
        } else {
            final var group = new NioEventLoopGroup(maxThreads, threadFactory);
            bootstrap.channel(NioSocketChannel.class);
            bootstrap.group(group);
            if (client.conf().tcpKeepAliveEnabled()) {
                bootstrap.option(ChannelOption.SO_KEEPALIVE, Boolean.TRUE);
                bootstrap.option(NioChannelOption.of(ExtendedSocketOptions.TCP_KEEPIDLE),
                    client.conf().tcpKeepAliveIdleTime());
                bootstrap.option(NioChannelOption.of(ExtendedSocketOptions.TCP_KEEPCOUNT),
                    client.conf().tcpKeepAliveRetransmissionCount());
                bootstrap.option(NioChannelOption.of(ExtendedSocketOptions.TCP_KEEPINTERVAL),
                    client.conf().tcpKeepAliveRetransmissionInterval());
            }
        }
        return bootstrap;
    }

    @Override
    public ConnectionDetails details() {
        return connDetails;
    }

    @Override
    public boolean isActive() {
        return nettyChannel != null && nettyChannel.isActive();
    }

    @Override
    public ListenableFuture<Session> newAnonimousSession() {
        return setupSession(newSessionDetails(sd -> sd.setAnonymous(true)));
    }

    @Override
    public ListenableFuture<Session> newGuestSession() {
        return setupSession(newSessionDetails(sd -> {
            sd.setGuest(true);
            // TODO take guest user credentials from config
            sd.setUserCredentials(new UserCredentials.PlaintextUserCredentials("Guest", ""));
        }));
    }

    @Override
    public ListenableFuture<Session> newSession(final UserCredentials credentials) {
        return setupSession(newSessionDetails(sd -> sd.setUserCredentials(credentials)));
    }

    @Override
    public ListenableFuture<Session> bindSession(final Session session) {
        // FIXME set session id to previousSessionId
        return setupSession(session.details());
    }

    private SessionDetails newSessionDetails(final Consumer<SessionDetails> configurator) {
        final var sessDetails = new SessionDetails();
        sessDetails.setConnection(this);
        // FIXME should be part of SessionSetupFlow
        final var serverSec = connDetails.server().securityMode();
        sessDetails.setSigningRequired(
            connDetails.clientSecurityMode().get(Smb2NegotiateFlags.SMB2_NEGOTIATE_SIGNING_REQUIRED)
                || serverSec != null && serverSec.get(Smb2NegotiateFlags.SMB2_NEGOTIATE_SIGNING_REQUIRED));
        configurator.accept(sessDetails);
        return sessDetails;
    }

    private ListenableFuture<Session> setupSession(final SessionDetails sessDetails) {
        final var session = new SmbClientSession(sessDetails, handler);
        return session.setup(newAuthMechInstance(sessDetails));
    }

    private static AuthMechanism newAuthMechInstance(final SessionDetails sessDetails) {
        final var ntlmDetails = new NtlmClientDetails();
        ntlmDetails.setClientVersion(new NtlmVersion(7, 0, 0, 15));
        ntlmDetails.setNtlmV2(true);
        ntlmDetails.setClientRequire128bitEncryption(true);
        // TODO make configurable via smb client config
        return new NtlmAuthMechanism(sessDetails, ntlmDetails, NtlmCodecUtils.AUTH_ENCODER);
    }

    @Override
    public ListenableFuture<Void> closeFuture() {
        return closeFuture;
    }

    @Override
    public ListenableFuture<Void> close() {
        if (nettyChannel != null && nettyChannel.isActive()) {
            handler.finish().addListener(() -> {
                nettyChannel.close().addListener(future -> {
                    closeFuture.set(null);
                });
            }, MoreExecutors.directExecutor());
            return closeFuture;
        } else {
            return Futures.immediateFuture(null);
        }
    }

    private void onClose() {
        if (group != null) {
            group.shutdownGracefully();
        }
        closeFuture.set(null);
    }
}
