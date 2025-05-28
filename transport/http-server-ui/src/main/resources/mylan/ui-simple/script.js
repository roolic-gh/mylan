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
        this.client.getCurrentUser((response) => {
            this.user.reset(response);
            this.ui.applyUser(this.user);
        });
    }

    login() {
        const creds = {username: byId('username-input').value, password: byId('password-input').value };
        const persist = byId('rememberme-input').checked;
        console.log("login())", creds, persist);
        this.client.authenticate(creds, (response) => {
            this.client.setToken(response.authToken, persist);
            this.user.reset(response.user);
            this.ui.applyUser(this.user);
            if (response.mustChangePassword) {
                this.ui.notifyPasswordChangeRequired();
            }
            this.ui.closeModal('login-modal');
        });
    }

    logout() {
        console.log("logout()");
        // TODO Notify server to cancel session
        this.user.reset({userId: null});
        this.ui.applyUser(this.user);
    }

}




class MaterialUi{
    selfContext = null;

    constructor(selfContext){
        this.selfContext = selfContext;
    }

    init() {
        M.Dropdown.init(document.querySelectorAll(`.dropdown-trigger`), {});
        M.Modal.init(document.querySelectorAll(`.modal`), {});
    }

    applyUser(user) {
        setVisible('volumes-menu-item', !user.isGuest);
        setVisible('bookmarks-menu-item', !user.isGuest);
        setVisible('users-menu-item', user.isAdmin);
        setVisible('registered-menu-item', !user.isGuest);
        setVisible('login-menu-item', user.isGuest);
        byId('username').innerHTML = user.displayName;
    }

    closeModal(id) {
        M.Modal.getInstance(byId(id)).close();
    }

    notifyPasswordChangeRequired(){
        M.toast({ html: "Password change is required" });
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
        //        callback({userId: null, username: "guest", displayName: "guest", admin: false});
        this.request("GET", "/user", callback);
    }

    authenticate(creds, callback) {
        //        callback({user: {userId: 10, username: "test", displayName: "Test", admin: false},
        //            authToken: "12345", mustChangePassword: true});
        this.request("POST", "/authenticate", callback, creds);

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
                if (response.status == 401) {
                    reportError("Session expired. Please re-login");
                    this.clearToken();
                    return {};
                } else {
                    return response.json()
                }
            }
        ).then(
            (json) => {
                console.log("fetch response: ", json)
                if (json.error == null) {
                    callback(json);
                } else {
                    reportError(json.error);
                }
            });
    }
}

class User {
    username = null;
    displayName = null;
    isAdmin = false;
    isGuest = true;

    reset(serverUser) {
        //        console.log("user reset: ", serverUser);
        if (serverUser.userId != null) {
            this.username = serverUser.username;
            this.displayName = serverUser.displayName;
            this.isAdmin = serverUser.admin;
            this.isGuest = false;
        } else {
            this.username = "anon";
            this.displayName = "Guest";
            this.isAdmin = false;
            this.isGuest = true;
        }
    }
}

function byId(id) {
    return document.getElementById(id);
}

function setVisible(id, visible) {
    byId(id).style.display = visible ? '' : 'none';
}

function buildElement(tag, classes, text, onclick){
    var elm = document.createElement(tag);
    elm.setAttribute("class", classes);
    elm.innerHtml = text;
    elm.onclick = onclick;
    return elm;
}


function reportError(message) {
    console.log(message);
    M.toast({ html: message, classes: 'red darken-1' });
}

