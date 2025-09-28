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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.util.concurrent.ListenableFuture;
import io.netty.bootstrap.Bootstrap;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import local.mylan.common.utils.ConfUtils;
import local.transport.netty.smb.protocol.details.Client;
import local.transport.netty.smb.protocol.details.ClientDetails;
import local.transport.netty.smb.protocol.details.Connection;

public class SmbClient implements Client {
    private final ClientDetails clientDetails = new ClientDetails();
    private final SmbClientConf clientConf;
    private final AtomicInteger nextConnectionId = new AtomicInteger(1);

    @VisibleForTesting
    Supplier<Bootstrap> bootstrapFactory;

    public SmbClient() {
        this((Path) null);
    }

    public SmbClient(final Path confPath) {
        this(ConfUtils.loadConfiguration(SmbClientConf.class, confPath));
    }

    public SmbClient(final SmbClientConf clientConf) {
        this.clientConf = requireNonNull(clientConf);
        configure();
    }

    @Override
    public ClientDetails details() {
        return clientDetails;
    }

    SmbClientConf conf() {
        return clientConf;
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

    @Override
    public ListenableFuture<Connection> connect(final InetAddress address) {
        return connect(new InetSocketAddress(requireNonNull(address), clientConf.smbServerDefaultPort()));
    }

    @Override
    public ListenableFuture<Connection> connect(final SocketAddress address) {
        // TODO check existing connection in connections map by server address
        final var connection = new SmbClientConnection(nextConnectionId.incrementAndGet(), this);
        connection.bootstrapFactory = bootstrapFactory;
        return connection.connect(address);
    }

}
