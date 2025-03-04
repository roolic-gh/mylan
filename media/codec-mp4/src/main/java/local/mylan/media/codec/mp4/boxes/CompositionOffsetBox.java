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

/**
 * Composition Time to Sample Box. Addresses ISO/IEC 14496-12 (8.6.1.3 Composition Time to Sample Box).
 */
public class CompositionOffsetBox extends FullBox {

    /*
    8.6.1.3.3 Semantics
        version ‐ is an integer that specifies the version of this box.
        entry_count - is an integer that gives the number of entries in the following table.
        sample_count - is an integer that counts the number of consecutive samples that have the given offset.
        sample_offset is an integer that gives the offset between CT and DT, such that CT(n) = DT(n) + CTTS(n).
     */
    public record Entry(int sampleCount, long sampleOffset) {
    }

    private Entry[] entries;

    CompositionOffsetBox(final BoxType boxType, final long offset, final long length) {
        super(boxType, offset, length);
    }

    @Override
    void readContent(final BoxReader reader) throws IOException {
        super.readContent(reader);
        final int entryCount = reader.readInt32();
        entries = new Entry[entryCount];
        for (int i = 0; i < entryCount; i++) {
            final var sampleCount = reader.readInt32();
            final long sampleDelta = version == 0 ? reader.readUint32() : reader.readInt32();
            entries[i] = new Entry(sampleCount, sampleDelta);
        }
    }

    @Override
    public String toString() {
        return super.toString() + " count=%s".formatted(entries.length);
    }
}
