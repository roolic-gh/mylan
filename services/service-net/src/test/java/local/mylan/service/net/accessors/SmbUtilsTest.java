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
package local.mylan.service.net.accessors;

import static local.mylan.service.net.accessors.SmbUtils.EMPTY_PATH;
import static local.mylan.service.net.accessors.SmbUtils.sharePath;
import static org.junit.jupiter.api.Assertions.assertEquals;

import local.mylan.service.net.accessors.SmbUtils.SharePath;
import org.junit.jupiter.api.Test;

class SmbUtilsTest {

    @Test
    void path() {
        assertEquals(EMPTY_PATH, sharePath(null));
        assertEquals(EMPTY_PATH, sharePath(""));
        assertEquals(EMPTY_PATH, sharePath("/"));

        final var shareOnly = new SharePath("share", ".");
        assertEquals(shareOnly, sharePath("share"));
        assertEquals(shareOnly, sharePath("/share"));
        assertEquals(shareOnly, sharePath("/share/"));

        assertEquals(new SharePath("test", "sub"), sharePath("/test/sub"));
        assertEquals(new SharePath("test", "sub\\sub1\\sub2"), sharePath("/test/sub/sub1/sub2"));
    }
}
