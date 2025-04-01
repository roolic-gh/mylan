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
package local.mylan.transport.http;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollChannelOption;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioChannelOption;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.ssl.SslContext;
import java.nio.file.Path;
import java.util.concurrent.ThreadFactory;
import jdk.net.ExtendedSocketOptions;
import local.mylan.transport.http.api.RequestDispatcher;
import local.mylan.utils.ConfUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class HttpServer {
    private static final Logger LOG = LoggerFactory.getLogger(HttpServer.class);

    private final HttpServerConfig config;
    private final SslContext sslContext;
    private final RequestDispatcher dispatcher;
    private EventLoopGroup parrentGroup;
    private EventLoopGroup childGroup;

    public HttpServer(final Path confDir, final RequestDispatcher dispatcher) {
        config = ConfUtils.loadConfiguration(HttpServerConfig.class, confDir);
        sslContext = TlsUtils.buildSslContext(confDir, config);
        this.dispatcher = dispatcher;
    }

    public void start() {
        final var bootstrap = new ServerBootstrap();

        if (Epoll.isAvailable()) {
            parrentGroup = new EpollEventLoopGroup(config.parentGroupThreads(),
                threadFactory(config.parentGroupName()));
            childGroup = new EpollEventLoopGroup(config.childGroupThreads(),
                threadFactory(config.childGroupName()));
            bootstrap.channel(EpollServerSocketChannel.class);
            if (config.tcpKeepAliveEnabled()) {
                bootstrap.childOption(ChannelOption.SO_KEEPALIVE, Boolean.TRUE);
                bootstrap.childOption(EpollChannelOption.TCP_KEEPIDLE, config.tcpKeepAliveIdleTime());
                bootstrap.childOption(EpollChannelOption.TCP_KEEPCNT, config.tcpKeepAliveRetransmissionCount());
                bootstrap.childOption(EpollChannelOption.TCP_KEEPINTVL, config.tcpKeepAliveRetransmissionInterval());
            }
        } else {
            parrentGroup = new NioEventLoopGroup(config.parentGroupThreads(),
                threadFactory(config.parentGroupName()));
            childGroup = new NioEventLoopGroup(config.childGroupThreads(),
                threadFactory(config.childGroupName()));
            bootstrap.channel(NioServerSocketChannel.class);
            if (config.tcpKeepAliveEnabled()) {
                bootstrap.childOption(ChannelOption.SO_KEEPALIVE, Boolean.TRUE);
                bootstrap.childOption(NioChannelOption.of(ExtendedSocketOptions.TCP_KEEPIDLE),
                    config.tcpKeepAliveIdleTime());
                bootstrap.childOption(NioChannelOption.of(ExtendedSocketOptions.TCP_KEEPIDLE),
                    config.tcpKeepAliveRetransmissionCount());
                bootstrap.childOption(NioChannelOption.of(ExtendedSocketOptions.TCP_KEEPIDLE),
                    config.tcpKeepAliveRetransmissionInterval());
            }
        }
        bootstrap.group(parrentGroup, childGroup);
        bootstrap.option(ChannelOption.SO_BACKLOG, config.backlogSize());
        bootstrap.childHandler(new HttpServerChannelInitizer(sslContext, dispatcher, config.maxContentLength()));
        final int bindPort = sslContext != null ? config.tlsPort() : config.tcpPort();
        bootstrap.bind(bindPort);
        LOG.info("HTTP server started at port {}", bindPort);
    }

    public void stop() {
        LOG.info("Shutting douwn HTTP server");
        if (childGroup != null) {
            childGroup.shutdownGracefully();
        }
        if (parrentGroup != null) {
            parrentGroup.shutdownGracefully();
        }
    }

    private static ThreadFactory threadFactory(final String namePrefix) {
        return new ThreadFactoryBuilder().setNameFormat(namePrefix + "-%d").build();
    }
}
