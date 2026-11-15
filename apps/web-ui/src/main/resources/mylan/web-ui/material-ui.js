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

import { byId, nextId, report, reportError } from "./utils.js";

export class MaterialUi {
    selfContext = null;
    curUser = null;
    curModal = null;
    devices = null;
    browseRefAction = null;

    constructor(selfContext) {
        this.selfContext = selfContext;
    }

    init(app) {
        intiDropdDowns(document);
        M.Modal.init(document.querySelectorAll(".modal"), {
            onOpenStart: M.updateTextFields, onOpenEnd: modalFocusFirst,
            onCloseEnd: () => { this.curModal = null }
        });
        byId("password-input").onkeydown = (evt) => onEnterClickButton(evt, "login-button");
        byId("unlock-key-input").onkeydown = (evt) => onEnterClickButton(evt, "unlock-button");
    }

    loginModal() {
        if (!this.curModal) {
            this.clearCreds();
        }
        return !this.openModal('login-modal');
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
        this.curUser = user;
        setVisible("resources-menu-item", !user.isGuest);
        setVisible("users-menu-item", user.isAdmin);
        setVisible("registered-menu-item", !user.isGuest);
        setVisible("login-menu-item", user.isGuest);
        byId("username").textContent = user.displayName;

        this.closeModal();
        this.clearCreds();
    }

    updatePasswordModal() {
        if (!this.curModal) {
            ['old-password-input', 'new-password-input', 'repeat-new-password-input']
                .forEach(id => byId(id).value = "");
        }
        return !this.openModal('change-password-modal');
    }

    newPasswordFromModal() {
        const oldPass = byId('old-password-input').value;
        const newPass = byId('new-password-input').value;
        const repPass = byId('repeat-new-password-input').value;
        if (!oldPass || !newPass) {
            return null;
        }
        if (newPass != repPass) {
            reportError("Repeat password missmatch.")
            return null;
        }
        return { userId: this.curUser.id, oldPassword: oldPass, newPassword: newPass };
    }

    updateUserProfileModal() {
        if (!this.curModal) {
            byId("edit-displayname-input").value = this.curUser.displayName;
        }
        return !this.openModal('edit-profile-modal');
    }

    userProfileFromModal() {
        return { userId: this.curUser.id, displayName: byId('edit-displayname-input').value };
    }

    drawResourcesTabs(navId, onTabShow) {
        setContent(fromTemplate("resources-tabs-template"));
        const tabs = M.Tabs.init(byId("resources-tabs"), { onShow: (elm) => onTabShow(elm.id) });
        tabs.select(navId ? navId : "resources-devices");
    }

    drawResourceDevicesList(devices, app) {
        byId("resources-devices").replaceChildren(
            tableContentFromTemplate("resources-devices-template", devices, deviceListRow));
    }

    setDeviceSelectors(devices) {
        this.devices = devices;
        const deviceSelector = byId("account-device-input");
        deviceSelector.innerHTML = "";
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
        setContent(
            tableContentFromTemplate("resources-accounts-template", accounts, (acc) => accountListRow(acc, app)),
            "resources-accounts"
        );
    }

    updateAccountListItem(account, app) {
        replaceById(accountListRow(account, app));
    }

    deleteAccountListItem(accountId) {
        byId("tr-account-" + accountId).remove();
    }

    accountModal(account) {
        byId("account-id-input").value = account ? account.accountId : "";
        byId("account-device-input").value = account ? account.deviceId : "";
        byId("account-username-input").value = account ? account.username : "";
        byId("account-password-input").value = "";
        byId("account-key-input").value = "";
        byId("account-validate-input").checked = true;
        byId("account-modal-header").innerHTML = account ? "Edit Account" : "New Account";
        byId("account-modal-action-btn").text = account ? "Update Account" : "Create Account";
        this.openModal("account-modal");
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

    unlockAccountModal(account) {
        if (!this.curModal) {
            byId("unlock-account-id-input").value = account.accountId;
            byId("unlock-key-input").value = "";
        }
        return !this.openModal("unlock-account-modal");
    }

    unlockAccountFromModal() {
        return {
            accountId: byId("unlock-account-id-input").value,
            key: byId("unlock-key-input").value
        };
    }

    drawResourceShareList(shares, app) {
        this.browseRefAction = () => app.listResources("resources-shares");
        const devicesTable = buildTableFromTemplate("resources-shares-template", shares,
            (share) => "rsrc-share-tr-" + share.shareId,
            (share) => [
                share.shareId, extName(share.account),
                share.name, share.path, share.type,
                [
                    button("Browse", () => app.browseShare(share)),
                    button("Edit", () => app.editShare(share)),
                    button("Delete", () => app.deleteShare(share))]
            ]);
        byId("resources-shares").replaceChildren(devicesTable);
    }

    drawUserList(users, app) {
        setContent(tableContentFromTemplate('user-list-template', users, (item) => userListRow(item, app)));
    }

    updateUserListItem(user, app) {
        replaceById(userListRow(user, app));
    }

    deleteUserListItem(userId) {
        byId("tr-user-" + userId).remove();
    }

    newUserModal() {
        if (!this.curModal) {
            byId('new-username-input').value = "";
            byId('new-displayname-input').value = "";
            byId('new-isadmin-input').checked = false;
        }
        return !this.openModal('new-user-modal');
    }

    newUserFromModal() {
        return {
            username: byId('new-username-input').value,
            displayName: byId('new-displayname-input').value,
            admin: byId('new-isadmin-input').checked
        };
    }

    drawDashboard(navId, onTabShow) {
        this.browseRefAction = () => app.dashboard(navId);
        if (!this.curUser || this.curUser.isGuest) {
            setContent(fromTemplate("guest-dashboard-template"));
            onTabShow("all-shares");
        } else {
            setContent(fromTemplate("dashboard-template"));
            const tabs = M.Tabs.init(byId("dashboard-tabs"), { onShow: (elm) => onTabShow(elm.id) });
            tabs.select(navId ? navId : "all-shares");
        }
    }

    drawAllShares(shares, app) {
        setContent(
            tableContentFromTemplate("all-shares-template", shares, (share) => shareListRow(share, app, true)),
            "all-shares"
        );
    }

    drawDirByAccount(dir, app) {
        const accountId = dir.account.accountId;
        const browseFunc = (path) => app.browseAccount(accountId, path)
        const execFunc = (path, action) => app.executeByAccount(action, path, accountId);
        const bc = breadcrumbs(
            () => app.listResources("resources-accounts"),
            a(extName(dir.account), () => app.browseAccount(accountId), "account"),
            dir.path, browseFunc);
        this.drawDirDetails(dir, app, bc, browseFunc, execFunc, true);
    }

    drawDirByShare(dir, app) {
        const shareId = dir.topResource.shareId;
        const browseFunc = (path) => app.browseShare(shareId, path);
        const execFunc = (path, action) => app.executeByShare(action, path, shareId);
        const bc = breadcrumbs(
            this.browseRefAction ? this.browseRefAction : () => app.dashboard(),
            a(extName(dir.topResource), () => app.browseShare(shareId), "folder_special"),
            dir.path, browseFunc);
        this.drawDirDetails(dir, app, bc, browseFunc, execFunc, true);
    }

    drawDirDetails(dir, app, dirBc, browseFunc, execFunc, canSetShare) {
        setContent(fromTemplate("nav-dir-template"));
        setContent(dirBc, "nav-dir-breadcrumbs");
        const tables = [];
        if (dir.subDirs && dir.subDirs.length > 0) {
            tables.push(table(dir.subDirs, d => navSubdirRow(d, app, browseFunc, canSetShare), ["highlight"]));
        }
        if (dir.files && dir.files.length > 0) {
            tables.push(table(dir.files, f => navFileRow(f, app, execFunc, canSetShare), ["highlight"]));
        }
        if (tables.length < 1) {
            tables.push(elm("i", "Directory is empty"));
        }
        setContent(tables, "nav-dir-tables");
    }

    openModal(id) {
        // return false if already open, otherwise open and return true
        if (this.curModal == id) {
            return false;
        }
        this.closeModal(); // ensure single modal is active
        M.Modal.getInstance(byId(id)).open();
        this.curModal = id;
        return true;
    }

    closeModal() {
        if (this.curModal) {
            M.Modal.getInstance(byId(this.curModal)).close();
            this.curModal = null;
        }
    }

    notifyPasswordChangeRequired() {
        M.toast({ html: "Password change is required" });
        this.openModal("change-password-modal")
    }
}

function intiDropdDowns(source) {
    M.Dropdown.init(source.querySelectorAll(".dropdown-trigger"), {
        coverTrigger: false, constrainWidth: false, alignment: "right"
    });
}

function userListRow(user, app) {
    const row = tr("tr-user-" + user.userId);
    row.append(
        td(user.userId),
        td(user.username, user.disabled ? "person_off" : "person"),
        td(user.displayName),
        td(user.admin ? 'admin' : 'user'),
        user.disabled ? td("disabled", "block") : td("active", "check_circle_outline"),
        actionTd("act-user-" + user.userId, user.disabled
            ? [
                a('Enable', () => app.setUserEnabled(user, true), "check_circle_outline"),
                a('Delete', () => app.deleteUser(user), "delete_outline")
            ]
            : [
                a('Reset Password', () => app.resetUserPassword(user), "lock_reset"),
                a('Disable', () => app.setUserEnabled(user, false), "block")
            ])
    );
    if (user.disabled) {
        row.classList.add("inactive");
    }
    return row;
}

function deviceListRow(device) {
    const row = tr("tr-device-" + device.deviceId);
    row.append(
        td(device.deviceId),
        td(device.identifier),
        td(device.ipAddresses.map(ipa => ipa.ipAddress).join(", ")),
        td(device.protocol == "LOCAL" ? "" : device.protocol)
    );
    if (device.state == 'ONLINE') {
        row.append(td("online", "check_circle_outline"));
    } else if (device.protocol == "LOCAL") {
        row.append(td(""));
    } else {
        row.append(td("offline", "block"));
        row.classList.add("inactive");
    }
    return row;
}

function accountListRow(acc, app) {
    const row = tr("tr-account-" + acc.accountId);
    row.append(td(acc.accountId), td(acc.username + " @ " + acc.deviceIdentifier));
    const actions = [];
    if (acc.lockState != "LOCKED" && acc.state == "VALID") {
        actions.push(a("Browse", () => app.browseAccount(acc.accountId), "outbound"));
    }
    if (acc.lockState == "LOCKED") {
        row.append(td("Locked", "lock"));
        actions.push(a("Unlock", () => app.unlockAccount(acc), "lock_open"));
    } else if (acc.lockState == "UNLOCKED") {
        row.append(td("Unlocked", "lock_open"));
        actions.push(a("Lock", () => app.lockAccount(acc), "lock"));
    } else {
        row.append(td("", "no_encryption"))
    }
    if (acc.state == "VALID") {
        row.append(td("Valid", "check_circle_outline"));
    } else if (acc.state == "INVALID") {
        row.append(td("Invalid", "block"));
    } else {
        row.append(td("Unknown", "help_outline"));
    }
    actions.push(
        a("Edit", () => app.editAccount(acc), "edit"),
        a("Delete", () => app.deleteAccount(acc), "delete")
    );
    row.append(actionTd("act-acc-" + acc.accountId, actions));
    return row;
}

function shareListRow(share, app) {
    const row = tr("tr-share-" + share.shareId);
    const actions = [];
    const name = share.name + " @ " + share.deviceIdentifier;
    if (share.file) {
        // TODO file icon and actions depending on type
        row.append(td(name, "file_present"));
        actions.push(a("Download", () => app.downloadByShare(share.shareId, ""), "download"))
    } else {
        row.append(td(a(name, () => app.browseShare(share.shareId, ""), "folder_special")));
        actions.push(a("Browse", () => app.browseShare(share.shareId, ""), "outbound"));
    }
    if (share.path) {
        actions.push(a("Edit", () => app.updateShare(share), "edit"));
        actions.push(a("Delete", () => app.deleteShare(share), "delete"));
    }
    row.append(actionTd("act-share-" + share.shareId, actions));
    return row;
}

function navSubdirRow(dir, app, browseFunc, canSetShare) {
    const row = tr("nav-dir-" + nextId());
    row.append(td(a(dir.name, () => browseFunc(dir.path), "folder")));
    return row;
}

function navFileRow(file, app, execFunc, canSetShare) {
    const row = tr("nav-file-" + nextId());
    row.append(
        td(file.name),
        td(timestamp(file.modified)),
        td(fileSize(file.size))
    );
    return row;
}

function table(items, rowBuilder, classes) {
    const table = document.createElement("table");
    const tbody = document.createElement("tbody");
    items.forEach(item => tbody.append(rowBuilder(item)));
    table.append(tbody);
    addClasses(table, classes);
    return table;
}

function fromTemplate(templateId) {
    return byId(templateId).content.cloneNode(true);
}

function tableContentFromTemplate(templateId, items, rowBuilder) {
    const fragment = byId(templateId).content.cloneNode(true);
    const tbody = fragment.querySelector("tbody");
    items.forEach(item => tbody.append(rowBuilder(item)));
    return fragment;
}

function actionTd(id, links, buttons) {
    const trigger = a(null, null, "more_vert", null, ["dropdown-trigger"]);
    trigger.setAttribute("data-target", id);
    const dropdown = document.createElement('ul');
    dropdown.classList.add("dropdown-content");
    dropdown.id = id;
    links.forEach(a => {
        const li = document.createElement('li');
        li.append(a);
        dropdown.append(li);
    });
    const td = document.createElement('td');
    td.classList.add("actions");
    if (buttons) {
        buttons.forEach(b => td.append(b));
    }
    td.append(trigger, dropdown);
    return td;
}

function tr(id) {
    const tr = document.createElement('tr');
    tr.id = id;
    return tr;
}

function td(text, iconId, classes) {
    return elm('td', text, iconId, classes);
}

function a(text, onclick, iconId, href, classes) {
    const a = elm('a', text, iconId);
    if (onclick) {
        a.onclick = onclick;
    }
    a.href = href ? href : "#";
    addClasses(a, classes);
    return a;
}

function elm(tag, inner, iconId, classes) {
    const elm = document.createElement(tag);
    if (iconId) {
        elm.append(icon(iconId));
    }
    if (inner) {
        elm.append(inner);
    }
    addClasses(elm, classes);
    return elm;
}

function addClasses(elm, classes) {
    if (classes) {
        if (Array.isArray(classes)) {
            classes.forEach(c => elm.classList.add(c));
        } else {
            elm.classList.add(classes);
        }
    }
}

function icon(iconId, classes) {
    const icon = document.createElement("i");
    icon.append(iconId);
    icon.classList.add("material-icons");
    addClasses(icon, classes);
    return icon;
}

function buildTableFromTemplate(templateId, items, toId, toCells) {
    const fragment = byId(templateId).content.cloneNode(true);
    const tbody = fragment.querySelector("tbody");
    items.forEach(item => tbody.append(buildTableRow(toId(item), toCells(item))));
    return fragment;
}

function setContent(newContent, containerId) {
    const container = byId(containerId ? containerId : 'content');
    if (Array.isArray(newContent)) {
        container.innerHTML = "";
        newContent.forEach(nc => container.append(nc));
    } else {
        container.replaceChildren(newContent);
    }
    intiDropdDowns(container);
}

function replaceById(elm) {
    const cur = byId(elm.id);
    if (cur) {
        cur.replaceWith(elm);
        intiDropdDowns(byId(elm.id));
    }
}

function setVisible(id, visible) {
    if (visible) {
        byId(id).classList.remove("hide");
    } else {
        byId(id).classList.add("hide");
    }
}

function buildTableRow(id, items) {
    const row = document.createElement('tr');
    row.id = id;
    items.forEach(item => row.append(Array.isArray(item) ? elementNode('td', item) : elementText('td', item)));
    return row;
}

function elementNode(tag, children) {
    const elm = document.createElement(tag);
    children.forEach((item => elm.append(item)));
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

function extName(item) {
    return (item.username ? item.username : item.name) + " @ " + item.deviceIdentifier;
}

function breadcrumbs(refAction, topElm, path, pathFunc) {
    const bcs = [];
    if (refAction) {
        bcs.push(a("", refAction, "list"));
    }
    bcs.push(topElm)
    if (path) {
        var curPath = "";
        for (const pathItem of path.split("/")) {
            if (pathItem) {
                curPath += "/" + pathItem;
                const linkPath = curPath;
                bcs.push(a(pathItem, () => pathFunc(linkPath), "navigate_next"));
            }
        }
    }
    return bcs;
}

function fileSize(value) {
    if (value < 1024) {
        return value + "b";
    }
    if (value < 1048576) {
        return (value / 1024).toFixed(2) + "K";
    }
    if (value < 1073741824) {
        return (value / 1048576).toFixed(2) + "M";
    }
    return (value / 1073741824).toFixed(2) + "G";
}

function timestamp(value) {
    return new Date(value).toISOString();
}

export function reportDiscoveryStatus(status) {
    const message = "<p>Discovery completed.<br /><b>{0} Devices</b> discovered.<br/>Start time: {1}<br/>End time: {2}</p>"
        .replace("{0}", status.devicesDiscovered)
        .replace("{1}", new Date(status.startTime).toLocaleString())
        .replace("{2}", new Date(status.endTime).toLocaleString());
    M.toast({ html: message, classes: 'teal darken-1', displayLength: 10000 });
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