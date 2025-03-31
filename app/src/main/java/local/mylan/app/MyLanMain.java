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
import local.mylan.transport.http.HttpServer;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class MyLanMain {

    private static final Logger LOG = LoggerFactory.getLogger(MyLanMain.class);

    private static final String DEFAULT_CONF_DIR = "mylan.conf";
    private static final String DEFAULT_WORK_DIR = "mylan.work";
    private static final String OPT_CONF_DIR = "c";
    private static final String OPT_WORK_DIR = "w";
    private static final String OPT_HELP = "h";

    public static void main(final String[] args) throws Exception {
        final var cmd = parseArgs(args);
        final var confDir = getConfDir(cmd.getOptionValue(OPT_CONF_DIR));
        LOG.info("Conf Dir: {}", confDir);
        final var workDir = getWorkDir(cmd.getOptionValue(OPT_WORK_DIR));
        LOG.info("Work Dir: {}", workDir);

        final var server = new HttpServer(confDir, (dir, dispatcher, ctx) -> false);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> server.stop()));

        try {
            server.start().get();
        } catch(InterruptedException e) {
            // ignore
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
            "Directory where configuration files are allocated; " +
            "user home subfolder will be created if not defined");
        opts.addOption(OPT_WORK_DIR, "work-dir", true,
            "Directory where work files will be written;" +
            " temp dir subfolder will be used if not defined");
        return opts;
    }

    private static Path getConfDir(final String path) throws IOException {
        if (path != null) {
            final var confPath = Path.of(path);
            if (Files.isDirectory(confPath)) {
                return confPath;
            } else {
                System.err.printf("conf-dir %s is not a valid directory%n", path);
            }
        }
        System.out.println("Home = " + System.getenv("HOME"));
        return null;
    }

    private static Path getWorkDir(final String path) throws IOException {
        if (path != null) {
            final var workPath = Path.of(path);
            if (Files.isDirectory(workPath)) {
                return workPath;
            }
        }
        return Files.createTempDirectory("mylan-work").toAbsolutePath();
    }
}
