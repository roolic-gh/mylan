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
package local.transport.netty.smb.protocol.details;

public interface UserCredentials {

    String username();

    String password();

    default String domain() {
        return null;
    }

    static UserCredentials plaintext(final String username, final String password){
        return new PlaintextUserCredentials(username, password);
    }

    record PlaintextUserCredentials(String username, String password) implements UserCredentials {
    }
}
