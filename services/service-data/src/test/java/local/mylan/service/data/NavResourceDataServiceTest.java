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

import static local.mylan.service.data.NavResourceDataService.LOCAL_ACCOUNT_USERNAME;
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
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import local.mylan.service.api.NavResourceService;
import local.mylan.service.api.exceptions.DataCollisionException;
import local.mylan.service.api.exceptions.UnauthorizedException;
import local.mylan.service.api.model.Device;
import local.mylan.service.api.model.DeviceAccount;
import local.mylan.service.api.model.DeviceIpAddress;
import local.mylan.service.api.model.DeviceProtocol;
import local.mylan.service.api.model.NavResource;
import local.mylan.service.api.model.NavResourceBookmark;
import local.mylan.service.api.model.NavResourceShare;
import local.mylan.service.api.model.ShareType;
import local.mylan.service.data.entities.DeviceAccountEntity;
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
    private static final String USERNAME_X = "username-X";
    private static final String PASSWORD_1 = "pa$$W0rd";
    private static final String PASSWORD_2 = "Pa$$w0rd";
    private static final String PASSWORD_X = "Pa5$wXXX";
    private static final String CRYPT_KEY = "crypt-key";

    private static final String IDENTIFIER_1 = "identifier-1";
    private static final String IDENTIFIER_2 = "identifier-2";
    private static final String IP_1 = "192.168.1.101";
    private static final String IP_2 = "192.168.1.102";

    private static final String SHARE_LOCAL_1 = "share-local-1";
    private static final String SHARE_LOCAL_2 = "share-local-2";
    private static final String SHARE_LOCAL_3 = "share-local-3";
    private static final String SHARE_NAME_1 = "share-name-1";
    private static final String SHARE_NAME_2 = "share-name-2";
    private static final String SHARE_NAME_3 = "share-name-3";
    private static final String SHARE_NAME_X = "share-name-X";
    private static final String SHARE_PATH_1 = "/share/path/1";
    private static final String SHARE_PATH_2 = "/share/path/2";
    private static final String SHARE_PATH_3 = "/share/path/3";
    private static final String BOOKMARK_PATH_1 = "/bookmark/path/1";
    private static final String BOOKMARK_PATH_2 = "/bookmark/path/2";
    private static final String BOOKMARK_PATH_3 = "/bookmark/path/3";

    static SessionFactory sessionFactory;
    static NavResourceService navResourceService;

    static Integer userId1;
    static Integer userId2;
    static Integer localDeviceId;
    static Integer localAccountId;
    static Device device1;
    static Device device2;
    static DeviceAccount account1;
    static DeviceAccount account2;
    static DeviceAccount account3;
    static NavResourceShare localShareAll;
    static NavResourceShare localShareReg;
    static NavResourceShare share1;
    static NavResourceShare share2;
    static NavResourceShare share3;
    static NavResourceBookmark bookmark1;
    static NavResourceBookmark bookmark2;
    static NavResourceBookmark bookmark3;
    static NavResourceBookmark bookmark4;
    static NavResourceBookmark bookmark5;
    static NavResourceBookmark bookmark6;
    static NavResourceBookmark bookmark7;

    @BeforeAll
    static void beforeAll() {
        sessionFactory = setupSessionFactory(UserEntity.class, DeviceEntity.class, DeviceAccountEntity.class,
            DeviceIpAddressEntity.class, NavResourceShareEntity.class, NavResourceBookmarkEntity.class);
        navResourceService = new NavResourceDataService(sessionFactory, notificationService());

        userId1 = newUserId("user-1");
        userId2 = newUserId("user-2");

        final var localAccount = navResourceService.getLocalAccount();
        assertNotNull(localAccount);
        assertNull(localAccount.getUserId());
        assertEquals(LOCAL_ACCOUNT_USERNAME, localAccount.getUsername());
        localAccountId = localAccount.getAccountId();
        assertNotNull(localAccountId);
        localDeviceId = localAccount.getDeviceId();
        assertNotNull(localDeviceId);

        final var localDevice = navResourceService.getDevice(localAccount.getDeviceId());
        assertNotNull(localDevice);
        assertEquals(LOCAL_DEVICE_IDENTIFIER, localDevice.getIdentifier());
        assertEquals(DeviceProtocol.LOCAL, localDevice.getProtocol());
    }

    private static Integer newUserId(final String username) {
        return sessionFactory.fromTransaction(session -> {
            final var userEntity = new UserEntity(username, false);
            session.persist(userEntity);
            return userEntity.getUserId();
        });
    }

    @AfterAll
    static void afterAll() {
        tearDownSessionFactory(sessionFactory);
    }

    @Test
    @Order(11)
    void createDevice() {
        final var inDevice1 = new Device(IDENTIFIER_1, DeviceProtocol.NFS);
        inDevice1.setIpAddresses(List.of(new DeviceIpAddress(IP_1)));
        device1 = navResourceService.createDevice(inDevice1);
        assertDevice(inDevice1, device1);
        assertDeviceIpAddresses(device1.getIpAddresses(), Set.of(IP_1));
        final var checkDevice1 = navResourceService.getDevice(device1.getDeviceId());
        assertDevice(device1, checkDevice1);
        assertDeviceIpAddresses(checkDevice1.getIpAddresses(), Set.of(IP_1));

        final var inDevice2 = new Device(IDENTIFIER_2, DeviceProtocol.SMB);
        device2 = navResourceService.createDevice(inDevice2);
        assertDevice(inDevice2, device2);

        // local not allower
        assertThrows(IllegalArgumentException.class,
            () -> navResourceService.createDevice(new Device("NEW LOCAL", DeviceProtocol.LOCAL)));

        // identifier conflict
        assertThrows(DataCollisionException.class, () -> navResourceService.createDevice(inDevice1));
    }

    private static void assertDevice(final Device expected, final Device actual) {
        assertNotNull(actual);
        if (expected.getDeviceId() == null) {
            assertNotNull(actual.getDeviceId());
        } else {
            assertEquals(expected.getDeviceId(), actual.getDeviceId());
        }
        assertEquals(expected.getIdentifier(), actual.getIdentifier());
        assertEquals(expected.getProtocol(), actual.getProtocol());
    }

    @Test
    @Order(12)
    void getAllDevices() {
        final var list = navResourceService.getAllDevices();
        assertNotNull(list);
        assertEquals(3, list.size());
        final var deviceMap = toMap(list, Device::getDeviceId);
        assertDevice(device1, deviceMap.get(device1.getDeviceId()));
        assertDevice(device2, deviceMap.get(device2.getDeviceId()));
        assertNotNull(deviceMap.get(localDeviceId));
    }

    @Test
    @Order(13)
    void addIpAddress() {
        final var deviceId = device1.getDeviceId();
        // already assigned to same device - omit error
        navResourceService.addDeviceAddress(deviceId, new DeviceIpAddress(IP_1));
        // new ip address
        navResourceService.addDeviceAddress(deviceId, new DeviceIpAddress(IP_2));
        final var check = navResourceService.getDevice(deviceId);
        assertDeviceIpAddresses(check.getIpAddresses(), Set.of(IP_1, IP_2));
        // already assigned to other device - collision error
        assertThrows(DataCollisionException.class, () ->
            navResourceService.addDeviceAddress(device2.getDeviceId(), new DeviceIpAddress(IP_1)));
    }

    private static void assertDeviceIpAddresses(final List<DeviceIpAddress> ipAddresses, final Set<String> expected) {
        assertNotNull(ipAddresses);
        final var actual = ipAddresses.stream().map(DeviceIpAddress::getIpAddress).collect(Collectors.toSet());
        assertEquals(expected, actual);
    }

    @Test
    @Order(14)
    void removeIpAddress() {
        navResourceService.removeDeviceAddress(device1.getDeviceId(), IP_2);
        // wrong device - collision error
        assertThrows(DataCollisionException.class, () ->
            navResourceService.removeDeviceAddress(device2.getDeviceId(), IP_1));
        // removing non-existent ip address - no error
        navResourceService.removeDeviceAddress(device1.getDeviceId(), IP_2);
        navResourceService.removeDeviceAddress(device2.getDeviceId(), IP_2);
        // validate db
        final var check = navResourceService.getDevice(device1.getDeviceId());
        assertDeviceIpAddresses(check.getIpAddresses(), Set.of(IP_1));
    }

    @Test
    @Order(21)
    void createAccount() {
        // single account to device 1
        final var inAccount1 = new DeviceAccount(device1.getDeviceId(), USERNAME_1, PASSWORD_1);
        account1 = navResourceService.createAccount(userId1, inAccount1);
        assertAccount(inAccount1, account1, true);
        final var checkAccount1 = navResourceService.getAccount(account1.getAccountId());
        assertAccount(account1, checkAccount1, true);

        // 2 accounts to device 2
        final var inAccount2 = new DeviceAccount(device2.getDeviceId(), USERNAME_2, PASSWORD_2, CRYPT_KEY);
        account2 = navResourceService.createAccount(userId2, inAccount2);
        assertAccount(inAccount2, account2, true);

        final var inAccount3 = new DeviceAccount(device2.getDeviceId(), USERNAME_1, PASSWORD_1);
        account3 = navResourceService.createAccount(userId2, inAccount3);
        assertAccount(inAccount3, account3, true);

        // error cases
        // local device is not allowed
        assertThrows(IllegalArgumentException.class, () -> navResourceService.createAccount(
            userId1, new DeviceAccount(localDeviceId, USERNAME_X, PASSWORD_X)));
        // account with same username already defined on target device
        assertThrows(DataCollisionException.class, () -> navResourceService.createAccount(
            userId1, new DeviceAccount(device1.getDeviceId(), USERNAME_1, PASSWORD_X)));
        // non-existing user
        assertThrows(IllegalArgumentException.class, () -> navResourceService.createAccount(
            -1, new DeviceAccount(device1.getDeviceId(), USERNAME_2, PASSWORD_X)));
    }

    private static void assertAccount(final DeviceAccount expected, final DeviceAccount actual,
        final boolean withPassword) {

        assertNotNull(actual);
        assertEquals(expected.getDeviceId(), actual.getDeviceId());
        if (localDeviceId.equals(expected.getDeviceId())) {
            // local
            assertNull(actual.getUserId());
            assertNull(actual.getUsername());
            assertNull(actual.getPassword());
            assertNull(actual.getKey());
            return;
        }
        if (expected.getUserId() == null) {
            assertNotNull(actual.getUserId());
        } else {
            assertEquals(expected.getUserId(), actual.getUserId());
        }
        assertEquals(expected.getUsername(), actual.getUsername());
        if (withPassword) {
            assertEquals(expected.getPassword(), actual.getPassword());
            if (expected.getKey() == null) {
                assertNull(actual.getKey());
            } else {
                assertEquals(expected.getKey(), actual.getKey());
            }
        } else {
            assertNull(actual.getPassword());
            assertNull(actual.getKey());
        }
    }

    @Test
    @Order(22)
    void updateAccount() {
        // only accountId and credentials are taken from input
        final var update = new DeviceAccount(null, USERNAME_X, PASSWORD_X, CRYPT_KEY);
        update.setAccountId(account3.getAccountId());
        navResourceService.updateAccount(userId2, update);

        // validate credentials only updated
        final var check = navResourceService.getAccount(account3.getAccountId());
        assertNotNull(check);
        assertEquals(userId2, check.getUserId());
        assertEquals(device2.getDeviceId(), check.getDeviceId());
        assertEquals(USERNAME_X, check.getUsername());
        assertEquals(PASSWORD_X, check.getPassword());
        assertEquals(CRYPT_KEY, check.getKey());
        account3 = check;

        // error cases
        // non-existent account
        update.setAccountId(-1);
        assertThrows(IllegalArgumentException.class, () -> navResourceService.updateAccount(userId2, update));

        // local account is not eligible for update
        update.setAccountId(localAccountId);
        assertThrows(IllegalArgumentException.class, () -> navResourceService.updateAccount(userId2, update));
        // account owned by other user
        update.setAccountId(account1.getAccountId());
        assertThrows(UnauthorizedException.class, () -> navResourceService.updateAccount(userId2, update));
    }

    @Test
    @Order(23)
    void getAccounts() {
        final var allList = navResourceService.getAllAccounts();
        assertNotNull(allList);
        assertEquals(4, allList.size());
        final var allMap = toMap(allList, DeviceAccount::getAccountId);
        assertAccount(account1, allMap.get(account1.getAccountId()), true);
        assertAccount(account2, allMap.get(account2.getAccountId()), true);
        assertAccount(account3, allMap.get(account3.getAccountId()), true);
        assertNotNull(allMap.get(localAccountId));

        final var list1 = navResourceService.getUserAccounts(userId1);
        assertNotNull(list1);
        assertEquals(1, list1.size());
        assertAccount(account1, list1.getFirst(), false);

        final var list2 = navResourceService.getUserAccounts(userId2);
        assertNotNull(list2);
        assertEquals(2, list2.size());
        final var map = toMap(list2, DeviceAccount::getAccountId);
        assertAccount(account2, map.get(account2.getAccountId()), false);
        assertAccount(account3, map.get(account3.getAccountId()), false);
    }

    @Test
    @Order(24)
    void removeAccount() {
        navResourceService.removeAccount(userId2, account3.getAccountId());
        account3 = navResourceService.getAccount(account3.getAccountId());
        assertNull(account3);

        // removing local account is not allowed
        assertThrows(IllegalArgumentException.class, () -> navResourceService.removeAccount(userId2, localAccountId));
        // account belongs to other user
        assertThrows(UnauthorizedException.class,
            () -> navResourceService.removeAccount(userId2, account1.getAccountId()));
    }

    @Test
    @Order(31)
    void syncLocalShares() {
        final var local1 = new NavResourceShare(null, SHARE_LOCAL_1, SHARE_PATH_1, ShareType.ALL);
        final var local2 = new NavResourceShare(null, SHARE_LOCAL_2, SHARE_PATH_2, ShareType.ALL);
        final var local2upd = new NavResourceShare(null, SHARE_NAME_X, SHARE_PATH_2, ShareType.REGISTERED);
        final var local2final = new NavResourceShare(null, SHARE_LOCAL_2, SHARE_PATH_2, ShareType.REGISTERED);
        final var local3 = new NavResourceShare(null, SHARE_LOCAL_3, SHARE_PATH_3, ShareType.ALL);

        // no shares
        final var initial = navResourceService.getAllSharedResources();
        assertNotNull(initial);
        assertTrue(initial.isEmpty());

        // 1st sync - 2 entries added
        final var expected1 = List.of(local1, local2);
        navResourceService.syncLocalSharedResources(expected1);
        final var actual1 = navResourceService.getAllSharedResources();
        assertSharesByPath(expected1, actual1);

        // 2nd sync -> 1 removed, 1 udated, 1 inserted
        final var expected2 = List.of(local2upd, local3);
        navResourceService.syncLocalSharedResources(expected2);
        final var actual2 = navResourceService.getAllSharedResources();
        assertSharesByPath(expected2, actual2);
        // assert 2nd entry updated -> id is same
        final var original = byPath(actual1, SHARE_PATH_2);
        final var updated = byPath(actual2, SHARE_PATH_2);
        assertEquals(original.getShareId(), updated.getShareId());

        // 3rd sync replace all for below tests
        final var expected3 = List.of(local1, local2final);
        navResourceService.syncLocalSharedResources(expected3);
        final var actual3 = navResourceService.getAllSharedResources();
        assertSharesByPath(expected3, actual3);
        localShareAll = byPath(actual3, SHARE_PATH_1);
        localShareReg = byPath(actual3, SHARE_PATH_2);
    }

    private static NavResourceShare byPath(final List<NavResourceShare> list, final String path) {
        return list.stream().filter(share -> path.equals(share.getPath())).findFirst().orElseThrow();
    }

    @Test
    @Order(32)
    void createShare() {
        final var inShare1 = new NavResourceShare(account1.getAccountId(), SHARE_NAME_1, SHARE_PATH_1, ShareType.ALL);
        share1 = navResourceService.createSharedResource(userId1, inShare1);
        assertShare(inShare1, share1);

        final var inShare2 =
            new NavResourceShare(account2.getAccountId(), SHARE_NAME_2, SHARE_PATH_2, ShareType.REGISTERED);
        share2 = navResourceService.createSharedResource(userId2, inShare2);
        assertShare(inShare2, share2);

        final var inShare3 =
            new NavResourceShare(account2.getAccountId(), SHARE_NAME_3, SHARE_PATH_3, ShareType.ALL);

        // non-existing user
        assertThrows(IllegalArgumentException.class, () -> navResourceService.createSharedResource(-1, inShare3));
        // user is not account owner
        assertThrows(UnauthorizedException.class, () -> navResourceService.createSharedResource(userId1, inShare3));
        // success
        share3 = navResourceService.createSharedResource(userId2, inShare3);
        assertShare(inShare3, share3);

        // non-existing account
        assertThrows(IllegalArgumentException.class, () -> navResourceService.createSharedResource(userId2,
            new NavResourceShare(-1, SHARE_NAME_X, SHARE_PATH_1, ShareType.ALL)));
        // local account is not allowed
        assertThrows(IllegalArgumentException.class, () -> navResourceService.createSharedResource(userId2,
            new NavResourceShare(localAccountId, SHARE_NAME_X, SHARE_PATH_1, ShareType.ALL)));
    }

    @Test
    @Order(33)
    void updateShare() {
        final var update =
            new NavResourceShare(account2.getAccountId(), SHARE_NAME_X, SHARE_PATH_1, ShareType.REGISTERED);
        update.setShareId(share3.getShareId());
        // user is not the owner
        assertThrows(UnauthorizedException.class, () -> navResourceService.updateSharedResource(userId1, update));
        // success
        navResourceService.updateSharedResource(userId2, update);
        // only share name and share type expected to be updated, path to remain same
        final var expected =
            new NavResourceShare(account2.getAccountId(), SHARE_NAME_X, SHARE_PATH_3, ShareType.REGISTERED);
        expected.setShareId(share3.getShareId());
        assertShare(expected, navResourceService.getSharedResource(share3.getShareId()));
    }

    @Test
    @Order(34)
    void removeShare() {
        // non-existing share remove omitted with no errors
        navResourceService.removeSharedResource(userId1, -1L);
        // user is not the owner
        assertThrows(UnauthorizedException.class,
            () -> navResourceService.removeSharedResource(userId1, share3.getShareId()));
        // success
        navResourceService.removeSharedResource(userId2, share3.getShareId());
        assertNull(navResourceService.getSharedResource(share3.getShareId()));
    }

    @Test
    @Order(35)
    void getShares() {
        // shares expected for registered and guest users accordingly
        final var expectedReg = List.of(localShareAll, localShareReg, share1, share2);
        final var expectedGuest = List.of(localShareAll, share1);

        assertSharesById(expectedReg, navResourceService.getAllSharedResources(), true);
        assertSharesById(expectedGuest, navResourceService.getSharedResources(null), false);
        assertSharesById(expectedReg, navResourceService.getSharedResources(userId1), false);
        assertSharesById(expectedReg, navResourceService.getSharedResources(userId2), false);
    }

    private static void assertSharesByPath(final List<NavResourceShare> expected,
        final List<NavResourceShare> actual) {
        assertSharesByKey(expected, actual, NavResourceShare::getPath, true);
    }

    private static void assertSharesById(final List<NavResourceShare> expected, final List<NavResourceShare> actual,
        final boolean full) {
        assertSharesByKey(expected, actual, NavResourceShare::getShareId, full);
    }

    private static void assertSharesByKey(final List<NavResourceShare> expected, final List<NavResourceShare> actual,
        final Function<NavResourceShare, Object> keyExtractor, final boolean full) {

        assertNotNull(actual);
        assertEquals(expected.size(), actual.size());
        final var expectedMap = toMap(expected, keyExtractor);
        final var actualMap = toMap(actual, keyExtractor);
        for (var entry : expectedMap.entrySet()) {
            final var expectedShare = entry.getValue();
            final var actualShare = actualMap.get(entry.getKey());
            if (full) {
                assertShare(expectedShare, actualShare);
            } else {
                // id + name only provided
                assertNotNull(actualShare);
                assertEquals(expectedShare.getResourceName(), actualShare.getResourceName());
            }
        }
    }

    private static void assertShare(final NavResourceShare expected, final NavResourceShare actual) {
        assertNotNull(actual);
        assertNotNull(actual.getShareId());
        if (expected.getShareId() != null) {
            assertEquals(expected.getShareId(), actual.getShareId());
        }
        assertEquals(expected.getResourceName(), actual.getResourceName());

        if (expected.getAccountId() == null) {
            assertEquals(localAccountId, actual.getAccountId());
        } else {
            assertEquals(expected.getAccountId(), actual.getAccountId());
        }
        assertEquals(expected.getPath(), actual.getPath());
        assertEquals(expected.getShareType(), actual.getShareType());
    }

    @Test
    @Order(41)
    void createBookmark() {
        final var resourceAccount1 = new NavResource(account1.getAccountId(), BOOKMARK_PATH_1);
        final var resourceAccount2 = new NavResource(account2.getAccountId(), BOOKMARK_PATH_1);
        final var resourceShare1 = new NavResource(share1.getShareId(), BOOKMARK_PATH_2);
        final var resourceShare2 = new NavResource(share2.getShareId(), BOOKMARK_PATH_2);
        final var resourceLocal1 = new NavResource(localShareAll.getShareId(), BOOKMARK_PATH_3);
        final var resourceLocal2 = new NavResource(localShareReg.getShareId(), BOOKMARK_PATH_3);

        // user1 bookmarks: 1) of own account, 2) of other user share, 3) of local share
        bookmark1 = createAssertBookmark(userId1, resourceAccount1);
        bookmark2 = createAssertBookmark(userId1, resourceShare2);
        bookmark3 = createAssertBookmark(userId1, resourceLocal1);

        // user2 bookmarks: 1) of own account, 2) of other user share, 3, 4) of local share
        bookmark4 = createAssertBookmark(userId2, resourceAccount2);
        bookmark5 = createAssertBookmark(userId2, resourceShare1);
        bookmark6 = createAssertBookmark(userId2, resourceLocal1);
        bookmark7 = createAssertBookmark(userId2, resourceLocal2);

        // error cases
        // cannot bookmark to other user's account
        assertThrows(UnauthorizedException.class, () -> navResourceService.createBookmark(userId1, resourceAccount2));
        // non-existent user
        assertThrows(IllegalArgumentException.class, () -> navResourceService.createBookmark(-1, resourceShare1));
        // non-existent account
        assertThrows(IllegalArgumentException.class,
            () -> navResourceService.createBookmark(userId1, new NavResource(-1, BOOKMARK_PATH_1)));
        // non-existent share
        assertThrows(IllegalArgumentException.class,
            () -> navResourceService.createBookmark(userId1, new NavResource(-1L, BOOKMARK_PATH_1)));
    }

    private static NavResourceBookmark createAssertBookmark(final Integer userId, final NavResource resource) {
        final var actual = navResourceService.createBookmark(userId, resource);
        assertBookmark(new NavResourceBookmark(userId, resource), actual);
        return actual;
    }

    private static void assertBookmark(final NavResourceBookmark expected, final NavResourceBookmark actual) {
        assertNotNull(actual);
        if (expected.getBookmarkId() == null) {
            assertNotNull(actual.getBookmarkId());
        } else {
            assertEquals(expected.getBookmarkId(), actual.getBookmarkId());
        }
        if (expected.getAccountId() != null) {
            assertEquals(expected.getAccountId(), actual.getAccountId());
        } else {
            assertEquals(expected.getShareId(), actual.getShareId());
        }
        assertEquals(expected.getUserId(), actual.getUserId());
        assertEquals(expected.getPath(), actual.getPath());
    }

    @Test
    @Order(42)
    void removeBookmark() {
        // non-existent bookmark remove omitted with no errors
        navResourceService.removeBookmark(userId1, -1L);
        // bookmark belongs to other user
        assertThrows(UnauthorizedException.class,
            () -> navResourceService.removeBookmark(userId1, bookmark7.getBookmarkId()));
        // success
        navResourceService.removeBookmark(userId2, bookmark7.getBookmarkId());
        bookmark7 = navResourceService.getBookmark(bookmark7.getBookmarkId());
        assertNull(bookmark7);
    }

    @Test
    @Order(43)
    void getBookmarks() {
        getAssertBookmarks(userId1, List.of(bookmark1, bookmark2, bookmark3));
        getAssertBookmarks(userId2, List.of(bookmark4, bookmark5, bookmark6));
    }

    private static void getAssertBookmarks(final Integer userId, final List<NavResourceBookmark> expected) {
        final var actual = navResourceService.getBookmarks(userId);
        assertNotNull(actual);
        assertEquals(expected.size(), actual.size());
        final var actualMap = toMap(actual, NavResourceBookmark::getBookmarkId);
        for (var expectedEntry : expected) {
            assertBookmark(expectedEntry, actualMap.get(expectedEntry.getBookmarkId()));
        }
    }

    @Test
    @Order(51)
    void removeDeviceCascade() {
        // removing local device is not allowed
        assertThrows(IllegalArgumentException.class, () -> navResourceService.removeDevice(localDeviceId));

        // success
        navResourceService.removeDevice(device1.getDeviceId());
        assertNull(navResourceService.getDevice(device1.getDeviceId()));

        // ensure dependent entries are removed via cascade
        assertNull(navResourceService.getAccount(account1.getAccountId())); // depends om device1
        assertNull(navResourceService.getSharedResource(share1.getShareId())); // depends on account1
        assertNull(navResourceService.getBookmark(bookmark1.getBookmarkId())); // depends on account1
        assertNull(navResourceService.getBookmark(bookmark5.getBookmarkId())); // depends on share1
    }

    @Test
    @Order(52)
    void removeUserCascade() {
        sessionFactory.inTransaction(session -> {
            final var user = session.get(UserEntity.class, userId2);
            session.remove(user);
        });

        // ensure dependent entries are removed via cascade
        assertNull(navResourceService.getAccount(account2.getAccountId())); // depends om user2
        assertNull(navResourceService.getSharedResource(share2.getShareId())); // depends on account2
        assertNull(navResourceService.getBookmark(bookmark2.getBookmarkId())); // depends on share2
        assertNull(navResourceService.getBookmark(bookmark4.getBookmarkId())); // depends on user2
        assertNull(navResourceService.getBookmark(bookmark6.getBookmarkId())); // depends on user2

        // ensure local bookmarks remain for user1
        assertBookmark(bookmark3, navResourceService.getBookmark(bookmark3.getBookmarkId()));
    }

    private static <K, V> Map<K, V> toMap(final List<V> list, Function<V, K> keyBuilder) {
        return list.stream().collect(Collectors.toMap(keyBuilder, Function.identity()));
    }
}
