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

import java.util.List;
import local.mylan.transport.smb.protocol.Flags;

/**
 * Connection Oriented Bind Ack PDU. Addresses C706 PCE 1.1 (#12.6.4.4 The bind_ack PDU).
 */
public final class PceBindAck extends PceMessage {

    private int maxTransmitFragmentSize;
    private int maxReceiveFragmentSize;
    private int assocGroupId;
    private String secAddress;
    private List<PduDataTypes.ResultElement> results;

    public PceBindAck(final int callId) {
        super(callId);
    }

    public PceBindAck(final PduDataTypes.Version pduVersion, final Flags<PduDataTypes.PfcFlags> pfcFlags,
        final NdrFormatLabel ndrFormatLabel, final int callId) {
        super(pduVersion, pfcFlags, ndrFormatLabel, callId);
    }

    @Override
    public PduType type() {
        return PduType.bind_ack;
    }

    public int maxTransmitFragmentSize() {
        return maxTransmitFragmentSize;
    }

    public void setMaxTransmitFragmentSize(final int maxTransmitFragmentSize) {
        this.maxTransmitFragmentSize = maxTransmitFragmentSize;
    }

    public int maxReceiveFragmentSize() {
        return maxReceiveFragmentSize;
    }

    public void setMaxReceiveFragmentSize(final int maxReceiveFragmentSize) {
        this.maxReceiveFragmentSize = maxReceiveFragmentSize;
    }

    public int assocGroupId() {
        return assocGroupId;
    }

    public void setAssocGroupId(final int assocGroupId) {
        this.assocGroupId = assocGroupId;
    }

    public String secAddress() {
        return secAddress;
    }

    public void setSecAddress(final String secAddress) {
        this.secAddress = secAddress;
    }

    public List<PduDataTypes.ResultElement> results() {
        return results;
    }

    public void setResults(final List<PduDataTypes.ResultElement> results) {
        this.results = results;
    }
}
