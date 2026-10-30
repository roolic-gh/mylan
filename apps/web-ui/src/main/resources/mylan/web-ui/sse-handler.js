/*
  Copyright 2026 Ruslan Kashapov

  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

      https://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
*/

export class SseHandler {
    uri = null;
    sse = null;
    token = null;
    callbacks = [];

    constructor(uri) {
        this.uri = uri;
    }

    setToken(newToken) {
        this.token = newToken;
    }

    clearToken() {
        this.token = null;
        this.stop();
        this.callbacks = [];
    }

    waitForEvent(type, callback) {
        this.callbacks[type] = callback
        if (!this.sse) {
            this.start();
        }
    }

    start() {
        if (!this.token) {
            return;
        }
        this.sse = new SSE(this.uri, {
            method: "GET",
            headers: {
                Authorization: "Bearer " + this.token,
                Accept: "text/event-stream"
            },
            autoReconnect: true,
            reconnectDelay: 3000,
            maxRetries: 1,
            start: false
        });
        this.sse.addEventListener("message", (evt) => {
            if (evt.data) {
                this.onEvent(JSON.parse(evt.data));
            }
        })
        this.sse.stream();
    }

    stop() {
        if (this.sse) {
            this.sse.close();
            this.sse = null;
        }
    }

    onEvent(event) {
        console.log(event);
        const type = event.eventType;
        const callback = type == null ? null : this.callbacks[type];
        if (callback) {
            callback(event);
            delete this.callbacks[type];
            if (this.callbacks.length < 1) {
                this.stop();
            }
        }
    }
}
