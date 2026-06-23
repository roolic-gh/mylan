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

import java.util.List;

public class NavDirectory extends NavResource {

    private NavResource topResource;
    private DeviceAccount account;

    private List<NavDirectory> subDirs;
    private List<NavFile> files;

    public NavDirectory() {
        // default
    }

    public NavDirectory(final String name) {
        setName(name);
    }

    public NavDirectory(final List<NavDirectory> subDirs, final List<NavFile> files) {
        this.subDirs = subDirs;
        this.files = files;
    }

    public DeviceAccount getAccount() {
        return account;
    }

    public void setAccount(final DeviceAccount account) {
        this.account = account;
    }

    public NavResource getTopResource() {
        return topResource;
    }

    public void setTopResource(final NavResource topResource) {
        this.topResource = topResource;
    }

    public List<NavDirectory> getSubDirs() {
        return subDirs;
    }

    public void setSubDirs(final List<NavDirectory> subDirs) {
        this.subDirs = subDirs;
    }

    public List<NavFile> getFiles() {
        return files;
    }

    public void setFiles(final List<NavFile> files) {
        this.files = files;
    }
}
