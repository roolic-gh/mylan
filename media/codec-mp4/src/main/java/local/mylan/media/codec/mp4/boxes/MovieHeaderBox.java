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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MovieHeaderBox extends FullBox {
    private static final Logger LOG = LoggerFactory.getLogger(MovieHeaderBox.class);

    private long creationTime;
    private long modificationTime;
    private long timescale;
    private long duration;
    private long rate;
    private int volume;
    private byte[] matrix;
    private long nextTrackId;

    MovieHeaderBox(long offset, long length) {
        super(offset, length);
    }

    @Override
    public BoxType boxType() {
        return BoxType.MovieHeader;
    }

    @Override
    void readContent(BoxReader reader) throws IOException {
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
        reader.readBytes(2); // bit(16) reserved
        reader.readBytes(4 * 2); // int(32)[2] reserved
        matrix = reader.readBytes(4 * 9); // int(32)[9]
        reader.readBytes(4 * 6); // bit(32)[6] reserved
        nextTrackId = reader.readUint32();

        LOG.info("parsed -> created={}, timescale={}, duration={}, nextTrackId={}",
            creationTime, timescale, duration, nextTrackId);
    }
}
