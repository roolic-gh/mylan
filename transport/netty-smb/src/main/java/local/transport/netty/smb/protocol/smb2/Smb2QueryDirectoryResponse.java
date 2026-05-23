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

import java.util.List;
import local.transport.netty.smb.protocol.Smb2Command;
import local.transport.netty.smb.protocol.Smb2Header;
import local.transport.netty.smb.protocol.Smb2Response;
import local.transport.netty.smb.protocol.fscc.FileInformation;

/**
 * Addresses MS-SMB2 (#2.2.34 SMB2 QUERY_DIRECTORY Response).
 */
public class Smb2QueryDirectoryResponse extends Smb2Response {

    private byte[] encoded;
    private List<FileInformation> decoded;

    public Smb2QueryDirectoryResponse() {
        // default
    }

    public Smb2QueryDirectoryResponse(final Smb2Header header) {
        super(header);
    }

    @Override
    protected Smb2Command command() {
        return Smb2Command.SMB2_QUERY_DIRECTORY;
    }

    public byte[] encoded() {
        return encoded;
    }

    public void setEncoded(final byte[] encoded) {
        this.encoded = encoded;
    }

    public List<FileInformation> decoded() {
        return decoded;
    }

    public void setDecoded(final List<FileInformation> decoded) {
        this.decoded = decoded;
    }
}
