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

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import local.mylan.service.api.UserContext;

class TestServiceProxy implements TestService {

    private final AtomicReference<TestService> delegateRef = new AtomicReference<>();

    void setDelegate(final TestService delegate){
        delegateRef.set(delegate);
    }

    private TestService delegate() {
        final var delegate = delegateRef.get();
        if (delegate == null){
            throw new IllegalStateException("No delegate");
        }
        return delegate;
    }

    @Override
    public List<TestPojo> getData(final int limit, final int offset) {
        return delegate().getData(limit, offset);
    }

    @Override
    public TestPojo getData(final String id) {
        return delegate().getData(id);
    }

    @Override
    public List<TestPojo> getData(final UserContext userContext) {
        return delegate().getData(userContext);
    }

    @Override
    public TestPojo insertData(final TestPojo data) {
        return delegate().insertData(data);
    }

    @Override
    public void insertData(final List<TestPojo> dataList) {
        delegate().insertData(dataList);
    }

    @Override
    public TestPojo updateData(final String id, final TestPojo data) {
        return delegate().updateData(id, data);
    }

    @Override
    public void deleteData(final String id) {
        delegate().deleteData(id);
    }
}
