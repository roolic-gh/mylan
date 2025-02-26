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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileTypeBox extends Box {
    private static final Logger LOG = LoggerFactory.getLogger(FileTypeBox.class);

    private String majorBrand;
    private long minorVersion;
    private final List<String> compatibleBrands = new ArrayList<>();

    public FileTypeBox(final long offset, final long length) {
        super(offset, length);
    }

    @Override
    public BoxType boxType() {
        return BoxType.FileType;
    }

    public String majorBrand() {
        return majorBrand;
    }

    public long minorVersion() {
        return minorVersion;
    }

    public List<String> compatibleBrands() {
        return List.copyOf(compatibleBrands);
    }

    @Override
    void readContent(final BoxReader reader) throws IOException {
        majorBrand = reader.read4CharCode();
        minorVersion = reader.readUint32();
        while (reader.currentOffset() < limit) {
            compatibleBrands.add(reader.read4CharCode());
        }
        LOG.info("parsed -> majorBrand={}, minorVersion={}, compatibleBrands={}",
            majorBrand, minorVersion, compatibleBrands);
    }
}
