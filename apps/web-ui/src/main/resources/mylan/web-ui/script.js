class App {
    ui = null;
    client = null;
    user = null;

    constructor(selfContext, restContext) {
        this.ui = new MaterialUi(selfContext);
        this.client = new Client(restContext);
        this.user = new User();
    }

    init() {
        this.ui.init();
        this.client.getCurrentUser((currentUser) => {
            this.user.reset(currentUser);
            this.ui.applyUser(this.user);
        });
    }

    login() {
        const creds = {username: byId('username-input').value, password: byId('password-input').value };
        const persist = byId('rememberme-input').checked;
        this.client.authenticate(creds, (response) => {
            this.client.setToken(response.authToken, persist);
            this.user.reset(response.user);
            this.ui.applyUser(this.user);
            this.ui.closeModal('login-modal');
            if (response.mustChangePassword) {
                this.ui.notifyPasswordChangeRequired();
            }
        });
    }

    logout() {
        // TODO Notify server to cancel session
        this.client.clearToken();
        this.user.reset({userId: null});
        this.ui.applyUser(this.user);
        // TODO refresh ui
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
            {userId:this.user.id, oldPassword: oldPass, newPassword: newPass},
            () => {
                report("Password updated. Please re-login.");
                this.ui.closeModal('change-password-modal');
            });
    }

    updateProfile() {
        this.client.updateUserDetails(
            {userId: this.user.id, displayName: byId('edit-displayname-input').value},
            (updatedUser) => {
                this.user.reset(updatedUser);
                this.ui.applyUser(this.user);
                this.ui.closeModal('edit-profile-modal');
            });
    }

    listUsers() {
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
        this.client.deleteUser( user.userId, () => {
                report("User @" + user.username + " deleted.");
                this.listUsers();
            });
    }
}

class MaterialUi{
    selfContext = null;

    constructor(selfContext){
        this.selfContext = selfContext;
    }

    init() {
        M.Dropdown.init(document.querySelectorAll(`.dropdown-trigger`), {});
        M.Modal.init(document.querySelectorAll(`.modal`), {onOpenStart: M.updateTextFields});
    }

    applyUser(user) {
        setVisible('volumes-menu-item', !user.isGuest);
        setVisible('bookmarks-menu-item', !user.isGuest);
        setVisible('users-menu-item', user.isAdmin);
        setVisible('registered-menu-item', !user.isGuest);
        setVisible('login-menu-item', user.isGuest);
        byId('username').textContent = user.displayName;
        byId('edit-displayname-input').value = user.displayName;
    }

    drawUserList(users, app) {
        console.log(users);
        setTableContentFromTemplate('user-lst-template', users, (user) => [
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
    }

    openModal(id) {
        M.Modal.getInstance(byId(id)).close();
    }

    closeModal(id) {
        M.Modal.getInstance(byId(id)).close();
    }

    notifyPasswordChangeRequired(){
        M.toast({ html: "Password change is required" });
        this.openModal('change-password-modal')
    }
}

class Client {
    restContext= null;
    token = null;
    constructor(restContext) {
        this.restContext = restContext;
        this.token = localStorage.getItem('token');
    }

    setToken(newToken, persist) {
        this.token = newToken;
        if (persist) {
            localStorage.setItem('token', newToken);
        }
    }

    clearToken() {
        this.token = null;
        localStorage.removeItem('token');
    }

    getCurrentUser(callback) {
        //        callback({userId: 10, username: "admin", displayName: "Admin", admin: true});
        this.request("GET", "/user", callback);
    }

    authenticate(creds, callback) {
        this.request("POST", "/authenticate", callback, creds);
    }

    updateUserPassword(creds, callback) {
        this.request("POST", "/user/change-password", callback, creds);
    }

    resetUserPassword(userId, callback){
        this.request("POST", "/user/" + userId + "/reset-password", callback);
    }

    createUser(user, callback) {
        this.request("POST", "/user/create", callback, user);
    }

    updateUserDetails(userDeatils, callback) {
        this.request("PATCH", "/user/details", callback, userDeatils);
    }

    updateUserStatus(userStatus, callback) {
        this.request("PATCH", "/user/status", callback, userStatus);
    }

    deleteUser(userId, callback) {
        this.request("DELETE", "/user/" + userId, callback);
    }

    getUserList(callback) {
        //        callback([
        //            {userId: 1, username: "admin", displayName: "Admin", admin: true, disabled: false},
        //            {userId: 2, username: "user1", displayName: "User 1", admin: false, disabled: false},
        //            {userId: 3, username: "user2", displayName: "User 3", admin: false, disabled: true}
        //        ]);
        this.request("GET", "/user/list", callback);
    }

    async request(method, path, callback, body) {
        var headers = new Headers();
        if (this.token != null) {
            headers.append("Authorization", "Bearer " + this.token);
        }
        var bodyJson = null;
        if (body != null) {
            headers.append("Content-Type", "application/json");
            bodyJson = JSON.stringify(body);
        }
        headers.append("Accept", "application/json");

        fetch(
            new Request(this.restContext + path, {
                method: method,
                headers: headers,
                body: bodyJson
            })
        ).then(
            (response) => {
                if (response.status == 401 && this.token != null) {
                    // authorization header is bein rejected
                    reportError("Session expired. Please reload & relogin.");
                    this.clearToken();
                    return {};
                }
                if (response.status == 204) {
                    return {};
                }
                return response.json();
            }
        ).then(
            (json) => {
                console.log("fetch response: ", json)
                if (json.hasOwnProperty('error')) {
                    reportError(json.error);
                } else {
                    callback(json);
                }
            }
        ).catch((error) => reportError(error));
    }
}

class User {
    id = null;
    username = null;
    displayName = null;
    isAdmin = false;
    isGuest = true;

    reset(serverUser) {
        if (serverUser.userId != null) {
            this.id = serverUser.userId;
            this.username = serverUser.username;
            this.displayName = serverUser.displayName;
            this.isAdmin = serverUser.admin;
            this.isGuest = false;
        } else {
            this.id = null;
            this.username = "anonimous";
            this.displayName = "Guest";
            this.isAdmin = false;
            this.isGuest = true;
        }
    }
}

function setTableContentFromTemplate(templateId, items, toCells) {
    const fragment = byId(templateId).content.cloneNode(true);
    const tbody = fragment.querySelector('tbody');
    items.forEach((item) => tbody.append(buildTableRow(toCells(item))));
    byId('content').replaceChildren(fragment);
}

function byId(id) {
    return document.getElementById(id);
}

function setVisible(id, visible) {
    byId(id).style.display = visible ? '' : 'none';
}

function buildTable(headers, rows){
    return elementNode('table', [elementNode('thead', [buildTableRow(headers)]), elementNode('tbody', rows)]);
}

function buildTableRow(items) {
    var row = document.createElement('tr');
    items.forEach((item) => row.append(Array.isArray(item)? elementNode('td', item) : elementText('td', item)));
    return row;
}

function elementNode(tag, children) {
    const elm = document.createElement(tag);
    children.forEach(((item) => elm.append(item)));
    return elm;
}

function elementText(tag, text){
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

function report(message) {
    M.toast({ html: message, classes: 'blue-grey darken-2' });
}

function reportError(message) {
    console.log(message);
    M.toast({ html: "Error: " + message, classes: 'red darken-2' });
}
