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

import local.mylan.common.annotations.conf.ConfProperty;
import local.transport.netty.smb.protocol.Smb2Dialect;

public @interface TestServerConf {
    @ConfProperty("server.dialect")
    Smb2Dialect serverDialect() default Smb2Dialect.SMB3_0;
    @ConfProperty("server.signEnabled")
    boolean signEnabled() default true;
    @ConfProperty("server.signRequired")
    boolean signRequired() default false;
    @ConfProperty("server.anonimousEnabled")
    boolean anonimousEnabled() default true;
    @ConfProperty("server.maxReadWriteSize")
    int maxReadWriteSize() default 0x10000;

}
