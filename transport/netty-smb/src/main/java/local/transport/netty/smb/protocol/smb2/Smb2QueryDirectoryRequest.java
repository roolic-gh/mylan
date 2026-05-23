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
package local.transport.netty.smb.protocol.smb2;

import java.util.UUID;
import local.transport.netty.smb.protocol.Flags;
import local.transport.netty.smb.protocol.Smb2Command;
import local.transport.netty.smb.protocol.Smb2Header;
import local.transport.netty.smb.protocol.Smb2Request;
import local.transport.netty.smb.protocol.fscc.FileInformationClass;

/**
 * Addresses MS-SMB2 (#2.2.33 SMB2 QUERY_DIRECTORY Request).
 */
public class Smb2QueryDirectoryRequest extends Smb2Request {

    private Flags<Smb2QueryDirectoryFlags> flags;
    private FileInformationClass fileInformationClass;
    private int fileIndex;
    private UUID fileId;
    private int outputBufferLength;
    private String searchPattern;

    public Smb2QueryDirectoryRequest() {
        // default
    }

    public Smb2QueryDirectoryRequest(final Smb2Header header) {
        super(header);
    }

    @Override
    protected Smb2Command command() {
        return Smb2Command.SMB2_QUERY_DIRECTORY;
    }

    public Flags<Smb2QueryDirectoryFlags> flags() {
        return flags;
    }

    public void setFlags(
        final Flags<Smb2QueryDirectoryFlags> flags) {
        this.flags = flags;
    }

    public FileInformationClass fileInformationClass() {
        return fileInformationClass;
    }

    public void setFileInformationClass(final FileInformationClass fileInformationClass) {
        this.fileInformationClass = fileInformationClass;
    }

    public int fileIndex() {
        return fileIndex;
    }

    public void setFileIndex(final int fileIndex) {
        this.fileIndex = fileIndex;
    }

    public UUID fileId() {
        return fileId;
    }

    public void setFileId(final UUID fileId) {
        this.fileId = fileId;
    }

    public int outputBufferLength() {
        return outputBufferLength;
    }

    public void setOutputBufferLength(final int outputBufferLength) {
        this.outputBufferLength = outputBufferLength;
    }

    public String searchPattern() {
        return searchPattern;
    }

    public void setSearchPattern(final String searchPattern) {
        this.searchPattern = searchPattern;
    }
}
