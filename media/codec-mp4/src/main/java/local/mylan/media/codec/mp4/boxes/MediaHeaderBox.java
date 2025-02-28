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
import java.nio.charset.StandardCharsets;
import local.mylan.media.codec.mp4.BcdUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Media Header Box. Addresses ISO/IEC 14496-12 (8.4.2. Media Header Box)
 */
public class MediaHeaderBox extends FullBox {
    private static final Logger LOG = LoggerFactory.getLogger(MediaHeaderBox.class);

    /*
        8.4.2.3 Semantics
            version -- is an integer that specifies the version of this box (0 or 1)
            creation_time -- is an integer that declares the creation time of the media in this track (in
                seconds since midnight, Jan. 1, 1904, in UTC time).
            modification_time -- is an integer that declares the most recent time the media in this track was
                modified (in seconds since midnight, Jan. 1, 1904, in UTC time).
            timescale -- is an integer that specifies the time‐scale for this media; this is the number of time
                units that pass in one second. For example, a time coordinate system that measures time in
                sixtieths of a second has a time scale of 60.
            duration --  is an integer that declares the duration of this media (in the scale of the timescale). If the
                duration cannot be determined then duration is set to all 1s.
            language -- declares the language code for this media. See ISO 639‐2/T for the set of three
                character codes. Each character is packed as the difference between its ASCII value and 0x60.
                Since the code is confined to being three lower‐case letters, these values are strictly positive.
     */
    private long creationTime;
    private long modificationTime;
    private long timescale;
    private long duration;
    private String language;

    MediaHeaderBox(final long offset, final long length) {
        super(offset, length);
    }

    @Override
    public BoxType boxType() {
        return BoxType.MediaHeader;
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
        // bit(1) + int(5)[3]
        final var langBytes = BcdUtils.decodeByteSequence(reader.readBytes(2), 5);
        for (int i = 0; i < langBytes.length; i++) {
            langBytes[i] += 0x60;
        }
        language = new String(langBytes, StandardCharsets.ISO_8859_1);
        reader.skipBytes(2); // int(16) pre-defined

        LOG.info("parsed -> timescale={}, duration={}, language={}", timescale, duration, language);
    }
}
