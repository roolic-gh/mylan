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

import static local.mylan.service.data.DataServiceProvider.CONF_FILENAME;
import static local.mylan.service.data.DataServiceProvider.DATABASE_SUBDIR;
import static org.hibernate.cfg.JdbcSettings.JAKARTA_JDBC_URL;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DataServiceProviderTest {

    @TempDir
    Path dir;

    DataServiceProvider dataServiceProvider;

    @AfterEach
    void afterEach() throws Exception {
        if(dataServiceProvider != null){
            dataServiceProvider.close();
        }
    }

    @Test
    void propsFromResourceOnly() throws Exception {
        dataServiceProvider = new DataServiceProvider(dir, dir);
        assertDatabaseDir("default");
    }

    @Test
    void propsFromFile() throws Exception {
        Files.writeString(dir.resolve(CONF_FILENAME),
            JAKARTA_JDBC_URL + "=jdbc:h2:file:${databaseDir}/override\n");
        dataServiceProvider = new DataServiceProvider(dir, dir);
        assertDatabaseDir("override");
    }

    void assertDatabaseDir(final String dbname) throws Exception {
        final var databaseDir = dir.resolve(DATABASE_SUBDIR);
        Assertions.assertTrue(Files.isDirectory(databaseDir));
        final var databaseFile = databaseDir.resolve(dbname + ".mv.db");
        Assertions.assertTrue(Files.isRegularFile(databaseFile));
    }
}
