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
package local.mylan.media.codec.mp4.boxes;

import java.io.IOException;
import java.util.BitSet;
import java.util.stream.IntStream;

/**
 * Box Base with version and flags fields. Addresses ISO/IEC 14496-12 (4.2 Object Structure).
 */
public class FullBox extends Box {

    protected FullBox(final BoxType boxType, final long offset, final long length) {
        super(boxType, offset, length);
    }

    protected int version;
    protected BitSet flags;

    @Override
    void readContent(final BoxReader reader) throws IOException {
        version = reader.readUint8();
        flags = BitSet.valueOf(reader.readBytes(3));
    }

    protected String flagsAsString() {
        final var sb = new StringBuilder();
        IntStream.range(0, flags.length()).forEach(i -> sb.append(flags.get(i) ? "1" : "0"));
        return sb.toString();
    }

    public boolean flag(int index) {
        // bits are allocated in a reverse order
        // 0000000000000000111 means bits 0,1,2 are enabled
        return flags.get(flags.length() - 1 - index);
    }
}
