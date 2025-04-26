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
package local.mylan.service.data;

import static org.hibernate.cfg.JdbcSettings.JAKARTA_JDBC_URL;

import com.google.common.annotations.VisibleForTesting;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;
import local.mylan.service.api.UserService;
import local.mylan.service.data.entities.UserCredEntity;
import local.mylan.service.data.entities.UserEntity;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class DataServiceProvider implements AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(DataServiceProvider.class);

    @VisibleForTesting
    static final String CONF_FILENAME = "persistence.conf";
    @VisibleForTesting
    static final String DATABASE_SUBDIR = "data";
    private static final String CONF_RESOURCE = '/' + CONF_FILENAME;
    private static final String DATABASE_DIR_REPLACE = "${databaseDir}";

    private static final List<Class<?>> ENTITYCLASSES = List.of(UserEntity.class, UserCredEntity.class);

    private final SessionFactory sessionFactory;
    private final UserService userService;

    public DataServiceProvider(final Path confDir, final Path workDir) {
        final var conf = new Configuration().setProperties(loadProperties(confDir, workDir));
        ENTITYCLASSES.forEach(conf::addAnnotatedClass);
        sessionFactory = conf.buildSessionFactory();
        sessionFactory.getSchemaManager().exportMappedObjects(true);
        userService = new UserDataService(sessionFactory);
        LOG.info("Initialized.");
    }

    private Properties loadProperties(final Path confDir, final Path workDir) {
        final var properties = new Properties();

        final var resource = getClass().getResource(CONF_RESOURCE);
        if (resource != null) {
            try (var in = resource.openStream()) {
                properties.load(in);
                LOG.info("Loaded configuration from resource {}", CONF_RESOURCE);
            } catch (IOException e) {
                throw new IllegalStateException("Error reading resource " + CONF_RESOURCE, e);
            }
        }
        final var confFile = confDir.resolve(CONF_FILENAME);
        if (Files.exists(confFile)) {
            try (var in = Files.newInputStream(confFile)) {
                properties.load(in);
                LOG.info("Loaded configuration from file {}", confFile);
            } catch (IOException e) {
                throw new IllegalStateException("Error reading file " + confFile, e);
            }
        } else if (resource == null) {
            throw new IllegalStateException("Configuration file %s not found".formatted(CONF_FILENAME));
        }

        final var url = properties.getProperty(JAKARTA_JDBC_URL);
        if (url == null) {
            throw new IllegalArgumentException("%s roperty is required".formatted(JAKARTA_JDBC_URL));
        }
        if (url.contains(DATABASE_DIR_REPLACE)) {
            final var databaseDir = workDir.resolve(DATABASE_SUBDIR);
            try {
                Files.createDirectories(databaseDir);
            } catch (IOException e) {
                throw new IllegalStateException("Exception on creating database subdir " + databaseDir, e);
            }
            properties.setProperty(JAKARTA_JDBC_URL, url.replace(DATABASE_DIR_REPLACE, databaseDir.toString()));
        }
        return properties;
    }

    @Override
    public void close() throws Exception {
        sessionFactory.close();
    }
}
