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
package local.mylan.service.net.accessors;

import java.util.List;
import local.mylan.service.api.model.NavDirectory;
import local.mylan.service.api.model.NavFile;
import local.mylan.transport.smb.protocol.fscc.FileAttributeFlags;
import local.mylan.transport.smb.protocol.fscc.FileDirectoryInformation;
import local.mylan.transport.smb.protocol.fscc.FileInformation;

final class SmbUtils {
    static final SharePath EMPTY_PATH = new SharePath("", "");

    private SmbUtils() {
        // utility class
    }

    static NavDirectory navDirFromShareNames(final List<String> shareNames) {
        final var dirs = shareNames.stream().map(NavDirectory::new).toList();
        return new NavDirectory(dirs, null);
    }

    static NavDirectory navDirFromFileInfo(final List<FileInformation> fileInfos) {
        final var filtered = fileInfos.stream().map(FileDirectoryInformation.class::cast)
            .filter(fi -> !fi.fileAttributes().get(FileAttributeFlags.FILE_ATTRIBUTE_HIDDEN)
                && !fi.fileAttributes().get(FileAttributeFlags.FILE_ATTRIBUTE_SYSTEM)
                && !fi.fileName().startsWith(".")).toList();

        final var dirs = filtered.stream().map(FileDirectoryInformation.class::cast)
            .filter(fi -> fi.fileAttributes().get(FileAttributeFlags.FILE_ATTRIBUTE_DIRECTORY))
            .map(fi -> new NavDirectory(fi.fileName())).toList();
        final var files = filtered.stream().map(FileDirectoryInformation.class::cast)
            .filter(fi -> !fi.fileAttributes().get(FileAttributeFlags.FILE_ATTRIBUTE_DIRECTORY))
            .map(fi -> new NavFile(fi.fileName(), fi.endOfFile())).toList();
        return new NavDirectory(dirs, files);
    }

    static String nameFromPath(final String path) {
        if (path == null || path.isEmpty() || path.endsWith("/")) {
            return null;
        }
        final var cutIdx = path.lastIndexOf('/');
        return cutIdx > 0 ? path.substring(cutIdx + 1) : null;
    }

    static SharePath sharePath(final String path) {
        if (path == null || path.isEmpty() || "/".equals(path)) {
            return EMPTY_PATH;
        }
        final var cutIdx0 = path.startsWith("/") ? 1 : 0;
        final var cutIdx1 = path.indexOf('/', cutIdx0 + 1);
        if (cutIdx1 > 0) {
            final var share = path.substring(cutIdx0, cutIdx1);
            final var subPath = path.substring(cutIdx1 + 1).replace("/", "\\");
            return new SharePath(share, subPath.isEmpty() ? "." : subPath);
        }
        final var share = path.substring(cutIdx0);
        return new SharePath(share, ".");
    }

    record SharePath(String shareName, String subPath) {
    }
}
