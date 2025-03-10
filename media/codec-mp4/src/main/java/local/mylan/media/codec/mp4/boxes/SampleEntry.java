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

public class SampleEntry extends Box {

    /*
    8.5.2.3 Semantics
        data_reference_index - is an integer that contains the index of the data reference to use to
            retrieve data associated with samples that use this sample description. Data references are
            stored in Data Reference Boxes. The index ranges from 1 to the number of data references.
     */
    protected int dataReferenceIndex;

    protected SampleEntry(final BoxType boxType, final long offset, final long length) {
        super(boxType, offset, length);
    }

    @Override
    void readContent(final BoxReader reader) throws IOException {
        reader.skipBytes(6); // int(8)[6] reserved
        dataReferenceIndex = reader.readUint16();
    }

    @Override
    public String toString() {
        return super.toString() + " dataRefIdx=%d".formatted(dataReferenceIndex);
    }
}
