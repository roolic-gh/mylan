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

import com.google.common.util.concurrent.Futures;
import java.util.UUID;
import java.util.function.Function;
import local.mylan.common.utils.ConfUtils;
import local.transport.netty.smb.protocol.Flags;
import local.transport.netty.smb.protocol.Smb2Request;
import local.transport.netty.smb.protocol.Smb2Response;
import local.transport.netty.smb.protocol.flows.ServerRequestDispatcher;
import local.transport.netty.smb.protocol.smb2.Smb2CapabilitiesFlags;
import local.transport.netty.smb.protocol.smb2.Smb2NegotiateFlags;
import local.transport.netty.smb.protocol.smb2.Smb2NegotiateRequest;
import local.transport.netty.smb.protocol.smb2.Smb2NegotiateResponse;
import local.transport.netty.smb.protocol.smb2.Smb2SessionSetupRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class TestServer {
    private static final Logger LOG = LoggerFactory.getLogger(TestServer.class);

    static final UUID GUID = UUID.randomUUID();
    static final String NAME = "TestServer";

    private final TestServerConf conf;

    TestServer() {
        conf = ConfUtils.loadConfiguration(TestServerConf.class);
    }

    TestServer(final TestServerConf conf) {
        this.conf = conf;
    }

    TestServerConf conf() {
        return conf;
    }

    ServerRequestDispatcher asDispatcher() {
        return asDispatcher(request -> null);
    }

    ServerRequestDispatcher asDispatcher(final Function<Smb2Request, Smb2Response> overrides) {
        return request -> {
            final var ovrResponse = overrides.apply(request);
            final var response = ovrResponse == null ? serverResponse(request) : ovrResponse;
            LOG.debug("{} -> {}", request, response);
            return response == null
                ? Futures.immediateFailedFuture(new IllegalStateException("No handler for request " + request))
                : Futures.immediateFuture(response);
        };
    }

    private Smb2Response serverResponse(final Smb2Request request) {
        return switch (request) {
            case Smb2NegotiateRequest req -> negotiateResponse(req);
            case Smb2SessionSetupRequest req -> sessionSetupResponse(req);
            default -> null;
        };
    }

    Smb2Response negotiateResponse(final Smb2NegotiateRequest request) {
        final var response = new Smb2NegotiateResponse();
        final var dialect = conf.serverDialect();
        if (!request.dialects().contains(dialect)) {
            throw new IllegalStateException("Unsupported dialect");
        }
        response.setDialectRevision(dialect);
        response.setServerGuid(GUID);
        response.setCapabilities(new Flags<Smb2CapabilitiesFlags>()
            .set(Smb2CapabilitiesFlags.SMB2_GLOBAL_CAP_DFS, true)
            .set(Smb2CapabilitiesFlags.SMB2_GLOBAL_CAP_LEASING, true)
            .set(Smb2CapabilitiesFlags.SMB2_GLOBAL_CAP_LARGE_MTU, true)
        );
        response.setSecurityMode(new Flags<Smb2NegotiateFlags>()
            .set(Smb2NegotiateFlags.SMB2_NEGOTIATE_SIGNING_ENABLED, conf.signEnabled())
            .set(Smb2NegotiateFlags.SMB2_NEGOTIATE_SIGNING_REQUIRED, conf.signRequired())
        );
        final var size = conf.maxReadWriteSize();
        response.setMaxReadSize(size);
        response.setMaxWriteSize(size);
        response.setMaxTransactSize(size);
        return response;
    }

    private Smb2Response sessionSetupResponse(final Smb2SessionSetupRequest request) {
        return null;
    }
}
