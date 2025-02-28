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
package local.mylan.media.codec.mp4;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

import com.google.common.primitives.Longs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility artifact related to parcing binary-coded decimals (BCD).
 */
public final class BcdUtils {
    private static final Logger LOG = LoggerFactory.getLogger(BcdUtils.class);

    private BcdUtils() {
        // utility class
    }

    public static byte[] decodeByteSequence(final byte[] encoded, final int bitsPerByte) {
        requireNonNull(encoded);
        checkArgument(encoded.length > 0 && encoded.length <= 8, "encoded data length expected 1 to 8 bytes");
        checkArgument(bitsPerByte > 0 && bitsPerByte <= 8, "bitsPerByte should be in range 1 to 8");

        var source = Longs.fromByteArray(normalize(encoded, 8));
        final var mask = 0xFFL >> (int) (8 - bitsPerByte);
        final var result = new byte[encoded.length * 8 / bitsPerByte];
        final var maxIndex = result.length - 1;
        for (int i = 0; i < result.length; i++) {
            result[maxIndex - i] = Long.valueOf(source & mask).byteValue();
            source >>= bitsPerByte;
        }
        return result;
    }

    private static byte[] normalize(final byte[] source, final int size) {
        if (source.length == size) {
            return source;
        }
        if (source.length > size) {
            throw new IllegalArgumentException();
        }
        final byte[] target = new byte[size];
        System.arraycopy(source, 0, target, size - source.length, source.length);
        return target;
    }

    public static FixedPoint decodeFixedPoint(final byte[] encoded, final int intBytes,final int fractionBytes) {
        requireNonNull(encoded);
        return null; // todo
    }

    public record FixedPoint(int integer, int fraction) {
        @Override
        public String toString() {
            return "%d.%d".formatted(integer(), fraction());
        }
    }
}
