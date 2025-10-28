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
package local.transport.netty.smb.protocol;

import static java.util.Objects.requireNonNull;

public abstract class Smb2Response {
    protected final Smb2Header header;

    protected Smb2Response() {
        this(new Smb2Header());
    }

    protected Smb2Response(final Smb2Header header) {
        this.header = requireNonNull(header);
        header.setCommand(command());
    }

    protected abstract Smb2Command command();

    public final Smb2Header header() {
        return header;
    }
}
