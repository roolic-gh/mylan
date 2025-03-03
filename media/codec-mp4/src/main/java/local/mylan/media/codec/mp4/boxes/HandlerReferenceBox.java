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

/**
 * Handler Reference Box. Addresses ISO/IEC 14496-12 (8.4.3 Handler Reference Box).
 */
public class HandlerReferenceBox extends FullBox {
    /*
        8.4.3.3 Semantics
        version -- is an integer that specifies the version of this box
        handler_type
            – when present in a media box, contains a value as defined in clause 12, or a value from a derived
            specification, or registration.
            - when present in a meta box, contains an appropriate value to indicate the format of the meta
            box contents. The value ‘null’ can be used in the primary meta box to indicate that it is
            merely being used to hold resources.
        name -- is a null‐terminated string in UTF‐8 characters which gives a human‐readable name for the
            track type (for debugging and inspection purposes).
     */
    private String handlerType;
    private String name;

    HandlerReferenceBox(final long offset, final long length) {
        super(offset, length);
    }

    @Override
    public BoxType boxType() {
        return BoxType.HandlerReference;
    }

    @Override
    void readContent(final BoxReader reader) throws IOException {
        super.readContent(reader);
        reader.skipBytes(4); // int(32) pre-defined
        handlerType = reader.read4CharCode();
        reader.skipBytes(4 * 3); // int(32)[3] reserved
        name = reader.readNullTerminatedString(limit, StandardCharsets.UTF_8);
    }

    @Override
    public String toString() {
        return super.toString() + " type=%s, name=%s".formatted(handlerType, name);
    }
}
