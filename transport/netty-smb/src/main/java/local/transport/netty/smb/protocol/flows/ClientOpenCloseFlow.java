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
package local.transport.netty.smb.protocol.flows;

import static java.util.Objects.requireNonNull;

import local.transport.netty.smb.protocol.Flags;
import local.transport.netty.smb.protocol.Smb2Request;
import local.transport.netty.smb.protocol.Smb2Response;
import local.transport.netty.smb.protocol.SmbException;
import local.transport.netty.smb.protocol.details.OpenDetails;
import local.transport.netty.smb.protocol.details.TreeConnectDetails;
import local.transport.netty.smb.protocol.smb2.Smb2CloseRequest;
import local.transport.netty.smb.protocol.smb2.Smb2CloseResponse;

public class ClientOpenCloseFlow extends AbstractClientFlow<Void> {

    final OpenDetails openDetails;
    final TreeConnectDetails treeConnectDetails;

    public ClientOpenCloseFlow(final OpenDetails openDetails, final RequestSender requestSender) {
        super(requestSender);
        this.openDetails = requireNonNull(openDetails);
        requireNonNull(openDetails.treeConnect());
        treeConnectDetails = requireNonNull(openDetails.treeConnect().details());
    }

    @Override
    protected Smb2Request initialRequest() {
        final var close = new Smb2CloseRequest();
        close.setFlags(new Flags<>());
        close.setFileId(openDetails.fileId());
        return close;
    }

    @Override
    public void handleResponse(final Smb2Response response) {
        try {
            if (response instanceof Smb2CloseResponse) {
                treeConnectDetails.opens().remove(openDetails.fileName());
                completeFuture.set(null);
                return;
            }
            throw new SmbException("Unexpected Close response" + response);
        } catch (SmbException e) {
            completeFuture.setException(e);
        }
    }
}
