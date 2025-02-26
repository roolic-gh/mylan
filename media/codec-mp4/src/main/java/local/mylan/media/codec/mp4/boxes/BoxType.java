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

public enum BoxType {
    FileType("ftyp", FileTypeBox::new),
    MediaData("mdat", MediaDataBox::new),
    Movie("moov", MovieBox::new),
    MovieHeader("mvhd", MovieHeaderBox::new),
    UserType("uuid", UserTypeBox::new),
    Unhandled("", null);

    private final String boxType;
    private final BoxBuilder boxBuilder;

    BoxType(final String boxType, final BoxBuilder boxBuilder) {
        this.boxType = boxType;
        this.boxBuilder = boxBuilder;
    }

    static BoxType from(final String boxType) {
        for (var type : values()) {
            if (type.boxType.equals(boxType)) {
                return type;
            }
        }
        return Unhandled;
    }

    Box newBox(final long offset, final long length) {
        return boxBuilder.build(offset, length);
    }

    @FunctionalInterface
    interface BoxBuilder {
        Box build(long offset, long length);
    }
}
