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

import static java.util.Objects.requireNonNull;

import local.transport.netty.smb.protocol.Flags;
import local.transport.netty.smb.protocol.Smb2Dialect;
import local.transport.netty.smb.protocol.Smb2Request;
import local.transport.netty.smb.protocol.Smb2Response;
import local.transport.netty.smb.protocol.SmbError;
import local.transport.netty.smb.protocol.SmbException;
import local.transport.netty.smb.protocol.details.ConnectionDetails;
import local.transport.netty.smb.protocol.details.SessionDetails;
import local.transport.netty.smb.protocol.details.TreeConnect;
import local.transport.netty.smb.protocol.smb2.Smb2ShareCapabilitiesFlags;
import local.transport.netty.smb.protocol.smb2.Smb2ShareFlags;
import local.transport.netty.smb.protocol.smb2.Smb2TreeConnectRequest;
import local.transport.netty.smb.protocol.smb2.Smb2TreeConnectResponse;

/**
 * Client Tree Connect Flow.
 * Addresses MS-SMB2 (#3.2.4.2.4 Connecting to the Share
 * and #3.2.5.5 Receiving an SMB2 TREE_CONNECT Response).
 */
public final class ClientTreeConnectFlow extends AbstractClientFlow<TreeConnect> {

    private final TreeConnect treeConnect;
    private final SessionDetails sessDetails;
    private final ConnectionDetails connDetails;

    public ClientTreeConnectFlow(final TreeConnect treeConnect, final RequestSender requestSender) {
        super(requestSender);
        this.treeConnect = requireNonNull(treeConnect);
        requireNonNull(treeConnect.details());
        requireNonNull(treeConnect.details().shareName());
        requireNonNull(treeConnect.details().sharePath());
        requireNonNull(treeConnect.details().session());
        sessDetails = requireNonNull(treeConnect.details().session().details());
        requireNonNull(sessDetails.connection());
        connDetails = requireNonNull(sessDetails.connection().details());
    }

    @Override
    protected Smb2Request initialRequest() {
        final var request = new Smb2TreeConnectRequest();
        request.setPath(treeConnect.details().sharePath());
        request.setFlags(new Flags<>());
        return request;
    }

    @Override
    public void handleResponse(final Smb2Response response) {
        try {
            if (response instanceof Smb2TreeConnectResponse tcResponse) {
                process(tcResponse);
            } else {
                throw new SmbException("Unexpected Tree Connect response: " + response);
            }
        } catch (Exception e) {
            completeFuture.setException(e);
        }
    }

    private void process(final Smb2TreeConnectResponse response) {
        if (response.header().status() != SmbError.STATUS_SUCCESS) {
            throw new SmbException("Error with status " + response.header().status());
        }
        treeConnect.details().setTreeConnectId(response.header().treeId());
        treeConnect.details().setDfsShare(response.capabilities().get(Smb2ShareCapabilitiesFlags.SMB2_SHARE_CAP_DFS));
        treeConnect.details().setCaShare(
            response.capabilities().get(Smb2ShareCapabilitiesFlags.SMB2_SHARE_CAP_CONTINUOUS_AVAILABILITY));
        if (connDetails.dialect().equalsOrHigher(Smb2Dialect.SMB3_0)) {
            treeConnect.details().setEncryptData(connDetails.supportsEncryption()
                && response.shareFlags().get(Smb2ShareFlags.SMB2_SHAREFLAG_ENCRYPT_DATA));
            treeConnect.details().setScaleoutShare(
                response.capabilities().get(Smb2ShareCapabilitiesFlags.SMB2_SHARE_CAP_SCALEOUT));
        }
        if (connDetails.dialect().equalsOrHigher(Smb2Dialect.SMB3_1_1)) {
            // TODO compressData and isolatedTransport flags
        }
        sessDetails.treeConnects().put(treeConnect.details().shareName(), treeConnect);
        completeFuture.set(treeConnect);
    }

}
