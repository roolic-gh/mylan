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
package local.transport.netty.smb.protocol.details;

import java.util.concurrent.atomic.AtomicInteger;
import local.transport.netty.smb.protocol.Flags;
import local.transport.netty.smb.protocol.spnego.ntlm.NtlmNegotiateFlags;
import local.transport.netty.smb.protocol.spnego.ntlm.NtlmVersion;

/**
 * Ntlm Client Details. Addresses MS-NLMP (#3.1.1.1 Variables Internal to the Protocol).
 */
public class NtlmClientDetails {
    private Flags<NtlmNegotiateFlags> clientConfigFlags;
    private byte[] exportedSessionKey;
    private Flags<NtlmNegotiateFlags> negFlags;
    private String user;
    private String userDomain;
    private boolean noLMResponseNTLMv1 = true;
    private boolean clientRequire128bitEncryption = true;
    private long maxLifetime;
    private byte[] clientSigningKey;
    private byte[] clientSealingKey;
    private final AtomicInteger seqNum = new AtomicInteger(0);
    private byte[] serverSealingKey;
    private byte[] serverSigningKey;
    private byte[]channelBindingUnhashed;
    private String suppliedTargetName;
    private boolean unverifiedTargetName = true;

    // non-spec
    private NtlmVersion clientVersion;
    private boolean ntlmV2 = true;

    public void setClientConfigFlags(final Flags<NtlmNegotiateFlags> clientConfigFlags) {
        this.clientConfigFlags = clientConfigFlags;
    }

    public Flags<NtlmNegotiateFlags> clientConfigFlags() {
        return clientConfigFlags;
    }

    public byte[] exportedSessionKey() {
        return exportedSessionKey;
    }

    public void setExportedSessionKey(final byte[] exportedSessionKey) {
        this.exportedSessionKey = exportedSessionKey;
    }

    public void setNegFlags(final Flags<NtlmNegotiateFlags> negFlags) {
        this.negFlags = negFlags;
    }

    public Flags<NtlmNegotiateFlags> negFlags() {
        return negFlags;
    }

    public String user() {
        return user;
    }

    public void setUser(final String user) {
        this.user = user;
    }

    public String userDomain() {
        return userDomain;
    }

    public void setUserDomain(final String userDomain) {
        this.userDomain = userDomain;
    }

    public boolean noLMResponseNTLMv1() {
        return noLMResponseNTLMv1;
    }

    public void setNoLMResponseNTLMv1(final boolean noLMResponseNTLMv1) {
        this.noLMResponseNTLMv1 = noLMResponseNTLMv1;
    }

    public boolean clientRequire128bitEncryption() {
        return clientRequire128bitEncryption;
    }

    public void setClientRequire128bitEncryption(final boolean clientRequire128bitEncryption) {
        this.clientRequire128bitEncryption = clientRequire128bitEncryption;
    }

    public long maxLifetime() {
        return maxLifetime;
    }

    public void setMaxLifetime(final long maxLifetime) {
        this.maxLifetime = maxLifetime;
    }

    public byte[] clientSigningKey() {
        return clientSigningKey;
    }

    public void setClientSigningKey(final byte[] clientSigningKey) {
        this.clientSigningKey = clientSigningKey;
    }

    public byte[] clientSealingKey() {
        return clientSealingKey;
    }

    public void setClientSealingKey(final byte[] clientSealingKey) {
        this.clientSealingKey = clientSealingKey;
    }

    public int seqNum() {
        return seqNum.getAndIncrement();
    }

    public byte[] serverSealingKey() {
        return serverSealingKey;
    }

    public void setServerSealingKey(final byte[] serverSealingKey) {
        this.serverSealingKey = serverSealingKey;
    }

    public byte[] serverSigningKey() {
        return serverSigningKey;
    }

    public void setServerSigningKey(final byte[] serverSigningKey) {
        this.serverSigningKey = serverSigningKey;
    }

    public byte[] channelBindingUnhashed() {
        return channelBindingUnhashed;
    }

    public void setChannelBindingUnhashed(final byte[] channelBindingUnhashed) {
        this.channelBindingUnhashed = channelBindingUnhashed;
    }

    public String suppliedTargetName() {
        return suppliedTargetName;
    }

    public void setSuppliedTargetName(final String suppliedTargetName) {
        this.suppliedTargetName = suppliedTargetName;
    }

    public boolean unverifiedTargetName() {
        return unverifiedTargetName;
    }

    public void setUnverifiedTargetName(final boolean unverifiedTargetName) {
        this.unverifiedTargetName = unverifiedTargetName;
    }

    public void setClientVersion(final NtlmVersion clientVersion) {
        this.clientVersion = clientVersion;
    }

    public NtlmVersion clientVersion() {
        return clientVersion;
    }

    public boolean ntlmV2() {
        return ntlmV2;
    }

    public void setNtlmV2(final boolean ntlmV2) {
        this.ntlmV2 = ntlmV2;
    }
}
