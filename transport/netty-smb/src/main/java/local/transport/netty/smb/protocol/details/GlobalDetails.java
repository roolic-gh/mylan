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

/**
 * Global Details. Addresses MS-SMB2 (#3.1.1.1 Global).
 */
public class GlobalDetails {
    private boolean requireMessageSigning;
    private boolean encryptionSupported;
    private boolean compressionSupported;
    private boolean chainedCompressionSupported;
    private boolean rdmaTransformSupported;
    private boolean disableEncryptionOverSecureTransport;
    private boolean signingCapabilitiesSupported;
    private boolean transportCapabilitiesSupported;
    private boolean serverToClientNotificationsSupported;

    public boolean requireMessageSigning() {
        return requireMessageSigning;
    }

    public void setRequireMessageSigning(final boolean requireMessageSigning) {
        this.requireMessageSigning = requireMessageSigning;
    }

    public boolean encryptionSupported() {
        return encryptionSupported;
    }

    public void setEncryptionSupported(final boolean encryptionSupported) {
        this.encryptionSupported = encryptionSupported;
    }

    public boolean compressionSupported() {
        return compressionSupported;
    }

    public void setCompressionSupported(final boolean compressionSupported) {
        this.compressionSupported = compressionSupported;
    }

    public boolean chainedCompressionSupported() {
        return chainedCompressionSupported;
    }

    public void setChainedCompressionSupported(final boolean chainedCompressionSupported) {
        this.chainedCompressionSupported = chainedCompressionSupported;
    }

    public boolean rdmaTransformSupported() {
        return rdmaTransformSupported;
    }

    public void setRdmaTransformSupported(final boolean rdmaTransformSupported) {
        this.rdmaTransformSupported = rdmaTransformSupported;
    }

    public boolean disableEncryptionOverSecureTransport() {
        return disableEncryptionOverSecureTransport;
    }

    public void setDisableEncryptionOverSecureTransport(final boolean disableEncryptionOverSecureTransport) {
        this.disableEncryptionOverSecureTransport = disableEncryptionOverSecureTransport;
    }

    public boolean signingCapabilitiesSupported() {
        return signingCapabilitiesSupported;
    }

    public void setSigningCapabilitiesSupported(final boolean signingCapabilitiesSupported) {
        this.signingCapabilitiesSupported = signingCapabilitiesSupported;
    }

    public boolean transportCapabilitiesSupported() {
        return transportCapabilitiesSupported;
    }

    public void setTransportCapabilitiesSupported(final boolean transportCapabilitiesSupported) {
        this.transportCapabilitiesSupported = transportCapabilitiesSupported;
    }

    public boolean serverToClientNotificationsSupported() {
        return serverToClientNotificationsSupported;
    }

    public void setServerToClientNotificationsSupported(final boolean serverToClientNotificationsSupported) {
        this.serverToClientNotificationsSupported = serverToClientNotificationsSupported;
    }
}
