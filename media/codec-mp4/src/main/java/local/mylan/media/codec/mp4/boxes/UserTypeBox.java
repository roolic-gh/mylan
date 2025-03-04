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
import java.util.UUID;

public class UserTypeBox extends Box {

    private UUID userType;

    UserTypeBox(final BoxType boxType, final long offset, final long length) {
        super(boxType, offset, length);
    }

    public UUID userType() {
        return userType;
    }

    @Override
    void readContent(final BoxReader reader) throws IOException {
        userType = UUID.nameUUIDFromBytes(reader.readBytes(16));
    }
}
