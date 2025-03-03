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
 * Sample Size Box. Addresses ISO/IEC 14496-12 (8.7.3.2 Sample Size Box).
 */
public class SampleSizeBox extends FullBox {
    /*
    8.7.3.2.2 Semantics
        sample_size --is integer specifying the default sample size. If all the samples are the same size,
            this field contains that size value. If this field is set to 0, then the samples have different sizes,
            and those sizes are stored in the sample size table. If this field is not 0, it specifies the constant
            sample size, and no array follows.
        sample_count --is an integer that gives the number of samples in the track; if sample‐size is 0, then
            it is also the number of entries in the following table.
        entry_size -- is an integer specifying the size of a sample, indexed by its number.
     */
    private long sampleSize;
    private int sampleCount;
    private long[] entrySizes;

    SampleSizeBox(final long offset, final long length) {
        super(offset, length);
    }

    @Override
    public BoxType boxType() {
        return BoxType.SampleSize;
    }

    @Override
    void readContent(final BoxReader reader) throws IOException {
        super.readContent(reader);
        sampleSize = reader.readUint32();
        sampleCount = reader.readInt32();
        if (sampleSize == 0 && sampleCount > 0) {
            entrySizes = new long[sampleCount];
            for (int i = 0; i < sampleCount; i++) {
                entrySizes[i] = reader.readUint32();
            }
        }
    }

    @Override
    public String toString() {
        return super.toString() + " size=%d, count=%d".formatted(sampleSize, sampleCount);
    }
}
