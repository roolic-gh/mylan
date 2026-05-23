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
package local.mylan.transport.smb.protocol.pcerpc;

import local.mylan.transport.smb.protocol.Flags;

/**
 * Abstraction representing common PDU fields.
 */
public abstract class PceMessage {

    private PduDataTypes.Version pduVersion = new PduDataTypes.Version(5, 0);
    private Flags<PduDataTypes.PfcFlags> pfcFlags = new Flags<PduDataTypes.PfcFlags>()
        .set(PduDataTypes.PfcFlags.PFC_FIRST_FRAG, true).set(PduDataTypes.PfcFlags.PFC_LAST_FRAG, true);
    private NdrFormatLabel ndrFormatLabel = NdrFormatLabel.getDefault();
    private int callId;

    protected PceMessage(final int callId){
        this.callId = callId;
    }

    protected PceMessage(final PduDataTypes.Version pduVersion, final Flags<PduDataTypes.PfcFlags> pfcFlags,
        final NdrFormatLabel ndrFormatLabel, final int callId) {

        this.pduVersion = pduVersion;
        this.pfcFlags = pfcFlags;
        this.ndrFormatLabel = ndrFormatLabel;
        this.callId = callId;
    }

    public abstract PduType type();

    public PduDataTypes.Version pduVersion() {
        return pduVersion;
    }

    public void setPduVersion(final PduDataTypes.Version pduVersion) {
        this.pduVersion = pduVersion;
    }

    public Flags<PduDataTypes.PfcFlags> pfcFlags() {
        return pfcFlags;
    }

    public void setPfcFlags(final Flags<PduDataTypes.PfcFlags> pfcFlags) {
        this.pfcFlags = pfcFlags;
    }

    public NdrFormatLabel ndrFormatLabel() {
        return ndrFormatLabel;
    }

    public void setNdrFormatLabel(final NdrFormatLabel ndrFormatLabel) {
        this.ndrFormatLabel = ndrFormatLabel;
    }

    public int callId() {
        return callId;
    }

    public void setCallId(final int callId) {
        this.callId = callId;
    }
}
