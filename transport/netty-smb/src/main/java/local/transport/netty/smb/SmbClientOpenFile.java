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
package local.transport.netty.smb;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.List;
import local.transport.netty.smb.protocol.details.OpenDetails;
import local.transport.netty.smb.protocol.details.OpenFile;
import local.transport.netty.smb.protocol.flows.ClientOpenCloseFlow;
import local.transport.netty.smb.protocol.flows.ClientOpenCreateFlow;
import local.transport.netty.smb.protocol.flows.ClientQueryDirectoryFlow;
import local.transport.netty.smb.protocol.flows.RequestSender;
import local.transport.netty.smb.protocol.fscc.FileInformation;
import local.transport.netty.smb.protocol.fscc.FileInformationClass;

public class SmbClientOpenFile implements OpenFile {

    private final OpenDetails details;
    private final RequestSender sender;

    public SmbClientOpenFile(final OpenDetails details, final RequestSender sender) {
        this.details = details;
        this.sender = sender;
    }

    @Override
    public OpenDetails details() {
        return details;
    }

    @Override
    public ListenableFuture<OpenFile> create() {
        final var flow = new ClientOpenCreateFlow(this, sender);
        flow.start();
        return flow.completeFuture();
    }

    @Override
    public ListenableFuture<List<FileInformation>> queryDirectory() {
        return queryDirectory("*", FileInformationClass.FileDirectoryInformation, -1);
    }

    @Override
    public ListenableFuture<List<FileInformation>> queryDirectory(final String searchPattern,
        final FileInformationClass fic, final int maxRead) {

        final var flow = new ClientQueryDirectoryFlow(sender, details.fileId(), fic, searchPattern, maxRead);
        flow.start();
        return flow.completeFuture();
    }

    @Override
    public ListenableFuture<Void> close() {
        if (details.fileId() == null) {
            // no id, assume closed
            return Futures.immediateFuture(null);
        }
        final var flow = new ClientOpenCloseFlow(details, sender);
        flow.start();
        return flow.completeFuture();
    }
}
