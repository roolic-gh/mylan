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
 * Edit List Box. Addresses ISO/IEC 14496-12 Addresses (8.6.6  Edit List Box).
 */
public class EditListBox extends FullBox {

    /*
    8.6.6.3  Semantics
        version -- is an integer that specifies the version of this box (0 or 1)
        entry_count -- is an integer that gives the number of entries in the following table
        segment_duration -- is an integer that specifies the duration of this edit segment in units of the
            timescale in the Movie Header Box
        media_time -- is an integer containing the starting time within the media of this edit segment (in
            media time scale units, in composition time). If this field is set to –1, it is an empty edit. The last
            edit in a track shall never be an empty edit. Any difference between the duration in the Movie
            Header Box, and the track’s duration is expressed as an implicit empty edit at the end.
        media_rate -- specifies the relative rate at which to play the media corresponding to this edit
            segment. If this value is 0, then the edit is specifying a ‘dwell’: the media at media‐time is
            presented for the segment‐duration. Otherwise, this field shall contain the value 1.
     */
    public record Entry(long segmentDuration, long mediaTime) {
    }

    private Entry[] entries;
    private int mediaRateInteger;
    private int mediaRateFraction;

    EditListBox(final BoxType boxType, final long offset, final long length) {
        super(boxType, offset, length);
    }

    @Override
    void readContent(BoxReader reader) throws IOException {
        super.readContent(reader);
        final var entryCount = reader.readInt32();
        entries = new Entry[entryCount];
        for (int i = 0; i < entryCount; i++) {
            entries[i] = version == 1
                ? new Entry(reader.readUint64(), reader.readUint64())
                : new Entry(reader.readUint32(), reader.readUint32());
        }
        mediaRateInteger = reader.readUint16();
        mediaRateFraction = reader.readUint16();
    }

    @Override
    public String toString() {
        return super.toString() + "count=%s, mediaRate=%d.%d"
            .formatted(entries.length, mediaRateInteger, mediaRateFraction);
    }
}
