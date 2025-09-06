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

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

import io.netty.buffer.ByteBuf;
import java.util.UUID;

public final class Utils {

    private Utils() {
        // utility class
    }

    public static int toIntValue(final byte[] bytes) {
        requireNonNull(bytes);
        checkArgument(bytes.length > 0 && bytes.length <= Integer.BYTES);
        var value = 0;
        for (var bt : bytes) {
            value = value << 8 | 0xFF & bt;
        }
        return value;
    }

    public static byte[] toByteArray(final int value, final int length) {
        checkArgument(length > 0 && length <= Integer.BYTES);
        final var bytes = new byte[length];
        for (int i = 0; i < length; i++) {
            bytes[i] = (byte) (0xFF & value >> 8 * (length - 1 - i));
        }
        return bytes;
    }

    public static byte[] readToByteArray(final ByteBuf byteBuf, final int length) {
        final var result = new byte[length];
        byteBuf.readBytes(result);
        return result;
    }

    public static byte[] getByteArray(final ByteBuf byteBuf, final int pos, final int length) {
        final var result = new byte[length];
        byteBuf.getBytes(pos, result);
        return result;
    }

    public static int readToIntValue(final ByteBuf byteBuf, final int length) {
        return toIntValue(readToByteArray(byteBuf, length));
    }

    public static byte[] readSmbBuffer(final ByteBuf byteBuf, final int maxIndex) {
        if (byteBuf.readableBytes() < 1) {
            return null;
        }
        final var bufType = byteBuf.readByte();
        if (bufType == 0x02) {
            // zero terminated string
            final var length = byteBuf.bytesBefore((byte) 0);
            if (length <= 0 || length == 0 || length > maxIndex - 1 - byteBuf.readerIndex()) {
                // unexpected length
                return null;
            }
            final var result = readToByteArray(byteBuf, length);
            byteBuf.skipBytes(1); // take into account terminating zero
            return result;
        }
        return null;
    }

    // MS-DTYP (#2.3.4.2 GUID--Packet Representation)

    public static UUID readGuid(final ByteBuf byteBuf) {
        var higher = byteBuf.readUnsignedIntLE() << 32;
        higher |= (long) byteBuf.readUnsignedShortLE() << 16;
        higher |= byteBuf.readUnsignedShortLE();
        return new UUID(higher, byteBuf.readLong());
    }

    public static void writeGuid(final ByteBuf byteBuf, final UUID guid) {
        final var higher = guid.getMostSignificantBits();
        byteBuf.writeIntLE((int) (higher >> 32));
        byteBuf.writeShortLE((int) (higher >> 16 & 0xFFFF));
        byteBuf.writeShortLE((int) (higher & 0xFFFF));
        byteBuf.writeLong(guid.getLeastSignificantBits());
    }

    public static long unixMillisFromFiletime(final long filetime) {
        return (filetime - 116_444_736_000_000_000L)/10_000L;
    }

    public static long filetimeFromUnixMillis(final long millis){
        return millis * 10_000 + 116_444_736_000_000_000L;
    }
}
