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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.SettableFuture;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import java.net.InetAddress;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import local.mylan.common.utils.ConfUtils;
import local.mylan.common.utils.InetAddressList;
import local.mylan.service.api.DeviceAccessor;
import local.mylan.service.api.DiscoveryService;
import local.mylan.service.api.NotificationService;
import local.mylan.service.api.events.DiscoveryDevicesEvent;
import local.mylan.service.api.model.Device;
import local.mylan.service.api.model.DeviceIpAddress;
import local.mylan.service.api.model.DiscoveryStatus;
import local.mylan.service.net.accessors.SmbDeviceAccessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NetworkDiscoveryService implements DiscoveryService {
    private static final Logger LOG = LoggerFactory.getLogger(NetworkDiscoveryService.class);

    private final ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
    private final ExecutorService executorService;

    private final AtomicReference<SettableFuture<DiscoveryStatus>> discoveryFutureRef = new AtomicReference<>();
    private final AtomicReference<SettableFuture<Void>> stopFutureRef = new AtomicReference<>();
    private final AtomicBoolean running = new AtomicBoolean();
    private final AtomicBoolean stopping = new AtomicBoolean();
    private final AtomicReference<DiscoveryStatus> currentStatus = new AtomicReference<>();

    private final InetAddressList addressList;
    private final NetworkDiscoveryServiceConf conf;
    private final NotificationService notificationService;
    private final Set<DeviceAccessor> accessors;

    public NetworkDiscoveryService(final Path confDir, final NotificationService notificationService) {
        this(ConfUtils.loadConfiguration(NetworkDiscoveryServiceConf.class, confDir), notificationService,
            defaultAccessors(confDir));
    }

    public NetworkDiscoveryService(final NetworkDiscoveryServiceConf conf, final NotificationService notificationService,
        final Collection<DeviceAccessor> accessors) {
        this(conf, notificationService, InetAddressList.valueOf(conf.subnets()), accessors);
    }

    @VisibleForTesting
    NetworkDiscoveryService(final NetworkDiscoveryServiceConf conf, final NotificationService notificationService,
        final InetAddressList addressList, final Collection<? extends DeviceAccessor> accessors) {

        this.conf = conf;
        this.addressList = addressList;
        this.notificationService = notificationService;
        executorService = Executors.newFixedThreadPool(conf.threads(),
            new ThreadFactoryBuilder().setNameFormat("remote-discovery-%d").build());
        this.accessors = Set.copyOf(accessors);
        currentStatus.set(new DiscoveryStatus());
        if (conf.rediscoverIntervalSeconds() > 0) {
            scheduledExecutorService.scheduleAtFixedRate(() -> executeNewDiscovery(true),
                conf.rediscoverDelaySeconds(), conf.rediscoverIntervalSeconds(), TimeUnit.SECONDS);
        }
        LOG.info("Initialized");
    }

    private static Set<DeviceAccessor> defaultAccessors(final Path confDir) {
        return Set.of(new SmbDeviceAccessor(confDir));
    }

    @Override
    public ListenableFuture<DiscoveryStatus> startDiscovery() {
        if (stopping.get()) {
            // terminate
            running.set(false);
            return Futures.immediateFuture(currentStatus.get());
        }
        return running.get() ? discoveryFutureRef.get() : executeNewDiscovery(false);
    }

    private ListenableFuture<DiscoveryStatus> executeNewDiscovery(final boolean scheduled) {
        synchronized (running) {
            // ensure explicit start does not coinside with scheduled one
            if (running.get()) {
                if (scheduled) {
                    // set next execution time to be present in status
                    currentStatus.get()
                        .setNextRunTime(System.currentTimeMillis() + conf.rediscoverIntervalSeconds() * 1000L);
                }
                return discoveryFutureRef.get();
            } else {
                final var newStatus = new DiscoveryStatus();
                newStatus.setStartTime(System.currentTimeMillis());
                if (scheduled) {
                    newStatus.setNextRunTime(System.currentTimeMillis() + conf.rediscoverIntervalSeconds() * 1000L);
                }
                currentStatus.set(newStatus);
                discoveryFutureRef.set(SettableFuture.create());
                running.set(true);
            }
        }

        final Map<String, Device> devices = new ConcurrentHashMap<>();
        final var results = addressList.allAddresses().stream().map(
            ipAddress -> Futures.submit(() -> checkDeviceAddress(ipAddress, devices), executorService)
        ).toList();
        Futures.whenAllComplete(results).run(() -> finalizeDiscovery(devices), MoreExecutors.directExecutor());
        LOG.info("Device discovery started...");
        return discoveryFutureRef.get();
    }

    private void checkDeviceAddress(final InetAddress ipAddress, final Map<String, Device> devices) {
        for (var accessor : accessors) {
            if (stopping.get()) {
                return; // terminate
            }
            final var name = accessor.extractDeviceName(ipAddress);
            if (name != null) {
                // check if there's device with same name, associate device to ip address
                final var device = devices.computeIfAbsent(name, key -> new Device(name, accessor.protocol()));
                synchronized (device) {
                    // ensure ip address list for a device always updated by a single thread
                    final var deviceAddresses = new ArrayList<DeviceIpAddress>();
                    Optional.ofNullable(device.getIpAddresses()).ifPresent(deviceAddresses::addAll);
                    deviceAddresses.add(new DeviceIpAddress(ipAddress.getHostAddress()));
                    device.setIpAddresses(deviceAddresses);
                }
                return;
            }
        }
    }

    private void finalizeDiscovery(final Map<String, Device> devices) {
        if (stopping.get()) {
            return; // service terminated, do nothing
        }

        final var now = System.currentTimeMillis();
        final var status = currentStatus.get();
        status.setEndTime(now);
        status.setRunning(false);
        status.setDevicesDiscovered(devices.size());
        discoveryFutureRef.get().set(status);
        running.set(false);
        notificationService.raiseEvent(new DiscoveryDevicesEvent(List.copyOf(devices.values())));
        LOG.info("Discovery completed -> {} devices found", devices.size());
    }

    @Override
    public void stop() {
        stopping.set(true);
        scheduledExecutorService.shutdown();
        executorService.shutdown();
        LOG.info("Stopped");
    }

    @Override
    public DiscoveryStatus currentStatus() {
        final var current = currentStatus.get();
        final var result = new DiscoveryStatus();
        result.setStartTime(current.getStartTime());
        result.setEndTime(current.getEndTime());
        result.setNextRunTime(current.getNextRunTime());
        result.setDevicesDiscovered(current.getDevicesDiscovered());
        result.setRunning(running.get());
        return result;
    }
}
