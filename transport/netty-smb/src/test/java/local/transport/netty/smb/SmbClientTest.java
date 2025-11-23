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
package local.transport.netty.smb;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.concurrent.TimeUnit;
import local.transport.netty.smb.protocol.Smb2Dialect;
import org.junit.jupiter.api.Test;

class SmbClientTest {

    private static final TestServer SERVER = new TestServer();

    @Test
    void connect() throws Exception {

        final var smbClient = new SmbClient();
        final var connection = smbClient.connect(TestUtils.channelToServer(SERVER.asDispatcher()))
            .get(1, TimeUnit.SECONDS);
        assertNotNull(connection);
        final var details = connection.details();
        assertNotNull(details);
        assertNotNull(details.serverGuid());
        assertEquals(Smb2Dialect.SMB3_0, details.dialect());
    }

}
