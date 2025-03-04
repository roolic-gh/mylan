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
 * File Type Box. Addresses ISO/IEC 14496-12 (4.3 File Type Box).
 */
public class FileTypeBox extends Box {

    /*
        4.3.3 Semantics
        major_brand – is a brand identifier
        minor_version – is an informative integer for the minor version of the major brand
        compatible_brands – is a list, to the end of the box, of brands
     */
    private String majorBrand;
    private long minorVersion;
    private final List<String> compatibleBrands = new ArrayList<>();

    public FileTypeBox(final BoxType boxType, final long offset, final long length) {
        super(boxType, offset, length);
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
    }

    @Override
    public String toString() {
        return super.toString() + " major=%s, minorVer=%s, compatible=%s"
            .formatted(majorBrand, minorVersion, compatibleBrands);
    }
}
