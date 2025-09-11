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
import local.transport.netty.smb.protocol.SmbCommand;
import local.transport.netty.smb.protocol.SmbResponseMessage;
import local.transport.netty.smb.protocol.spnego.NegToken;

public class Smb2SessionSetupResponse implements SmbResponseMessage {
    private Flags<Smb2SessionResponseFlags> sessionFlags;
    private NegToken token;

    @Override
    public SmbCommand command() {
        return SmbCommand.SMB2_SESSION_SETUP;
    }

    public Flags<Smb2SessionResponseFlags> sessionFlags() {
        return sessionFlags;
    }

    public void setSessionFlags(final Flags<Smb2SessionResponseFlags> sessionFlags) {
        this.sessionFlags = sessionFlags;
    }

    public NegToken token() {
        return token;
    }

    public void setToken(final NegToken token) {
        this.token = token;
    }
}
