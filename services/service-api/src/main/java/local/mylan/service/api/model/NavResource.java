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
    private Long shareId;
    private String resourceName;
    private String path;

    public NavResource() {
        // default
    }

    public NavResource(final NavResource other) {
        accountId = other.accountId;
        shareId = other.shareId;
        resourceName = other.resourceName;
        path = other.path;
    }

    public NavResource(final Integer accountId, final String path) {
        this.accountId = accountId;
        this.path = path;
    }

    public NavResource(final Integer accountId, final String resourceName, final String path) {
        this.accountId = accountId;
        this.resourceName = resourceName;
        this.path = path;
    }

    public NavResource(final Long shareId, final String path) {
        this.shareId = shareId;
        this.path = path;
    }

    public NavResource(final Long shareId, final String resourceName, final String path) {
        this.shareId = shareId;
        this.resourceName = resourceName;
        this.path = path;
    }

    public Integer getAccountId() {
        return accountId;
    }

    public void setAccountId(final Integer accountId) {
        this.accountId = accountId;
    }

    public Long getShareId() {
        return shareId;
    }

    public void setShareId(final Long shareId) {
        this.shareId = shareId;
    }

    public String getResourceName() {
        return resourceName;
    }

    public void setResourceName(final String resourceName) {
        this.resourceName = resourceName;
    }

    public String getPath() {
        return path;
    }

    public void setPath(final String path) {
        this.path = path;
    }
}
