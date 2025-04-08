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
package local.mylan.transport.http;

import local.mylan.common.annotations.conf.ConfFile;
import local.mylan.common.annotations.conf.ConfProperty;

@ConfFile("http-server.conf")
public @interface HttpServerConfig {

    @ConfProperty("netty.parent-group.name")
    String parentGroupName() default "http-parent";

    @ConfProperty("netty.parent-group.max-threads")
    int parentGroupThreads() default 0;

    @ConfProperty("netty.child-group.name")
    String childGroupName() default "http-child";

    @ConfProperty("netty.child-group.max-threads")
    int childGroupThreads() default 0;

    @ConfProperty("backlog.size")
    int backlogSize() default 1024;

    @ConfProperty("tcp.keepalive.enabled")
    boolean tcpKeepAliveEnabled() default true;

    @ConfProperty("tcp.keepalive.idle.time")
    int tcpKeepAliveIdleTime() default 7200;

    @ConfProperty("tcp.keepalive.retransmission.count")
    int tcpKeepAliveRetransmissionCount() default 8;

    @ConfProperty("tcp.keepalive.retransmission.interval")
    int tcpKeepAliveRetransmissionInterval() default 75;

    @ConfProperty("tcp.port")
    int tcpPort() default 8080;

    @ConfProperty("tls.port")
    int tlsPort() default 8443;

    @ConfProperty("tls.enabled")
    boolean tlsEnabled() default false;

    @ConfProperty("tls.certificate.persist-generated")
    boolean tlsCertPersistGenerated() default true;

    @ConfProperty("tls.certificate.path")
    String tlsCertPath() default "tls/cert.pem";

    @ConfProperty("tls.certificate.private-key.path")
    String tlsPrivateKeyPath() default "tls/key.pem";

    @ConfProperty("http.inbound.max-length")
    int maxContentLength() default 16 * 1024;
}
