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

import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

/**
 * SMB2 Message ID Sequencer. Addresses MS-SMB2 (# 3.2.4.1.6 Algorithm for Handling Available Message
 * Sequence Numbers by the Client).
 */
public class SequenceWindow {
    private final AtomicLong nextMessageId = new AtomicLong(0);
    private final AtomicLong maxMessageId = new AtomicLong(0);

    public synchronized Optional<Long> nextMessageId() {
        if (nextMessageId.get() <= maxMessageId.get()) {
            return Optional.of(maxMessageId.getAndIncrement());
        }
        return Optional.empty();
    }

    public void acceptGranted(final int granted) {
        if (granted > 0) {
            maxMessageId.addAndGet(granted);
        }
    }
}
