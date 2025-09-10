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
package local.transport.netty.smb.protocol;

import com.google.common.base.Objects;

public final class Flags<T extends Flags.BitMaskProvider> implements Cloneable {

    int bitset;

    public Flags() {
        // default;
    }

    public Flags(int bitset) {
        this.bitset = bitset;
    }

    public int asIntValue() {
        return bitset;
    }

    public Flags<T> set(final T flag, final boolean value) {
        final var mask = flag.mask();
        if (value) {
            bitset |= mask;
        } else {
            bitset &= ~mask;
        }
        return this;
    }

    public boolean get(final T flag) {
        return (bitset & flag.mask()) != 0;
    }

    @Override
    protected Flags<T> clone() {
        return new Flags<>(bitset);
    }

    @Override
    public String toString() {
        return "%s (%s)".formatted(Integer.toHexString(bitset), Integer.toBinaryString(bitset));
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Flags<?> flags)) {
            return false;
        }
        return bitset == flags.bitset;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(bitset);
    }

    @FunctionalInterface
    public interface BitMaskProvider {

        int mask();
    }
}
