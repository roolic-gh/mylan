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
package local.mylan.service.api.model;

public class NavResource {

    private Integer accountId;
    private String deviceIdentifier;
    private Long shareId;
    private Long bookmarkId;
    private String name;
    private String path;
    private boolean file;

    public Integer getAccountId() {
        return accountId;
    }

    public void setAccountId(final Integer accountId) {
        this.accountId = accountId;
    }

    public String getDeviceIdentifier() {
        return deviceIdentifier;
    }

    public void setDeviceIdentifier(final String deviceIdentifier) {
        this.deviceIdentifier = deviceIdentifier;
    }

    public Long getShareId() {
        return shareId;
    }

    public void setShareId(final Long shareId) {
        this.shareId = shareId;
    }

    public Long getBookmarkId() {
        return bookmarkId;
    }

    public void setBookmarkId(final Long bookmarkId) {
        this.bookmarkId = bookmarkId;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public boolean isFile() {
        return file;
    }

    public void setFile(final boolean file) {
        this.file = file;
    }

    public String getPath() {
        return path;
    }

    public void setPath(final String path) {
        this.path = path;
    }
}
