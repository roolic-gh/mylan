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
package local.transport.netty.smb.protocol.flows;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import local.transport.netty.smb.protocol.Smb2Request;

abstract class AbstractClientFlow<T> implements ClientFlow<T> {

    protected final RequestSender requestSender;
    protected final SettableFuture<T> completeFuture = SettableFuture.create();

    protected AbstractClientFlow(final RequestSender requestSender) {
        this.requestSender = requestSender;
    }

    @Override
    public void start() {
        sendRequest(initialRequest());
    }

    protected void sendRequest(Smb2Request request){
        requestSender.send(initialRequest(), this::handleResponse);
    }

    protected abstract Smb2Request initialRequest();

    @Override
    public boolean isComplete() {
        return completeFuture.isDone();
    }

    @Override
    public ListenableFuture<T> completeFuture() {
        return completeFuture;
    }
}
