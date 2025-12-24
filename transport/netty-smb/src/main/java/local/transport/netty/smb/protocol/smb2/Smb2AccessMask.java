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
package local.transport.netty.smb.protocol.smb2;

import local.transport.netty.smb.protocol.Flags;

/**
 * SMB2 Access Mask Flags. Addresses MS-SMB2 (#2.2.13.1 SMB2 Access Mask Encoding).
 */
public enum Smb2AccessMask implements Flags.BitMaskProvider {

    // Files, pipes and printers (#2.2.13.1.1 File_Pipe_Printer_Access_Mask).

    FILE_READ_DATA(0x00000001),
    FILE_WRITE_DATA(0x00000002),
    FILE_APPEND_DATA(0x00000004),
    FILE_READ_EA(0x00000008),
    FILE_WRITE_EA(0x00000010),
    FILE_EXECUTE(0x00000020),
    FILE_READ_ATTRIBUTES(0x00000080),
    FILE_WRITE_ATTRIBUTES(0x00000100),
    DELETE(0x00010000),
    READ_CONTROL(0x00020000),
    WRITE_DAC(0x00040000),
    WRITE_OWNER(0x00080000),
    SYNCHRONIZE(0x00100000),
    ACCESS_SYSTEM_SECURITY(0x01000000),
    MAXIMUM_ALLOWED(0x02000000),
    GENERIC_ALL(0x10000000),
    GENERIC_EXECUTE(0x20000000),
    GENERIC_WRITE(0x40000000),
    GENERIC_READ(0x80000000),

    // Directories (#2.2.13.1.2 Directory_Access_Mask).

    FILE_LIST_DIRECTORY(0x00000001),
    FILE_ADD_FILE(0x00000002),
    FILE_ADD_SUBDIRECTORY(0x00000004),
    FILE_TRAVERSE(0x00000020),
    FILE_DELETE_CHILD(0x00000040);

    private final int mask;

    Smb2AccessMask(int mask) {
        this.mask = mask;
    }

    @Override
    public int mask() {
        return mask;
    }
}
