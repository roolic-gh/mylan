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
import { SseHandler}  from "./sse-handler.js";
import { MaterialUi } from "./material-ui.js";
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
                this.sse.start(this.client.token);
            }
            this.refresh();
        });
    }

    login() {
        const creds = this.ui.credsFromModal();
        this.client.authenticate(creds, (response) => {
            this.client.setToken(response.authToken, creds.persist);
            this.user.reset(response.user);
            this.sse.start(response.authToken);
            this.ui.applyUser(this.user);
            this.ui.closeModal('login-modal');
            this.ui.clearCreds();
            if (response.mustChangePassword) {
                this.ui.notifyPasswordChangeRequired();
            }
        });
    }

    logout() {
        // TODO Notify server to cancel session
        this.client.clearToken();
        this.user.reset({ userId: null });
        this.ui.applyUser(this.user);
        this.sse.stop();
        this.refresh();
    }

    updatePassword() {
        const oldPass = byId('old-password-input').value;
        const newPass = byId('new-password-input').value;
        const repPass = byId('repeat-new-password-input').value;
        if (newPass != repPass) {
            reportError("Repeat password missmatch.")
            return;
        }
        this.client.updateUserPassword(
            { userId: this.user.id, oldPassword: oldPass, newPassword: newPass },
            () => {
                report("Password updated. Please re-login.");
                this.ui.closeModal('change-password-modal');
            });
    }

    updateProfile() {
        this.client.updateUserDetails(
            { userId: this.user.id, displayName: byId('edit-displayname-input').value },
            (updatedUser) => {
                this.user.reset(updatedUser);
                this.ui.applyUser(this.user);
                this.ui.closeModal('edit-profile-modal');
            });
    }

    refresh() {
        if (!this.user || this.user.isGuest) {
            this.dashboard();
            return;
        }
        const navId = getNav();
        if ("users" === navId) {
            this.listUsers();
        } else if ("resources" === navId) {
            this.listResources();
        } else if ("bookmarks" === navId) {
            this.listBookmarks();
        } else if ("recent" === navId) {
            this.listRecent();
        } else {
            this.dashboard();
        }
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

    listResources() {
        setNav("resources");
        this.ui.drawResourcesTabs(this);
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
        this.client.startDiscovery((status) => report("Discovery started."));
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

    listResourcesShares() {
        this.client.getShareList((shares) => this.ui.drawResourceShareList(shares, this));
    }

    listUsers() {
        setNav("users");
        this.client.getUserList((users) => this.ui.drawUserList(users, this));
    }

    addUser() {
        this.client.createUser(
            {
                username: byId('new-username-input').value,
                displayName: byId('new-displayname-input').value,
                admin: byId('new-isadmin-input').checked
            },
            (response) => {
                this.ui.closeModal('new-user-modal');
                this.listUsers();
            });
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
                this.listUsers();
            });
    }

    deleteUser(user) {
        // TODO delete confirm dialog
        this.client.deleteUser(user.userId, () => {
            report("User @" + user.username + " deleted.");
            this.listUsers();
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
    return null;
}
