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
import java.util.BitSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class FullBox extends Box {
    private static final Logger LOG = LoggerFactory.getLogger(FullBox.class);

    protected int version;
    protected BitSet flags;

    protected FullBox(final long offset, final long length) {
        super(offset, length);
    }

    @Override
    void readContent(final BoxReader reader) throws IOException {
        version = reader.readUint8();
        flags = BitSet.valueOf(reader.readBytes(3));
        LOG.info("parsed -> version={}, flags={}", version, flags);
    }
}
