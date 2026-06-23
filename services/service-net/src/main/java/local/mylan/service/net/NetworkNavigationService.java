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

import static java.util.stream.Collectors.toMap;

import com.google.common.annotations.VisibleForTesting;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import local.mylan.service.api.DeviceAccessor;
import local.mylan.service.api.NavResourceService;
import local.mylan.service.api.NavigationService;
import local.mylan.service.api.NotificationService;
import local.mylan.service.api.events.DeviceAccountCrudEvent;
import local.mylan.service.api.events.DeviceCrudEvent;
import local.mylan.service.api.events.DiscoveryDevicesEvent;
import local.mylan.service.api.exceptions.UnauthorizedException;
import local.mylan.service.api.model.Device;
import local.mylan.service.api.model.DeviceAccount;
import local.mylan.service.api.model.DeviceAccountLockState;
import local.mylan.service.api.model.DeviceAccountState;
import local.mylan.service.api.model.DeviceAccountWithCredentials;
import local.mylan.service.api.model.DeviceIpAddress;
import local.mylan.service.api.model.DeviceProtocol;
import local.mylan.service.api.model.DeviceState;
import local.mylan.service.api.model.NavDirectory;
import local.mylan.service.api.model.NavResourceBookmark;
import local.mylan.service.api.model.NavResourceShare;
import local.mylan.service.net.accessors.SmbDeviceAccessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class NetworkNavigationService implements NavigationService {
    private static final Logger LOG = LoggerFactory.getLogger(NetworkNavigationService.class);
    private static final Comparator<Device> DEVICE_COMPARATOR = (a, b) ->
        CharSequence.compare(a.getIdentifier(), b.getIdentifier());
    private static final Comparator<DeviceAccount> ACCOUNT_COMPARATOR = (a, b) -> {
        final var byDevice = CharSequence.compare(a.getDeviceIdentifier(), b.getDeviceIdentifier());
        return byDevice == 0 ? CharSequence.compare(a.getUsername(), b.getUsername()) : byDevice;
    };

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private final NavResourceService navResourceService;
    private final Set<String> pendingOnlineDeviceIdentifiers = ConcurrentHashMap.newKeySet();
    private final Map<DeviceProtocol, DeviceAccessor> accessorsMap;
    private final Map<Integer, Device> deviceMap = new ConcurrentHashMap<>();
    private final Map<Integer, DeviceAccountWithCredentials> accountMap = new ConcurrentHashMap<>();

    public NetworkNavigationService(final Path confDir, final NavResourceService navResourceService,
        final NotificationService notificationService) {
        this(navResourceService, notificationService, defaultAccessors(confDir));
    }

    @VisibleForTesting
    NetworkNavigationService(final NavResourceService navResourceService, final NotificationService notificationService,
        final Collection<? extends DeviceAccessor> accessors) {

        this.navResourceService = navResourceService;
        accessorsMap = accessors.stream().collect(toMap(DeviceAccessor::protocol, accr -> accr));

        notificationService.registerEventListener(DiscoveryDevicesEvent.class, this::onDiscovery);
        notificationService.registerEventListener(DeviceCrudEvent.class, this::onDeviceCrud);
        notificationService.registerEventListener(DeviceAccountCrudEvent.class, this::onDeviceAccountCrud);

        initCaches();
        LOG.info("Initialized.");
    }

    private void initCaches() {
        navResourceService.getAllDevices().forEach(device -> {
            device.setState(DeviceState.OFFLINE);
            deviceMap.put(device.getDeviceId(), device);
        });

        navResourceService.getAllAccountsWithCredentials().forEach(account -> {
            final var device = deviceMap.get(account.getDeviceId());
            account.setDeviceIdentifier(device == null ? "" : device.getIdentifier());
            account.setState(DeviceAccountState.UNKNOWN);
            accountMap.put(account.getAccountId(), account);
        });
        // valide unlocked accounts
        executor.submit(() -> accountMap.values().forEach(this::validateUpdateAccountState));
    }

    private static Set<DeviceAccessor> defaultAccessors(final Path confDir) {
        return Set.of(new SmbDeviceAccessor(confDir));
    }

    private void onDiscovery(final DiscoveryDevicesEvent event) {
        // update caches first: set online statuses and refresh ip addresses
        pendingOnlineDeviceIdentifiers.clear();
        final var onlineDevicesMap = new HashMap<String, Device>(
            event.devices().stream().collect(toMap(Device::getIdentifier, device -> device)));

        for (var device : deviceMap.values()) {
            final var onlineDevice = onlineDevicesMap.remove(device.getIdentifier());
            if (onlineDevice == null) {
                device.setState(DeviceState.OFFLINE);
            } else {
                device.setIpAddresses(copyIpAddresses(onlineDevice.getIpAddresses()));
                device.setState(DeviceState.ONLINE);
            }
        }

        if (!onlineDevicesMap.isEmpty()) {
            pendingOnlineDeviceIdentifiers.addAll(onlineDevicesMap.keySet());
        }
        // update data layer, causing DeviceCrudEvent with new device id if new device detected
        navResourceService.syncDeviceAddresses(event.devices());
    }

    private void onDeviceCrud(final DeviceCrudEvent event) {
        switch (event.operation()) {
            case CREATE -> {
                final var device = navResourceService.getDevice(event.deviceId());
                device.setState(pendingOnlineDeviceIdentifiers.remove(device.getIdentifier())
                    ? DeviceState.ONLINE : DeviceState.OFFLINE);
                deviceMap.put(device.getDeviceId(), device);
            }
            case DELETE -> {
                deviceMap.remove(event.deviceId());
                // TODO clear caches cascade
            }
        }
    }

    @Override
    public List<Device> listDevices() {
        return deviceMap.values().stream()
            .map(NetworkNavigationService::copyDevice).sorted(DEVICE_COMPARATOR).toList();
    }

    private static Device copyDevice(final Device device) {
        final var copy = new Device(device.getIdentifier(), device.getProtocol());
        copy.setDeviceId(device.getDeviceId());
        copy.setState(device.getState());
        copy.setIpAddresses(copyIpAddresses(device.getIpAddresses()));
        return copy;
    }

    private static List<DeviceIpAddress> copyIpAddresses(final List<DeviceIpAddress> ipAddresses) {
        return ipAddresses == null
            ? List.of() : ipAddresses.stream().map(ipa -> new DeviceIpAddress(ipa.getIpAddress())).toList();
    }

    @Override
    public List<DeviceAccount> listUserDeviceAccounts(final Integer userId) {
        return accountMap.values().stream().filter(account -> Objects.equals(userId, account.getUserId()))
            .map(NetworkNavigationService::copyAccount).sorted(ACCOUNT_COMPARATOR).toList();
    }

    private static DeviceAccount copyAccount(final DeviceAccountWithCredentials account) {
        final var copy = new DeviceAccount();
        copy.setAccountId(account.getAccountId());
        copy.setDeviceId(account.getDeviceId());
        copy.setDeviceIdentifier(account.getDeviceIdentifier());
        copy.setUserId(account.getUserId());
        copy.setUsername(account.getUsername());
        copy.setState(account.getState());
        copy.setLockState(account.getLockState());
        return copy;
    }

    private static DeviceAccount copyAccount(final DeviceAccount account) {
        final var copy = new DeviceAccount();
        copy.setAccountId(account.getAccountId());
        copy.setDeviceId(account.getDeviceId());
        copy.setUserId(account.getUserId());
        copy.setUsername(account.getUsername());
        copy.setState(account.getState());
        return copy;
    }

    @Override
    public DeviceAccount unlockAccount(final Integer userId, final Integer accountId, final String key) {
        final var account = validUserAccount(accountId, userId);
        if (account.getLockState() == DeviceAccountLockState.LOCKED) {
            account.unlock(key);
            if (account.getLockState() == DeviceAccountLockState.UNLOCKED) {
                validateUpdateAccountState(account);
            }
        }
        return copyAccount(account);
    }

    @Override
    public DeviceAccount lockAccount(final Integer userId, final Integer accountId) {
        final var account = validUserAccount(accountId, userId);
        if (account.getLockState() == DeviceAccountLockState.UNLOCKED) {
            account.lock();
        }
        return copyAccount(account);
    }

    private void onDeviceAccountCrud(final DeviceAccountCrudEvent event) {
        final var accountId = event.accountId();
        switch (event.operation()) {
            case CREATE, UPDATE -> {
                final var account = navResourceService.getAccountWithCredentials(accountId);
                final var device = deviceMap.get(account.getDeviceId());
                account.setDeviceIdentifier(device == null ? "" : device.getIdentifier());
                account.setState(DeviceAccountState.UNKNOWN);
                validateUpdateAccountState(account);
                accountMap.put(accountId, account);
            }
            case DELETE -> {
                accountMap.remove(accountId);
                // TODO delete cascase
            }
        }
    }

    private void validateUpdateAccountState(final DeviceAccountWithCredentials account) {
        if (account.getLockState() == DeviceAccountLockState.LOCKED) {
            return;
        }
        final var device = deviceMap.get(account.getDeviceId());
        final var accessor = device == null ? null : accessorsMap.get(device.getProtocol());
        if (accessor == null) {
            return;
        }
        try {
            account.setState(accessor.validateCredentials(device, account));
        } catch (Exception e) {
            // ignore
        }
    }

    @Override
    public DeviceAccount validateAccount(final DeviceAccount account) {
        final var device = validDevice(account.getDeviceId());
        final var accessor = validAccessor(device.getProtocol());
        final var state = accessor.validateCredentials(device, account);
        final var result = copyAccount(account);
        result.setState(state);
        return result;
    }

    @Override
    public List<NavResourceShare> listShares(final Integer userId) {
        return List.of();
    }

    @Override
    public List<NavResourceShare> listUserShares(final Integer userId) {
        return List.of();
    }

    @Override
    public List<NavResourceBookmark> listUserBookmarks(final Integer userId) {
        return List.of();
    }

    @Override
    public NavDirectory readDeviceDirectoryByAccount(final Integer userId, final Integer accountId, final String path) {
        final var account = ensureUnlocked(validUserAccount(accountId, userId));
        final var device = validDevice(account.getDeviceId());
        final var accessor = validAccessor(device.getProtocol());
        final var dir = accessor.listDirectory(device, account, path);
        dir.setAccount(copyAccount(account));
        setDirPaths(dir, path);
        return dir;
    }

    @Override
    public NavDirectory readDeviceDirectoryByShare(final Integer userId, final Integer shareId, final String path) {
        return null;
    }

    @Override
    public void stop() {
        executor.shutdown();
        accessorsMap.values().forEach(DeviceAccessor::stop);
        LOG.info("Stopped.");
    }

    private Device validDevice(final Integer deviceId) {
        final var device = deviceId == null ? null : deviceMap.get(deviceId);
        if (device == null) {
            throw new IllegalArgumentException("Invalid device ID %s.".formatted(deviceId));
        }
        return device;
    }

    private DeviceAccessor validAccessor(final DeviceProtocol protocol) {
        final var accessor = accessorsMap.get(protocol);
        if (accessor == null) {
            throw new IllegalStateException("Unsupported protocol %s.".formatted(protocol));
        }
        return accessor;
    }

    private DeviceAccountWithCredentials validAccount(final Integer accountId) {
        final var account = accountMap.get(accountId);
        if (account == null) {
            throw new IllegalArgumentException("Device account with id % does not exuist.".formatted(accountId));
        }
        return account;
    }

    private DeviceAccountWithCredentials validUserAccount(final Integer accountId, final Integer userId) {
        final var account = validAccount(accountId);
        if (!Objects.equals(userId, account.getUserId())) {
            throw new UnauthorizedException("Only account owner can interact with the account.");
        }
        return account;
    }

    private static DeviceAccountWithCredentials ensureUnlocked(final DeviceAccountWithCredentials account) {
        if (account.getLockState() == DeviceAccountLockState.LOCKED) {
            throw new IllegalStateException("Account is locked.");
        }
        return account;
    }

    private static void setDirPaths(final NavDirectory dir, final String path) {
        dir.setPath(path);
        final var pathPrefix = path == null || path.isEmpty() || "/".equals(path) ? "/" : path + '/';
        if (dir.getSubDirs() != null) {
            dir.getSubDirs().forEach(navDir -> navDir.setPath(pathPrefix + navDir.getName()));
        }
        if(dir.getFiles() != null){
            dir.getFiles().forEach(file -> file.setPath(pathPrefix + file.getName()));
        }
    }
}
