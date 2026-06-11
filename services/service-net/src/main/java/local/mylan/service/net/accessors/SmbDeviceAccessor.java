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
package local.mylan.service.net.accessors;

import java.net.InetAddress;
import java.nio.file.Path;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import local.mylan.service.api.model.DeviceProtocol;
import local.mylan.service.net.DeviceAccessor;
import local.mylan.transport.smb.SmbClient;

public class SmbDeviceAccessor implements DeviceAccessor {

    private final SmbClient client;

    public SmbDeviceAccessor(final Path confDir) {
        client = new SmbClient(confDir);
    }

    @Override
    public DeviceProtocol protocol() {
        return DeviceProtocol.SMB;
    }

    @Override
    public String extractDeviceName(final InetAddress address) {

        try {
            final var conn = client.connect(address).get(2, TimeUnit.SECONDS);
            try {
                // Netbios name of a server is taken from a server response (NTLM authorization flow)
                // then stored as server name property within a client connection details
                conn.newAnonimousSession().get(2, TimeUnit.SECONDS);
            } catch (Exception e) {
                // expected
            }
            final var name = conn.details().serverName(); // non-null if provided
            conn.close();
            return name;
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            // ignore
        }
        return null;
    }
}
