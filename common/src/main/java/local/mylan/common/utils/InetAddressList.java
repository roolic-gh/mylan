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

import com.google.common.net.InetAddresses;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

public final class InetAddressList {
    private static final String IPV4_RE = "[1-2]*\\d{1,2}\\.[1-2]*\\d{1,2}\\.[1-2]*\\d{1,2}\\.[1-2]*\\d{1,2}";
    private static final Pattern ADDRESS_PATTERN = Pattern.compile(IPV4_RE);
    private static final Pattern MASK_PATTERN = Pattern.compile('(' + IPV4_RE + ")/(\\d{1,2})");
    private static final Pattern RANGE_PATTERN = Pattern.compile('(' + IPV4_RE + ")\\s*-\\s*(" + IPV4_RE + ')');

    private final List<Subnet> subnets;

    private InetAddressList(final List<Subnet> subnets) {
        this.subnets = subnets;
    }

    public boolean contains(final InetAddress address) {
        final var addrInt = InetAddresses.coerceToInteger(address);
        return subnets.stream().anyMatch(im -> im.includes(addrInt));
    }

    public int addressCount() {
        return subnets.stream().mapToInt(Subnet::count).sum();
    }

    public List<? extends InetAddress> allAddresses() {
        return subnets.stream().flatMap(
            mask -> IntStream.rangeClosed(mask.from(), mask.to()).mapToObj(InetAddresses::fromInteger)
        ).toList();
    }

    public static InetAddressList valueOf(final String subnetsString) {
        final var subnets = Arrays.stream(subnetsString.split("\\s*,\\s*"))
            .map(InetAddressList::parseSubnet).filter(im -> im != null).toList();
        return new InetAddressList(subnets);
    }

    private static Subnet parseSubnet(final String str) {
        final var matcher = MASK_PATTERN.matcher(str);
        if (matcher.matches()) {
            final var addrInt = inetAddressStrToInt(matcher.group(1));
            final int bits = Integer.valueOf(matcher.group(2));
            final var mask = 0xffffffff << (32 - bits);
            final int min = addrInt & mask;
            final int max = addrInt | ~mask;
            return new Subnet(min, max, str);
        }
        final var matcher2 = RANGE_PATTERN.matcher(str);
        if (matcher2.matches()) {
            final var min = inetAddressStrToInt(matcher2.group(1));
            final var max = inetAddressStrToInt(matcher2.group(2));
            return new Subnet(min, max, str);
        }
        if (ADDRESS_PATTERN.matcher(str).matches()) {
            final var addr = inetAddressStrToInt(str);
            return new Subnet(addr, addr, str);
        }
        return null;
    }

    private record Subnet(int from, int to, String conf) {
        boolean includes(final int value) {
            return value >= from && value <= to;
        }

        int count() {
            return to - from + 1;
        }
    }

    private static int inetAddressStrToInt(final String str) {
        return InetAddresses.coerceToInteger(InetAddresses.forString(str));
    }
}
