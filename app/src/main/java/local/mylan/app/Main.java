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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import local.mylan.service.api.rest.RestUserService;
import local.mylan.transport.http.CompositeDispatcher;
import local.mylan.transport.http.HttpServer;
import local.mylan.transport.http.rest.SwaggerUiDispatcher;
import local.mylan.transport.http.ui.SimpleUiDispatcher;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Main {

    private static final Logger LOG = LoggerFactory.getLogger(Main.class);

    private static final String DEFAULT_CONF_SUBDIR = "mylan/conf";
    private static final String DEFAULT_WORK_SUBDIR = "mylan/work";
    private static final String OPT_CONF_DIR = "c";
    private static final String OPT_WORK_DIR = "w";
    private static final String OPT_HELP = "h";

    public static void main(final String[] args) throws Exception {
        final var cmd = parseArgs(args);
        final var confDir = getConfDir(cmd.getOptionValue(OPT_CONF_DIR));
        LOG.info("Conf Dir: {}", confDir);
        final var workDir = getWorkDir(cmd.getOptionValue(OPT_WORK_DIR));
        LOG.info("Work Dir: {}", workDir);

        final var dispatcher = CompositeDispatcher.builder()
            .defaultDispatcher(new SimpleUiDispatcher("/ui", "/rest"))
            .dispatcher(new SwaggerUiDispatcher("/swagger-ui", "/rest", RestUserService.class))
            .build();

        final var server = new HttpServer(confDir, dispatcher);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> server.stop()));
        server.start();

        synchronized (server) {
            try {
                server.wait();
            } catch (InterruptedException e) {
                // ignore
            }
        }
    }

    private static CommandLine parseArgs(final String[] args) throws ParseException {
        final var opts = options();
        final var cmd = new DefaultParser().parse(opts, args);
        if (cmd.hasOption(OPT_HELP)) {
            new HelpFormatter().printHelp("<app>", opts);
            System.exit(0);
        }
        return cmd;
    }

    private static Options options() {
        final var opts = new Options();
        opts.addOption(OPT_HELP, "help", false, "Print help");
        opts.addOption(OPT_CONF_DIR, "conf-dir", true,
            ("Directory where configuration files are allocated; " +
             "user home subfolder %s will be used if not defined").formatted(DEFAULT_CONF_SUBDIR));
        opts.addOption(OPT_WORK_DIR, "work-dir", true,
            ("Directory where work files will be written; " +
             "user home subfolder %s will be used if not defined").formatted(DEFAULT_WORK_SUBDIR));
        return opts;
    }

    private static Path getConfDir(final String path) throws IOException {
        if (path != null) {
            final var confPath = Path.of(path);
            if (Files.isDirectory(confPath)) {
                return confPath;
            } else {
                LOG.warn("Configuration directory {} does not exist or isn't a directory -> ignored", path);
            }
        }
        return defaultDir(DEFAULT_CONF_SUBDIR);
    }

    private static Path getWorkDir(final String path) throws IOException {
        if (path != null) {
            final var workPath = Path.of(path);
            if (Files.isDirectory(workPath)) {
                return workPath;
            } else {
                LOG.warn("Work directory {} does not exist or isn't a directory -> ignored", path);
            }
        }
        return defaultDir(DEFAULT_WORK_SUBDIR);
    }

    private static Path defaultDir(final String subdir) throws IOException {
        final var parentDir = Path.of(System.getProperty("user.home"));
        if (Files.isDirectory(parentDir)) {
            final var dir = parentDir.resolve(subdir);
            Files.createDirectories(dir);
            return dir;
        }
        LOG.warn("Home directory {} is invalid -> creating temp directory", parentDir);
        return Files.createTempDirectory("mylan-");
    }

}
