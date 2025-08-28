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
import local.transport.netty.smb.protocol.SmbDialect;

@ConfFile("smb-client.conf")
public @interface SmbClientConf {

    @ConfProperty("netty.group.name")
    String groupName() default "smb-client";

    @ConfProperty("netty.group.max-threads")
    int groupThreads() default 0;

    @ConfProperty("tcp.backlog.size")
    int backlogSize() default 1024;

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

    @ConfProperty("smb.dialect.min")
    SmbDialect smbDialectMin() default SmbDialect.NTLM_012;

    @ConfProperty("smb.dialect.max")
    SmbDialect smbDialectMax() default SmbDialect.SMB3_1_1;


}
