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

import com.google.common.primitives.Ints;
import com.google.common.primitives.Longs;
import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import local.mylan.media.codec.mp4.boxes.BoxReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractFileReader implements Closeable {
    private static final Logger LOG = LoggerFactory.getLogger(BoxReader.class);

    private final ByteBuffer buffer;
    private final FileChannel channel;
    private long currentOffset = 0;

    protected AbstractFileReader(final Path path, final int bufferSize) throws IOException {
        channel = FileChannel.open(path, StandardOpenOption.READ);
        buffer = ByteBuffer.allocate(bufferSize);
        fillBuffer();
    }

    private void fillBuffer() throws IOException {
        channel.read(buffer);
        buffer.flip();
    }

    public long currentOffset() {
        return currentOffset;
    }

    public long remaining() throws IOException {
        return channel.size() - currentOffset;
    }

    public void setOffset(long newOffset) throws IOException {
        channel.position(newOffset);
        buffer.clear();
        fillBuffer();
        currentOffset = newOffset;
    }

    public byte[] readBytes(final int numOfBytes) throws IOException {
        checkCanRead(numOfBytes);
        final var result = new byte[numOfBytes];
        buffer.get(result);
        currentOffset += numOfBytes;
        return result;
    }

    public byte[] readBytes(final int numOfBytes, final int targetArraySize) throws IOException {
        checkCanRead(numOfBytes);
        final var result = new byte[targetArraySize];
        buffer.get(result, targetArraySize - numOfBytes, numOfBytes);
        currentOffset += numOfBytes;
        return result;
    }

    public void skipBytes(final int numOfBytes) throws IOException {
        checkCanRead(numOfBytes);
        buffer.position(buffer.position() + numOfBytes);
        currentOffset += numOfBytes;
    }

    private void checkCanRead(final long numOfBytes) throws IOException {
        if (numOfBytes > buffer.remaining()) {
            buffer.compact();
            fillBuffer();
        }
        if (numOfBytes > buffer.remaining()) {
            throw new IOException("No remaining data to read. Remaining: %d, requested: %d"
                .formatted(buffer.remaining(), numOfBytes));
        }
    }

    public int readUint8() throws IOException {
        return Ints.fromByteArray(readBytes(1, 4));
    }

    public int readUint16() throws IOException {
        return Ints.fromByteArray(readBytes(2, 4));
    }

    public int readInt32() throws IOException {
        return Ints.fromByteArray(readBytes(4));
    }

    public long readUint32() throws IOException {
        return Longs.fromByteArray(readBytes(4, 8));
    }

    public long readUint64() throws IOException {
        return Longs.fromByteArray(readBytes(8));
    }

    public String readNullTerminatedString(final long limit, final Charset charset) throws IOException {
        final var maxLength = Math.min(limit - currentOffset, buffer.limit());
        checkCanRead(maxLength);
        buffer.mark();
        var terminated = false;
        var length = 0;
        while (buffer.hasRemaining() && length++ < maxLength) {
            if (buffer.get() == 0x00) {
                terminated = true;
                break;
            }
        }
        if (!terminated) {
            throw new IOException(("Null terminated string read failure. No zero byte detected within %d bytes" +
                                   " following initial offset %d").formatted(maxLength, currentOffset));
        }
        buffer.reset();
        final var bytes = new byte[length - 1];
        buffer.get(bytes);
        buffer.position(buffer.position() + 1);
        currentOffset += length;
        return new String(bytes, charset);
    }

    @Override
    public void close() throws IOException {
        channel.close();
    }
}
