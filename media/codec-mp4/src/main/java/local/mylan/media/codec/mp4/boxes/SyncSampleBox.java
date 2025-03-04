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
 * Sync Sample Box. Addresses ISO/IEC 14496-12 (8.6.2 Sync Sample Box)
 */
public class SyncSampleBox extends FullBox {

    private long[] sampleNumbers;

    SyncSampleBox(final BoxType type, final long offset, final long length) {
        super(type, offset, length);
    }

    @Override
    void readContent(final BoxReader reader) throws IOException {
        super.readContent(reader);
        final var count = reader.readInt32();
        sampleNumbers = new long[count];
        for (int i = 0; i < count; i++) {
            sampleNumbers[i] = reader.readUint32();
        }
    }

    @Override
    public String toString() {
        return super.toString() + " count=%d".formatted(sampleNumbers.length);
    }
}
