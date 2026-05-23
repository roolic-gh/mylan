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
package local.transport.netty.smb.protocol.fscc;

import local.transport.netty.smb.protocol.Flags;

/**
 * Addresses MS-FSCC (#2.4.10 FileDirectoryInformation)
 */

public class FileDirectoryInformation implements FileInformation {
    private int fileIndex;
    private long creationTime;
    private long lastAccessTime;
    private long lastWriteTime;
    private long changeTime;
    private long endOfFile;
    private long allocationSize;
    private Flags<FileAttributeFlags> fileAttributes;
    private String fileName;

    public int fileIndex() {
        return fileIndex;
    }

    public void setFileIndex(final int fileIndex) {
        this.fileIndex = fileIndex;
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

    public long endOfFile() {
        return endOfFile;
    }

    public void setEndOfFile(final long endOfFile) {
        this.endOfFile = endOfFile;
    }

    public long allocationSize() {
        return allocationSize;
    }

    public void setAllocationSize(final long allocationSize) {
        this.allocationSize = allocationSize;
    }

    public Flags<FileAttributeFlags> fileAttributes() {
        return fileAttributes;
    }

    public void setFileAttributes(
        final Flags<FileAttributeFlags> fileAttributes) {
        this.fileAttributes = fileAttributes;
    }

    public String fileName() {
        return fileName;
    }

    public void setFileName(final String fileName) {
        this.fileName = fileName;
    }
}
