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
package local.mylan.app;

import static local.mylan.transport.http.ext.StaticContentDispatcher.SourceType.FILE_SYSTEM;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import local.mylan.service.api.DiscoveryService;
import local.mylan.service.api.NotificationService;
import local.mylan.service.data.DataServiceProvider;
import local.mylan.service.remote.NetworkNavigationService;
import local.mylan.service.remote.RemoteDiscoveryService;
import local.mylan.service.rest.api.DiscoveryRestService;
import local.mylan.service.rest.api.NavResourceRestService;
import local.mylan.service.rest.api.NavigationRestService;
import local.mylan.service.rest.api.UserRestService;
import local.mylan.service.rest.spi.DefaultDiscoveryRestService;
import local.mylan.service.rest.spi.DefaultNavResourceRestService;
import local.mylan.service.rest.spi.DefaultNavigationRestService;
import local.mylan.service.rest.spi.DefaultUserRestService;
import local.mylan.service.spi.DefaultEncryptionService;
import local.mylan.service.spi.DefaultNotificationService;
import local.mylan.transport.http.CompositeDispatcher;
import local.mylan.transport.http.HttpServer;
import local.mylan.transport.http.ext.SseDispatcher;
import local.mylan.transport.http.ext.StaticContentDispatcher;
import local.mylan.transport.http.rest.RestServiceDispatcher;
import local.mylan.transport.http.rest.SwaggerUiDispatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class AppServer {
    private static final Logger LOG = LoggerFactory.getLogger(AppServer.class);

    private final Path confDir;
    private final Path workDir;

    private DataServiceProvider dataServiceProvider;
    private NotificationService notificationService;
    private DiscoveryService discoveryService;
    private HttpServer server;

    AppServer(final Path confDir, final Path workDir) {
        this.confDir = confDir;
        this.workDir = workDir;
    }

    void start() {
        LOG.info("Starting application with conf dir: {} and work dir: {}", confDir, workDir);

        // independent services
        notificationService = new DefaultNotificationService();
        final var encryptionService = new DefaultEncryptionService(confDir, workDir);

        // persistence layer services
        dataServiceProvider = new DataServiceProvider(confDir, workDir);
        final var userService = dataServiceProvider.buildUserService(encryptionService, notificationService);
        final var navResourceService =
            dataServiceProvider.buildNavResourceService(encryptionService, notificationService);

        // networking
        discoveryService = new RemoteDiscoveryService(confDir, notificationService);
        final var navService = new NetworkNavigationService(confDir, navResourceService, notificationService);

        // rest endpoints
        final var userRestService = new DefaultUserRestService(userService);
        final var restDispatcher = new RestServiceDispatcher("/rest", List.of(
            userRestService,
            new DefaultDiscoveryRestService(discoveryService, notificationService),
            new DefaultNavResourceRestService(navResourceService),
            new DefaultNavigationRestService(navService)
        ));
        final var swaggerDispatcher = new SwaggerUiDispatcher("/swagger-ui", "/rest", List.of(
            UserRestService.class, DiscoveryRestService.class,
            NavResourceRestService.class, NavigationRestService.class));

        // streaming
        final var sseDispatcher = new SseDispatcher("/sse", notificationService, 10_000L);

        // web ui
        boolean devMode = true; // TODO make configurable
        final StaticContentDispatcher uiDispatcher;
        if (devMode) {
            uiDispatcher = new StaticContentDispatcher("/ui",
                "apps/web-ui/src/main/resources/mylan/web-ui", FILE_SYSTEM);
            uiDispatcher.setCheckFileUpdates(true);
            uiDispatcher.substitute("/index.html", Map.of(
                "${SELF_CONTEXT}", "/ui", "${LIBS_CONTEXT}", "/ui/target-libs",
                "${REST_CONTEXT}", "/rest", "${SSE_CONTEXT}", "/sse"));
        } else {
            uiDispatcher = new StaticContentDispatcher("/ui", "/mylan/web-ui");
            uiDispatcher.substitute("/index.html", Map.of(
                "${SELF_CONTEXT}", "/ui", "${LIBS_CONTEXT}", "/ui/libs",
                "${REST_CONTEXT}", "/rest", "${SSE_CONTEXT}", "/sse"));
        }

        // http server
        final var dispatcher = CompositeDispatcher.builder()
            .authenticator(userRestService::authenticate)
            .defaultDispatcher(uiDispatcher)
            .dispatchers(sseDispatcher, swaggerDispatcher, restDispatcher)
            .build();
        server = new HttpServer(confDir, dispatcher);
        server.start();
    }

    void stop() {
        if (server != null) {
            server.stop();
        }
        if (dataServiceProvider != null) {
            try {
                dataServiceProvider.close();
            } catch (Exception e) {
                // ignore;
            }
        }
        if (notificationService != null) {
            notificationService.stop();
        }
        if (discoveryService != null) {
            discoveryService.stop();
        }
    }
}
