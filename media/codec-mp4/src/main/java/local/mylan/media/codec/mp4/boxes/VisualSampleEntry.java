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
import local.mylan.media.codec.mp4.FixedPoint;

/**
 * Visual Sample Entry. Addresses ISO/IEC 14496-12 (12.1.3 Sample entry).
 */
public class VisualSampleEntry extends SampleEntry {

    /*
    12.1.3.3 Semantics
        resolution fields -- give the resolution of the image in pixels‐per‐inch, as a fixed 16.16 number
        frame_count -- indicates how many frames of compressed video are stored in each sample. The
            default is 1, for one frame per sample; it may be more than 1 for multiple frames per sample
        Compressorname -- is a name, for informative purposes. It is formatted in a fixed 32‐byte field, with
            the first byte set to the number of bytes to be displayed, followed by that number of bytes of
            displayable data, and then padding to complete 32 bytes total (including the size byte). The field
            may be set to 0.
        depth -- takes one of the following values
            0x0018 – images are in colour with no alpha
        width and height--  are the maximum visual width and height of the stream described by this
            sample description, in pixels
     */
    private int width;
    private int height;
    private FixedPoint horizResolution;
    private FixedPoint vertResolution;
    private int frameCount;
    private String compressorName;
    private int depth;

    protected VisualSampleEntry(final BoxType boxType, final long offset, final long length) {
        super(boxType, offset, length);
    }

    @Override
    void readContent(final BoxReader reader) throws IOException {
        super.readContent(reader);
        reader.skipBytes(2); // int(16) predefined
        reader.skipBytes(2); // int(16) reserved
        reader.skipBytes(4*3); // int(32)[3] predefined
        width = reader.readUint16();
        height = reader.readUint16();
        horizResolution = reader.readFixedPoint(2,2);
        vertResolution = reader.readFixedPoint(2,2);
        reader.skipBytes(2); // int(32) reserved
        frameCount = reader.readUint16();
        compressorName = reader.readFixedLengthString(32);
        depth = reader.readUint16();
        reader.skipBytes(2); // int(16) predefined
    }

    @Override
    public String toString() {
        return super.toString() + " width=%d, height=%d, hRes=%s, vRes=%s, compressor=%s, frameCount=%s, depth=%s"
            .formatted(width, height, horizResolution, vertResolution, compressorName, frameCount, depth);
    }
}
