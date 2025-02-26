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

public abstract class Box {

    private final long offset;
    private final long length;
    protected final long limit;

    protected Box(long offset, long length) {
        this.offset = offset;
        this.length = length;
        limit = offset + length;
    }

    public abstract BoxType boxType();

    final long offset() {
        return offset;
    }

    final long length() {
        return length;
    }

    void readContent(final BoxReader reader) throws IOException {
        // ignore content by default
    }

}
