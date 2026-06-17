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

import {byId, report, reportError} from "./utils.js";

export class MaterialUi {
    selfContext = null;

    constructor(selfContext) {
        this.selfContext = selfContext;
    }

    init(app) {
        M.Dropdown.init(document.querySelectorAll(".dropdown-trigger"), {});
        M.Modal.init(document.querySelectorAll(".modal"), {
            onOpenStart: M.updateTextFields, onOpenEnd: modalFocusFirst
        });
        byId("password-input").onkeydown = (evt) => onEnterClickButton(evt, "login-button");
        byId("unlock-key-input").onkeydown = (evt) => onEnterClickButton(evt, "unlock-button");
    }

    credsFromModal() {
        return {
            username: byId('username-input').value,
            password: byId('password-input').value,
            persist: byId('rememberme-input').checked
        };
    }

    clearCreds() {
        byId('username-input').value = "";
        byId('password-input').value = "";
        byId('rememberme-input').checked = false;
    }

    applyUser(user) {
        setVisible("resources-menu-item", !user.isGuest);
        setVisible("bookmarks-menu-item", !user.isGuest);
        setVisible("users-menu-item", user.isAdmin);
        setVisible("registered-menu-item", !user.isGuest);
        setVisible("login-menu-item", user.isGuest);
        byId("username").textContent = user.displayName;
        byId("edit-displayname-input").value = user.displayName;
    }

    drawResourcesTabs() {
        setContent(byId("resources-tabs-template").content.cloneNode(true));
        const tabs = M.Tabs.init(byId("resources-tabs"), { onShow: (div) => localStorage.setItem("rsrc-tab", div.id) });
        const prevTabId = localStorage.getItem("rsrc-tab");
        if (prevTabId != null) {
            tabs.select(prevTabId);
        }
    }

    drawResourceDevicesList(devices, app) {
        const devicesTable = buildTableFromTemplate("resources-devices-template", devices,
            (device) => "rsrc-device-tr-" + device.deviceId,
            (device) => [device.deviceId, device.identifier,
            device.ipAddresses.map(ipa => ipa.ipAddress).join(", "),
            device.protocol, device.state]);
        byId("resources-devices").replaceChildren(devicesTable);
    }

    setDeviceSelectors(devices) {
        const deviceSelector = byId("account-device-input");
        devices.filter(device => device.protocol != "LOCAL")
            .forEach(device => {
                const option = document.createElement("option");
                option.value = device.deviceId;
                option.text = device.identifier + " (" + device.protocol + ")";
                deviceSelector.add(option);
            });
        M.FormSelect.init(deviceSelector, {});
    }

    drawResourceAccountList(accounts, app) {
        const devicesTable = buildTableFromTemplate("resources-accounts-template", accounts,
            (acc) => "rsrc-acc-tr-" + acc.accountId,
            (acc) => [
                acc.accountId, acc.username + " @ " + acc.deviceIdentifier,
                acc.lockState == "HAS_NO_LOCK" ? "--" : acc.lockState,
                acc.state,
                [
                    acc.lockState != "LOCKED" ? button("Browse", () => app.browseAcc(acc)) : "",
                    acc.lockState == "UNLOCKED" ? button("Lock", () => app.lockAccount(acc)) : "",
                    acc.lockState == "LOCKED" ? button("Unlock", () => app.ui.openUnlockModal(acc)) : "",
                    button("Edit", () => app.editAccount(acc)),
                    button("Delete", () => app.deleteAccount(acc))
                ]
            ]);
        byId("resources-accounts").replaceChildren(devicesTable);
    }

    accountFromModal() {
        return {
            accountId: byId("account-id-input").value,
            deviceId: byId("account-device-input").value,
            username: byId("account-username-input").value,
            password: byId("account-password-input").value,
            key: byId("account-key-input").value,
            validate: byId("account-validate-input").checked
        };
    }

    setAccountToModal(account) {
        byId("account-id-input").value = account.accountId;
        byId("account-device-input").value = account.deviceId;
        byId("account-username-input").value = account.username;
        byId("account-password-input").value = "";
        byId("account-key-input").value = "";
        byId("account-validate-input").checked = true;
    }

    openNewAccountModal() {
        this.setAccountToModal({ accountId: "", username: "", deviceId: "" });
        byId("account-modal-header").innerHTML = "New Account";
        byId("account-modal-action-btn").text = "Create Account";
        this.openModal("account-modal");
    }

    openEditAccountModal(account) {
        this.setAccountToModal(account);
        byId("account-modal-header").innerHTML = "Edit Account";
        byId("account-modal-action-btn").text = "Update Account";
        this.openModal("account-modal");
    }

    openUnlockModal(account) {
        byId("unlock-account-id-input").value = account.accountId;
        byId("unlock-key-input").value = "";
        this.openModal("unlock-account-modal");
    }

    unlockRequestFromModal() {
        return {
            accountId: byId("unlock-account-id-input").value,
            key: byId("unlock-key-input").value
        };
    }

    drawResourceShareList(shares, app) {
        const devicesTable = buildTableFromTemplate("resources-shares-template", shares,
            (share) => "rsrc-share-tr-" + share.shareId,
            (share) => [
                share.shareId, share.account.username + " @ " + share.account.deviceIdentifier,
                share.name, share.path, share.type,
                [
                    button("Browse", () => app.browseShare(share)),
                    button("Edit", () => app.editShare(share)),
                    button("Delete", () => app.deleteShare(share))]
            ]);
        byId("resources-shares").replaceChildren(devicesTable);
    }

    drawUserList(users, app) {
        const usersTable = buildTableFromTemplate('user-list-template', users,
            (user) => "user-list-tr-" + user.userId,
            (user) => [
                user.userId, user.username, user.displayName,
                user.admin ? 'admin' : 'user',
                user.disabled ? 'disabled' : 'active',
                user.disabled
                    ? [
                        button('Enable', () => app.setUserEnabled(user, true)),
                        button('Delete', () => app.deleteUser(user))
                    ]
                    : [
                        button('Reset Password', () => app.resetUserPassword(user)),
                        button('Disable', () => app.setUserEnabled(user, false))
                    ]
            ]);
        setContent(usersTable);
    }

    openModal(id) {
        M.Modal.getInstance(byId(id)).open();
    }

    closeModal(id) {
        M.Modal.getInstance(byId(id)).close();
    }

    notifyPasswordChangeRequired() {
        M.toast({ html: "Password change is required" });
        this.openModal('change-password-modal')
    }
}

function buildTableFromTemplate(templateId, items, toId, toCells) {
    const fragment = byId(templateId).content.cloneNode(true);
    const tbody = fragment.querySelector('tbody');
    items.forEach((item) => tbody.append(buildTableRow(toId(item), toCells(item))))
    return fragment;
}

function setContent(newContent) {
    byId('content').replaceChildren(newContent);
}

function setVisible(id, visible) {
    byId(id).style.display = visible ? '' : 'none';
}

function buildTable(headers, rows) {
    return elementNode('table', [elementNode('thead', [buildTableRow(headers)]), elementNode('tbody', rows)]);
}

function buildTableRow(id, items) {
    var row = document.createElement('tr');
    row.id = id;
    items.forEach((item) => row.append(Array.isArray(item) ? elementNode('td', item) : elementText('td', item)));
    return row;
}

function elementNode(tag, children) {
    const elm = document.createElement(tag);
    children.forEach(((item) => elm.append(item)));
    return elm;
}

function elementText(tag, text) {
    const elm = document.createElement(tag);
    elm.textContent = text;
    return elm;
}

function button(label, onclick, cls) {
    const btn = elementText('a', label);
    btn.setAttribute('class', 'btn-small btn-flat');
    btn.onclick = onclick;
    return btn;
}


function modalFocusFirst(elm) {
    var input = elm.querySelector("input");
    if (input != null) {
        input.focus();
    }
}

function onEnterClickButton(evt, buttonId) {
    if (evt.key == "Enter") {
        byId(buttonId).click();
    }
}