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
 * Track Header Box. Addresses ISO/IEC 14496-12 (8.3.2 Track Header Box)
 */
public class TrackHeaderBox extends FullBox {

    /*
        8.3.2.3  Semantics
        version -- is an integer that specifies the version of this box (0 or 1 in this specification)
        flags -- is a 24‐bit integer with flags; the following values are defined:
            Track_enabled: Indicates that the track is enabled. Flag value is 0x000001. A disabled track (the
                low bit is zero) is treated as if it were not present.
            Track_in_movie: Indicates that the track is used in the presentation. Flag value is 0x000002.
            Track_in_preview: Indicates that the track is used when previewing the presentation. Flag value
                is 0x000004.
            Track_size_is_aspect_ratio: Indicates that the width and height fields are not expressed in
                pixel units. The values have the same units but these units are not specified. The values are
                only an indication of the desired aspect ratio. If the aspect ratios of this track and other
                related tracks are not identical, then the respective positioning of the tracks is undefined,
                possibly defined by external contexts. Flag value is 0x000008.
        creation_time -- is an integer that declares the creation time of this track (in seconds since
            midnight, Jan. 1, 1904, in UTC time).
        modification_time -- is an integer that declares the most recent time the track was modified (in
            seconds since midnight, Jan. 1, 1904, in UTC time).
        track_ID -- is an integer that uniquely identifies this track over the entire life‐time of this
            presentation. Track IDs are never re‐used and cannot be zero.
        duration -- is an integer that indicates the duration of this track (in the timescale indicated in the
            Movie Header Box). The value of this field is equal to the sum of the durations of all of the track’s
            edits. If there is no edit list, then the duration is the sum of the sample durations, converted into
            the timescale in the Movie Header Box. If the duration of this track cannot be determined then
            duration is set to all 1s.
        layer -- specifies the front‐to‐back ordering of video tracks; tracks with lower numbers are closer
            to the viewer. 0 is the normal value, and ‐1 would be in front of track 0, and so on.
        alternate_group -- is an integer that specifies a group or collection of tracks. If this field is 0
            there is no information on possible relations to other tracks. If this field is not 0, it should be the
            same for tracks that contain alternate data for one another and different for tracks belonging to
            different such groups. Only one track within an alternate group should be played or streamed at
            any one time, and must be distinguishable from other tracks in the group via attributes such as
            bitrate, codec, language, packet size etc. A group may have only one member.
        volume -- is a fixed 8.8 value specifying the track's relative audio volume. Full volume is 1.0
            (0x0100) and is the normal value. Its value is irrelevant for a purely visual track. Tracks may be
            composed by combining them according to their volume, and then using the overall Movie
            Header Box volume setting; or more complex audio composition (e.g. MPEG‐4 BIFS) may be used.
        matrix - provides a transformation matrix for the video; (u,v,w) are restricted here to (0,0,1), hex
            (0,0,0x40000000).
        width and height -- fixed‐point 16.16 values are track‐dependent as follows:
            For text and subtitle tracks, they may, depending on the coding format, describe the suggested
                size of the rendering area. For such tracks, the value 0x0 may also be used to indicate that the
                data may be rendered at any size, that no preferred size has been indicated and that the actual
                size may be determined by the external context or by reusing the width and height of another
                track. For those tracks, the flag track_size_is_aspect_ratio may also be used.
            For non‐visual tracks (e.g. audio), they should be set to zero.
            For all other tracks, they specify the track's visual presentation size. These need not be the same
                as the pixel dimensions of the images, which is documented in the sample description(s); all
                images in the sequence are scaled to this size, before any overall transformation of the track
                represented by the matrix. The pixel dimensions of the images are the default values.
     */

    private long creationTime;
    private long modificationTime;
    private long trackId;
    private long duration;
    private int layer;
    private int alternateGroup;
    private int volume; // todo fixed point 8.8
    private long width; // todo fixed point 16.16
    private long height; // todo fixed point 16.16

    TrackHeaderBox(long offset, long length) {
        super(offset, length);
    }

    @Override
    public BoxType boxType() {
        return BoxType.TrackHeader;
    }

    @Override
    void readContent(final BoxReader reader) throws IOException {
        super.readContent(reader);
        if (version == 1) {
            creationTime = reader.readUint64();
            modificationTime = reader.readUint64();
            trackId = reader.readUint32();
            reader.skipBytes(4); // int(32) reserved
            duration = reader.readUint64();
        } else { // version == 0
            creationTime = reader.readUint32();
            modificationTime = reader.readUint32();
            trackId = reader.readUint32();
            reader.skipBytes(4); // int(32) reserved
            duration = reader.readUint32();
        }
        reader.skipBytes(4 * 2); // int(32)[2] reserved
        layer = reader.readUint16();
        alternateGroup = reader.readUint16();
        volume = reader.readUint16();
        reader.skipBytes(2); // int(16) reserved
        reader.skipBytes(4 * 9); // int(32)[9] matrix -- omitted
        width = reader.readUint32();
        height = reader.readUint32();
    }

    @Override
    public String toString() {
        return super.toString() + " trackId=%s, duration=%s, volume=%s, width=%s, height=%s".formatted(
            trackId, duration, volume, width, height);
    }
}
