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
package local.mylan.common.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.common.net.InetAddresses;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class InetAddressListTest {

    @Test
    void masks() {
        final var iaList = InetAddressList.valueOf("192.168.0.0/23");
        assertTrue(iaList.contains(address("192.168.0.100")));
        assertFalse(iaList.contains(address("192.168.2.200")));
        assertEquals(512, iaList.addressCount());

        final var all = iaList.allAddresses();
        assertNotNull(all);
        assertEquals(512, all.size());
        assertEquals(address("192.168.0.0"), all.getFirst());
        assertEquals(address("192.168.1.255"), all.getLast());
    }

    @Test
    void range() {
        final var iaList = InetAddressList.valueOf("192.168.1.1-192.168.1.3, 192.168.1.5-192.168.1.7");
        assertFalse(iaList.contains(address("192.168.1.4")));
        assertList(iaList, addressList(
            "192.168.1.1", "192.168.1.2", "192.168.1.3", "192.168.1.5", "192.168.1.6", "192.168.1.7"));
    }

    @Test
    void enumeration() {
        final var iaList = InetAddressList.valueOf("192.168.1.1, 192.168.1.5, 192.168.1.9");
        assertFalse(iaList.contains(address("192.168.1.3")));
        assertList(iaList, addressList("192.168.1.1", "192.168.1.5", "192.168.1.9"));
    }

    @Test
    void combined() {
        final var iaList = InetAddressList.valueOf("192.168.0.6/31, 192.168.1.11, 192.168.2.100-192.168.2.102");
        assertList(iaList, addressList(
            "192.168.0.6", "192.168.0.7", "192.168.1.11", "192.168.2.100", "192.168.2.101", "192.168.2.102"));
    }

    private static void assertList(final InetAddressList iaList, final List<InetAddress> expected) {
        for (var ia : expected) {
            assertTrue(iaList.contains(ia));
        }
        assertEquals(expected.size(), iaList.addressCount());
        assertEquals(expected, iaList.allAddresses());
    }

    private static InetAddress address(String str) {
        return InetAddresses.forString(str);
    }

    private static List<InetAddress> addressList(final String... strList) {
        return Arrays.stream(strList).map(InetAddresses::forString).toList();
    }
}
