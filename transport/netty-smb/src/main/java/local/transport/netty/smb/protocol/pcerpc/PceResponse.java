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
package local.transport.netty.smb.protocol.pcerpc;

import local.transport.netty.smb.protocol.Flags;

/**
 * Connection Oriented Response PDU. Addresses C706 PCE 1.1 (#12.6.4.10 The response PDU).
 */
public final class PceResponse extends PceMessage {

    private int contextId;
    private int cancelCount;
    private Object object;

    public PceResponse(final int callId) {
        super(callId);
    }

    public PceResponse(final PduDataTypes.Version pduVersion, final Flags<PduDataTypes.PfcFlags> pfcFlags,
        final NdrFormatLabel ndrFormatLabel, final int callId) {
        super(pduVersion, pfcFlags, ndrFormatLabel, callId);
    }

    @Override
    public PduType type() {
        return PduType.response;
    }

    public int contextId() {
        return contextId;
    }

    public void setContextId(final int contextId) {
        this.contextId = contextId;
    }

    public int cancelCount() {
        return cancelCount;
    }

    public void setCancelCount(final int cancelCount) {
        this.cancelCount = cancelCount;
    }

    public Object object() {
        return object;
    }

    public void setObject(final Object object) {
        this.object = object;
    }
}
