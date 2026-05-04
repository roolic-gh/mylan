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
import local.transport.netty.smb.protocol.Smb2Command;
import local.transport.netty.smb.protocol.Smb2Header;
import local.transport.netty.smb.protocol.Smb2Request;
import local.transport.netty.smb.protocol.fscc.FsctlCode;

/**
 * SMB2 IOCTL Request. Addresses MS-SMB2 (#2.2.31 SMB2 IOCTL Request).
 */
public class Smb2IoctlRequest extends Smb2Request {

    private FsctlCode ctlCode;
    private UUID fileId;
    private boolean isFsctl;
    private int maxInputResponse;
    private int maxOutputResponse;
    private Object input;
    private Object output;

    public Smb2IoctlRequest() {
        // default
    }

    public Smb2IoctlRequest(final Smb2Header header) {
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

    public boolean isFsctl() {
        return isFsctl;
    }

    public void setFsctl(final boolean fsctl) {
        isFsctl = fsctl;
    }

    public int maxInputResponse() {
        return maxInputResponse;
    }

    public void setMaxInputResponse(final int maxInputResponse) {
        this.maxInputResponse = maxInputResponse;
    }

    public int maxOutputResponse() {
        return maxOutputResponse;
    }

    public void setMaxOutputResponse(final int maxOutputResponse) {
        this.maxOutputResponse = maxOutputResponse;
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
