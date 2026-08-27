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
import static local.mylan.service.data.TestUtils.setupSessionFactory;
import static local.mylan.service.data.TestUtils.tearDownSessionFactory;
import static local.mylan.service.test.NavResourceTestUtils.assertBookmark;
import static local.mylan.service.test.NavResourceTestUtils.assertBookmarkList;
import static local.mylan.service.test.NavResourceTestUtils.assertList;
import static local.mylan.service.test.NavResourceTestUtils.bookmarkByAccount;
import static local.mylan.service.test.NavResourceTestUtils.bookmarkByShare;
import static local.mylan.service.test.NavResourceTestUtils.share;
import static local.mylan.service.test.NavResourceTestUtils.toMap;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import local.mylan.service.api.NavResourceService;
import local.mylan.service.api.events.CrudOperation;
import local.mylan.service.api.events.DeviceAccountCrudEvent;
import local.mylan.service.api.events.DeviceCrudEvent;
import local.mylan.service.api.events.Event;
import local.mylan.service.api.events.UserBookmarkCrudEvent;
import local.mylan.service.api.events.UserShareCrudEvent;
import local.mylan.service.api.exceptions.DataCollisionException;
import local.mylan.service.api.exceptions.UnauthorizedException;
import local.mylan.service.api.model.Device;
import local.mylan.service.api.model.DeviceAccount;
import local.mylan.service.api.model.DeviceAccountLockState;
import local.mylan.service.api.model.DeviceAccountWithCredentials;
import local.mylan.service.api.model.DeviceIpAddress;
import local.mylan.service.api.model.DeviceProtocol;
import local.mylan.service.api.model.NavResourceBookmark;
import local.mylan.service.api.model.NavResourceShare;
import local.mylan.service.api.model.ShareType;
import local.mylan.service.data.entities.DeviceAccountEntity;
import local.mylan.service.data.entities.DeviceEntity;
import local.mylan.service.data.entities.DeviceIpAddressEntity;
import local.mylan.service.data.entities.NavResourceBookmarkEntity;
import local.mylan.service.data.entities.NavResourceShareEntity;
import local.mylan.service.data.entities.UserEntity;
import local.mylan.service.spi.DefaultEncryptionService;
import local.mylan.service.test.NavResourceTestUtils;
import local.mylan.service.test.TestNotificationService;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.io.TempDir;

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

    private static final String SYNC_1 = "sync-1";
    private static final String SYNC_2 = "sync-2";
    private static final String SYNC_3 = "sync-3";

    private static final String SYNC_IP_1 = "192.168.1.181";
    private static final String SYNC_IP_2 = "192.168.1.182";
    private static final String SYNC_IP_3 = "192.168.1.183";
    private static final String SYNC_IP_4 = "192.168.1.184";
    private static final String SYNC_IP_5 = "192.168.1.185";
    private static final String SYNC_IP_6 = "192.168.1.186";
    private static final String SYNC_IP_7 = "192.168.1.187";
    private static final String SYNC_IP_8 = "192.168.1.188";

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
    private static final String BKM_NAME_1 = "bookmark-name-1";
    private static final String BKM_NAME_2 = "bookmark-name-2";
    private static final String BKM_NAME_3 = "bookmark-name-3";
    private static final String BKM_NAME_X = "bookmark-name-X";
    private static final String BKM_PATH_1 = "/bookmark/path/1";
    private static final String BKM_PATH_2 = "/bookmark/path/2";
    private static final String BKM_PATH_3 = "/bookmark/path/3";

    private static final boolean IS_FILE = true;
    private static final boolean IS_DIR = false;

    @TempDir
    static Path tempDir;

    static SessionFactory sessionFactory;
    static NavResourceService navResourceService;
    static TestNotificationService notificationService;

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
        notificationService = new TestNotificationService();
        navResourceService = new NavResourceDataService(sessionFactory,
            new DefaultEncryptionService(tempDir, tempDir), notificationService);

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

    @BeforeEach
    void beforeEach() {
        notificationService.clearEvents();
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

        // events raised
        assertEquals(List.of(
            new DeviceCrudEvent(device1.getDeviceId(), CrudOperation.CREATE),
            new DeviceCrudEvent(device2.getDeviceId(), CrudOperation.CREATE)
        ), notificationService.getEvents());

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
    @Order(15)
    void syncDeviceAddresses() {
        // devices before: sync1(ip1, ip2, ip3) + sync2(ip4, ip5, ip6)
        navResourceService.createDevice(new Device(SYNC_1, DeviceProtocol.SMB, List.of(
            new DeviceIpAddress(SYNC_IP_1), new DeviceIpAddress(SYNC_IP_2), new DeviceIpAddress(SYNC_IP_3))));
        navResourceService.createDevice(new Device(SYNC_2, DeviceProtocol.SMB, List.of(
            new DeviceIpAddress(SYNC_IP_4), new DeviceIpAddress(SYNC_IP_5), new DeviceIpAddress(SYNC_IP_6))));
        notificationService.clearEvents(); // reset to check sync events only

        // sync with devices: sync1(ip1, ip6, ip7), sync3(ip2, ip4, ip8) -->
        // 2 reassigned, (sync2 offline) 2 new, 1 removed
        final var syncList = List.of(
            new Device(SYNC_1, DeviceProtocol.SMB, List.of(
                new DeviceIpAddress(SYNC_IP_1), new DeviceIpAddress(SYNC_IP_6), new DeviceIpAddress(SYNC_IP_7))),
            new Device(SYNC_3, DeviceProtocol.NFS, List.of(
                new DeviceIpAddress(SYNC_IP_2), new DeviceIpAddress(SYNC_IP_4), new DeviceIpAddress(SYNC_IP_8))));

        // execute and check
        navResourceService.syncDeviceAddresses(syncList);

        final var deviceMap = toMap(navResourceService.getAllDevices(), Device::getIdentifier);
        assertNotNull(deviceMap.get(SYNC_1));
        assertDeviceIpAddresses(deviceMap.get(SYNC_1).getIpAddresses(), Set.of(SYNC_IP_1, SYNC_IP_6, SYNC_IP_7));
        assertNotNull(deviceMap.get(SYNC_2));
        assertDeviceIpAddresses(deviceMap.get(SYNC_2).getIpAddresses(), Set.of(SYNC_IP_5));
        assertNotNull(deviceMap.get(SYNC_3));
        assertDeviceIpAddresses(deviceMap.get(SYNC_3).getIpAddresses(), Set.of(SYNC_IP_2, SYNC_IP_4, SYNC_IP_8));

        // validate released ip3 address removed
        assertNull(sessionFactory.fromSession(session -> session.get(DeviceIpAddressEntity.class, SYNC_IP_3)));

        // new device regisered (created) event
        assertEquals(
            List.of(new DeviceCrudEvent(deviceMap.get(SYNC_3).getDeviceId(), CrudOperation.CREATE)),
            notificationService.getEvents());
    }

    @Test
    @Order(21)
    void createAccount() {
        // single account to device 1
        final var inAccount1 = new DeviceAccount(device1.getDeviceId(), USERNAME_1, PASSWORD_1);
        account1 = navResourceService.createAccount(userId1, inAccount1);
        assertAccount(inAccount1, account1);
        final var checkAccount1 = navResourceService.getAccount(account1.getAccountId());
        assertAccount(account1, checkAccount1);
        assertAccountWithCredentials(inAccount1, navResourceService.getAccountWithCredentials(account1.getAccountId()));
        account1.setPassword(PASSWORD_1); // for subsequent checks

        // 2 accounts to device 2
        final var inAccount2 = new DeviceAccount(device2.getDeviceId(), USERNAME_2, PASSWORD_2, CRYPT_KEY);
        account2 = navResourceService.createAccount(userId2, inAccount2);
        assertAccount(inAccount2, account2);
        assertAccountWithCredentials(inAccount2, navResourceService.getAccountWithCredentials(account2.getAccountId()));
        account2.setPassword(PASSWORD_2); // for subsequent checks
        account2.setKey(CRYPT_KEY);

        final var inAccount3 = new DeviceAccount(device2.getDeviceId(), USERNAME_1, PASSWORD_1);
        account3 = navResourceService.createAccount(userId2, inAccount3);
        assertAccount(inAccount3, account3);
        assertAccountWithCredentials(inAccount3, navResourceService.getAccountWithCredentials(account3.getAccountId()));
        account3.setPassword(PASSWORD_1); // for subsequent checks

        // events
        final var expectedEvents = List.of(
            new DeviceAccountCrudEvent(account1.getAccountId(), CrudOperation.CREATE),
            new DeviceAccountCrudEvent(account2.getAccountId(), CrudOperation.CREATE),
            new DeviceAccountCrudEvent(account3.getAccountId(), CrudOperation.CREATE));
        assertEquals(expectedEvents, notificationService.getEvents());

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

    private static void assertAccount(final DeviceAccount expected, final DeviceAccount actual) {
        assertNotNull(actual);
        assertEquals(expected.getDeviceId(), actual.getDeviceId());
        assertNull(actual.getPassword());
        assertNull(actual.getKey());
        if (localDeviceId.equals(expected.getDeviceId())) {
            // local
            assertNull(actual.getUserId());
            assertNull(actual.getUsername());
            return;
        }
        if (expected.getUserId() == null) {
            assertNotNull(actual.getUserId());
        } else {
            assertEquals(expected.getUserId(), actual.getUserId());
        }
        assertEquals(expected.getUsername(), actual.getUsername());
    }

    private static void assertAccountWithCredentials(final DeviceAccount expected,
        final DeviceAccountWithCredentials actual) {

        assertNotNull(actual);
        assertNotNull(actual.getAccountId());
        assertEquals(expected.getDeviceId(), actual.getDeviceId());
        if (expected.getUserId() != null) {
            assertEquals(expected.getUserId(), actual.getUserId());
        }
        assertEquals(expected.getUsername(), actual.getUsername());
        if (expected.getKey() != null) {
            assertEquals(DeviceAccountLockState.LOCKED, actual.getLockState());
            actual.unlock(expected.getKey());
            assertEquals(DeviceAccountLockState.UNLOCKED, actual.getLockState());
            assertEquals(expected.getPassword(), actual.getPassword());
        } else {
            assertEquals(DeviceAccountLockState.HAS_NO_LOCK, actual.getLockState());
            assertEquals(expected.getPassword(), actual.getPassword());
        }
    }

    @Test
    @Order(22)
    void updateAccount() {
        // only accountId and credentials are taken from input
        final var update = new DeviceAccount(null, USERNAME_X, PASSWORD_X, CRYPT_KEY);
        update.setAccountId(account3.getAccountId());
        final var updated = navResourceService.updateAccount(userId2, update);
        assertNotNull(updated);
        assertEquals(userId2, updated.getUserId());
        assertEquals(device2.getDeviceId(), updated.getDeviceId());
        assertEquals(USERNAME_X, updated.getUsername());

        // validate credentials only updated
        final var check = navResourceService.getAccountWithCredentials(account3.getAccountId());
        assertEquals(DeviceAccountLockState.LOCKED, check.getLockState());
        check.unlock(CRYPT_KEY);
        assertEquals(DeviceAccountLockState.UNLOCKED, check.getLockState());
        assertEquals(USERNAME_X, check.getUsername());
        assertEquals(PASSWORD_X, check.getPassword());

        account3 = updated;
        account3.setPassword(PASSWORD_X); //  for subsequent checks
        account3.setKey(CRYPT_KEY);

        // checkEvents
        final var expectedEvents = List.of(new DeviceAccountCrudEvent(account3.getAccountId(), CrudOperation.UPDATE));
        assertEquals(expectedEvents, notificationService.getEvents());

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

        assertAccount(account1, allMap.get(account1.getAccountId()));
        assertAccount(account2, allMap.get(account2.getAccountId()));
        assertAccount(account3, allMap.get(account3.getAccountId()));
        assertNotNull(allMap.get(localAccountId));

        final var credsAllList = navResourceService.getAllAccountsWithCredentials();
        assertNotNull(credsAllList);
        assertEquals(4, credsAllList.size());
        final var credsAllMap = toMap(credsAllList, DeviceAccountWithCredentials::getAccountId);

        assertAccountWithCredentials(account1, credsAllMap.get(account1.getAccountId()));
        assertAccountWithCredentials(account2, credsAllMap.get(account2.getAccountId()));
        assertAccountWithCredentials(account3, credsAllMap.get(account3.getAccountId()));
        assertNotNull(credsAllMap.get(localAccountId));

        final var list1 = navResourceService.getUserAccounts(userId1);
        assertNotNull(list1);
        assertEquals(1, list1.size());
        assertAccount(account1, list1.getFirst());

        final var list2 = navResourceService.getUserAccounts(userId2);
        assertNotNull(list2);
        assertEquals(2, list2.size());
        final var map = toMap(list2, DeviceAccount::getAccountId);
        assertAccount(account2, map.get(account2.getAccountId()));
        assertAccount(account3, map.get(account3.getAccountId()));
    }

    @Test
    @Order(24)
    void removeAccount() {
        final var accountId = account3.getAccountId();
        navResourceService.removeAccount(userId2, accountId);
        account3 = navResourceService.getAccount(accountId);
        assertNull(account3);

        // checkEvents
        final var expectedEvents = List.of(new DeviceAccountCrudEvent(accountId, CrudOperation.DELETE));
        assertEquals(expectedEvents, notificationService.getEvents());

        // removing local account is not allowed
        assertThrows(IllegalArgumentException.class, () -> navResourceService.removeAccount(userId2, localAccountId));
        // account belongs to other user
        assertThrows(UnauthorizedException.class,
            () -> navResourceService.removeAccount(userId2, account1.getAccountId()));
    }

    @Test
    @Order(31)
    void syncLocalShares() {
        final var local1 = share(null, null, SHARE_LOCAL_1, SHARE_PATH_1, IS_DIR, ShareType.ALL);
        final var local2 = share(null, null, SHARE_LOCAL_2, SHARE_PATH_2, IS_DIR, ShareType.ALL);
        final var local2upd = share(null, null, SHARE_NAME_X, SHARE_PATH_2, IS_DIR, ShareType.REGISTERED);
        final var local2final = share(null, null, SHARE_LOCAL_2, SHARE_PATH_2, IS_DIR, ShareType.REGISTERED);
        final var local3 = share(null, null, SHARE_LOCAL_3, SHARE_PATH_3, IS_FILE, ShareType.ALL);

        // no shares
        final var initial = navResourceService.getAllSharedResources();
        assertNotNull(initial);
        assertTrue(initial.isEmpty());

        // 1st sync - 2 entries added
        final var expected1 = List.of(local1, local2);
        navResourceService.syncLocalSharedResources(expected1);
        final var actual1 = navResourceService.getAllSharedResources();
        assertLocalShares(expected1, actual1);

        // 2nd sync -> 1 removed, 1 udated, 1 inserted
        final var expected2 = List.of(local2upd, local3);
        navResourceService.syncLocalSharedResources(expected2);
        final var actual2 = navResourceService.getAllSharedResources();
        assertLocalShares(expected2, actual2);
        // assert 2nd entry updated -> id is same
        final var original = byPath(actual1, SHARE_PATH_2);
        final var updated = byPath(actual2, SHARE_PATH_2);
        assertEquals(original.getShareId(), updated.getShareId());

        // 3rd sync replace all for below tests
        final var expected3 = List.of(local1, local2final);
        navResourceService.syncLocalSharedResources(expected3);
        final var actual3 = navResourceService.getAllSharedResources();
        assertLocalShares(expected3, actual3);
        localShareAll = byPath(actual3, SHARE_PATH_1);
        localShareReg = byPath(actual3, SHARE_PATH_2);
    }

    private static void assertLocalShares(final List<NavResourceShare> expected, final List<NavResourceShare> actual) {
        assertList(expected, actual, NavResourceShare::getPath, NavResourceDataServiceTest::assertShare);
    }

    private static NavResourceShare byPath(final List<NavResourceShare> list, final String path) {
        return list.stream().filter(share -> path.equals(share.getPath())).findFirst().orElseThrow();
    }

    @Test
    @Order(32)
    void createShare() {
        final var inShare1 = share(null, account1.getAccountId(), SHARE_NAME_1, SHARE_PATH_1, IS_DIR, ShareType.ALL);
        share1 = navResourceService.createSharedResource(userId1, inShare1);
        assertShare(inShare1, share1);

        final var inShare2 =
            share(null, account2.getAccountId(), SHARE_NAME_2, SHARE_PATH_2, IS_FILE, ShareType.REGISTERED);
        share2 = navResourceService.createSharedResource(userId2, inShare2);
        assertShare(inShare2, share2);

        final var inShare3 =
            share(null, account2.getAccountId(), SHARE_NAME_3, SHARE_PATH_3, IS_DIR, ShareType.ALL);

        // non-existing user
        assertThrows(IllegalArgumentException.class, () -> navResourceService.createSharedResource(-1, inShare3));
        // user is not account owner
        assertThrows(UnauthorizedException.class, () -> navResourceService.createSharedResource(userId1, inShare3));
        // success
        share3 = navResourceService.createSharedResource(userId2, inShare3);
        assertShare(inShare3, share3);

        // non-existing account
        assertThrows(IllegalArgumentException.class, () -> navResourceService.createSharedResource(userId2,
            share(null, -1, SHARE_NAME_X, SHARE_PATH_1, IS_DIR, ShareType.ALL)));
        // local account is not allowed
        assertThrows(IllegalArgumentException.class, () -> navResourceService.createSharedResource(userId2,
            share(null, localAccountId, SHARE_NAME_X, SHARE_PATH_1, IS_DIR, ShareType.ALL)));

        // events
        assertShareEvents(List.of(share1, share2, share3), CrudOperation.CREATE, notificationService.getEvents());
    }

    @Test
    @Order(33)
    void updateShare() {
        final var shareId = share3.getShareId();
        final var accountId = account2.getAccountId();
        final var update = share(shareId, accountId, SHARE_NAME_X, SHARE_PATH_1, IS_FILE, ShareType.REGISTERED);
        final var updateInvalid = share(-1L, accountId, SHARE_NAME_X, SHARE_PATH_1, IS_FILE, ShareType.REGISTERED);

        // non-existent share
        assertThrows(IllegalArgumentException.class,
            () -> navResourceService.updateSharedResource(userId1, updateInvalid));
        // user is not the owner
        assertThrows(UnauthorizedException.class, () -> navResourceService.updateSharedResource(userId1, update));
        // success
        final var updated = navResourceService.updateSharedResource(userId2, update);
        // only share name and share type expected to be updated, path and file flag to remain same
        final var expected = share(shareId, accountId, SHARE_NAME_X, SHARE_PATH_3, IS_DIR, ShareType.REGISTERED);
        assertShare(expected, updated);
        // event
        assertShareEvents(List.of(expected), CrudOperation.UPDATE, notificationService.getEvents());
    }

    @Test
    @Order(34)
    void removeShare() {
        final var shareId = share3.getShareId();
        // non-existing share remove omitted with no errors
        navResourceService.removeSharedResource(userId1, -1L);
        // user is not the owner
        assertThrows(UnauthorizedException.class,
            () -> navResourceService.removeSharedResource(userId1, shareId));
        // success
        navResourceService.removeSharedResource(userId2, shareId);
        assertNull(navResourceService.getSharedResource(shareId));
        // event
        assertEquals(List.of(new UserShareCrudEvent(shareId, null, CrudOperation.DELETE)),
            notificationService.getEvents());
    }

    @Test
    @Order(35)
    void getShares() {
        // shares expected for registered and guest users accordingly
        final var expectedReg = List.of(localShareAll, localShareReg, share1, share2);
        final var expectedGuest = List.of(localShareAll, share1);

        assertShareList(expectedReg, navResourceService.getAllSharedResources());
        assertShareList(expectedGuest, navResourceService.getSharedResources(null));
        assertShareList(expectedReg, navResourceService.getSharedResources(userId1));
        assertShareList(expectedReg, navResourceService.getSharedResources(userId2));
    }

    private static void assertShare(final NavResourceShare expected, final NavResourceShare actual) {
        NavResourceTestUtils.assertShare(expected, actual, localAccountId);
    }

    private static void assertShareList(final List<NavResourceShare> expected,
        final List<NavResourceShare> actual) {
        NavResourceTestUtils.assertShareList(expected, actual, localAccountId);
    }

    private static void assertShareEvents(final List<NavResourceShare> expShares, final CrudOperation expOperation,
        final List<Event> events) {

        assertNotNull(events);
        assertEquals(expShares.size(), events.size());
        final var actShares = new ArrayList<NavResourceShare>();
        for (var event : events) {
            final var casted = assertInstanceOf(UserShareCrudEvent.class, event);
            assertEquals(casted.id(), casted.share().getShareId());
            assertEquals(expOperation, casted.operation());
            actShares.add(casted.share());
        }
        assertShareList(expShares, actShares);
    }

    @Test
    @Order(41)
    void createBookmark() {
        final var bookmarkByAcc1 = bookmarkByAccount(account1.getAccountId(), BKM_NAME_1, BKM_PATH_1, IS_DIR);
        final var bookmarkByAcc2 = bookmarkByAccount(account2.getAccountId(), BKM_NAME_1, BKM_PATH_1, IS_DIR);
        final var bookmarkByShare1 = bookmarkByShare(share1.getShareId(), BKM_NAME_2, BKM_PATH_2, IS_DIR);
        final var bookmarkByShare2 = bookmarkByShare(share2.getShareId(), BKM_NAME_2, BKM_PATH_2, IS_DIR);
        final var bookmarkByLocal1 = bookmarkByShare(localShareAll.getShareId(), BKM_NAME_3, BKM_PATH_3, IS_FILE);
        final var bookmarkByLocal2 = bookmarkByShare(localShareReg.getShareId(), BKM_NAME_3, BKM_PATH_3, IS_FILE);

        // user1 bookmarks: 1) of own account, 2) of other user share, 3) of local share
        bookmark1 = createAssertBookmark(userId1, bookmarkByAcc1);
        bookmark2 = createAssertBookmark(userId1, bookmarkByShare2);
        bookmark3 = createAssertBookmark(userId1, bookmarkByLocal1);

        // user2 bookmarks: 1) of own account, 2) of other user share, 3, 4) of local share
        bookmark4 = createAssertBookmark(userId2, bookmarkByAcc2);
        bookmark5 = createAssertBookmark(userId2, bookmarkByShare1);
        bookmark6 = createAssertBookmark(userId2, bookmarkByLocal1);
        bookmark7 = createAssertBookmark(userId2, bookmarkByLocal2);

        // error cases
        // cannot bookmark to other user's account
        assertThrows(UnauthorizedException.class, () -> navResourceService.createBookmark(userId1, bookmarkByAcc2));
        // non-existent user
        assertThrows(IllegalArgumentException.class, () -> navResourceService.createBookmark(-1, bookmarkByShare1));
        // non-existent account
        assertThrows(IllegalArgumentException.class,
            () -> navResourceService.createBookmark(userId1, bookmarkByAccount(-1, BKM_NAME_1, BKM_PATH_1, IS_DIR)));
        // non-existent share
        assertThrows(IllegalArgumentException.class,
            () -> navResourceService.createBookmark(userId1, bookmarkByShare(-1L, BKM_NAME_1, BKM_PATH_1, IS_DIR)));

        // events
        assertBookmarkEvents(List.of(bookmark1, bookmark2, bookmark3, bookmark4, bookmark5, bookmark6, bookmark7),
            CrudOperation.CREATE, notificationService.getEvents());
    }

    private static NavResourceBookmark createAssertBookmark(final Integer userId, final NavResourceBookmark expected) {
        final var actual = navResourceService.createBookmark(userId, expected);
        assertBookmark(expected, actual);
        assertEquals(userId, actual.getUserId());
        return actual;
    }

    private static void assertBookmarkEvents(final List<NavResourceBookmark> expBookmarks,
        final CrudOperation expOperation, final List<Event> events) {

        assertNotNull(events);
        assertEquals(expBookmarks.size(), events.size());
        final var actBookmarks = new ArrayList<NavResourceBookmark>();
        for (var event : events) {
            final var casted = assertInstanceOf(UserBookmarkCrudEvent.class, event);
            assertEquals(casted.id(), casted.bookmark().getBookmarkId());
            assertEquals(expOperation, casted.operation());
            actBookmarks.add(casted.bookmark());
        }
        assertBookmarkList(expBookmarks, actBookmarks);
    }

    @Test
    @Order(43)
    void updateBookmark() {
        final var bookmarkId = bookmark6.getBookmarkId();
        final var update =
            bookmarkByShare(bookmarkId, userId1, localShareReg.getShareId(), BKM_NAME_X, BKM_PATH_1, IS_DIR);
        final var updateInvalid =
            bookmarkByShare(-1L, userId1, localShareReg.getShareId(), BKM_NAME_X, BKM_PATH_1, IS_DIR);

        // non-existent bookmark
        assertThrows(IllegalArgumentException.class, () -> navResourceService.updateBookmark(userId1, updateInvalid));
        // not-owner
        assertThrows(UnauthorizedException.class, () -> navResourceService.updateBookmark(userId1, update));
        // success
        bookmark6 = navResourceService.updateBookmark(userId2, update);
        // name only updated
        final var expected =
            bookmarkByShare(bookmarkId, userId2, localShareAll.getShareId(), BKM_NAME_X, BKM_PATH_3, IS_FILE);
        assertBookmark(expected, bookmark6);
        // event
        assertBookmarkEvents(List.of(expected), CrudOperation.UPDATE, notificationService.getEvents());
    }

    @Test
    @Order(43)
    void removeBookmark() {
        final var bookmarkId = bookmark7.getBookmarkId();

        // non-existent bookmark remove omitted with no errors
        navResourceService.removeBookmark(userId1, -1L);
        // bookmark belongs to other user
        assertThrows(UnauthorizedException.class,
            () -> navResourceService.removeBookmark(userId1, bookmarkId));
        // success
        navResourceService.removeBookmark(userId2, bookmarkId);
        bookmark7 = navResourceService.getBookmark(bookmarkId);
        assertNull(bookmark7);

        // event
        assertEquals(List.of(new UserBookmarkCrudEvent(bookmarkId, null, CrudOperation.DELETE)),
            notificationService.getEvents());
    }

    @Test
    @Order(44)
    void getBookmarks() {
        assertBookmarkList(List.of(bookmark1, bookmark2, bookmark3, bookmark4, bookmark5, bookmark6),
            navResourceService.getAllBookmarks());
        assertBookmarkList(List.of(bookmark1, bookmark2, bookmark3), navResourceService.getBookmarks(userId1));
        assertBookmarkList(List.of(bookmark4, bookmark5, bookmark6), navResourceService.getBookmarks(userId2));
    }

    @Test
    @Order(51)
    void removeDeviceCascade() {
        // removing local device is not allowed
        assertThrows(IllegalArgumentException.class, () -> navResourceService.removeDevice(localDeviceId));

        // success
        navResourceService.removeDevice(device1.getDeviceId());
        assertNull(navResourceService.getDevice(device1.getDeviceId()));
        assertEquals(List.of(new DeviceCrudEvent(device1.getDeviceId(), CrudOperation.DELETE)),
            notificationService.getEvents());

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
}
