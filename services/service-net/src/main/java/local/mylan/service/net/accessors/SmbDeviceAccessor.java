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

import com.google.common.net.InetAddresses;
import java.net.InetAddress;
import java.nio.file.Path;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import local.mylan.service.api.DeviceAccessor;
import local.mylan.service.api.exceptions.NoConnectionException;
import local.mylan.service.api.model.Device;
import local.mylan.service.api.model.DeviceAccountState;
import local.mylan.service.api.model.DeviceProtocol;
import local.mylan.service.api.model.HavingCredentials;
import local.mylan.transport.smb.SmbClient;
import local.mylan.transport.smb.protocol.details.UserCredentials;

public class SmbDeviceAccessor implements DeviceAccessor {

    private final SmbClient checkClient;

    public SmbDeviceAccessor(final Path confDir) {
        checkClient = new SmbClient(confDir);
    }

    @Override
    public DeviceProtocol protocol() {
        return DeviceProtocol.SMB;
    }

    @Override
    public String extractDeviceName(final InetAddress address) {

        try {
            final var conn = checkClient.connect(address).get(2, TimeUnit.SECONDS);
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

    @Override
    public DeviceAccountState validateCredentials(final Device device, final HavingCredentials creds) {
        try {
            final var conn = checkClient.connect(getInetAddress(device)).get(2, TimeUnit.SECONDS);
            try {
                final var session = conn.newSession(credentials(creds)).get(5, TimeUnit.SECONDS);
                session.close();
                return DeviceAccountState.VALID;
            } catch (Exception e) {
                return DeviceAccountState.INVALID;
            } finally {
                conn.close();
            }
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            throw new NoConnectionException("Cannot connect device " + device.getIdentifier());
        }
    }

    private static InetAddress getInetAddress(final Device device) {
        if (device.getIpAddresses() == null || device.getIpAddresses().isEmpty()) {
            throw new NoConnectionException("Device %s has no IP address assignes".formatted(device.getIdentifier()));
        }
        return InetAddresses.forString(device.getIpAddresses().getFirst().getIpAddress());
    }

    private static UserCredentials credentials(final HavingCredentials havingCredentials) {
        return new UserCredentials() {
            @Override
            public String username() {
                return havingCredentials.getUsername();
            }

            @Override
            public String password() {
                return havingCredentials.getPassword();
            }
        };
    }
}
