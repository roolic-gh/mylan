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

import static com.google.common.base.Preconditions.checkArgument;

import java.io.IOException;

/**
 * Chunk Offset Box. Addresses ISO/IEC 14496-12 (8.7.5 Chunk Offset Box)
 */
public class ChunkOffsetBox extends FullBox {

    private final BoxType type;
    private long[] chunkOffsets;

    ChunkOffsetBox(final BoxType type, final long offset, final long length) {
        super(offset, length);
        checkArgument(type == BoxType.ChunkOffset || type == BoxType.ChunkLargeOffset);
        this.type = type;
    }

    @Override
    public BoxType boxType() {
        return type;
    }

    @Override
    void readContent(final BoxReader reader) throws IOException {
        super.readContent(reader);
        final var count = reader.readInt32();
        chunkOffsets = new long[count];
        final var isUint32 = type == BoxType.ChunkOffset;
        for (int i = 0; i < count; i++) {
            chunkOffsets[i] = isUint32 ? reader.readUint32() : reader.readUint64();
        }
    }

    @Override
    public String toString() {
        return super.toString() + " count=%d".formatted(chunkOffsets.length);
    }
}
