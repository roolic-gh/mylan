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
 * Sample To Chunk Box. Addresses ISO/IEC 14496-12 (8.7.4 Sample To Chunk Box)
 */
public class SampleToChunkBox extends FullBox {

    /*
    8.7.4.3 Semantics
        entry_count - is an integer that gives the number of entries in the following table
        first_chunk - is an integer that gives the index of the first chunk in this run of chunks that share
            the same samples‐per‐chunk and sample‐description‐index; the index of the first chunk in a
            track has the value 1 (the first_chunk field in the first record of this box has the value 1,
            identifying that the first sample maps to the first chunk).
        samples_per_chunk - is an integer that gives the number of samples in each of these chunks
        sample_description_index - is an integer that gives the index of the sample entry that
            describes the samples in this chunk. The index ranges from 1 to the number of sample entries in
            the Sample Description Box
     */
    public record Entry(int firstChunk, int samplesPerChunk, int sampleDescriptionIndex) {
    }

    private Entry[] entries;

    SampleToChunkBox(final BoxType boxType, final long offset, final long length) {
        super(boxType, offset, length);
    }

    @Override
    void readContent(final BoxReader reader) throws IOException {
        super.readContent(reader);
        final int entryCount = reader.readInt32();
        entries = new Entry[entryCount];
        for (int i = 0; i < entryCount; i++) {
            final var firstChunk = reader.readInt32();
            final var samplesPerChunk = reader.readInt32();
            final var sampleDescriptionIndex = reader.readInt32();
            entries[i] = new Entry(firstChunk, samplesPerChunk, sampleDescriptionIndex);
        }
    }

    @Override
    public String toString() {
        return super.toString() + " count=%s".formatted(entries.length);
    }
}
