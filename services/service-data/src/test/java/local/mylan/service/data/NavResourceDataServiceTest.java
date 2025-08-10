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
package local.mylan.service.data;

import static java.util.stream.Collectors.toMap;
import static local.mylan.service.data.NavResourceDataService.LOCAL_DEVICE_IDENTIFIER;
import static local.mylan.service.data.TestUtils.notificationService;
import static local.mylan.service.data.TestUtils.setupSessionFactory;
import static local.mylan.service.data.TestUtils.tearDownSessionFactory;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import local.mylan.service.api.NavResourceService;
import local.mylan.service.api.exceptions.DataCollisionException;
import local.mylan.service.api.exceptions.UnauthorizedException;
import local.mylan.service.api.model.Device;
import local.mylan.service.api.model.DeviceCredentials;
import local.mylan.service.api.model.DeviceIpAddress;
import local.mylan.service.api.model.DeviceProtocol;
import local.mylan.service.api.model.DeviceWithCredentials;
import local.mylan.service.api.model.NavResourceShare;
import local.mylan.service.api.model.ShareType;
import local.mylan.service.api.model.User;
import local.mylan.service.data.entities.DeviceCredEntity;
import local.mylan.service.data.entities.DeviceEntity;
import local.mylan.service.data.entities.DeviceIpAddressEntity;
import local.mylan.service.data.entities.NavResourceBookmarkEntity;
import local.mylan.service.data.entities.NavResourceShareEntity;
import local.mylan.service.data.entities.UserEntity;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class NavResourceDataServiceTest {

    private static final String USERNAME_1 = "username-1";
    private static final String USERNAME_2 = "username-2";
    private static final String USERNAME_3 = "username-3";
    private static final String PASSWORD_1 = "pa$$W0rd";
    private static final String PASSWORD_2 = "Pa$$w0rd";
    private static final String PASSWORD_3 = "pA$$w0RD";
    private static final String CRYPT_KEY = "crypt-key";

    private static final String IDENTIFIER_1 = "identifier-1";
    private static final String IDENTIFIER_2 = "identifier-2";
    private static final String IP_1 = "192.168.1.101";
    private static final String IP_2 = "192.168.1.102";

    private static final String SHARE_NAME_1 = "share-name-1";
    private static final String SHARE_NAME_2 = "share-name-2";
    private static final String SHARE_NAME_3 = "share-name-3";
    private static final String SHARE_NAME_UPDATED = "share-name-UPDATED";
    private static final String SHARE_PATH_1 = "/share/path/1";
    private static final String SHARE_PATH_2 = "/share/path/2";
    private static final String SHARE_PATH_3 = "/share/path/3";

    private static final String BOOKMARK_1 = "bookmark-1";
    private static final String BOOKMARK_2 = "bookmark-2";
    private static final String BOOKMARK_PATH_1 = "/bookmark/path/1";
    private static final String BOOKMARK_PATH_2 = "/bookmark/path/2";

    static SessionFactory sessionFactory;
    static NavResourceService navResourceService;
    static User user1;
    static User user2;
    static Device userDevice1;
    static Device userDevice2;
    static Device localDevice;

    static NavResourceShare localShareAll;
    static NavResourceShare localShareReg;

    static Long shareId1;
    static Long shareId2;
    static Long shareId3;
    static Long bookmarkId1;
    static Long bookmarkId2;

    @BeforeAll
    static void beforeAll() {
        sessionFactory = setupSessionFactory(UserEntity.class, DeviceEntity.class, DeviceCredEntity.class,
            DeviceIpAddressEntity.class, NavResourceShareEntity.class, NavResourceBookmarkEntity.class);
        navResourceService = new NavResourceDataService(sessionFactory, notificationService());

        user1 = createUser("user-1");
        user2 = createUser("user-2");
        localDevice = navResourceService.getLocalDevice();
        assertNotNull(localDevice);
        assertNotNull(localDevice.getDeviceId());
        assertEquals(LOCAL_DEVICE_IDENTIFIER, localDevice.getIdentifier());
        assertEquals(DeviceProtocol.LOCAL, localDevice.getProtocol());
        assertNull(localDevice.getUserId());
    }

    private static User createUser(final String username) {
        final var userId = sessionFactory.fromTransaction(session -> {
            final var userEntity = new UserEntity(username, false);
            session.persist(userEntity);
            return userEntity.getUserId();
        });
        return new User(userId, username, username, false);
    }

    @AfterAll
    static void afterAll() {
        tearDownSessionFactory(sessionFactory);
    }

    @Test
    @Order(1)
    void createDevice() {
        final var device = new Device(IDENTIFIER_1, DeviceProtocol.NFS);
        device.setIpAddresses(List.of(new DeviceIpAddress(IP_1)));
        final var creds = new DeviceCredentials(USERNAME_1, PASSWORD_1);
        userDevice1 = navResourceService.createUserDevice(user1.getUserId(), device, creds);
        assertDevice(userDevice1, user1.getUserId(), IDENTIFIER_1, DeviceProtocol.NFS, USERNAME_1, false);
        assertDeviceIpAddresses(userDevice1.getIpAddresses(), Set.of(IP_1));

        userDevice2 = navResourceService.createUserDevice(user2.getUserId(),
            new Device(IDENTIFIER_2, DeviceProtocol.SMB),
            new DeviceCredentials(USERNAME_2, PASSWORD_2, CRYPT_KEY));
        assertDevice(userDevice2, user2.getUserId(), IDENTIFIER_2, DeviceProtocol.SMB, USERNAME_2, true);

        // non-existing user
        assertThrows(IllegalArgumentException.class,
            () -> navResourceService.createUserDevice(-1, device, creds));

        // identifier exists
        assertThrows(DataCollisionException.class,
            () -> navResourceService.createUserDevice(user2.getUserId(), device, creds));
    }

    @Test
    @Order(2)
    void getUserDevices() {
        assertUserList(user1.getUserId(), userDevice1);
        assertUserList(user2.getUserId(), userDevice2);
    }

    private static void assertUserList(final int userId, final Device expected) {
        final var list = navResourceService.getUserDevices(userId);
        assertNotNull(list);
        assertEquals(1, list.size());
        assertDevice(expected, list.getFirst());
    }

    @Test
    @Order(3)
    void getWithCredentials() {
        final var fullLocal = navResourceService.getDevice(localDevice.getDeviceId());
        assertWithCredentials(fullLocal, localDevice, null, null);
        final var fullDevice1 = navResourceService.getDevice(userDevice1.getDeviceId());
        assertWithCredentials(fullDevice1, userDevice1, PASSWORD_1, null);
        assertDeviceIpAddresses(fullDevice1.getIpAddresses(), Set.of(IP_1));
        final var fullDevice2 = navResourceService.getDevice(userDevice2.getDeviceId());
        assertWithCredentials(fullDevice2, userDevice2, PASSWORD_2, CRYPT_KEY);

        final var list = navResourceService.getAllDevices();
        assertNotNull(list);
        assertEquals(3, list.size());
        final var map = list.stream().collect(toMap(DeviceWithCredentials::getDeviceId, Function.identity()));
        assertWithCredentials(map.get(localDevice.getDeviceId()), localDevice, null, null);
        assertWithCredentials(map.get(userDevice1.getDeviceId()), userDevice1, PASSWORD_1, null);
        assertWithCredentials(map.get(userDevice2.getDeviceId()), userDevice2, PASSWORD_2, CRYPT_KEY);
    }

    private static void assertDevice(final Device device, final Integer userId, final String identifier,
        final DeviceProtocol protocol, final String username, final boolean keyLocked) {
        assertNotNull(device);
        assertNotNull(device.getDeviceId());
        assertEquals(userId, device.getUserId());
        assertEquals(identifier, device.getIdentifier());
        assertEquals(protocol, device.getProtocol());
        assertEquals(username, device.getUsername());
        assertEquals(keyLocked, device.isKeyLocked());
    }

    private static void assertDevice(final Device expected, final Device actual) {
        assertNotNull(actual);
        assertEquals(expected.getDeviceId(), actual.getDeviceId());
        assertEquals(expected.getUserId(), actual.getUserId());
        assertEquals(expected.getIdentifier(), actual.getIdentifier());
        assertEquals(expected.getProtocol(), actual.getProtocol());
        assertEquals(expected.getUsername(), actual.getUsername());
        assertEquals(expected.isKeyLocked(), actual.isKeyLocked());
    }

    private static void assertWithCredentials(final DeviceWithCredentials actual, final Device expected,
        final String password, final String key) {
        assertNotNull(actual);
        assertEquals(expected.getDeviceId(), actual.getDeviceId());
        assertEquals(expected.getIdentifier(), actual.getIdentifier());
        assertEquals(expected.getProtocol(), actual.getProtocol());
        if (expected.getUserId() == null) {
            // local
            assertNull(actual.getUserId());
            assertNull(actual.getCredentials());
        } else {
            assertEquals(expected.getUserId(), actual.getUserId());
            final var creds = actual.getCredentials();
            assertNotNull(creds);
            assertEquals(expected.getUsername(), creds.getUsername());
            assertEquals(password, creds.getPassword());
            if (key == null) {
                assertNull(creds.getKey());
            } else {
                assertEquals(key, creds.getKey());
            }
        }
    }

    @Test
    @Order(4)
    void updateCredentials() {
        final var creds = new DeviceCredentials(USERNAME_3, PASSWORD_3, CRYPT_KEY);
        navResourceService.updateDeviceCredentials(userDevice1.getDeviceId(), creds);
        final var check = navResourceService.getDevice(userDevice1.getDeviceId());
        assertNotNull(check);
        final var checkCreds = check.getCredentials();
        assertNotNull(checkCreds);
        assertEquals(USERNAME_3, checkCreds.getUsername());
        assertEquals(PASSWORD_3, checkCreds.getPassword());
        assertEquals(CRYPT_KEY, checkCreds.getKey());

        // cannot update non-existent creds
        assertThrows(IllegalArgumentException.class, () ->
            navResourceService.updateDeviceCredentials(-1, creds));
        assertThrows(IllegalArgumentException.class, () ->
            navResourceService.updateDeviceCredentials(localDevice.getDeviceId(), creds));
    }

    @Test
    @Order(5)
    void addIpAddress() {
        final var deviceId = userDevice1.getDeviceId();
        // already assigned to same device - omit error
        navResourceService.addDeviceAddress(deviceId, new DeviceIpAddress(IP_1));
        // new ip address
        navResourceService.addDeviceAddress(deviceId, new DeviceIpAddress(IP_2));
        final var check = navResourceService.getDevice(deviceId);
        assertDeviceIpAddresses(check.getIpAddresses(), Set.of(IP_1, IP_2));
        // already assigned to other device - collision error
        assertThrows(DataCollisionException.class, () ->
            navResourceService.addDeviceAddress(userDevice2.getDeviceId(), new DeviceIpAddress(IP_1)));
    }

    @Test
    @Order(6)
    void removeIpAddress() {
        navResourceService.removeDeviceAddress(userDevice1.getDeviceId(), IP_2);
        // wrong device - collision error
        assertThrows(DataCollisionException.class, () ->
            navResourceService.removeDeviceAddress(userDevice2.getDeviceId(), IP_1));
        // removing non-existent ip address - no error
        navResourceService.removeDeviceAddress(userDevice1.getDeviceId(), IP_2);
        navResourceService.removeDeviceAddress(userDevice2.getDeviceId(), IP_2);
        // validate db
        final var check = navResourceService.getDevice(userDevice1.getDeviceId());
        assertDeviceIpAddresses(check.getIpAddresses(), Set.of(IP_1));
    }

    private static void assertDeviceIpAddresses(final List<DeviceIpAddress> ipAddresses, final Set<String> expected) {
        assertNotNull(ipAddresses);
        final var actual = ipAddresses.stream().map(DeviceIpAddress::getIpAddress).collect(Collectors.toSet());
        assertEquals(expected, actual);
    }

    @Test
    @Order(7)
    void syncLocalShares() {
        final var local1 = new NavResourceShare(null, SHARE_NAME_1, SHARE_PATH_1, ShareType.ALL);
        final var local2 = new NavResourceShare(null, SHARE_NAME_2, SHARE_PATH_2, ShareType.ALL);
        final var local2upd = new NavResourceShare(null, SHARE_NAME_UPDATED, SHARE_PATH_2, ShareType.REGISTERED);
        final var local3 = new NavResourceShare(null, SHARE_NAME_3, SHARE_PATH_3, ShareType.ALL);
        final var local4 = new NavResourceShare(null, SHARE_NAME_2, SHARE_PATH_2, ShareType.REGISTERED);

        // no shares
        final var initial = navResourceService.getAllSharedResources();
        assertNotNull(initial);
        assertTrue(initial.isEmpty());

        // first sync - 2 entries added
        final var firstExpected = List.of(local1, local2);
        navResourceService.syncLocalSharedResources(firstExpected);
        final var firstActual = navResourceService.getAllSharedResources();
        assertSharesByPath(firstExpected, firstActual);

        // second sync -> 1 removed, 2 udated, 3 inserted
        final var secondExpected = List.of(local2upd, local3);
        navResourceService.syncLocalSharedResources(secondExpected);
        final var secondActual = navResourceService.getAllSharedResources();
        assertSharesByPath(secondExpected, secondActual);
        // assert 2nd entry updated -> id is same
        final var original = byPath(firstActual, SHARE_PATH_2);
        final var updated = byPath(secondActual, SHARE_PATH_2);
        assertEquals(original.getShareId(),updated.getShareId());

        // third sync replace all for below tests
        final var thirdExpected = List.of(local1, local4);
        navResourceService.syncLocalSharedResources(firstExpected);
        final var thirdActual = navResourceService.getAllSharedResources();
        assertSharesByPath(thirdExpected, thirdActual);
        localShareAll = byPath(thirdActual, SHARE_PATH_1);
        localShareReg = byPath(thirdActual, SHARE_PATH_2);
    }

    private static NavResourceShare byPath(final List<NavResourceShare> list, final String path){
        return list.stream().filter(share -> path.equals(share.getPath())).findFirst().orElseThrow();
    }

    private static void assertSharesByPath(final List<NavResourceShare> expected, final List<NavResourceShare> actual) {
        assertNotNull(actual);
        assertEquals(expected.size(), actual.size());
        final var expectedMap = expected.stream().collect(toMap(NavResourceShare::getPath, Function.identity()));
        final var actualMap = actual.stream().collect(toMap(NavResourceShare::getPath, Function.identity()));
        for (var entry : expectedMap.entrySet()) {
            assertShare(entry.getValue(), actualMap.get(entry.getKey()));
        }
    }

    private static void assertShare(final NavResourceShare expected, final NavResourceShare actual) {
        assertNotNull(actual);
        assertNotNull(actual.getShareId());
        if (expected.getShareId() != null) {
            assertEquals(expected.getShareId(), actual.getShareId());
        }
        assertEquals(expected.getResourceName(), actual.getResourceName());

        if (expected.getDeviceId() == null) {
            assertEquals(localDevice.getDeviceId(), actual.getDeviceId());
        } else {
            assertEquals(expected.getDeviceId(), actual.getDeviceId());
        }
        assertEquals(expected.getPath(), actual.getPath());
    }

    @Test
    @Order(20)
    void removeDevice() {
        navResourceService.removeDevice(userDevice1.getUserId(), userDevice1.getDeviceId());
        assertNull(navResourceService.getDevice(userDevice1.getDeviceId()));

        // TODO validate dependent resources removed

        // device does not belong to user
        assertThrows(UnauthorizedException.class, () ->
            navResourceService.removeDevice(userDevice1.getUserId(), userDevice2.getDeviceId()));
    }

}
