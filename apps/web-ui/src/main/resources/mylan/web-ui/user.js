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

export class User {
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