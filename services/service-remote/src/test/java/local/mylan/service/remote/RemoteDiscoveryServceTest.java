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
package local.mylan.service.remote;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import com.google.common.net.InetAddresses;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import java.net.InetAddress;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nullable;
import local.mylan.common.utils.ConfUtils;
import local.mylan.common.utils.InetAddressList;
import local.mylan.service.api.DiscoveryService;
import local.mylan.service.api.NotificationService;
import local.mylan.service.api.events.DiscoveryDevicesEvent;
import local.mylan.service.api.events.Event;
import local.mylan.service.api.model.DeviceProtocol;
import local.mylan.service.remote.accessors.SmbDeviceAccessor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RemoteDiscoveryServceTest {
    private static final String SUBNET = "192.168.1.0/24";
    private static final String IP_ADDRESS_1 = "192.168.1.101";
    private static final String IP_ADDRESS_2 = "192.168.1.102";
    private static final String IP_ADDRESS_3 = "192.168.1.103";
    private static final String IP_ADDRESS_4 = "192.168.1.104";
    private static final String DEVICE_NAME_1 = "Device-1";
    private static final String DEVICE_NAME_2 = "Device-2";
    private static final String DEVICE_NAME_3 = "Device-3";

    @Mock
    NotificationService notificationService;
    @Captor
    ArgumentCaptor<Event> eventCaptor;

    DiscoveryService discoveryService;

    @AfterEach
    void afterEach() {
        if (discoveryService != null) {
            discoveryService.stop();
        }
    }

    @Test
    @Disabled
    void live() throws Exception {
        discoveryService = new RemoteDiscoveryService(
            ConfUtils.loadConfiguration(RemoteDiscoveryServiceConf.class),
            notificationService,
            InetAddressList.valueOf(SUBNET),
            Set.of(new SmbDeviceAccessor(Path.of("~"))));
        discoveryService.startDiscovery().get(60, TimeUnit.SECONDS);
    }

    @Test
    void discoveryFlow() throws Exception {
        // disable scheduled invokation
        final var conf = ConfUtils.loadConfiguration(RemoteDiscoveryServiceConf.class, """
                remote.discover.threads=2
                remote.discover.interval=0
            """);
        // discovery delay flag to pause discovery on device check stage
        final var delayFlag = SettableFuture.<Void>create();
        // 2 x smb devices and 1 x nfs device online
        final var accessors = Set.of(
            new TestDeviceAccessor(DeviceProtocol.SMB, delayFlag, Map.of(
                address(IP_ADDRESS_1), DEVICE_NAME_1,
                address(IP_ADDRESS_2), DEVICE_NAME_2,
                address(IP_ADDRESS_3), DEVICE_NAME_2)),
            new TestDeviceAccessor(DeviceProtocol.NFS, delayFlag, Map.of(
                address(IP_ADDRESS_4), DEVICE_NAME_3))
        );
        // 256 addresses to scan
        final var subnets = InetAddressList.valueOf(SUBNET);

        discoveryService = new RemoteDiscoveryService(conf, notificationService, subnets, accessors);

        // start discovery
        final var future = discoveryService.startDiscovery();
        assertNotNull(future);
        assertFalse(future.isDone());
        final var runStatus = discoveryService.currentStatus();
        assertNotNull(runStatus);
        assertTrue(runStatus.isRunning());
        assertTrue(runStatus.getStartTime() > 0);

        // start discovery while running -- same future to be returned
        final var future2 = discoveryService.startDiscovery();
        assertEquals(future, future2);

        // release discovery paused
        delayFlag.set(null);
        final var completeStatus = future.get(1, TimeUnit.SECONDS);
        assertNotNull(completeStatus);
        assertFalse(completeStatus.isRunning());
        assertEquals(runStatus.getStartTime(), completeStatus.getStartTime());
        assertTrue(completeStatus.getEndTime() > 0);
        assertEquals(3, completeStatus.getDevicesDiscovered());

        // verify second invocation is notified
        assertTrue(future2.isDone());

        // verify devices notification event sent
        verify(notificationService, timeout(2000).times(1)).raiseEvent(eventCaptor.capture());
        final var event = assertInstanceOf(DiscoveryDevicesEvent.class, eventCaptor.getValue());
        final var discovered = event.devices();
        assertNotNull(discovered);
        assertEquals(3, discovered.size());

        // verify devices, ip addresses and protocls are mapped properly
        final var actualList = new ArrayList<DiscoveredDevice>();
        for (var device : discovered) {
            Optional.ofNullable(device.getIpAddresses())
                .ifPresent(ipas -> ipas.forEach(ipa -> actualList.add(
                    new DiscoveredDevice(ipa.getIpAddress(), device.getIdentifier(), device.getProtocol()))
                ));
        }
        final var expected = Set.of(
            new DiscoveredDevice(IP_ADDRESS_1, DEVICE_NAME_1, DeviceProtocol.SMB),
            new DiscoveredDevice(IP_ADDRESS_2, DEVICE_NAME_2, DeviceProtocol.SMB),
            new DiscoveredDevice(IP_ADDRESS_3, DEVICE_NAME_2, DeviceProtocol.SMB),
            new DiscoveredDevice(IP_ADDRESS_4, DEVICE_NAME_3, DeviceProtocol.NFS));
        assertEquals(expected, Set.copyOf(actualList));
    }

    private static InetAddress address(final String address) {
        return InetAddresses.forString(address);
    }

    record DiscoveredDevice(String ipAddress, String name, DeviceProtocol protocol) {
    }

    private static class TestDeviceAccessor implements RemoteDeviceAccessor {

        private final DeviceProtocol protocol;
        private final Map<InetAddress, String> deviceMap;
        private final ListenableFuture<Void> delay;

        TestDeviceAccessor(final DeviceProtocol protocol, final ListenableFuture<Void> delay,
            final Map<InetAddress, String> deviceMap) {

            this.protocol = protocol;
            this.delay = delay;
            this.deviceMap = deviceMap;
        }

        @Nullable
        @Override
        public String extractDeviceName(final InetAddress address) {
            if (!delay.isDone()) {
                try {
                    delay.get();
                } catch (InterruptedException | ExecutionException e) {
                    // ignore;
                }
            }
            return deviceMap.get(address);
        }

        @Override
        public DeviceProtocol protocol() {
            return protocol;
        }
    }
}
