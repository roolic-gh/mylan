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

import java.net.InetAddress;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import local.transport.netty.smb.protocol.details.UserCredentials;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

class SmbTest {

    @Test
    @Disabled
    void client() throws Exception {

        final var client = new SmbClient();
        final var conn = client.connect(InetAddress.getByName("192.168.1.69")).get(5, TimeUnit.SECONDS);
        final var creds = UserCredentials.plaintext("test","test");
        final var sess = conn.newSession(creds).get(5, TimeUnit.SECONDS);
        sess.close().get();
        conn.close().get();
    }

    @Test
    @Disabled
    void serverName() throws Exception {
        final var client = new SmbClient();
        final var conn = client.connect(InetAddress.getByName("192.168.1.69")).get(5, TimeUnit.SECONDS);
        try {
            final var sess = conn.newAnonimousSession().get(5, TimeUnit.SECONDS);
            sess.close().get();
        } catch(ExecutionException | TimeoutException | InterruptedException e){
            // ignore
        }
        System.out.println("Server name: " + conn.details().serverName());
        conn.close();
    }
}
