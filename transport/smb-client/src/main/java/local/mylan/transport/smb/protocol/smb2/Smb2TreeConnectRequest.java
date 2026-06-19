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

import local.mylan.transport.smb.protocol.Flags;
import local.mylan.transport.smb.protocol.Smb2Command;
import local.mylan.transport.smb.protocol.Smb2Header;
import local.mylan.transport.smb.protocol.Smb2Request;

/**
 * SMB2 Tree Connect Request. Addresses MS-SMB2 (#2.2.9 SMB2 TREE_CONNECT Request).
 */
public final class Smb2TreeConnectRequest  extends Smb2Request {

    private Flags<Smb2TreeConnectFlags> flags;
    private String path;

    public Smb2TreeConnectRequest() {
    }

    public Smb2TreeConnectRequest(final Smb2Header header) {
        super(header);
    }

    @Override
    public Smb2Command command() {
        return Smb2Command.SMB2_TREE_CONNECT;
    }

    public Flags<Smb2TreeConnectFlags> flags() {
        return flags;
    }

    public void setFlags(final Flags<Smb2TreeConnectFlags> flags) {
        this.flags = flags;
    }

    public String path() {
        return path;
    }

    public void setPath(final String path) {
        this.path = path;
    }
}
