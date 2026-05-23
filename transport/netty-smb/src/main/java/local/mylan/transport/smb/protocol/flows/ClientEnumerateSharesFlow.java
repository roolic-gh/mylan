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

import io.netty.buffer.Unpooled;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.Nonnull;
import local.mylan.transport.smb.handler.codec.SrvsCodecUtils;
import local.mylan.transport.smb.protocol.Flags;
import local.mylan.transport.smb.protocol.Smb2Request;
import local.mylan.transport.smb.protocol.Smb2Response;
import local.mylan.transport.smb.protocol.SmbException;
import local.mylan.transport.smb.protocol.fscc.Blob;
import local.mylan.transport.smb.protocol.fscc.FsctlCode;
import local.mylan.transport.smb.protocol.pcerpc.PceBind;
import local.mylan.transport.smb.protocol.pcerpc.PceBindAck;
import local.mylan.transport.smb.protocol.pcerpc.PceMessage;
import local.mylan.transport.smb.protocol.pcerpc.PceRequest;
import local.mylan.transport.smb.protocol.pcerpc.PceResponse;
import local.mylan.transport.smb.protocol.pcerpc.PduDataTypes;
import local.mylan.transport.smb.protocol.pcerpc.PduDataTypes.ContextElement;
import local.mylan.transport.smb.protocol.pcerpc.PduDataTypes.Syntax;
import local.mylan.transport.smb.protocol.pcerpc.PduDataTypes.Version;
import local.mylan.transport.smb.protocol.smb2.Smb2AccessMask;
import local.mylan.transport.smb.protocol.smb2.Smb2CreateAction;
import local.mylan.transport.smb.protocol.smb2.Smb2CreateDisposition;
import local.mylan.transport.smb.protocol.smb2.Smb2CreateRequest;
import local.mylan.transport.smb.protocol.smb2.Smb2CreateResponse;
import local.mylan.transport.smb.protocol.smb2.Smb2ImpersonationLevel;
import local.mylan.transport.smb.protocol.smb2.Smb2IoctlRequest;
import local.mylan.transport.smb.protocol.smb2.Smb2IoctlResponse;
import local.mylan.transport.smb.protocol.smb2.Smb2OpLockLevel;
import local.mylan.transport.smb.protocol.smb2.Smb2ShareAccessFlags;
import local.mylan.transport.smb.protocol.srvs.SrvShareEnumStruct;
import local.mylan.transport.smb.protocol.srvs.SrvsNetrShareEnum;
import local.mylan.transport.smb.protocol.srvs.SrvsShareInfo;
import local.mylan.transport.smb.protocol.srvs.SrvsShareInfoLevel;

public final class ClientEnumerateSharesFlow extends AbstractClientFlow<List<SrvsShareInfo>> {

    private static final int PCE_MAX_SIZE = 4280;
    private static final Syntax SRVSVC_SYNTAX =
        new Syntax(UUID.fromString("4b324fc8-1670-01d3-1278-5a47bf6ee188"), new Version(3, 0));
    private static final Syntax NDR_SYNTAX =
        new Syntax(UUID.fromString("8a885d04-1ceb-11c9-9fe8-08002b104860"), new Version(2, 0));

    private final AtomicInteger pceCallCount = new AtomicInteger(0);
    private final List<SrvsShareInfo> results = new ArrayList<>();
    private final String serverName;
    private UUID fileId;

    public ClientEnumerateSharesFlow(final String serverName, final RequestSender requestSender) {
        super(requestSender);
        this.serverName = serverName;
    }

    @Override
    protected Smb2Request initialRequest() {
        // Stage 1: open file handle to access (read-only) Server Service (SRVSVC)
        final var create = new Smb2CreateRequest();
        create.setName("srvsvc");
        create.setCreateOptions(new Flags<>());
        create.setFileAttributes(new Flags<>());
        create.setDesiredAccess(new Flags<Smb2AccessMask>()
            .set(Smb2AccessMask.FILE_READ_DATA, true)
            .set(Smb2AccessMask.READ_CONTROL, true));
        create.setShareAccess(new Flags<Smb2ShareAccessFlags>()
            .set(Smb2ShareAccessFlags.FILE_SHARE_READ, true));
        create.setImpersonationLevel(Smb2ImpersonationLevel.Impersonation);
        create.setCreateDisposition(Smb2CreateDisposition.FILE_OPEN);
        create.setOpLockLevel(Smb2OpLockLevel.SMB2_OPLOCK_LEVEL_NONE);
        return create;
    }

    @Override
    public void handleResponse(@Nonnull final Smb2Response response) {
        try {
            switch (response) {
                case Smb2CreateResponse cr -> processCreateResponse(cr);
                case Smb2IoctlResponse ioctlr -> processIocltResponse(ioctlr);
                default -> throw new SmbException("Unexpected response: " + response);
            }
        } catch (Exception e) {
            completeFuture.setException(e);
        }
    }

    private void processCreateResponse(final Smb2CreateResponse response) {
        if (response.createAction() != Smb2CreateAction.FILE_OPENED) {
            throw new SmbException("SRVSVC open action failed with result " + response.createAction());
        }
        fileId = response.fileId();

        // Stage 2: bind IOCTL RPC pipe to SRVSVC handle
        final var bind = new PceBind(pceCallCount.incrementAndGet());
        bind.setMaxReceiveFragmentSize(PCE_MAX_SIZE);
        bind.setMaxTransmitFragmentSize(PCE_MAX_SIZE);
        bind.setContexts(List.of(new ContextElement(0, SRVSVC_SYNTAX, List.of(NDR_SYNTAX))));
        sendIoctlRequest(bind);
    }

    private void processIocltResponse(final Smb2IoctlResponse response) {
        switch (response.output()) {
            case PceBindAck bindAck -> processBindAck(bindAck);
            case PceResponse resp -> processPceResp(resp);
            default -> throw new SmbException("Unexpected IOCTL Response output " + response.output());
        }
    }

    private void processBindAck(final PceBindAck bindAck) {
        final var result = bindAck.results().getFirst();
        if (result.result() != PduDataTypes.Result.acceptance) {
            throw new SmbException("SRVSVS binding failed with result " + result.result()
                + " and reson " + result.reason());
        }

        // Stage 3: enumerate shares via invoking NetrShareEnum RPC method on SRVSVC
        final var request = new PceRequest(pceCallCount.incrementAndGet());
        request.setObject(newShareEnumMessage(0));
        sendIoctlRequest(request);
    }

    private void processPceResp(final PceResponse resp) {
        if (resp.object() instanceof Blob blob) {
            resp.setObject(SrvsCodecUtils.decodeResponse(
                Unpooled.wrappedBuffer(blob.bytes()), SrvsNetrShareEnum.OPNUM));
        }
        if (resp.object() instanceof SrvsNetrShareEnum shareEnum) {
            results.addAll(shareEnum.infoStruct().infos());
            switch (shareEnum.error()) {
                case NERR_Success -> completeFuture.set(List.copyOf(results)); // finish the flow
                case ERROR_MORE_DATA -> {
                    // more data available, repeat NetrShareEnum with resume handle
                    final var request = new PceRequest(pceCallCount.incrementAndGet());
                    request.setObject(newShareEnumMessage(shareEnum.resumeHandle()));
                    sendIoctlRequest(request);
                }
                default -> completeFuture.set(List.copyOf(results)); // ignore error, return what's collected
            }
        } else {
            throw new SmbException("Could not decode PCE response object");
        }
    }

    private SrvsNetrShareEnum newShareEnumMessage(final int resumeHandle) {
        final var shareEnum = new SrvsNetrShareEnum();
        shareEnum.setServerName(serverName);
        shareEnum.setInfoStruct(new SrvShareEnumStruct(SrvsShareInfoLevel.SHARE_INFO_1, List.of()));
        shareEnum.setResumeHandle(resumeHandle);
        return shareEnum;
    }

    private void sendIoctlRequest(final PceMessage input) {
        final var ioctl = new Smb2IoctlRequest();
        ioctl.setCtlCode(FsctlCode.FSCTL_PIPE_TRANSCEIVE);
        ioctl.setFsctl(true);
        ioctl.setMaxOutputResponse(PCE_MAX_SIZE);
        ioctl.setFileId(fileId);
        ioctl.setInput(input);
        sendRequest(ioctl);
    }
}
