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
import java.util.ArrayList;
import java.util.List;

/**
 * Base Box which only contains other boxes.
 */
public abstract class ContainerFullBox extends FullBox {
    protected final List<Box> subBoxes = new ArrayList<>();

    ContainerFullBox(long offset, long length) {
        super(offset, length);
    }

    @Override
    public final List<Box> subBoxes() {
        return List.copyOf(subBoxes);
    }

    @Override
    void readContent(final BoxReader reader) throws IOException {
        super.readContent(reader);
        final var count = reader.readInt32();
        for(int i = 0; i < count; i++) {
            subBoxes.add(reader.readBox());
        }
    }

    static ContainerFullBox typed(final BoxType boxType, final long offset, final long length) {
        return new ContainerFullBox(offset, length) {
            @Override
            public BoxType boxType() {
                return boxType;
            }
        };
    }
}
