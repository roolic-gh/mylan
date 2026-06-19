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
import local.mylan.transport.smb.protocol.Smb2Request;

/**
 * Addresses MS-SMB2 (#2.2.15 SMB2 CLOSE Request).
 */
public final class Smb2CloseRequest extends Smb2Request {

    private Flags<Smb2CloseFlags> flags;
    private UUID fileId;

    public Smb2CloseRequest() {
        // default
    }

    public Smb2CloseRequest(final Smb2Header header) {
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

    public UUID fileId() {
        return fileId;
    }

    public void setFileId(final UUID fileId) {
        this.fileId = fileId;
    }
}
