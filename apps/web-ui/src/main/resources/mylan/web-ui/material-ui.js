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

import { byId, reportError } from "./utils.js";

export class MaterialUi {
    selfContext = null;
    curUser = null;
    curModal = null;

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
        setVisible("bookmarks-menu-item", !user.isGuest);
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

    drawResourcesTabs(app, navId) {
        setContent(byId("resources-tabs-template").content.cloneNode(true));
        const tabs = M.Tabs.init(byId("resources-tabs"), { onShow: (div) => app.setNav(div.id) });
        if (navId) {
            tabs.select(navId);
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
                acc.accountId,
                acc.username + " @ " + acc.deviceIdentifier,
                acc.lockState == "HAS_NO_LOCK" ? "--" : acc.lockState,
                acc.state,
                [
                    acc.lockState != "LOCKED" ? button("Browse", () => app.browseAccount(acc.accountId, "")) : "",
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

    drawDir(dir, app) {
        if (dir.account) {
            const accountId = dir.account.accountId;
            const top = accountLink(dir.account, () => app.browseAccount(accountId, ""));
            this.drawDirDetails(dir, app, true, top, (path) => app.browseAccount(accountId, path));
        }
    }

    drawDirDetails(dir, app, canShare, topBreadcrumb, browseFunc) {
        const content = byId("nav-dir-template").content.cloneNode(true);
        content.querySelector(".breadcrumbs-container")
            .replaceChildren(breadcrumbs(topBreadcrumb, dir.path, browseFunc));

        const tbody = content.querySelector("tbody");
        if (dir.subDirs) {
            dir.subDirs.forEach(sd => {
                const folderLink = linkElement(sd.name, () => browseFunc(sd.path), "folder");
                const shareInfo = sd.shareId ? icon("share") : "";
                const bookmarkInfo = sd.bookmarkId ? icon("bookmark") : "";

                const row = elementNode("tr", [elementNode("td", [folderLink])])
                tbody.append(row);
            });
        }
        if (dir.files) {
            dir.files.forEach(file => {
                const row = elementNode("tr", [elementText("td", file.name)])
                tbody.append(row);
            });
        }
        setContent(content);
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
        coverTrigger: false, constraintWidth: false, alignment: "left", onOpenEnd: (elm) => console.log(elm)
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

function tableContentFromTemplate(templateId, items, rowBuilder) {
    const fragment = byId(templateId).content.cloneNode(true);
    const tbody = fragment.querySelector("tbody");
    items.forEach((item) => tbody.append(rowBuilder(item)));
    return fragment;
}

function actionTd(id, links) {
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

function elm(tag, text, iconId, classes) {
    const elm = document.createElement(tag);
    if (iconId) {
        elm.append(icon(iconId));
    }
    if (text) {
        elm.append(text);
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
    items.forEach((item) => tbody.append(buildTableRow(toId(item), toCells(item))));
    return fragment;
}

function setContent(newContent) {
    const container = byId('content');
    container.replaceChildren(newContent);
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

function accountLink(acc, onclick) {
    return linkElement(acc.username + " @ " + acc.deviceIdentifier, onclick, "account");
}

function linkElement(text, onclick, cls) {
    const link = elementText("a", text);
    link.classList.add(cls);
    link.onclick = onclick;
    return link;
}



function breadcrumbs(topElm, path, pathFunc) {
    const bcs = [topElm];
    if (path) {
        var curPath = "";
        for (const pathItem of path.split("/")) {
            if (pathItem) {
                curPath += "/" + pathItem;
                const linkPath = curPath;
                bcs.push(linkElement(pathItem, () => pathFunc(linkPath), "folder"));
            }
        }
    }
    return bcs;
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