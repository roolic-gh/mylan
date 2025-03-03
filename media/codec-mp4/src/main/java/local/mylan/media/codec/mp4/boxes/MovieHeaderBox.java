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
 * Movie Header Box. Addresses ISO/IEC 14496-12 (8.2.2 Movie Header Box).
 */
public class MovieHeaderBox extends FullBox {

    /*
        8.2.2.3 Semantics
        version -- is an integer that specifies the version of this box (0 or 1 in this specification)
        creation_time -- is an integer that declares the creation time of the presentation (in seconds
            since midnight, Jan. 1, 1904, in UTC time)
        modification_time -- is an integer that declares the most recent time the presentation was
            modified (in seconds since midnight, Jan. 1, 1904, in UTC time)
        timescale -- is an integer that specifies the time‐scale for the entire presentation; this is the
            number of time units that pass in one second. For example, a time coordinate system that
            measures time in sixtieths of a second has a time scale of 60.
        duration -- is an integer that declares length of the presentation (in the indicated timescale). This
            property is derived from the presentation’s tracks: the value of this field corresponds to the
            duration of the longest track in the presentation. If the duration cannot be determined then
            duration is set to all 1s.
        rate -- is a fixed point 16.16 number that indicates the preferred rate to play the presentation; 1.0
            (0x00010000) is normal forward playback
        volume --  is a fixed point 8.8 number that indicates the preferred playback volume. 1.0 (0x0100) is
            full volume.
        matrix -- provides a transformation matrix for the video; (u,v,w) are restricted here to (0,0,1), hex
            values (0,0,0x40000000).
        next_track_ID --  is a non‐zero integer that indicates a value to use for the track ID of the next track
            to be added to this presentation. Zero is not a valid track ID value. The value of
            next_track_ID shall be larger than the largest track‐ID in use. If this value is equal to all 1s
            (32‐bit maxint), and a new media track is to be added, then a search must be made in the file for
            an unused track identifier.
     */

    private long creationTime;
    private long modificationTime;
    private long timescale;
    private long duration;
    private long rate;
    private int volume;
    private long nextTrackId;

    MovieHeaderBox(final long offset, final long length) {
        super(offset, length);
    }

    @Override
    public BoxType boxType() {
        return BoxType.MovieHeader;
    }

    @Override
    void readContent(final BoxReader reader) throws IOException {
        super.readContent(reader);
        if (version == 1) {
            creationTime = reader.readUint64();
            modificationTime = reader.readUint64();
            timescale = reader.readUint32();
            duration = reader.readUint64();
        } else { // version == 0
            creationTime = reader.readUint32();
            modificationTime = reader.readUint32();
            timescale = reader.readUint32();
            duration = reader.readUint32();
        }
        rate = reader.readUint32(); // todo decumal 16.16
        volume = reader.readUint16();
        reader.skipBytes(2); // bit(16) reserved
        reader.skipBytes(4 * 2); // int(32)[2] reserved
        reader.skipBytes(4 * 9); // int(32)[9] matrix (not used)
        reader.skipBytes(4 * 6); // bit(32)[6] reserved
        nextTrackId = reader.readUint32();
    }

    @Override
    public String toString() {
        return super.toString() + " timescale=%s, duration=%s, nextTrackId=%d"
            .formatted(timescale, duration, nextTrackId);
    }
}
