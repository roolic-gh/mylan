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
package local.mylan.service.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import local.mylan.service.api.model.Device;
import local.mylan.service.api.model.DeviceAccount;
import local.mylan.service.api.model.DeviceIpAddress;
import local.mylan.service.api.model.DeviceProtocol;
import local.mylan.service.api.model.DeviceState;

public final class NavResourceTestUtils {

    private NavResourceTestUtils() {
        // utility class
    }

    // devices

    public static Device device(final Integer deviceId, final String deviceName, final DeviceProtocol protocol,
        final List<String> ipList, final DeviceState state) {

        final var device = new Device(deviceName, protocol);
        device.setDeviceId(deviceId);
        device.setIpAddresses(ipList.stream().map(DeviceIpAddress::new).toList());
        device.setState(state);
        return device;
    }

    public static <K> void assertDeviceList(final List<Device> expectedList, final List<Device> actualList) {
        assertDeviceList(expectedList, actualList, Device::getDeviceId);
    }

    public static <K> void assertDeviceList(final List<Device> expectedList, final List<Device> actualList,
        final Function<Device, K> keyBuilder) {

        assertList(expectedList, actualList, keyBuilder, NavResourceTestUtils::assertDevice);
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

    // device accounts

    public static DeviceAccount deviceAccount(final Integer accountId, final Integer userId, final Integer deviceId,
        final String username, final String password, final String key) {
        final var account = new DeviceAccount(deviceId, username, password, key);
        account.setAccountId(accountId);
        account.setUserId(userId);
        return account;
    }

    public static void assertAccountList(final List<DeviceAccount> expected, final List<DeviceAccount> actual) {
        assertList(expected, actual, DeviceAccount::getAccountId, NavResourceTestUtils::assertAccount);
    }

    public static void assertAccount(final DeviceAccount expected, final DeviceAccount actual) {
        assertAccount(expected, actual, false);
    }

    public static void assertAccountWithPassword(final DeviceAccount expected, final DeviceAccount actual) {
        assertAccount(expected, actual, true);
    }

    private static void assertAccount(final DeviceAccount expected, final DeviceAccount actual, boolean withPassword) {
        assertNotNull(actual);
        assertEquals(expected.getDeviceId(), actual.getDeviceId());
        assertEquals(expected.getUserId(), actual.getUserId());
        assertEquals(expected.getUsername(), actual.getUsername());
        if (withPassword) {
            assertEquals(expected.getPassword(), actual.getPassword());
            assertEquals(expected.getKey(), actual.getKey());
        } else {
            assertNull(actual.getPassword());
            assertNull(actual.getKey());
        }
    }

    // utility

    public static <K, V> void assertList(final List<V> expectedList, final List<V> actualList,
        final Function<V, K> keyBuilder, final ValueAsserter<V> asserter) {

        assertNotNull(actualList);
        assertEquals(expectedList.size(), actualList.size());

        final var expectedMap = toMap(expectedList, keyBuilder);
        final var actualMap = toMap(actualList, keyBuilder);
        for (var entry : expectedMap.entrySet()) {
            asserter.assertValue(entry.getValue(), actualMap.get(entry.getKey()));
        }
    }

    public static <K, V> Map<K, V> toMap(final List<V> list, final Function<V, K> keyBuilder) {
        return list.stream().collect(Collectors.toMap(keyBuilder, value -> value));
    }

    @FunctionalInterface
    interface ValueAsserter<V> {
        void assertValue(V expected, V actual);
    }
}
