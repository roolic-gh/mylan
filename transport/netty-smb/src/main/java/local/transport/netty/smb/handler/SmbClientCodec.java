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
import local.transport.netty.smb.handler.codec.CodecUtils;
import local.transport.netty.smb.protocol.SmbRequest;
import local.transport.netty.smb.protocol.SmbResponse;
import local.transport.netty.smb.protocol.details.ConnectionDetails;

public class SmbClientCodec extends SmbCodec<SmbResponse, SmbRequest> {
    final ConnectionDetails details;

    public SmbClientCodec(final ConnectionDetails details) {
        this.details = details;
    }

    @Override
    void encode(final SmbRequest request, final ByteBuf byteBuf) {
        try {
            CodecUtils.encodeRequest(request, byteBuf, details.dialect());
        } catch(Exception e){
            e.printStackTrace();
        }
    }

    @Override
    SmbResponse decode(final ByteBuf byteBuf) {
        return CodecUtils.decodeResponse(byteBuf, details.dialect());
    }
}
