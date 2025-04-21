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
package local.mylan.service.data.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "user_main")
public class UserEntity {
    @Id
    @Column(name = "user_id")
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Integer id;

    @Column(name = "username", unique = true, length = 32)
    private String username;

    @Column(name = "display_name", length = 64)
    private String displayName;

    @Column(name = "admin")
    private boolean isAdmin;

    @Column(name = "deleted")
    private boolean deleted;

    public UserEntity() {
        // default
    }

    public UserEntity(final String username, final boolean isAdmin) {
        this.username = username;
        displayName = username;
        this.isAdmin = isAdmin;
    }



}
