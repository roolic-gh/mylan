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
package local.mylan.service.rest.spi;

import static local.mylan.service.api.model.DeviceProtocol.NFS;
import static local.mylan.service.api.model.DeviceProtocol.SMB;
import static local.mylan.service.test.NavResourceTestUtils.assertAccount;
import static local.mylan.service.test.NavResourceTestUtils.assertAccountList;
import static local.mylan.service.test.NavResourceTestUtils.assertBookmark;
import static local.mylan.service.test.NavResourceTestUtils.assertBookmarkList;
import static local.mylan.service.test.NavResourceTestUtils.assertDeviceList;
import static local.mylan.service.test.NavResourceTestUtils.bookmarkByAccount;
import static local.mylan.service.test.NavResourceTestUtils.bookmarkByShare;
import static local.mylan.service.test.NavResourceTestUtils.device;
import static local.mylan.service.test.NavResourceTestUtils.deviceAccount;
import static local.mylan.service.test.NavResourceTestUtils.share;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.List;
import local.mylan.service.api.NavResourceService;
import local.mylan.service.api.UserContext;
import local.mylan.service.api.exceptions.NoDataException;
import local.mylan.service.api.exceptions.UnauthorizedException;
import local.mylan.service.api.model.Device;
import local.mylan.service.api.model.DeviceAccount;
import local.mylan.service.api.model.NavResourceBookmark;
import local.mylan.service.api.model.NavResourceShare;
import local.mylan.service.api.model.ShareType;
import local.mylan.service.api.model.User;
import local.mylan.service.rest.api.NavResourceRestService;
import local.mylan.service.test.NavResourceTestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DefaultNavResourceRestServiceTest {

    private static final Integer DEVICE_ID1 = 101;
    private static final Integer DEVICE_ID2 = 102;
    private static final String DEVICE_NAME1 = "NAME1";
    private static final String DEVICE_NAME2 = "NAME2";
    private static final String IP1 = "192.168.1.101";
    private static final String IP2 = "192.168.1.102";
    private static final String IP3 = "192.168.1.103";
    private static final Device DEVICE1 = device(DEVICE_ID1, DEVICE_NAME1, SMB, List.of(IP1), null);
    private static final Device DEVICE2 = device(DEVICE_ID2, DEVICE_NAME2, NFS, List.of(IP2), null);

    private static final Integer USER_ID1 = 501;
    private static final Integer USER_ID2 = 502;
    private static final User USER1 = new User(USER_ID1, "user1", "USR1", false);
    private static final User USER2 = new User(USER_ID2, "user2", "USR2", false);
    private static final User GUEST = new User("", "", false);
    private static final UserContext USER_CTX1 = new UserContext(USER1, "u1");
    private static final UserContext USER_CTX2 = new UserContext(USER2, "u2");
    private static final UserContext GUEST_CTX = new UserContext(GUEST, null);

    private static final Integer ACCOUNT_ID1 = 601;
    private static final Integer ACCOUNT_ID2 = 602;
    private static final Integer ACCOUNT_ID_INAVLID = 606;

    private static final String USERNAME1 = "username-1";
    private static final String USERNAME2 = "username-1";
    private static final String PASSWORD1 = "PaS$W0rd1";
    private static final String PASSWORD2 = "Pa$SW0rd2";
    private static final String KEY1 = "k1";
    private static final String KEY2 = "k2";

    private static final DeviceAccount ACCOUNT1 = deviceAccount(ACCOUNT_ID1, USER_ID1, DEVICE_ID1, USERNAME1);
    private static final DeviceAccount ACCOUNT2 = deviceAccount(ACCOUNT_ID2, USER_ID2, DEVICE_ID2, USERNAME2);

    private static final boolean IS_FILE = true;
    private static final boolean IS_DIR = false;

    private static final String NAME_1 = "name-1";
    private static final String NAME_2 = "name-1";
    private static final String NAME_X = "name-X";
    private static final String PATH_1 = "/path/1";
    private static final String PATH_2 = "/path/2";

    private static final Long SHARE_ID1 = 3001L;
    private static final Long SHARE_ID2 = 3002L;
    private static final NavResourceShare SHARE_1 =
        share(SHARE_ID1, ACCOUNT_ID1, NAME_1, PATH_1, IS_DIR, ShareType.ALL);
    private static final NavResourceShare SHARE_2 =
        share(SHARE_ID2, ACCOUNT_ID2, NAME_2, PATH_2, IS_FILE, ShareType.REGISTERED);

    private static final Long BOOKMARK_ID1 = 4001L;
    private static final Long BOOKMARK_ID2 = 4002L;
    private static final NavResourceBookmark BOOKMARK_1 =
        bookmarkByAccount(BOOKMARK_ID1, USER_ID1, ACCOUNT_ID1, NAME_1, PATH_1, IS_DIR);
    private static final NavResourceBookmark BOOKMARK_2 =
        bookmarkByShare(BOOKMARK_ID2, USER_ID1, SHARE_ID1, NAME_2, PATH_2, IS_FILE);

    @Mock
    NavResourceService navResourceService;
    @Captor
    ArgumentCaptor<DeviceAccount> accountCaptor;
    @Captor
    ArgumentCaptor<NavResourceShare> shareCaptor;
    @Captor
    ArgumentCaptor<NavResourceBookmark> bookmarkCaptor;

    NavResourceRestService restService;

    @BeforeEach
    void beforeEach() {
        restService = new DefaultNavResourceRestService(navResourceService);
    }

    @Test
    void listDevices() {
        final var devices = List.of(DEVICE1, DEVICE2);
        doReturn(devices).when(navResourceService).getAllDevices();
        assertDeviceList(devices, restService.getDevices());
    }

    @Test
    void listAccounts() {
        final var expected = List.of(ACCOUNT1);
        doReturn(expected).when(navResourceService).getUserAccounts(USER_ID1);
        assertAccountList(expected, restService.listDeviceAccounts(USER_CTX1));
    }

    @Test
    void getAccount() {
        doReturn(ACCOUNT1).when(navResourceService).getAccount(ACCOUNT_ID1);
        doReturn(ACCOUNT2).when(navResourceService).getAccount(ACCOUNT_ID2);
        doReturn(null).when(navResourceService).getAccount(ACCOUNT_ID_INAVLID);

        assertAccount(ACCOUNT1, restService.getDeviceAccount(ACCOUNT_ID1, USER_CTX1));
        assertAccount(ACCOUNT2, restService.getDeviceAccount(ACCOUNT_ID2, USER_CTX2));

        assertThrows(UnauthorizedException.class, () -> restService.getDeviceAccount(ACCOUNT_ID1, USER_CTX2));
        assertThrows(UnauthorizedException.class, () -> restService.getDeviceAccount(ACCOUNT_ID2, USER_CTX1));
        assertThrows(NoDataException.class, () -> restService.getDeviceAccount(ACCOUNT_ID_INAVLID, USER_CTX1));
    }

    @Test
    void createAccount() {
        final var newAccount = new DeviceAccount(DEVICE_ID1, USERNAME1, PASSWORD1, KEY1);
        doReturn(ACCOUNT1).when(navResourceService).createAccount(USER_ID1, newAccount);
        assertAccount(ACCOUNT1, restService.createDeviceAccount(newAccount, USER_CTX1));
    }

    @Test
    void updateAccount() {
        final var account = deviceAccount(null, null, DEVICE_ID1, USERNAME1, PASSWORD1, KEY1);
        doReturn(ACCOUNT1).when(navResourceService).updateAccount(USER_ID1, account);
        assertAccount(ACCOUNT1, restService.updateDeviceAccount(ACCOUNT_ID1, account, USER_CTX1));
        assertEquals(ACCOUNT_ID1, account.getAccountId()); // populated before invoking the service
    }

    @Test
    void deleteAccount() {
        restService.deleteDeviceAccount(ACCOUNT_ID1, USER_CTX1);
        verify(navResourceService, times(1)).removeAccount(eq(USER_ID1), eq(ACCOUNT_ID1));
    }

    @Test
    void listShares() {
        final var list1 = List.of(SHARE_1);
        final var list2 = List.of(SHARE_1, SHARE_2);
        doReturn(list1).when(navResourceService).getSharedResources(isNull());
        doReturn(list2).when(navResourceService).getSharedResources(USER_ID2);
        assertShareList(list1, restService.listUserShares(GUEST_CTX));
        assertShareList(list2, restService.listUserShares(USER_CTX2));
    }

    @Test
    void createShare() {
        final var newShare = share(null, ACCOUNT_ID1, NAME_1, PATH_1, IS_DIR, ShareType.ALL);
        doReturn(SHARE_1).when(navResourceService).createSharedResource(USER_ID1, newShare);
        assertShare(SHARE_1, restService.createUserShare(newShare, USER_CTX1));
    }

    @Test
    void updateShare() {
        final var updateShare = share(null, null, NAME_X, null, IS_DIR, null);
        final var updateCheckShare = share(SHARE_ID1, null, NAME_X, null, IS_DIR, null);
        final var updatedShare = share(SHARE_ID1, ACCOUNT_ID1, NAME_X, PATH_1, IS_DIR, ShareType.ALL);

        doReturn(updatedShare).when(navResourceService).updateSharedResource(eq(USER_ID1), any());
        assertShare(updatedShare, restService.updateUserShare(SHARE_ID1, updateShare, USER_CTX1));
        verify(navResourceService, times(1)).updateSharedResource(eq(USER_ID1), shareCaptor.capture());
        assertShare(updateCheckShare, shareCaptor.getValue()); // id populated by rest service
    }

    @Test
    void removeShare() {
        restService.removeUserShare(SHARE_ID1, USER_CTX1);
        verify(navResourceService, times(1)).removeSharedResource(USER_ID1, SHARE_ID1);
    }

    private static void assertShare(final NavResourceShare expected, final NavResourceShare actual) {
        NavResourceTestUtils.assertShare(expected, actual, null);
    }

    private static void assertShareList(final List<NavResourceShare> expected, final List<NavResourceShare> actual) {
        NavResourceTestUtils.assertShareList(expected, actual, null);
    }

    @Test
    void listBookmarks() {
        final var list = List.of(BOOKMARK_1, BOOKMARK_2);
        doReturn(list).when(navResourceService).getBookmarks(USER_ID1);
        assertBookmarkList(list, restService.listUserBookmarks(USER_CTX1));
    }

    @Test
    void createBookmark() {
        final var newBookmark = bookmarkByAccount(ACCOUNT_ID1, NAME_1, PATH_1, IS_DIR);
        doReturn(BOOKMARK_1).when(navResourceService).createBookmark(USER_ID1, newBookmark);
        assertBookmark(BOOKMARK_1, restService.createUserBookmark(newBookmark, USER_CTX1));
    }

    @Test
    void updateBookmark() {
        final var updateBookmark = bookmarkByShare(null, USER_ID1, null, NAME_X, null, IS_DIR);
        final var updateCheckBookmark = bookmarkByShare(BOOKMARK_ID2, USER_ID1, null, NAME_X, null, IS_DIR);
        final var updatedBookmark = bookmarkByShare(BOOKMARK_ID2, USER_ID1, SHARE_ID1, NAME_X, PATH_2, IS_FILE);

        doReturn(updatedBookmark).when(navResourceService).updateBookmark(eq(USER_ID1), any());
        assertBookmark(updatedBookmark, restService.updateUserBookmark(BOOKMARK_ID2, updateBookmark, USER_CTX1));
        verify(navResourceService, times(1)).updateBookmark(eq(USER_ID1), bookmarkCaptor.capture());
        assertBookmark(updateCheckBookmark, bookmarkCaptor.getValue()); // id populated by rest service
    }

    @Test
    void removeBookmark() {
        restService.removeUserBookmark(BOOKMARK_ID1, USER_CTX1);
        verify(navResourceService, times(1)).removeBookmark(USER_ID1, BOOKMARK_ID1);
    }
}
