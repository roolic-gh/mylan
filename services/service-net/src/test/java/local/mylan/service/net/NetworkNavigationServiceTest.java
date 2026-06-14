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

import static local.mylan.service.api.model.DeviceAccountLockState.HAS_NO_LOCK;
import static local.mylan.service.api.model.DeviceAccountLockState.LOCKED;
import static local.mylan.service.api.model.DeviceAccountLockState.UNLOCKED;
import static local.mylan.service.api.model.DeviceAccountState.INVALID;
import static local.mylan.service.api.model.DeviceAccountState.UNKNOWN;
import static local.mylan.service.api.model.DeviceAccountState.VALID;
import static local.mylan.service.api.model.DeviceProtocol.NFS;
import static local.mylan.service.api.model.DeviceProtocol.SMB;
import static local.mylan.service.api.model.DeviceState.OFFLINE;
import static local.mylan.service.api.model.DeviceState.ONLINE;
import static local.mylan.service.test.NavResourceTestUtils.accountWithCreds;
import static local.mylan.service.test.NavResourceTestUtils.assertAccountListWithStates;
import static local.mylan.service.test.NavResourceTestUtils.assertAccountWithStates;
import static local.mylan.service.test.NavResourceTestUtils.assertDeviceList;
import static local.mylan.service.test.NavResourceTestUtils.device;
import static local.mylan.service.test.NavResourceTestUtils.deviceAccount;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.List;
import local.mylan.service.api.DeviceAccessor;
import local.mylan.service.api.NavResourceService;
import local.mylan.service.api.NavigationService;
import local.mylan.service.api.events.CrudOperation;
import local.mylan.service.api.events.DeviceAccountCrudEvent;
import local.mylan.service.api.events.DeviceCrudEvent;
import local.mylan.service.api.events.DiscoveryDevicesEvent;
import local.mylan.service.api.exceptions.NoDataException;
import local.mylan.service.api.exceptions.UnauthorizedException;
import local.mylan.service.api.model.Device;
import local.mylan.service.api.model.DeviceAccount;
import local.mylan.service.api.model.DeviceAccountState;
import local.mylan.service.test.TestNotificationService;
import org.junit.jupiter.api.AfterEach;
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
    private static final Integer DEVICE_INVALID = 1001;
    private static final String DEVICE_NAME1 = "NAME1";
    private static final String DEVICE_NAME2 = "NAME2";
    private static final String DEVICE_NAME3 = "NAME3";
    private static final String IP1 = "192.168.1.101";
    private static final String IP2 = "192.168.1.102";
    private static final String IP3 = "192.168.1.103";
    private static final String IP4 = "192.168.1.104";

    private static final String USERNAME1 = "username1";
    private static final String USERNAME2 = "username2";
    private static final String PASSWORD1 = "PaS$W0rd";
    private static final String PASSWORD2 = "Pa$SW0rd";
    private static final String KEY = "key";

    private static final Integer USER_ID1 = 1001;
    private static final Integer USER_ID2 = 1002;

    private static final Integer ACCOUNT_ID1 = 101;
    private static final Integer ACCOUNT_ID2 = 102;
    private static final Integer ACCOUNT_ID3 = 103;
    private static final Integer ACCOUNT_ID4 = 105;
    private static final Integer ACCOUNT_ID5 = 105;

    @Mock
    DeviceAccessor accessor;
    @Mock
    NavResourceService navResourceService;
    @Captor
    ArgumentCaptor<List<Device>> deviceListCaptor;

    TestNotificationService notificationService;
    NavigationService service;

    @BeforeEach
    void beforeEach() {
        notificationService = new TestNotificationService();
    }

    @AfterEach
    void afterEach() {
        if (service != null) {
            service.stop();
        }
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

    @Test
    void accountStates() {
        // existing devices
        final var device1 = device(DEVICE_ID1, DEVICE_NAME1, SMB, List.of(IP1), null);
        final var device2 = device(DEVICE_ID2, DEVICE_NAME2, NFS, List.of(IP2), null);
        doReturn(List.of(device1, device2)).when(navResourceService).getAllDevices();

        // existing accounts
        final var account1 = accountWithCreds(ACCOUNT_ID1, USER_ID1, DEVICE_ID1, USERNAME1, PASSWORD1, null);
        final var account2 = accountWithCreds(ACCOUNT_ID2, USER_ID2, DEVICE_ID2, USERNAME1, PASSWORD1, null);
        final var account3 = accountWithCreds(ACCOUNT_ID3, USER_ID2, DEVICE_ID3, USERNAME2, PASSWORD2, null);
        doReturn(List.of(account1, account2, account3)).when(navResourceService).getAllAccountsWithCredentials();
        // account update
        final var account1upd = accountWithCreds(ACCOUNT_ID1, USER_ID1, DEVICE_ID1, USERNAME2, PASSWORD2, KEY);
        doReturn(account1upd).when(navResourceService).getAccountWithCredentials(ACCOUNT_ID1);
        // account create
        final var account4 = accountWithCreds(ACCOUNT_ID4, USER_ID2, DEVICE_ID1, USERNAME2, PASSWORD2, null);
        doReturn(account4).when(navResourceService).getAccountWithCredentials(ACCOUNT_ID4);

        // accessor
        doReturn(SMB).when(accessor).protocol();
        doReturn(INVALID).when(accessor).validateCredentials(device1, account1);
        doReturn(VALID).when(accessor).validateCredentials(device1, account1upd);
        doReturn(VALID).when(accessor).validateCredentials(device1, account4);

        // test
        service = new NetworkNavigationService(navResourceService, notificationService, List.of(accessor));

        // account 1 is the only account to be verified on start: has accessor for device,
        verify(accessor, timeout(2000).times(1)).validateCredentials(device1, account1);
        verify(accessor, never()).validateCredentials(device1, account2);
        verify(accessor, never()).validateCredentials(device1, account3);

        assertAccountListWithStates(List.of(
                deviceAccount(ACCOUNT_ID1, USER_ID1, DEVICE_ID1, USERNAME1, INVALID, HAS_NO_LOCK)),
            service.listUserDeviceAccounts(USER_ID1));

        final var account2exp = deviceAccount(ACCOUNT_ID2, USER_ID2, DEVICE_ID2, USERNAME1, UNKNOWN, HAS_NO_LOCK);
        final var account3exp = deviceAccount(ACCOUNT_ID3, USER_ID2, DEVICE_ID3, USERNAME2, UNKNOWN, HAS_NO_LOCK);
        assertAccountListWithStates(List.of(account2exp, account3exp), service.listUserDeviceAccounts(USER_ID2));

        // account 1 update with lock
        notificationService.raiseEvent(new DeviceAccountCrudEvent(ACCOUNT_ID1, CrudOperation.UPDATE));
        verify(accessor, never()).validateCredentials(device1, account1upd);
        assertAccountListWithStates(
            List.of(deviceAccount(ACCOUNT_ID1, USER_ID1, DEVICE_ID1, USERNAME2, UNKNOWN, LOCKED)),
            service.listUserDeviceAccounts(USER_ID1));

        // unlock error cases
        assertThrows(NoDataException.class, () -> service.unlockAccount(USER_ID2, ACCOUNT_ID5, KEY));
        assertThrows(UnauthorizedException.class, () -> service.unlockAccount(USER_ID2, ACCOUNT_ID1, KEY));

        // unlock bad key, no state changed
        assertThrows(UnauthorizedException.class, () -> service.unlockAccount(USER_ID2, ACCOUNT_ID1, "bad-key"));
        assertAccountListWithStates(
            List.of(deviceAccount(ACCOUNT_ID1, USER_ID1, DEVICE_ID1, USERNAME2, UNKNOWN, LOCKED)),
            service.listUserDeviceAccounts(USER_ID1));

        // unlock with validation
        final var unlocked = deviceAccount(ACCOUNT_ID1, USER_ID1, DEVICE_ID1, USERNAME2, VALID, UNLOCKED);
        assertAccountWithStates(unlocked, service.unlockAccount(USER_ID1, ACCOUNT_ID1, KEY));
        assertAccountListWithStates(List.of(unlocked), service.listUserDeviceAccounts(USER_ID1));

        // lock -- error cases
        assertThrows(NoDataException.class, () -> service.lockAccount(USER_ID2, ACCOUNT_ID5));
        assertThrows(UnauthorizedException.class, () -> service.lockAccount(USER_ID2, ACCOUNT_ID1));

        // lock with lock status only update
        final var locked = deviceAccount(ACCOUNT_ID1, USER_ID1, DEVICE_ID1, USERNAME2, VALID, LOCKED);
        assertAccountWithStates(locked, service.lockAccount(USER_ID1, ACCOUNT_ID1));
        assertAccountListWithStates(List.of(locked), service.listUserDeviceAccounts(USER_ID1));

        // create account
        notificationService.raiseEvent(new DeviceAccountCrudEvent(ACCOUNT_ID4, CrudOperation.CREATE));
        final var account4exp = deviceAccount(ACCOUNT_ID4, USER_ID2, DEVICE_ID1, USERNAME2, VALID, HAS_NO_LOCK);
        assertAccountListWithStates(
            List.of(account2exp, account3exp, account4exp), service.listUserDeviceAccounts(USER_ID2));

        // delete account
        notificationService.raiseEvent(new DeviceAccountCrudEvent(ACCOUNT_ID3, CrudOperation.DELETE));
        assertAccountListWithStates(List.of(account2exp, account4exp), service.listUserDeviceAccounts(USER_ID2));
    }

    @Test
    void validateAccount() {
        // setup devices
        final var device1 = device(DEVICE_ID1, DEVICE_NAME1, SMB, List.of(IP1), null);
        final var device2 = device(DEVICE_ID2, DEVICE_NAME2, NFS, List.of(IP2), null);
        doReturn(List.of(device1, device2)).when(navResourceService).getAllDevices();
        doReturn(List.of()).when(navResourceService).getAllAccountsWithCredentials();

        // accessor
        final var account1 = new DeviceAccount(DEVICE_ID1, USERNAME1, PASSWORD1);
        final var account2 = new DeviceAccount(DEVICE_ID1, USERNAME2, PASSWORD2);
        doReturn(SMB).when(accessor).protocol();
        doReturn(VALID).when(accessor).validateCredentials(device1, account1);
        doReturn(INVALID).when(accessor).validateCredentials(device1, account2);

        // test
        service = new NetworkNavigationService(navResourceService, notificationService, List.of(accessor));
        assertAccountState(account1, VALID, service.validateAccount(account1));
        assertAccountState(account2, INVALID, service.validateAccount(account2));

        // unknown device
        assertThrows(IllegalArgumentException.class,
            () -> service.validateAccount(new DeviceAccount(DEVICE_INVALID, USERNAME1, PASSWORD1)));
        // no accessor for NFS protocol
        assertThrows(IllegalStateException.class,
            () -> service.validateAccount(new DeviceAccount(DEVICE_ID2, USERNAME1, PASSWORD1)));
    }

    private static void assertAccountState(final DeviceAccount expected, final DeviceAccountState expectedState,
        final DeviceAccount actual) {

        assertNotNull(actual);
        assertEquals(expected.getDeviceId(), actual.getDeviceId());
        assertEquals(expected.getUsername(), actual.getUsername());
        assertNull(actual.getPassword());
        assertEquals(expectedState, actual.getState());
    }

}
