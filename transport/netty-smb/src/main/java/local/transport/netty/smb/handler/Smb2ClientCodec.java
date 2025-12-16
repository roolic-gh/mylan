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

import io.netty.buffer.ByteBuf;
import local.transport.netty.smb.handler.codec.Smb2CodecUtils;
import local.transport.netty.smb.protocol.Smb2Request;
import local.transport.netty.smb.protocol.Smb2Response;
import local.transport.netty.smb.protocol.details.ConnectionDetails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Smb2ClientCodec extends Smb2Codec<Smb2Response, Smb2Request> {
    private static final Logger LOG = LoggerFactory.getLogger(Smb2ClientCodec.class);
    private final ConnectionDetails details;

    public Smb2ClientCodec(final ConnectionDetails details) {
        this.details = details;
    }

    @Override
    void encode(final Smb2Request request, final ByteBuf byteBuf) {
        try {
            final var startPos = byteBuf.writerIndex();
            Smb2CodecUtils.encodeRequest(request, byteBuf, details.dialect());
            final var signer = details.packetSigners().get(request.header().sessionId());
            if (signer != null) {
                signer.signOutbound(byteBuf.slice(startPos, byteBuf.writerIndex() - startPos));
            }
        } catch (Exception e) {
            LOG.error("Error encoding request {}", request, e);
        }
    }

    @Override
    Smb2Response decode(final ByteBuf byteBuf) {
        final var startPos = byteBuf.readerIndex();
        final var length = byteBuf.readableBytes();
        final var response = Smb2CodecUtils.decodeResponse(byteBuf, details.dialect());
        final var signer = details.packetSigners().get(response.header().sessionId());
        if (signer != null) {
            // TODO handle verification result
            signer.verifyInboundSignature(byteBuf.slice(startPos, length));
        }
        return response;
    }
}
