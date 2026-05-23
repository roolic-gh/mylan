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
import local.mylan.transport.smb.protocol.Smb2Command;
import local.mylan.transport.smb.protocol.Smb2Header;
import local.mylan.transport.smb.protocol.Smb2Response;
import local.mylan.transport.smb.protocol.fscc.FsctlCode;

/**
 * SMB2 IOCTL Response. Addresses MS-SMB2 (#2.2.32 SMB2 IOCTL Response).
 */
public class Smb2IoctlResponse extends Smb2Response {

    private FsctlCode ctlCode;
    private UUID fileId;
    private Object input;
    private Object output;

    public Smb2IoctlResponse() {
        // default
    }

    public Smb2IoctlResponse(final Smb2Header header) {
        super(header);
    }

    @Override
    protected Smb2Command command() {
        return Smb2Command.SMB2_IOCTL;
    }

    public FsctlCode ctlCode() {
        return ctlCode;
    }

    public void setCtlCode(final FsctlCode ctlCode) {
        this.ctlCode = ctlCode;
    }

    public UUID fileId() {
        return fileId;
    }

    public void setFileId(final UUID fileId) {
        this.fileId = fileId;
    }

    public Object input() {
        return input;
    }

    public void setInput(final Object input) {
        this.input = input;
    }

    public Object output() {
        return output;
    }

    public void setOutput(final Object output) {
        this.output = output;
    }
}
