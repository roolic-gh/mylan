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
import javax.annotation.Nonnull;
import local.mylan.transport.smb.exceptions.SmbException;
import local.mylan.transport.smb.handler.codec.FsccCodecUtils;
import local.mylan.transport.smb.protocol.Flags;
import local.mylan.transport.smb.protocol.Smb2Request;
import local.mylan.transport.smb.protocol.Smb2Response;
import local.mylan.transport.smb.protocol.fscc.FileInformation;
import local.mylan.transport.smb.protocol.fscc.FileInformationClass;
import local.mylan.transport.smb.protocol.smb2.Smb2QueryDirectoryRequest;
import local.mylan.transport.smb.protocol.smb2.Smb2QueryDirectoryResponse;

public class ClientQueryDirectoryFlow extends AbstractClientFlow<List<FileInformation>> {
    private static final int OUTPUT_BUFFER_LENGTH = 0x00800000; // todo set from config

    final UUID fileId;
    final FileInformationClass fic;
    final int maxRead;
    final String searchPattern;
    final List<FileInformation> result = new ArrayList<>();

    public ClientQueryDirectoryFlow(final RequestSender requestSender, final UUID fileId,
        final FileInformationClass fic, final String searchPattern, final int maxRead) {

        super(requestSender);
        this.fileId = fileId;
        this.fic = fic;
        this.maxRead = maxRead;
        this.searchPattern = searchPattern;
    }

    @Override
    protected Smb2Request initialRequest() {
        return queryDirRequest();
    }

    private Smb2Request queryDirRequest() {
        final var request = new Smb2QueryDirectoryRequest();
        request.setFileId(fileId);
        request.setFileInformationClass(fic);
        request.setFlags(new Flags<>());
        request.setOutputBufferLength(OUTPUT_BUFFER_LENGTH);
        request.setSearchPattern(searchPattern);
        // Explicit non-zero credit charge according to
        // MS-SMB2 (#3.2.4.17 Application Requests Enumerating a Directory)
        request.header().setCreditCharge(1 + (OUTPUT_BUFFER_LENGTH - 1) / 65536);
        return request;
    }

    @Override
    public void handleResponse(@Nonnull final Smb2Response response) {
        try {
            if (response instanceof Smb2QueryDirectoryResponse dir) {
                final var status = dir.header().status();
                switch (status) {
                    case STATUS_SUCCESS -> {
                        final var buf = Unpooled.wrappedBuffer(dir.encoded());
                        result.addAll(FsccCodecUtils.decodeFileInformation(buf, fic));
                        if (maxRead > 0 && result.size() > maxRead) {
                            completeFuture.set(List.copyOf(result));
                        } else {
                            // repeat request for remaining data
                            sendRequest(queryDirRequest());
                        }
                    }
                    case STATUS_NO_MORE_FILES -> completeFuture.set(List.copyOf(result));
                    default -> throw new SmbException("QueryDirectory failed with status " + status);
                }
                return;

            }
            throw new SmbException("Unexpected QueryDirectory response" + response);

        } catch (SmbException e) {
            completeFuture.setException(e);
        }
    }
}
