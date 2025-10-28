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
package local.transport.netty.smb;

import local.mylan.common.annotations.conf.ConfFile;
import local.mylan.common.annotations.conf.ConfProperty;
import local.transport.netty.smb.protocol.Smb2Dialect;

@ConfFile("smb-client.conf")
public @interface SmbClientConf {

    @ConfProperty("netty.group.name")
    String groupName() default "smb-client";

    @ConfProperty("netty.group.max-threads")
    int groupThreads() default 0;

    @ConfProperty("tcp.keepalive.enabled")
    boolean tcpKeepAliveEnabled() default true;

    @ConfProperty("tcp.keepalive.idle.time")
    int tcpKeepAliveIdleTime() default 7200;

    @ConfProperty("tcp.keepalive.retransmission.count")
    int tcpKeepAliveRetransmissionCount() default 8;

    @ConfProperty("tcp.keepalive.retransmission.interval")
    int tcpKeepAliveRetransmissionInterval() default 75;

    @ConfProperty("smb.server.deault-port")
    int smbServerDefaultPort() default 445;

    // Global Details

    @ConfProperty("smb.client.require-signing")
    boolean requireMessageSigning() default false;

    @ConfProperty("smb.client.encryption-supported")
    boolean encryptionSupported() default false;

    @ConfProperty("smb.client.compression-supported")
    boolean compressionSupported() default false;

    @ConfProperty("smb.client.chained-compression-supported")
    boolean chainedCompressionSupported() default false;

    @ConfProperty("smb.client.rdma-transform-supported")
    boolean rdmaTransformSupported() default false;

    @ConfProperty("smb.client.disable-encryption-on-tls")
    boolean disableEncryptionOverSecureTransport() default true;

    @ConfProperty("smb.client.signing-capabilities-supported")
    boolean signingCapabilitiesSupported() default true;

    @ConfProperty("smb.client.transport-capabilities-supported")
    boolean transportCapabilitiesSupported() default false;

    @ConfProperty("smb.client.server-notifiaction-supported")
    boolean serverToClientNotificationsSupported() default false;

    // Connection Details

    @ConfProperty("smb.client.setup-request-credits")
    int setupCreditsRequest() default 128;

    @ConfProperty("smb.client.dialect.min")
    Smb2Dialect smbDialectMin() default Smb2Dialect.SMB2_0_2;

    @ConfProperty("smb.client.dialect.max")
    Smb2Dialect smbDialectMax() default Smb2Dialect.SMB3_0;

}
