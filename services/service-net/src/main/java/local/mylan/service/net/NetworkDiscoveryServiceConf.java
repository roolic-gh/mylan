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
package local.mylan.service.net;

import local.mylan.common.annotations.conf.ConfFile;
import local.mylan.common.annotations.conf.ConfProperty;

@ConfFile("remote-discovery.conf")
public @interface NetworkDiscoveryServiceConf {

    @ConfProperty("remote.discover.threads")
    int threads() default 10;

    @ConfProperty("remote.discover.delay")
    long rediscoverDelaySeconds() default 60L;

    @ConfProperty("remote.discover.interval")
    long rediscoverIntervalSeconds() default 3600L;

    @ConfProperty("remote.discover.subnets")
    String subnets() default "192.168.0.0/23";
}
