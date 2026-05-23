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
package local.mylan.transport.smb.protocol.smb2;

import java.util.UUID;
import local.mylan.transport.smb.protocol.Flags;
import local.mylan.transport.smb.protocol.Smb2Command;
import local.mylan.transport.smb.protocol.Smb2Header;
import local.mylan.transport.smb.protocol.Smb2Response;
import local.mylan.transport.smb.protocol.fscc.FileAttributeFlags;

/**
 * Addresses MS-SMB2 (#2.2.14 SMB2 CREATE Response).
 */
public final class Smb2CreateResponse extends Smb2Response {

    private Smb2OpLockLevel opLockLevel;
    private Flags<Smb2CreateFlags> flags;
    private Smb2CreateAction createAction;
    private long creationTime;
    private long lastAccessTime;
    private long lastWriteTime;
    private long changeTime;
    private long allocationSize;
    private long endOfFile;
    private Flags<FileAttributeFlags> fileAttributes;
    private UUID fileId;

    public Smb2CreateResponse() {
        // default
    }

    public Smb2CreateResponse(final Smb2Header header) {
        super(header);
    }

    @Override
    protected Smb2Command command() {
        return Smb2Command.SMB2_CREATE;
    }

    public Smb2OpLockLevel opLockLevel() {
        return opLockLevel;
    }

    public void setOpLockLevel(final Smb2OpLockLevel opLockLevel) {
        this.opLockLevel = opLockLevel;
    }

    public Flags<Smb2CreateFlags> flags() {
        return flags;
    }

    public void setFlags(final Flags<Smb2CreateFlags> flags) {
        this.flags = flags;
    }

    public Smb2CreateAction createAction() {
        return createAction;
    }

    public void setCreateAction(final Smb2CreateAction createAction) {
        this.createAction = createAction;
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

    public UUID fileId() {
        return fileId;
    }

    public void setFileId(final UUID fileId) {
        this.fileId = fileId;
    }
}
