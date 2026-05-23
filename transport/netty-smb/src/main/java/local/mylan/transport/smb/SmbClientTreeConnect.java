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
package local.mylan.transport.smb;

import static java.util.Objects.requireNonNull;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.function.Consumer;
import local.mylan.transport.smb.protocol.Flags;
import local.mylan.transport.smb.protocol.Smb2Request;
import local.mylan.transport.smb.protocol.Smb2Response;
import local.mylan.transport.smb.protocol.details.OpenDetails;
import local.mylan.transport.smb.protocol.details.OpenFile;
import local.mylan.transport.smb.protocol.details.Session;
import local.mylan.transport.smb.protocol.details.SessionDetails;
import local.mylan.transport.smb.protocol.details.TreeConnect;
import local.mylan.transport.smb.protocol.details.TreeConnectDetails;
import local.mylan.transport.smb.protocol.flows.ClientTreeConnectFlow;
import local.mylan.transport.smb.protocol.flows.ClientTreeDisconnectFlow;
import local.mylan.transport.smb.protocol.flows.RequestSender;
import local.mylan.transport.smb.protocol.smb2.Smb2AccessMask;
import local.mylan.transport.smb.protocol.smb2.Smb2CreateDisposition;
import local.mylan.transport.smb.protocol.smb2.Smb2OpLockLevel;
import local.mylan.transport.smb.protocol.smb2.Smb2ShareAccessFlags;

public class SmbClientTreeConnect implements TreeConnect, RequestSender {

    private final TreeConnectDetails details;
    private final SessionDetails sessDetails;
    private final RequestSender sender;

    public SmbClientTreeConnect(final String shareName, final Session session, final RequestSender sender) {
        details = new TreeConnectDetails();
        details.setShareName(requireNonNull(shareName));
        details.setSession(requireNonNull(session));
        sessDetails = requireNonNull(session.details());
        requireNonNull(sessDetails.connection());
        requireNonNull(sessDetails.connection().details());
        details.setSharePath(
            "\\\\%s\\%s".formatted(requireNonNull(sessDetails.connection().details().serverName()), shareName));
        this.sender = sender;
    }

    @Override
    public TreeConnectDetails details() {
        return details;
    }

    ListenableFuture<TreeConnect> connect() {
        final var flow = new ClientTreeConnectFlow(this, this);
        flow.start();
        return flow.completeFuture();
    }

    @Override
    public ListenableFuture<Void> disconnect() {
        final var flow = new ClientTreeDisconnectFlow(this, this);
        flow.start();
        return flow.completeFuture();
    }

    @Override
    public ListenableFuture<OpenFile> openFile(final String path) {
        final var name = path == null || path.isEmpty() ? "." : path;
        final var opened = details.opens().get(name);
        if (opened != null) {
            return Futures.immediateFuture(opened);
        }
        final var openDetails = defaultOpenDetails(name);
        return new SmbClientOpenFile(openDetails, this).create();
    }

    private OpenDetails defaultOpenDetails(final String name) {
        final var openDetails = new OpenDetails();
        openDetails.setTreeConnect(this);
        openDetails.setFileName(name);
        openDetails.setCreateOptions(new Flags<>());
        openDetails.setFileAttributes(new Flags<>());
        openDetails.setDesiredAccess(new Flags<Smb2AccessMask>()
                .set(Smb2AccessMask.FILE_READ_DATA, true)
                .set(Smb2AccessMask.FILE_READ_ATTRIBUTES, true));
        openDetails.setShareAccess(new Flags<Smb2ShareAccessFlags>()
            .set(Smb2ShareAccessFlags.FILE_SHARE_READ, true));
        openDetails.setCreateDisposition(Smb2CreateDisposition.FILE_OPEN);
        openDetails.setOpLockLevel(Smb2OpLockLevel.SMB2_OPLOCK_LEVEL_NONE);
        return openDetails;
    }

    @Override
    public void send(final Smb2Request request, final Consumer<Smb2Response> callback) {
        if (details.treeConnectId() != null) {
            request.header().setTreeId(details.treeConnectId());
        }
        sender.send(request, callback);
    }
}
