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
    HandlerReference("hdlr", HandlerReferenceBox::new),
    Media("mdia", ContainerBox::typed),
    MediaData("mdat", MediaDataBox::new),
    MediaHeader("mdhd", MediaHeaderBox::new),
    MediaInformation("minf", ContainerBox::typed),
    Movie("moov", ContainerBox::typed),
    MovieHeader("mvhd", MovieHeaderBox::new),
    Track("trak", ContainerBox::typed),
    TrackHeader("tkhd", TrackHeaderBox::new),
    UserType("uuid", UserTypeBox::new),
    Unhandled("", (BoxBuilder) null);

    private final String boxType;
    private final BoxBuilder boxBuilder;

    BoxType(final String boxType, final BoxBuilder boxBuilder) {
        this.boxType = boxType;
        this.boxBuilder = boxBuilder;
    }

    BoxType(final String boxType, final TypedBoxBuilder typedBoxBuilder) {
        this.boxType = boxType;
        boxBuilder = (offset, length) -> typedBoxBuilder.build(this, offset, length);
    }

    Box newBox(final long offset, final long length) {
        return boxBuilder.build(offset, length);
    }

    @Override
    public String toString() {
        return "%s(%s)".formatted(name(), boxType);
    }

    static BoxType from(final String boxType) {
        for (var type : values()) {
            if (type.boxType.equals(boxType)) {
                return type;
            }
        }
        return Unhandled;
    }

    @FunctionalInterface
    interface BoxBuilder {
        Box build(long offset, long length);
    }

    @FunctionalInterface
    interface TypedBoxBuilder {
        Box build(BoxType boxType, long offset, long length);
    }
}
