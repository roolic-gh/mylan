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
package local.mylan.transport.http.rest;

import com.google.common.base.Objects;

public final class TestPojo {

    TestPojo() {
        // default;
    }

    TestPojo(final String id, final String name, final long timestamp) {
        this.id = id;
        this.name = name;
        this.timestamp = timestamp;
    }

    private String id;
    private String name;
    private long timestamp;

    public String getId() {
        return id;
    }

    public void setId(final String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(final long timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof final TestPojo data)) {
            return false;
        }
        return timestamp == data.timestamp && Objects.equal(id,
            data.id) && Objects.equal(name, data.name);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id, name, timestamp);
    }
}