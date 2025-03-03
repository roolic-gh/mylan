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
package local.mylan.media.codec.mp4;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import local.mylan.media.codec.mp4.boxes.Box;
import local.mylan.media.codec.mp4.boxes.BoxReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Mp4FileStructure {
    private static final Logger LOG = LoggerFactory.getLogger(Mp4FileStructure.class);

    final Path filePath;
    final List<Box> rootBoxes;

    public Mp4FileStructure(final Path filePath, final int bufferSize) throws IOException {
        this.filePath = filePath;
        try (var reader = new BoxReader(filePath, bufferSize)) {
            rootBoxes = reader.readBoxes();
        }
    }

    public List<Box> rootBoxes() {
        return List.copyOf(rootBoxes);
    }

    public void trace() {
        trace("", rootBoxes);
    }

    private static void trace(final String indent, final List<Box> boxes) {
        if (boxes.isEmpty()) {
            return;
        }
        for (var box : boxes) {
            LOG.info("{} - {}", indent, box);
            trace(indent + "    ", box.subBoxes());
        }
    }
}
