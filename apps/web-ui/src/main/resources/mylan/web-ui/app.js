/*
  Copyright 2026 Ruslan Kashapov

  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

      https://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
*/

import { Client } from "./client.js";
import { SseHandler } from "./sse-handler.js";
import { MaterialUi, reportDiscoveryStatus } from "./material-ui.js";
import { User } from "./user.js";
import { byId, report, reportError } from "./utils.js";

export class App {
    ui = null;
    client = null;
    user = null;
    sse = null;
    nav = null;

    constructor(selfContext, restContext, sseContext) {
        this.ui = new MaterialUi(selfContext);
        this.client = new Client(restContext);
        this.user = new User();
        this.sse = new SseHandler(sseContext);
    }

    init() {
        this.ui.init(this);
        this.client.getCurrentUser((currentUser) => {
            this.user.reset(currentUser);
            this.ui.applyUser(this.user);
            if (!this.user.isGuest) {
                this.sse.setToken(this.client.token);
            }
            this.refresh();
        });
    }

    login() {
        if (this.ui.loginModal()) {
            const creds = this.ui.credsFromModal();
            this.client.authenticate(creds, (response) => {
                this.client.setToken(response.authToken, creds.persist);
                this.sse.setToken(response.authToken);
                this.user.reset(response.user);
                this.ui.applyUser(this.user);
                if (response.mustChangePassword) {
                    this.ui.notifyPasswordChangeRequired();
                }
            });
        }
    }

    logout() {
        // TODO Notify server to cancel session
        this.client.clearToken();
        this.sse.clearToken();
        this.user.reset({ userId: null });
        this.ui.applyUser(this.user);
        this.refresh();
    }

    updatePassword() {
        if (this.ui.updatePasswordModal()) {
            const payload = this.ui.newPasswordFromModal();
            if (payload) {
                this.client.updateUserPassword(payload, () => {
                    report("Password updated. Please re-login.");
                    this.ui.closeModal();
                });
            }
        }
    }

    updateUserProfile() {
        if (this.ui.updateUserProfileModal()) {
            this.client.updateUserDetails(
                this.ui.userProfileFromModal(),
                (updatedUser) => {
                    this.user.reset(updatedUser);
                    this.ui.applyUser(this.user);
                });
        }
    }

    refresh() {
        if (!this.user || this.user.isGuest) {
            this.dashboard();
            return;
        }
        const navId = getNav();
        if ("users" === navId) {
            this.listUsers();
        } else if (navId.startsWith("resources")) {
            this.listResources(navId);
        } else if ("bookmarks" === navId) {
            this.listBookmarks();
        } else if ("recent" === navId) {
            this.listRecent();

        } else if (navId.startsWith("browse-")) {
            const subs = navId.substring(7).split("/", 3);
            if (subs[0] === "account") {
                this.browseAccount(subs[1], '/' + subs[2]);
            } else {
                this.dashboard()
            }
        } else {
            this.dashboard();
        }
    }

    setNav(navId) {
        setNav(navId);
    }

    dashboard() {
        setNav("home");
    }

    listBookmarks() {
        setNav("bookmarks");
    }

    listRecent() {
        setNav("recent");
    }

    listResources(navId) {
        if (navId) {
            this.ui.drawResourcesTabs(this, navId);
        } else {
            setNav("resources");
            this.ui.drawResourcesTabs(this, null);
        }
        this.listResourcesDevices();
        this.listResourcesAccounts();
        this.listResourcesShares();
    }

    listResourcesDevices() {
        this.client.getDeviceList((devices) => {
            this.ui.setDeviceSelectors(devices);
            this.ui.drawResourceDevicesList(devices, this);
        });
    }

    startDiscovery() {
        // fixme: report actual status
        this.client.startDiscovery((status) => {
            this.sse.waitForEvent("discovery-status", reportDiscoveryStatus)
            report("Discovery started.")
        });
    }

    listResourcesAccounts() {
        this.client.getAccountList((accounts) => this.ui.drawResourceAccountList(accounts, this));
    }

    createAccount() {
        this.ui.openNewAccountModal();
    }

    editAccount(account) {
        this.ui.openEditAccountModal(account);
    }

    processAccount() {
        const account = this.ui.accountFromModal();
        const callback = (response) => {
            this.ui.closeModal("account-modal");
            this.listResourcesAccounts();
        };
        this.client.createOrUpdateAccount(account, callback);
    }

    unlockAccount() {
        this.client.unlockAccount(this.ui.unlockRequestFromModal(), (response) => {
            report("Account unlocked.")
            this.ui.closeModal("unlock-account-modal");
            this.listResourcesAccounts();
        });
    }

    lockAccount(account) {
        this.client.lockAccount(account.accountId, (response) => {
            report("Account locked.")
            this.listResourcesAccounts();
        });
    }

    deleteAccount(account) {
        this.client.deleteAccount(account.accountId, (response) => {
            report("Account deleted.")
            this.listResourcesAccounts();
        });
    }

    browseAccount(accountId, path) {
        this.client.readDirByAccount(accountId, path, (response) => {
            setNav("browse-account/" + accountId + (path ? path : "/"));
            this.ui.drawDir(response, this);
        });
    }

    listResourcesShares() {
        this.client.getShareList((shares) => this.ui.drawResourceShareList(shares, this));
    }

    listUsers() {
        setNav("users");
        this.client.getUserList((users) => this.ui.drawUserList(users, this));
    }

    addUser() {
        if (this.ui.newUserModal()) {
            this.client.createUser(
                this.ui.newUserFromModal(),
                (response) => {
                    this.ui.closeModal();
                    this.listUsers();
                });
        }
    }

    resetUserPassword(user) {
        this.client.resetUserPassword(user.userId,
            (response) => report("Password for user @" + user.username + " was reset."));
    }

    setUserEnabled(user, enabled) {
        this.client.updateUserStatus(
            { userId: user.userId, disabled: !enabled },
            (response) => {
                report("User @" + user.username + " " + (enabled ? "enabled." : "disabled."));
                user.disabled = !enabled;
                this.ui.updateUserListItem(user, this);
            });
    }

    deleteUser(user) {
        // TODO delete confirm dialog
        this.client.deleteUser(user.userId, () => {
            report("User @" + user.username + " deleted.");
            this.ui.deleteUserListItem(user.userId);
        });
    }
}

function setNav(value) {
    window.history.pushState({ nav: value }, null, "?" + value);
}

function getNav() {
    const search = window.location.search;
    if (search != null && search.length > 1) {
        return search.substring(1);
    }
    return "";
}
