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

export class Client {
    restContext = null;
    token = null;

    constructor(restContext) {
        this.restContext = restContext;
        var tokenFromSession = sessionStorage.getItem("token");
        this.token = tokenFromSession == null ? localStorage.getItem("token") : tokenFromSession;
    }

    setToken(newToken, persist) {
        console.log("token: ", newToken, persist);
        this.token = newToken;
        sessionStorage.setItem("token", newToken);
        if (persist) {
            localStorage.setItem("token", newToken);
        }
    }

    clearToken() {
        this.token = null;
        sessionStorage.removeItem("token");
        localStorage.removeItem("token");
    }

    getCurrentUser(callback) {
        this.request("GET", "/user", callback);
    }

    authenticate(creds, callback) {
        this.request("POST", "/authenticate", callback, creds);
    }

    getDeviceList(callback) {
        this.request("GET", "/nav/devices", callback);
    }

    startDiscovery(callback) {
        this.request("POST", "/discovery/start", callback);
    }

    getAccountList(callback) {
        this.request("GET", "/nav/accounts", callback);
    }

    createOrUpdateAccount(account, callback) {
        const path = "/nav/res/account" + (account.accountId ? "/" + account.accountId : "");
        const method = account.accountId ? "PATCH" : "POST";
        if (account.validate) {
            this.validateAccount(account, (response) => {
                this.request(method, path, callback, account);
            });
        } else {
            this.request("POST", path, callback, account);
        }
    }

    validateAccount(account, callback) {
        this.request("POST", "/nav/account/validate", callback, account);
    }

    unlockAccount(req, callback) {
        this.request("POST", "/nav/account/" + req.accountId + "/unlock", callback, req);
    }

    lockAccount(accountId, callback) {
        this.request("POST", "/nav/account/" + accountId + "/lock", callback);
    }

    deleteAccount(accountId, callback) {
        this.request("DELETE", "/nav/res/account/" + accountId, callback);
    }

    getShareList(callback) {
        callback([{
            shareId: 1,
            account: { accountId: 1, username: "username", deviceId: 1, deviceIdentifier: "ROOLIC-TEST" },
            name: "My Files", path: "path/to/files", type: "ALL"
        }]);
    }


    updateUserPassword(creds, callback) {
        this.request("POST", "/user/change-password", callback, creds);
    }

    resetUserPassword(userId, callback) {
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
        this.request("GET", "/user/list", callback);
    }

    async request(method, path, callback, body) {
        console.log("fetch request: " + method + " " + path);
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
        showProgress(true);

        fetch(
            new Request(this.restContext + path, {
                method: method,
                headers: headers,
                body: bodyJson
            })
        ).then(
            (response) => {
                showProgress(false);
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

function showProgress(display) {
    const elm = byId("progress-bar");
    if (display) {
        elm.classList.remove("hide");
    } else {
        elm.classList.add("hide");
    }
}