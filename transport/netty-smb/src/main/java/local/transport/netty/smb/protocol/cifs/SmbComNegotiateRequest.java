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
package local.transport.netty.smb.protocol.cifs;

import com.google.common.base.Objects;
import java.util.List;
import local.transport.netty.smb.protocol.SmbCommand;
import local.transport.netty.smb.protocol.SmbDialect;
import local.transport.netty.smb.protocol.SmbRequestMessage;

public class SmbComNegotiateRequest implements SmbRequestMessage {
    private List<SmbDialect> dialects;

    @Override
    public SmbCommand command() {
        return SmbCommand.SMB_COM_NEGOTIATE;
    }

    public List<SmbDialect> dialects() {
        return dialects;
    }

    public void setDialects(final List<SmbDialect> dialects) {
        this.dialects = dialects;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof SmbComNegotiateRequest that)) {
            return false;
        }
        return Objects.equal(dialects, that.dialects);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(dialects);
    }
}
