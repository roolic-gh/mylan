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
package local.mylan.transport.smb.handler.codec;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;
import local.mylan.transport.smb.protocol.fscc.Blob;
import local.mylan.transport.smb.protocol.srvs.SrvShareEnumStruct;
import local.mylan.transport.smb.protocol.srvs.SrvsError;
import local.mylan.transport.smb.protocol.srvs.SrvsMessage;
import local.mylan.transport.smb.protocol.srvs.SrvsNetrShareEnum;
import local.mylan.transport.smb.protocol.srvs.SrvsShareInfo;
import local.mylan.transport.smb.protocol.srvs.SrvsShareInfoLevel;
import local.mylan.transport.smb.protocol.srvs.SrvsShareType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class SrvsCodecUtils {
    private static final Logger LOG = LoggerFactory.getLogger(SrvsCodecUtils.class);

    private SrvsCodecUtils() {
        // utility class
    }

    static void encodeRequest(final ByteBuf byteBuf, final SrvsMessage srvsMessage) {
        encode(byteBuf, srvsMessage, Dir.INBOUND);
    }

    static void encodeResponse(final ByteBuf byteBuf, final SrvsMessage srvsMessage) {
        encode(byteBuf, srvsMessage, Dir.OUTBOUND);
    }

    private static void encode(final ByteBuf byteBuf, final SrvsMessage srvsMessage, final Dir direction) {
        if (srvsMessage instanceof SrvsNetrShareEnum netShareEnum) {
            encodeNetShareEnum(byteBuf, netShareEnum, direction);
            return;
        }
        throw new IllegalArgumentException("Unsupported SRVS Message type " + srvsMessage);
    }

    static Object decodeRequest(final ByteBuf byteBuf, final int opnum) {
        return decode(byteBuf, opnum, Dir.INBOUND);
    }

    public static Object decodeResponse(final ByteBuf byteBuf, final int opnum) {
        return decode(byteBuf, opnum, Dir.OUTBOUND);
    }

    private static Object decode(final ByteBuf byteBuf, final int opnum, final Dir direction) {
        try {
            if (opnum == SrvsNetrShareEnum.OPNUM) {
                return decodeNetShareEnum(byteBuf, direction);
            }
        } catch (Exception e) {
            LOG.error("Error decoding SRVS message", e);
        }
        return new Blob(ByteBufUtil.getBytes(byteBuf));
    }

    // MS-SRVS #3.1.4.8 NetrShareEnum (Opnum 15)

    private static void encodeNetShareEnum(final ByteBuf byteBuf, final SrvsNetrShareEnum netShareEnum,
        final Dir direction) {

        final int startPos = byteBuf.writerIndex();
        final var rc = new RefCount();
        if (direction == Dir.INBOUND) {
            writeStringPointer(byteBuf, netShareEnum.serverName(), rc, startPos).writeValue();
        }
        writeShareInfoStruct(byteBuf, netShareEnum.infoStruct(), rc, startPos);
        byteBuf.writeIntLE(
            direction == Dir.INBOUND ? netShareEnum.preferedMaximumLength() : netShareEnum.totalEntries());
        // resume handle
        byteBuf.writeIntLE(rc.next());
        byteBuf.writeIntLE(netShareEnum.resumeHandle());
        if (direction == Dir.OUTBOUND) {
            byteBuf.writeIntLE(netShareEnum.error().code());
        }
    }

    private static SrvsNetrShareEnum decodeNetShareEnum(final ByteBuf byteBuf, final Dir direction) {
        final int startPos = byteBuf.readerIndex();
        final var netShareEnum = new SrvsNetrShareEnum();
        if (direction == Dir.INBOUND) {
            netShareEnum.setServerName(readStringPointer(byteBuf, startPos).readValue());
        }
        netShareEnum.setInfoStruct(readShareInfoStruct(byteBuf, startPos));
        if (direction == Dir.INBOUND) {
            netShareEnum.setPreferedMaximumLength(byteBuf.readIntLE());
        } else {
            netShareEnum.setTotalEntries(byteBuf.readIntLE());
        }
        // resume handle
        byteBuf.skipBytes(4); // ignore referent Id
        netShareEnum.setResumeHandle(byteBuf.readIntLE());
        if (direction == Dir.OUTBOUND) {
            netShareEnum.setError(SrvsError.fromCode(byteBuf.readIntLE()));
        }
        return netShareEnum;
    }

    private static void writeShareInfoStruct(final ByteBuf byteBuf, final SrvShareEnumStruct struct,
        final RefCount rc, final int startPos) {

        final var level = struct.level();
        byteBuf.writeIntLE(level.code());
        byteBuf.writeIntLE(1); // number of entries (containers ?)
        byteBuf.writeIntLE(rc.next()); // struct referent id
        final var count = struct.infos().size();
        byteBuf.writeIntLE(count);
        if (count == 0) {
            byteBuf.writeIntLE(0); // array value is null
            return;
        }
        byteBuf.writeIntLE(rc.next()); // array referent id
        byteBuf.writeIntLE(count); // array size
        final var writers = struct.infos().stream()
            .map(info -> writeShareInfo(byteBuf, info, level, rc, startPos)).toList();
        writers.forEach(ValueWriter::writeValue);
    }

    private static SrvShareEnumStruct readShareInfoStruct(final ByteBuf byteBuf, final int startPos) {
        final var level = SrvsShareInfoLevel.fromCode(byteBuf.readIntLE());
        byteBuf.skipBytes(8); // number of entries (containers ?) + struct referent id (ignored)
        final var count = byteBuf.readIntLE();
        if (count == 0) {
            byteBuf.skipBytes(4); // array value (null)
            return new SrvShareEnumStruct(level, List.of());
        }
        byteBuf.skipBytes(8); // array referent id + array size (same as count)
        final var readers = IntStream.range(0, count)
            .mapToObj(num -> readShareInfo(byteBuf, level, startPos)).toList();
        final var infos = readers.stream().map(ValueReader::readValue).toList();
        return new SrvShareEnumStruct(level, infos);
    }

    private static ValueWriter writeShareInfo(final ByteBuf byteBuf, final SrvsShareInfo info,
        final SrvsShareInfoLevel level, final RefCount rc, final int startPos) {

        final var netNameWriter = writeStringPointer(byteBuf, info.netName(), rc, startPos);
        return switch (level) {
            case SHARE_INFO_0 -> netNameWriter;
            case SHARE_INFO_1 -> {
                byteBuf.writeIntLE(info.type().code());
                final var remarkWriter = writeStringPointer(byteBuf, info.remark(), rc, startPos);
                yield () -> {
                    netNameWriter.writeValue();
                    remarkWriter.writeValue();
                };
            }
            default -> throw new IllegalStateException(
                String.format("Encoding of ShareInfo of level %s is not supported", level.name()));
        };
    }

    private static ValueReader<SrvsShareInfo> readShareInfo(final ByteBuf byteBuf, final SrvsShareInfoLevel level,
        final int startPos) {

        final var info = new SrvsShareInfo();
        final var netNameReader = readStringPointer(byteBuf, startPos);
        return switch (level) {
            case SHARE_INFO_0 -> () -> {
                info.setNetName(netNameReader.readValue());
                return info;
            };
            case SHARE_INFO_1 -> {
                info.setType(new SrvsShareType(byteBuf.readIntLE()));
                final var remarkReader = readStringPointer(byteBuf, startPos);
                yield () -> {
                    info.setNetName(netNameReader.readValue());
                    info.setRemark(remarkReader.readValue());
                    return info;
                };
            }
            default -> throw new IllegalStateException(
                String.format("Decoding of ShareInfo of level %s is not supported", level.name()));
        };
    }

    private static ValueWriter writeStringPointer(final ByteBuf byteBuf, final String str, final RefCount rc,
        final int startPos) {

        byteBuf.writeIntLE(rc.next());
        final var length = str.length() + 1;
        return () -> {
            byteBuf.writeIntLE(length); // max size
            byteBuf.writeZero(4); // offset
            byteBuf.writeIntLE(length); // actual size
            byteBuf.writeCharSequence(str, StandardCharsets.UTF_16LE);
            byteBuf.writeZero(2); // trailing zero char
            alignWriter(byteBuf, startPos);
        };
    }

    private static ValueReader<String> readStringPointer(final ByteBuf byteBuf, final int startPos) {
        byteBuf.skipBytes(4); // referent id (ignored)
        return () -> {
            byteBuf.skipBytes(8); // max length (same as length expected) + offset (always 0 expected)
            final var length = byteBuf.readIntLE();
            final var value = byteBuf.readCharSequence((length - 1) * 2, StandardCharsets.UTF_16LE).toString();
            byteBuf.skipBytes(2); // trailing zero char
            alignReader(byteBuf, startPos);
            return value;
        };
    }

    private static void alignWriter(final ByteBuf byteBuf, final int startPos) {
        CodecUtils.alignWriter(byteBuf, startPos, 4);
    }

    static void alignReader(final ByteBuf byteBuf, final int startPos) {
        CodecUtils.alignReader(byteBuf, startPos, 4);
    }

    private enum Dir {INBOUND, OUTBOUND}

    static class RefCount {

        private final AtomicInteger count = new AtomicInteger(0x00010000);

        int next() {
            return count.incrementAndGet();
        }
    }

    @FunctionalInterface
    interface ValueWriter {

        void writeValue();
    }

    @FunctionalInterface
    interface ValueReader<T> {
        T readValue();
    }
}








