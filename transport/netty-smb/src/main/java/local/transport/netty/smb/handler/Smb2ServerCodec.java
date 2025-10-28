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

public class Smb2ServerCodec extends Smb2Codec<Smb2Request, Smb2Response> {

    @Override
    void encode(final Smb2Response outObj, final ByteBuf byteBuf) {
        Smb2CodecUtils.encodeResponse(outObj, byteBuf, null);
    }

    @Override
    Smb2Request decode(final ByteBuf byteBuf) {
        return Smb2CodecUtils.decodeRequest(byteBuf, null);
    }
}
