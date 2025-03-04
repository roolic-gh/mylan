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
 * Decoding Time to Sample Box. Addresses ISO/IEC 14496-12 (8.6.1.2 Decoding Time to Sample Box).
 */
public class TimeToSampleBox extends FullBox {

    /*
    8.6.1.2.3 Semantics
        version ‐ is an integer that specifies the version of this box.
        entry_count ‐ is an integer that gives the number of entries in the following table.
        sample_count ‐ is an integer that counts the number of consecutive samples that have the given duration.
        sample_delta ‐ is an integer that gives the delta of these samples in the time‐scale of the media.
     */
    public record Entry(int sampleCount, int sampleDelta) {
    }

    private Entry[] entries;

    TimeToSampleBox(final BoxType boxType, final long offset, final long length) {
        super(boxType, offset, length);
    }

    @Override
    void readContent(final BoxReader reader) throws IOException {
        super.readContent(reader);
        final int entryCount = reader.readInt32();
        entries = new Entry[entryCount];
        for (int i = 0; i < entryCount; i++) {
            final var sampleCount = reader.readInt32();
            final var sampleDelta = reader.readInt32();
            entries[i] = new Entry(sampleCount, sampleDelta);
        }
    }

    @Override
    public String toString() {
        return super.toString() + " count=%s".formatted(entries.length);
    }
}
