/*
 * Copyright 2026 Ruslan Kashapov
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
package local.transport.netty.smb.handler.codec;

import io.netty.buffer.ByteBuf;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import local.transport.netty.smb.protocol.Flags;
import local.transport.netty.smb.protocol.fscc.FileDirectoryInformation;
import local.transport.netty.smb.protocol.fscc.FileInformation;
import local.transport.netty.smb.protocol.fscc.FileInformationClass;

public final class FsccCodecUtils {

    private FsccCodecUtils() {
        // utility class
    }

    static void encodeFileInformation(ByteBuf byteBuf, final List<? extends FileInformation> fiList) {
        final var lastIndex = fiList.size() - 1;
        int idx = 0;
        for (var fi : fiList) {
            encodeFileInformation(byteBuf, fi, idx++ == lastIndex);
        }
    }

    public static List<FileInformation> decodeFileInformation(final ByteBuf byteBuf, final FileInformationClass fic) {
        final var result = new ArrayList<FileInformation>();
        while (byteBuf.readableBytes() > 8) {
            final var fi = fic.newInstance();
            decodeFileInformation(byteBuf, fi);
            result.add(fi);
        }
        return result;
    }

    private static void decodeFileInformation(final ByteBuf byteBuf, final FileInformation fi) {
        if (fi instanceof FileDirectoryInformation fid) {
            final var pos = byteBuf.readerIndex();
            final var nextEntryOffset = byteBuf.readIntLE();
            fid.setFileIndex(byteBuf.readIntLE());
            fid.setCreationTime(byteBuf.readLongLE());
            fid.setLastAccessTime(byteBuf.readLongLE());
            fid.setLastWriteTime(byteBuf.readLongLE());
            fid.setChangeTime(byteBuf.readLongLE());
            fid.setEndOfFile(byteBuf.readLongLE());
            fid.setAllocationSize(byteBuf.readLongLE());
            fid.setFileAttributes(new Flags<>(byteBuf.readIntLE()));
            final var nameLength = byteBuf.readIntLE();
            fid.setFileName(byteBuf.readCharSequence(nameLength, StandardCharsets.UTF_16LE).toString());
            if (nextEntryOffset > 0) {
                byteBuf.readerIndex(pos + nextEntryOffset);
            }
        } else {
            throw new IllegalStateException("Unsupported FileInformation class " + fi);
        }
    }

    private static void encodeFileInformation(final ByteBuf byteBuf, final FileInformation fi, boolean isLast) {
        if (fi instanceof FileDirectoryInformation fdi) {
            final var pos = byteBuf.writerIndex();
            byteBuf.writeZero(4); // next entry offset
            byteBuf.writeIntLE(fdi.fileIndex());
            byteBuf.writeLongLE(fdi.creationTime());
            byteBuf.writeLongLE(fdi.lastAccessTime());
            byteBuf.writeLongLE(fdi.lastWriteTime());
            byteBuf.writeLongLE(fdi.changeTime());
            byteBuf.writeLongLE(fdi.endOfFile());
            byteBuf.writeLongLE(fdi.allocationSize());
            byteBuf.writeIntLE(fdi.fileAttributes().asIntValue());
            byteBuf.writeIntLE(fdi.fileName().length() * 2);
            byteBuf.writeCharSequence(fdi.fileName(), StandardCharsets.UTF_16LE);
            CodecUtils.alignWriter(byteBuf, pos, 8);
            if (!isLast) {
                byteBuf.setIntLE(pos, byteBuf.writerIndex() - pos);
            }
        } else {
            throw new IllegalStateException("Unsupported FileInformation class " + fi);
        }
    }
}
