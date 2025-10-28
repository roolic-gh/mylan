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
package local.transport.netty.smb.protocol;

/**
 * SMB2 Command codes. Addresses MS-SMB2 (#2.2.1 SMB2 Packet Header)
 */
public enum Smb2Command {
    SMB2_NEGOTIATE(0x0000),
    SMB2_SESSION_SETUP(0x0001),
    SMB2_LOGOFF(0x0002),
    SMB2_TREE_CONNECT(0x0003),
    SMB2_TREE_DISCONNECT(0x0004),
    SMB2_CREATE(0x0005),
    SMB2_CLOSE(0x0006),
    SMB2_FLUSH(0x0007),
    SMB2_READ(0x0008),
    SMB2_WRITE(0x0009),
    SMB2_LOCK(0x000A),
    SMB2_IOCTL(0x000B),
    SMB2_CANCEL(0x000C),
    SMB2_ECHO(0x000D),
    SMB2_QUERY_DIRECTORY(0x000E),
    SMB2_CHANGE_NOTIFY(0x000F),
    SMB2_QUERY_INFO(0x0010),
    SMB2_SET_INFO(0x0011),
    SMB2_OPLOCK_BREAK(0x0012),
    SMB2_SERVER_TO_CLIENT_NOTIFICATION(0x0013);

    private final int code;

    Smb2Command(final int code) {
        this.code = code;
    }

    public int code() {
        return code;
    }

    public static Smb2Command fromCode(final int code) {
        for (var cmd : values()) {
            if (cmd.code == code) {
                return cmd;
            }
        }
        throw new IllegalArgumentException("unknown command code " + code);
    }

}
