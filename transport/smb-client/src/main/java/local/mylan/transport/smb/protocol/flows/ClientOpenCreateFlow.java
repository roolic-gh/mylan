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
package local.mylan.transport.smb.protocol.flows;

import static java.util.Objects.requireNonNull;

import javax.annotation.Nonnull;
import local.mylan.transport.smb.exceptions.SmbException;
import local.mylan.transport.smb.protocol.Smb2Request;
import local.mylan.transport.smb.protocol.Smb2Response;
import local.mylan.transport.smb.protocol.details.OpenDetails;
import local.mylan.transport.smb.protocol.details.OpenFile;
import local.mylan.transport.smb.protocol.details.TreeConnectDetails;
import local.mylan.transport.smb.protocol.smb2.Smb2CreateAction;
import local.mylan.transport.smb.protocol.smb2.Smb2CreateRequest;
import local.mylan.transport.smb.protocol.smb2.Smb2CreateResponse;
import local.mylan.transport.smb.protocol.smb2.Smb2ImpersonationLevel;

public class ClientOpenCreateFlow extends AbstractClientFlow<OpenFile> {
    private final OpenFile open;
    private final OpenDetails openDetails;
    private final TreeConnectDetails treeConnectDetails;

    public ClientOpenCreateFlow(final OpenFile open, final RequestSender requestSender) {
        super(requestSender);
        this.open = requireNonNull(open);
        openDetails = requireNonNull(open.details());
        requireNonNull(openDetails.treeConnect());
        treeConnectDetails = requireNonNull(openDetails.treeConnect().details());
    }

    @Override
    protected Smb2Request initialRequest() {
        final var details = open.details();
        final var name = ".".equals(details.fileName()) ? "" : details.fileName();
        final var create = new Smb2CreateRequest();
        create.setName(name);
        create.setCreateOptions(details.createOptions());
        create.setFileAttributes(details.fileAttributes());
        create.setDesiredAccess(details.desiredAccess());
        create.setShareAccess(details.shareAccess());
        create.setImpersonationLevel(Smb2ImpersonationLevel.Impersonation);
        create.setCreateDisposition(details.createDisposition());
        create.setOpLockLevel(details.opLockLevel());
        return create;
    }

    @Override
    public void handleResponse(@Nonnull final Smb2Response response) {
        try {
            if (response instanceof Smb2CreateResponse create
                && create.createAction() == Smb2CreateAction.FILE_OPENED) {

                openDetails.setFileId(create.fileId());
                openDetails.setFileAttributes(create.fileAttributes());
                // todo handle file details (dates, size)
                treeConnectDetails.opens().put(openDetails.fileName(), open);
                completeFuture.set(open);
                return;
            }
            // todo handle error case
            throw new SmbException("Unexpected Create response" + response);
        } catch (SmbException e) {
            completeFuture.setException(e);
        }
    }
}
