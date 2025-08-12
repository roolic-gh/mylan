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

public class NavResourceShare extends NavResource {

    private ShareType shareType;

    public NavResourceShare() {
        super();
    }

    public NavResourceShare(final Long shareId, final String name) {
        super(shareId, name, null);
    }

    public NavResourceShare(final Integer accountId, final String name, final String path, final ShareType shareType) {
        super(accountId, name, path);
        this.shareType = shareType;
    }

    public ShareType getShareType() {
        return shareType;
    }

    public void setShareType(final ShareType shareType) {
        this.shareType = shareType;
    }

}
