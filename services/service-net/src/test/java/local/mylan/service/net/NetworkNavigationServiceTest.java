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

import static local.mylan.service.api.model.DeviceProtocol.NFS;
import static local.mylan.service.api.model.DeviceProtocol.SMB;
import static local.mylan.service.api.model.DeviceState.OFFLINE;
import static local.mylan.service.api.model.DeviceState.ONLINE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import local.mylan.service.api.NavResourceService;
import local.mylan.service.api.NavigationService;
import local.mylan.service.api.events.CrudOperation;
import local.mylan.service.api.events.DeviceCrudEvent;
import local.mylan.service.api.events.DiscoveryDevicesEvent;
import local.mylan.service.api.model.Device;
import local.mylan.service.api.model.DeviceIpAddress;
import local.mylan.service.api.model.DeviceProtocol;
import local.mylan.service.api.model.DeviceState;
import local.mylan.service.spi.model.EncryptedDeviceAccountWithCredentials;
import local.mylan.service.test.TestEncryptionService;
import local.mylan.service.test.TestNotificationService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NetworkNavigationServiceTest {

    private static final Integer DEVICE_ID1 = 1;
    private static final Integer DEVICE_ID2 = 2;
    private static final Integer DEVICE_ID3 = 3;
    private static final String DEVICE_NAME1 = "NAME1";
    private static final String DEVICE_NAME2 = "NAME2";
    private static final String DEVICE_NAME3 = "NAME3";
    private static final String IP1 = "192.168.1.101";
    private static final String IP2 = "192.168.1.102";
    private static final String IP3 = "192.168.1.103";
    private static final String IP4 = "192.168.1.104";

    static EncryptedDeviceAccountWithCredentials.Encryptor encryptor;
    static EncryptedDeviceAccountWithCredentials.Decryptor decryptor;

    @Mock
    DeviceAccessor accessor;
    @Mock
    NavResourceService navResourceService;
    @Captor
    ArgumentCaptor<List<Device>> deviceListCaptor;

    TestNotificationService notificationService;
    NavigationService service;

    @BeforeAll
    static void beforeAll() {
        final var encService = new TestEncryptionService();
        encryptor = encService.credentialsEncryptor();
        decryptor = encService.credentialsDecryptor();
    }

    @BeforeEach
    void beforeEach() {
        notificationService = new TestNotificationService();
    }

    @Test
    void deviceStates() {
        // setup
        // initial data from db
        doReturn(List.of(
            device(DEVICE_ID1, DEVICE_NAME1, SMB, List.of(IP1), null),
            device(DEVICE_ID2, DEVICE_NAME2, NFS, List.of(IP2), null)
        )).when(navResourceService).getAllDevices();
        doReturn(List.of()).when(navResourceService).getAllAccountsWithCredentials();

        //discovery result
        final var discoveryEvent = new DiscoveryDevicesEvent(List.of(
            device(null, DEVICE_NAME1, SMB, List.of(IP4), null),
            device(null, DEVICE_NAME3, NFS, List.of(IP3), null)));

        // new device determined by nav resource service after discovery event sync
        final var newDeviceEvent = new DeviceCrudEvent(DEVICE_ID3, CrudOperation.CREATE);
        doReturn(device(DEVICE_ID3, DEVICE_NAME3, NFS, List.of(IP3), null))
            .when(navResourceService).getDevice(DEVICE_ID3);

        // start service
        service = new NetworkNavigationService(navResourceService, notificationService, List.of(accessor));

        // all devices marked offline initially
        assertDeviceList(List.of(
                device(DEVICE_ID1, DEVICE_NAME1, SMB, List.of(IP1), OFFLINE),
                device(DEVICE_ID2, DEVICE_NAME2, NFS, List.of(IP2), OFFLINE)),
            service.listDevices());

        // discovery event
        notificationService.raiseEvent(discoveryEvent);
        verify(navResourceService, times(1)).syncDeviceAddresses(deviceListCaptor.capture());
        assertDeviceList(discoveryEvent.devices(), deviceListCaptor.getValue(), Device::getIdentifier);

        // new device event
        notificationService.raiseEvent(newDeviceEvent);
        assertDeviceList(List.of(
                device(DEVICE_ID1, DEVICE_NAME1, SMB, List.of(IP4), ONLINE),
                device(DEVICE_ID2, DEVICE_NAME2, NFS, List.of(IP2), OFFLINE),
                device(DEVICE_ID3, DEVICE_NAME3, NFS, List.of(IP3), ONLINE)),
            service.listDevices());
    }

    private static Device device(final Integer deviceId, final String deviceName, final DeviceProtocol protocol,
        final List<String> ipList, final DeviceState state) {

        final var device = new Device(deviceName, protocol);
        device.setDeviceId(deviceId);
        device.setIpAddresses(ipList.stream().map(DeviceIpAddress::new).toList());
        device.setState(state);
        return device;
    }

    private static <K> void assertDeviceList(final List<Device> expectedList, final List<Device> actualList) {
        assertDeviceList(expectedList, actualList, Device::getDeviceId);
    }

    private static <K> void assertDeviceList(final List<Device> expectedList, final List<Device> actualList,
        final Function<Device, K> keyBuilder) {
        assertNotNull(actualList);
        assertEquals(expectedList.size(), actualList.size());

        final var expectedMap = toMap(expectedList, keyBuilder);
        final var actualMap = toMap(actualList, keyBuilder);
        for (var entry : expectedMap.entrySet()) {
            assertDevice(entry.getValue(), actualMap.get(entry.getKey()));
        }
    }

    private static void assertDevice(final Device expected, final Device actual) {
        assertNotNull(actual);
        assertEquals(expected.getDeviceId(), actual.getDeviceId());
        assertEquals(expected.getIdentifier(), actual.getIdentifier());
        assertEquals(expected.getProtocol(), actual.getProtocol());
        assertDeviceIpAddresses(expected.getIpAddresses(), actual.getIpAddresses());
        assertEquals(expected.getState(), actual.getState());
    }

    private static void assertDeviceIpAddresses(final List<DeviceIpAddress> expected,
        final List<DeviceIpAddress> actual) {

        assertNotNull(actual);
        final var expectedSet = expected.stream().map(DeviceIpAddress::getIpAddress).collect(Collectors.toSet());
        final var actualSet = actual.stream().map(DeviceIpAddress::getIpAddress).collect(Collectors.toSet());
        assertEquals(expectedSet, actualSet);
    }

    private static <K, V> Map<K, V> toMap(final List<V> list, final Function<V, K> keyBuilder) {
        return list.stream().collect(Collectors.toMap(keyBuilder, value -> value));
    }

}
