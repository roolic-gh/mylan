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
package local.transport.netty.smb.protocol;

import static local.transport.netty.smb.protocol.ProtocolVersion.CIFS_SMB;
import static local.transport.netty.smb.protocol.ProtocolVersion.SMB2;

/**
 * SMB Command codes. Addresses MS-CIFS (#2.2.2.1 SMB_COM Command Codes)
 */
public enum SmbCommand {

    // MS-CIFS - obsolete and non-implemented commands are omitted
    @Deprecated SMB_COM_CREATE_DIRECTORY(0x00, CIFS_SMB),
    SMB_COM_DELETE_DIRECTORY(0x01, CIFS_SMB),
    @Deprecated SMB_COM_OPEN(0x02, CIFS_SMB),
    @Deprecated SMB_COM_CREATE(0x03, CIFS_SMB),
    SMB_COM_CLOSE(0x04, CIFS_SMB),
    SMB_COM_FLUSH(0x05, CIFS_SMB),
    SMB_COM_DELETE(0x06, CIFS_SMB),
    SMB_COM_RENAME(0x07, CIFS_SMB),
    @Deprecated SMB_COM_QUERY_INFORMATION(0x08, CIFS_SMB),
    @Deprecated SMB_COM_SET_INFORMATION(0x09, CIFS_SMB),
    @Deprecated SMB_COM_READ(0x0A, CIFS_SMB),
    @Deprecated SMB_COM_WRITE(0x0B, CIFS_SMB),
    @Deprecated SMB_COM_LOCK_BYTE_RANGE(0x0C, CIFS_SMB),
    @Deprecated SMB_COM_UNLOCK_BYTE_RANGE(0x0D, CIFS_SMB),
    @Deprecated SMB_COM_CREATE_NEW(0x0F, CIFS_SMB),
    SMB_COM_CHECK_DIRECTORY(0x10, CIFS_SMB),
    @Deprecated SMB_COM_LOCK_AND_READ(0x13, CIFS_SMB),
    @Deprecated SMB_COM_WRITE_AND_UNLOCK(0x14, CIFS_SMB),
    @Deprecated SMB_COM_READ_RAW(0x1A, CIFS_SMB),
    @Deprecated SMB_COM_WRITE_RAW(0x1D, CIFS_SMB),
    @Deprecated SMB_COM_WRITE_COMPLETE(0x20, CIFS_SMB),
    @Deprecated SMB_COM_SET_INFORMATION2(0x22, CIFS_SMB),
    @Deprecated SMB_COM_QUERY_INFORMATION2(0x23, CIFS_SMB),
    SMB_COM_LOCKING_ANDX(0x24, CIFS_SMB),
    SMB_COM_TRANSACTION(0x25, CIFS_SMB),
    SMB_COM_TRANSACTION_SECONDARY(0x26, CIFS_SMB),
    SMB_COM_ECHO(0x2B, CIFS_SMB),
    @Deprecated SMB_COM_WRITE_AND_CLOSE(0x2C, CIFS_SMB),
    @Deprecated SMB_COM_OPEN_ANDX(0x2D, CIFS_SMB),
    SMB_COM_READ_ANDX(0x2E, CIFS_SMB),
    SMB_COM_WRITE_ANDX(0x2F, CIFS_SMB),
    SMB_COM_TRANSACTION2(0x32, CIFS_SMB),
    SMB_COM_TRANSACTION2_SECONDARY(0x33, CIFS_SMB),
    SMB_COM_FIND_CLOSE2(0x34, CIFS_SMB),
    @Deprecated SMB_COM_TREE_CONNECT(0x70, CIFS_SMB),
    SMB_COM_TREE_DISCONNECT(0x71, CIFS_SMB),
    SMB_COM_NEGOTIATE(0x72, CIFS_SMB),
    SMB_COM_SESSION_SETUP_ANDX(0x73, CIFS_SMB),
    SMB_COM_LOGOFF_ANDX(0x74, CIFS_SMB),
    SMB_COM_TREE_CONNECT_ANDX(0x75, CIFS_SMB),
    @Deprecated SMB_COM_QUERY_INFORMATION_DISK(0x80, CIFS_SMB),
    @Deprecated SMB_COM_SEARCH(0x81, CIFS_SMB),
    @Deprecated SMB_COM_FIND(0x82, CIFS_SMB),
    @Deprecated SMB_COM_FIND_UNIQUE(0x83, CIFS_SMB),
    @Deprecated SMB_COM_FIND_CLOSE(0x84, CIFS_SMB),
    SMB_COM_NT_TRANSACT(0xA0, CIFS_SMB),
    SMB_COM_NT_TRANSACT_SECONDARY(0xA1, CIFS_SMB),
    SMB_COM_NT_CREATE_ANDX(0xA2, CIFS_SMB),
    SMB_COM_NT_CANCEL(0xA4, CIFS_SMB),
    SMB_COM_OPEN_PRINT_FILE(0xC0, CIFS_SMB),
    @Deprecated SMB_COM_WRITE_PRINT_FILE(0xC1, CIFS_SMB),
    @Deprecated SMB_COM_CLOSE_PRINT_FILE(0xC2, CIFS_SMB),
    SMB_COM_INVALID(0xFE, CIFS_SMB),
    SMB_COM_NO_ANDX_COMMAND(0xFF, CIFS_SMB),

    // MS-SMB2
    SMB2_NEGOTIATE(0x0000, SMB2),
    SMB2_SESSION_SETUP(0x0001, SMB2),
    SMB2_LOGOFF(0x0002, SMB2),
    SMB2_TREE_CONNECT(0x0003, SMB2),
    SMB2_TREE_DISCONNECT(0x0004, SMB2),
    SMB2_CREATE(0x0005, SMB2),
    SMB2_CLOSE(0x0006, SMB2),
    SMB2_FLUSH(0x0007, SMB2),
    SMB2_READ(0x0008, SMB2),
    SMB2_WRITE(0x0009, SMB2),
    SMB2_LOCK(0x000A, SMB2),
    SMB2_IOCTL(0x000B, SMB2),
    SMB2_CANCEL(0x000C, SMB2),
    SMB2_ECHO(0x000D, SMB2),
    SMB2_QUERY_DIRECTORY(0x000E, SMB2),
    SMB2_CHANGE_NOTIFY(0x000F, SMB2),
    SMB2_QUERY_INFO(0x0010, SMB2),
    SMB2_SET_INFO(0x0011, SMB2),
    SMB2_OPLOCK_BREAK(0x0012, SMB2),
    SMB2_SERVER_TO_CLIENT_NOTIFICATION(0x0013, SMB2);

    private final int code;
    private final ProtocolVersion protocolVersion;

    SmbCommand(final int code, final ProtocolVersion protocolVersion) {
        this.code = code;
        this.protocolVersion = protocolVersion;
    }

    public int code() {
        return code;
    }

    public ProtocolVersion protocolVersion() {
        return protocolVersion;
    }

    public static SmbCommand fromCode(final int code, final ProtocolVersion protocolVersion) {
        for (var cmd : values()) {
            if (cmd.code == code && cmd.protocolVersion == protocolVersion) {
                return cmd;
            }
        }
        throw new IllegalArgumentException("unknown command code " + code + " for ptotocol " + protocolVersion);
    }

}
