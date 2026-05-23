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
package local.mylan.transport.smb.protocol.pcerpc;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

/**
 * NDR Format label. Addresses C706 PCE 1.1 (#14.1 Data Representation Format Label).
 */
public record NdrFormatLabel(ByteOrder byteOrder, Character character, FloatingPoint floatingPoint) {

    public byte[] bytes() {
        final var bytes = new byte[4];
        if (byteOrder == ByteOrder.LITTLE_ENDIAN) {
            bytes[0] |= 0x10;
        }
        bytes[0] |= (byte) (character == Character.ASCII ? 0 : 1);
        bytes[1] = floatingPoint.code;
        return bytes;
    }

    public static NdrFormatLabel fromBytes(final byte[] bytes) {
        requireNonNull(bytes);
        checkArgument(bytes.length == 4, "Invalid length, 4 bytes array expected");
        return new NdrFormatLabel(
            (bytes[0] & 0x10) == 0 ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN,
            (bytes[0] & 0x01) == 0 ? Character.ASCII : Character.EBCDIC,
            FloatingPoint.fromCode(bytes[1])
        );
    }

    public static NdrFormatLabel getDefault() {
        return new NdrFormatLabel(ByteOrder.LITTLE_ENDIAN, Character.ASCII, FloatingPoint.IEEE);
    }

    public enum ByteOrder {BIG_ENDIAN, LITTLE_ENDIAN}

    public enum Character {ASCII, EBCDIC}

    public enum FloatingPoint {
        IEEE(0),
        VAX(1),
        Cray(2),
        IBM(3);
        private final byte code;

        FloatingPoint(final int code) {
            this.code = (byte) code;
        }

        private static FloatingPoint fromCode(final byte code) {
            for (var fp : values()) {
                if (fp.code == code) {
                    return fp;
                }
            }
            throw new IllegalArgumentException("Invalid floating point representation code " + code);
        }
    }
}
