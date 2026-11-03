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
package local.mylan.service.spi;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import local.mylan.common.utils.ConfUtils;
import local.mylan.service.api.DeviceAccessor;
import local.mylan.service.api.NavResourceService;
import local.mylan.service.api.model.Device;
import local.mylan.service.api.model.DeviceProtocol;
import local.mylan.service.api.model.HavingCredentials;
import local.mylan.service.api.model.NavDirectory;
import local.mylan.service.api.model.NavFile;
import local.mylan.service.api.model.NavResourceShare;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

public class LocalFsAccessor implements DeviceAccessor {
    private static final Logger LOG = LoggerFactory.getLogger(LocalFsAccessor.class);

    public LocalFsAccessor(final Path confPath, final NavResourceService navResourceService) {
        final var conf = ConfUtils.loadConfiguration(LocalFsAccessorConf.class, confPath);
        initLocalShares(confPath.resolve(conf.localSharesJsonFilename()), navResourceService);
        LOG.info("Initialized.");
    }

    private static void initLocalShares(final Path sharesFile, final NavResourceService navResourceService) {
        if (Files.exists(sharesFile)) {
            try {
                final var typeRef = new TypeReference<List<NavResourceShare>>() {
                };
                final var confShares = new ArrayList<NavResourceShare>();
                for (var share : new ObjectMapper().readValue(sharesFile, typeRef)) {
                    final var file = new File(share.getPath());
                    if (file.exists()) {
                        share.setFile(file.isFile());
                        confShares.add(share);
                    } else {
                        LOG.warn("Local share path {} does not exist -> defintion ignored.", file.getPath());
                    }
                }
                if (!confShares.isEmpty()) {
                    navResourceService.syncLocalSharedResources(confShares);
                }
            } catch (JacksonException e) {
                LOG.error("Error reading local shares file {} --> Content ignored.", sharesFile, e);
            }
        }
    }

    @Override
    public DeviceProtocol protocol() {
        return DeviceProtocol.LOCAL;
    }

    @Override
    public NavDirectory listDirectory(final Device device, final HavingCredentials creds, final String path) {
        final var dir = new File(path);
        if (!dir.exists()) {
            throw new IllegalArgumentException("Requested directory does not exist.");
        }
        if (!dir.isDirectory()) {
            throw new IllegalArgumentException("Requested file is not a directory.");
        }

        final var subdirs = new ArrayList<NavDirectory>();
        final var files = new ArrayList<NavFile>();
        for (var file : dir.listFiles((f, filename) -> !filename.startsWith("."))) {
            if (file.isDirectory()) {
                subdirs.add(new NavDirectory(file.getName()));
            } else if (file.isFile()) {
                files.add(new NavFile(file.getName(), file.length(), file.lastModified()));
            }
        }
        return new NavDirectory(subdirs, files);
    }
}
