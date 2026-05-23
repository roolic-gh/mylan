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
package local.mylan.transport.smb.protocol.fscc;

import java.lang.reflect.InvocationTargetException;

/**
 * Addresses MS-FSCC (#2.4 File Information Classes)
 */
public enum FileInformationClass {
    FileAccessInformation(8, Use.QUERY),
    FileAlignmentInformation(17, Use.QUERY),
    FileAllInformation(18, Use.QUERY),
    FileAllocationInformation(19, Use.SET),
    FileAlternateNameInformation(21, Use.QUERY),
    FileAttributeTagInformation(35, Use.QUERY),
    FileBasicInformation(4, Use.QUERY | Use.SET),
    FileBothDirectoryInformation(3, Use.QUERY),
    FileCompressionInformation(28, Use.QUERY),
    FileDirectoryInformation(1, Use.QUERY, FileDirectoryInformation.class),
    FileDispositionInformation(13, Use.SET),
    FileDispositionInformationEx(64, Use.SET),
    FileEaInformation(7, Use.QUERY),
    FileEndOfFileInformation(20, Use.SET),
    FileFullDirectoryInformation(2, Use.QUERY),
    FileFullEaInformation(15, Use.QUERY | Use.SET),
    FileHardLinkInformation(46, Use.LOCAL),
    FileId64ExtdBothDirectoryInformation(79, Use.QUERY),
    FileId64ExtdDirectoryInformation(78, Use.QUERY),
    FileIdAllExtdBothDirectoryInformation(81, Use.QUERY),
    FileIdAllExtdDirectoryInformation(80, Use.QUERY),
    FileIdBothDirectoryInformation(37, Use.QUERY),
    FileIdExtdDirectoryInformation(60, Use.QUERY),
    FileIdFullDirectoryInformation(38, Use.QUERY),
    FileIdGlobalTxDirectoryInformation(50, Use.LOCAL),
    FileIdInformation(59, Use.QUERY),
    FileInternalInformation(6, Use.QUERY),
    FileLinkInformation(11, Use.SET),
    FileMailslotQueryInformation(26, Use.LOCAL),
    FileMailslotSetInformation(27, Use.LOCAL),
    FileModeInformation(16, Use.QUERY | Use.SET),
    FileMoveClusterInformation(31, Use.NONE),
    FileNameInformation(9, Use.LOCAL),
    FileNamesInformation(12, Use.QUERY),
    FileNetworkOpenInformation(34, Use.QUERY),
    FileNormalizedNameInformation(48, Use.QUERY),
    FileObjectIdInformation(29, Use.LOCAL),
    FilePipeInformation(23, Use.QUERY | Use.SET),
    FilePipeLocalInformation(24, Use.QUERY),
    FilePipeRemoteInformation(25, Use.QUERY),
    FilePositionInformation(14, Use.QUERY | Use.SET),
    FileQuotaInformation(32, Use.QUERY | Use.SET),
    FileRenameInformation(10, Use.SET),
    FileRenameInformationEx(65, Use.SET),
    FileReparsePointInformation(33, Use.LOCAL),
    FileSfioReserveInformation(44, Use.LOCAL),
    FileSfioVolumeInformation(45, Use.NONE),
    FileShortNameInformation(40, Use.SET),
    FileStandardInformation(5, Use.QUERY),
    FileStandardLinkInformation(54, Use.LOCAL),
    FileStreamInformation(22, Use.QUERY),
    FileTrackingInformation(36, Use.LOCAL),
    FileValidDataLengthInformation(39, Use.SET);

    final int value;
    final int useFlags;
    final Class<? extends FileInformation> implClass;

    FileInformationClass(final int value, final int useFlags) {
        this.value = value;
        this.useFlags = useFlags;
        implClass = null;
    }

    FileInformationClass(final int value, final int useFlags, final Class<? extends FileInformation> implClass) {
        this.value = value;
        this.useFlags = useFlags;
        this.implClass = implClass;
    }

    public boolean isForQuery() {
        return (useFlags & Use.QUERY) != 0;
    }

    public boolean isForSet() {
        return (useFlags & Use.SET) != 0;
    }

    public boolean isForLocal() {
        return (useFlags & Use.LOCAL) != 0;
    }

    public FileInformation newInstance() {

        if (implClass != null) {
            try {
                return implClass.getDeclaredConstructor().newInstance();
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException
                     | NoSuchMethodException e) {
                throw new RuntimeException(e);
            }
        }
        throw new IllegalStateException("Instantiation is not supported for " + name());
    }

    public int value() {
        return value;
    }

    public static FileInformationClass fromValue(final int value) {
        for (var fic : values()) {
            if (fic.value == value) {
                return fic;
            }
        }
        return null;
    }

    private interface Use {
        int NONE = 0;
        int QUERY = 0x00000001;
        int SET = 0x00000002;
        int LOCAL = 0x00000004;
    }
}
