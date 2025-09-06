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
package local.transport.netty.smb.protocol.spnego.ntlm;

import java.util.EnumMap;
import java.util.Map;
import local.transport.netty.smb.protocol.Flags;

/**
 * AV Pair Structure. Addresses MS-NLMP (#2.2.2.1 AV_PAIR).
 */
public class NtlmAvPairs {

    private final Map<NtlmAvId, Object> attributes = new EnumMap<>(NtlmAvId.class);
    /*
    private String netbiosComputerName;
    private String netbiosDomainName;
    private String dnsComputerName;
    private String dnsDomainName;
    private String dnsTreeName;
    private Flags<NtlmAvFlags> flags;
    private Long serverTime;
    private NtlmSingleHostData singleHostData;
    private String targetServerName;
    private NtlmChannelBindingHash channelBinding;
    */

    public NtlmAvPairs() {
        // default
    }

    public NtlmAvPairs(final Map<NtlmAvId, Object> attributes) {
        this.attributes.putAll(attributes);
    }

    public Map<NtlmAvId, Object> asMap() {
        return attributes;
    }

    public String netbiosComputerName() {
        return castOrNull(attributes.get(NtlmAvId.MsvAvNbComputerName), String.class);
    }

    public void setNetbiosComputerName(final String netbiosComputerName) {
        attributes.put(NtlmAvId.MsvAvNbComputerName, netbiosComputerName);
    }

    public String netbiosDomainName() {
        return castOrNull(attributes.get(NtlmAvId.MsvAvNbDomainName), String.class);
    }

    public void setNetbiosDomainName(final String netbiosDomainName) {
        attributes.put(NtlmAvId.MsvAvNbDomainName, netbiosDomainName);
    }

    public String dnsComputerName() {
        return castOrNull(attributes.get(NtlmAvId.MsvAvDnsComputerName), String.class);
    }

    public void setDnsComputerName(final String dnsComputerName) {
        attributes.put(NtlmAvId.MsvAvDnsComputerName, dnsComputerName);
    }

    public String dnsDomainName() {
        return castOrNull(attributes.get(NtlmAvId.MsvAvDnsDomainName), String.class);
    }

    public void setDnsDomainName(final String dnsDomainName) {
        attributes.put(NtlmAvId.MsvAvDnsDomainName, dnsDomainName);
    }

    public String dnsTreeName() {
        return castOrNull(attributes.get(NtlmAvId.MsvAvDnsTreeName), String.class);
    }

    public void setDnsTreeName(final String dnsTreeName) {
        attributes.put(NtlmAvId.MsvAvDnsTreeName, dnsTreeName);
    }

    @SuppressWarnings("unchecked")
    public Flags<NtlmAvFlags> flags() {
        final var flags = attributes.get(NtlmAvId.MsvAvFlags);
        return flags instanceof Flags<?> ? (Flags<NtlmAvFlags>) flags : null;
    }

    public void setFlags(final Flags<NtlmAvFlags> flags) {
        attributes.put(NtlmAvId.MsvAvFlags, flags);
    }

    public Long serverTime() {
        return castOrNull(attributes.get(NtlmAvId.MsvAvTimestamp), Long.class);
    }

    public void setServerTime(final Long serverTime) {
        attributes.put(NtlmAvId.MsvAvTimestamp, serverTime);
    }

    public NtlmSingleHostData singleHostData() {
        return castOrNull(attributes.get(NtlmAvId.MsvAvSingleHost), NtlmSingleHostData.class);
    }

    public void setSingleHostData(final NtlmSingleHostData singleHostData) {
        attributes.put(NtlmAvId.MsvAvSingleHost, singleHostData);
    }

    public String targetServerName() {
        return castOrNull(attributes.get(NtlmAvId.MsvAvTargetName), String.class);
    }

    public void setTargetServerName(final String targetServerName) {
        attributes.put(NtlmAvId.MsvAvTargetName, targetServerName);
    }

    public NtlmChannelBindingHash channelBinding() {
        return castOrNull(attributes.get(NtlmAvId.MsvAvChannelBindings), NtlmChannelBindingHash.class);
    }

    public void setChannelBinding(final NtlmChannelBindingHash channelBinding) {
        attributes.put(NtlmAvId.MsvAvChannelBindings, channelBinding);
    }

    private static <T> T castOrNull(final Object obj, final Class<T> type) {
        return type.isInstance(obj) ? type.cast(obj) : null;
    }
}
