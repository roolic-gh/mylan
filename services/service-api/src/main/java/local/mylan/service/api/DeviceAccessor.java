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
package local.mylan.service.api;

import java.net.InetAddress;
import javax.annotation.Nullable;
import local.mylan.service.api.model.Device;
import local.mylan.service.api.model.DeviceAccountState;
import local.mylan.service.api.model.DeviceProtocol;
import local.mylan.service.api.model.HavingCredentials;
import local.mylan.service.api.model.NavDirectory;

public interface DeviceAccessor {

    DeviceProtocol protocol();

    @Nullable
    String extractDeviceName(InetAddress address);

    DeviceAccountState validateCredentials(Device device, HavingCredentials creds);

    NavDirectory listDirectory(Device device, HavingCredentials creds, String path);

    default void stop(){
    }
}
