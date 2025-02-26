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

public final class Mp4FileStructure {

    final Path filePath;
    final List<Box> rootBoxes;

    public Mp4FileStructure(Path filePath) throws IOException {
        this.filePath = filePath;
        try (var reader = new BoxReader(filePath)) {
            rootBoxes = reader.readBoxes();
        }
    }
    public List<Box> rootBoxes() {
        return List.copyOf(rootBoxes);
    }
}
