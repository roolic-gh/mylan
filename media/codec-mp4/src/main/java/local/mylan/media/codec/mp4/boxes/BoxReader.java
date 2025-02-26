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
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import local.mylan.media.codec.mp4.AbstractFileReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class BoxReader extends AbstractFileReader {
    private static final Logger LOG = LoggerFactory.getLogger(BoxReader.class);

    public BoxReader(final Path path) throws IOException {
        super(path, 1024);
    }

    public List<Box> readBoxes() throws IOException {
        return readBoxes(remaining());
    }

    public List<Box> readBoxes(final long uptoPos) throws IOException {
        final var boxes = new ArrayList<Box>();
        while (currentOffset() < uptoPos) {
            final var box = readBox();
            if (box == null) {
                break;
            } else {
                boxes.add(box);
            }
        }
        return List.copyOf(boxes);
    }

    public String read4CharCode() throws IOException {
        return new String(readBytes(4), StandardCharsets.ISO_8859_1);
    }

    private Box readBox() throws IOException {
        final var boxOffset = currentOffset();
        final var remaining = remaining();
        var size = readUint32();
        var boxType = new String(readBytes(4), StandardCharsets.ISO_8859_1);
        if (size == 1) {
            size = readUint64();
        } else if (size == 0) {
            size = remaining;
        }
        final var type = BoxType.from(boxType);
        final var box = type == BoxType.Unhandled
            ? new UnhandledBox(boxType, boxOffset, size)
            : type.newBox(boxOffset, size);
        box.readContent(this);
        LOG.info("parsed -> {} (offset: {}, size: {})", boxType, boxOffset, size);

        // validate offset after box being parsed (or content skipped)
        final var parsedSize = currentOffset() - boxOffset;
        final var expectedOffset = boxOffset + size;
        if (parsedSize < size) {
            setOffset(expectedOffset);
        } else if (parsedSize > size) {
            throw new IOException("error parsing box %s at offset %d. Actual length (%d) exceeds expected (%d)"
                .formatted(boxType, boxOffset, parsedSize, size));
        }
        return box;
    }
}
