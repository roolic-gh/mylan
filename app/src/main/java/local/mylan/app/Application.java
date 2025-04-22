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

import java.nio.file.Path;
import local.mylan.service.rest.api.RestUserService;
import local.mylan.transport.http.CompositeDispatcher;
import local.mylan.transport.http.HttpServer;
import local.mylan.transport.http.rest.SwaggerUiDispatcher;
import local.mylan.transport.http.ui.SimpleUiDispatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class Application {
    private static final Logger LOG = LoggerFactory.getLogger(Application.class);

    private final Path confDir;
    private final Path workDir;

    private HttpServer server;

    Application(final Path confDir, final Path workDir) {
        this.confDir = confDir;
        this.workDir = workDir;
    }

    void start() {
        LOG.info("Starting application with conf dir: {} and work dir: {}", confDir, workDir);

        final var dispatcher = CompositeDispatcher.builder()
            .defaultDispatcher(new SimpleUiDispatcher("/ui", "/rest"))
            .dispatcher(new SwaggerUiDispatcher("/swagger-ui", "/rest", RestUserService.class))
            .build();

        server = new HttpServer(confDir, dispatcher);
        server.start();
    }

    void stop() {
        if (server != null) {
            server.stop();
        }
    }
}
