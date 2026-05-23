/*
 * Copyright 2026 Ruslan Kashapov
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
package local.mylan.transport.smb.handler.codec;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import local.mylan.transport.smb.protocol.fscc.Blob;
import local.mylan.transport.smb.protocol.fscc.FsctlCode;
import local.mylan.transport.smb.protocol.pcerpc.PceMessage;

final class IoctlCodecUtils {

    private IoctlCodecUtils() {
        // utility class
    }

    static Object decodeInput(final ByteBuf byteBuf, final FsctlCode ctlCode) {
        return switch (ctlCode) {
            case FSCTL_PIPE_TRANSCEIVE -> PceCodecUtils.decode(byteBuf);
            default -> blob(byteBuf);
        };
    }

    static void encodeInput(final ByteBuf byteBuf, final Object input, final FsctlCode ctlCode) {
        if (input instanceof PceMessage pce) {
            PceCodecUtils.encode(byteBuf, pce);
            return;
        }

    }

    static Object decodeOutput(final ByteBuf byteBuf, final FsctlCode ctlCode) {
        return switch (ctlCode) {
            case FSCTL_PIPE_TRANSCEIVE -> PceCodecUtils.decode(byteBuf);
            default -> blob(byteBuf);
        };
    }

    static void encodeOutput(final ByteBuf byteBuf, final Object output, final FsctlCode ctlCode) {
        if (output instanceof PceMessage pce) {
            PceCodecUtils.encode(byteBuf, pce);
            return;
        }
    }

    private static Blob blob(final ByteBuf byteBuf) {
        return new Blob(ByteBufUtil.getBytes(byteBuf));

    }

}
