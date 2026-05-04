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
import local.transport.netty.smb.protocol.Smb2Command;
import local.transport.netty.smb.protocol.Smb2Header;
import local.transport.netty.smb.protocol.Smb2Response;
import local.transport.netty.smb.protocol.fscc.FileAttributeFlags;

/**
 * Addresses MS-SMB2 (#2.2.16 SMB2 CLOSE Response).
 */

public final class Smb2CloseResponse extends Smb2Response {

    private Flags<Smb2CloseFlags> flags;
    private long creationTime;
    private long lastAccessTime;
    private long lastWriteTime;
    private long changeTime;
    private long allocationSize;
    private long endOfFile;
    private Flags<FileAttributeFlags> fileAttributes;

    public Smb2CloseResponse() {
        // default
    }

    public Smb2CloseResponse(final Smb2Header header) {
        super(header);
    }

    @Override
    protected Smb2Command command() {
        return Smb2Command.SMB2_CLOSE;
    }

    public Flags<Smb2CloseFlags> flags() {
        return flags;
    }

    public void setFlags(final Flags<Smb2CloseFlags> flags) {
        this.flags = flags;
    }

    public long creationTime() {
        return creationTime;
    }

    public void setCreationTime(final long creationTime) {
        this.creationTime = creationTime;
    }

    public long lastAccessTime() {
        return lastAccessTime;
    }

    public void setLastAccessTime(final long lastAccessTime) {
        this.lastAccessTime = lastAccessTime;
    }

    public long lastWriteTime() {
        return lastWriteTime;
    }

    public void setLastWriteTime(final long lastWriteTime) {
        this.lastWriteTime = lastWriteTime;
    }

    public long changeTime() {
        return changeTime;
    }

    public void setChangeTime(final long changeTime) {
        this.changeTime = changeTime;
    }

    public long allocationSize() {
        return allocationSize;
    }

    public void setAllocationSize(final long allocationSize) {
        this.allocationSize = allocationSize;
    }

    public long endOfFile() {
        return endOfFile;
    }

    public void setEndOfFile(final long endOfFile) {
        this.endOfFile = endOfFile;
    }

    public Flags<FileAttributeFlags> fileAttributes() {
        return fileAttributes;
    }

    public void setFileAttributes(
        final Flags<FileAttributeFlags> fileAttributes) {
        this.fileAttributes = fileAttributes;
    }
}
