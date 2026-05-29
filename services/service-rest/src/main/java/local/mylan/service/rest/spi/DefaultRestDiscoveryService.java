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

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.MoreExecutors;
import local.mylan.service.api.DiscoveryService;
import local.mylan.service.api.NotificationService;
import local.mylan.service.api.UserContext;
import local.mylan.service.api.events.DiscoveryStatusEvent;
import local.mylan.service.api.model.DiscoveryStatus;
import local.mylan.service.rest.api.RestDiscoveryService;

public class DefaultRestDiscoveryService implements RestDiscoveryService {

    private final DiscoveryService discoveryService;
    private final NotificationService notificationService;

    public DefaultRestDiscoveryService(final DiscoveryService discoveryService,
        final NotificationService notificationService) {
        this.discoveryService = discoveryService;
        this.notificationService = notificationService;
    }

    @Override
    public DiscoveryStatus startDiscovery(final UserContext userCtx) {
        final var userId = userCtx.currentUser().getUserId();
        if (userId != null) {
            Futures.addCallback(discoveryService.startDiscovery(),
                new FutureCallback<DiscoveryStatus>() {
                    @Override
                    public void onSuccess(final DiscoveryStatus result) {
                        notificationService.raiseEvent(userId, new DiscoveryStatusEvent(result));
                    }

                    @Override
                    public void onFailure(final Throwable t) {
                        // ignore
                    }
                }, MoreExecutors.directExecutor());
        }
        return discoveryService.currentStatus();
    }

    @Override
    public DiscoveryStatus getDiscoveryStatus() {
        return discoveryService.currentStatus();
    }
}
