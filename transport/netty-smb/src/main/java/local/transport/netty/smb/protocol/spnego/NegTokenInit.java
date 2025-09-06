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
package local.transport.netty.smb.protocol.spnego;

import java.util.List;

/**
 * SPNEGO negotiation token.
 * Addresses <a href="https://www.rfc-editor.org/rfc/rfc4178.html#section-4.2.1.">RFC 4178 #4.2.1. negTokenInit</a>
 */
public class NegTokenInit implements NegToken {
    private List<MechType> mechTypes;
    private MechToken mechToken;
    private MechListMIC mechListMIC;

    public List<MechType> mechTypes() {
        return mechTypes;
    }

    public MechType optimisticMechType() {
        return mechTypes != null && !mechTypes.isEmpty() ? mechTypes.getFirst() : MechType.OTHER;
    }

    public void setMechTypes(final List<MechType> mechTypes) {
        this.mechTypes = mechTypes;
    }

    public MechToken mechToken() {
        return mechToken;
    }

    public void setMechToken(final MechToken mechToken) {
        this.mechToken = mechToken;
    }

    public MechListMIC mechListMIC() {
        return mechListMIC;
    }

    public void setMechListMIC(final MechListMIC mechListMIC) {
        this.mechListMIC = mechListMIC;
    }
}
