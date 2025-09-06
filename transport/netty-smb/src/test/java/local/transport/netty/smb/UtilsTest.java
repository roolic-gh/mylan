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
package local.transport.netty.smb;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import io.netty.buffer.Unpooled;
import java.util.Base64;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class UtilsTest {

    @Test
    void readWriteGuid() {
        final var expected = UUID.fromString("996e16b1-e7fb-9464-6b35-35057de29f07");
        final var readBytes = Base64.getDecoder().decode("sRZumfvnZJRrNTUFfeKfBw==");
        final var readBuf = Unpooled.wrappedBuffer(readBytes);

        final var actual = Utils.readGuid(readBuf);
        assertEquals(expected, actual);

        final var writeBytes = new byte[16];
        final var writeBuf = Unpooled.wrappedBuffer(writeBytes);
        writeBuf.writerIndex(0);

        Utils.writeGuid(writeBuf, actual);
        assertArrayEquals(readBytes, writeBytes);
    }

    @Test
    void filetimeToMillisAndBack() {
        final var millis = 1755237329892L;
        final var filetime = 133997109298920000L;
        assertEquals(filetime, Utils.filetimeFromUnixMillis(millis));
        assertEquals(millis, Utils.unixMillisFromFiletime(filetime));
    }
}
