# User Management 

< Back to [index](index.md)

### Overview

While number of LAN users is not expected to be large the user subsystem design is 
*simplified*. The major purpose of user account is make possible to store the data and 
settings (preferences) on server side on per-user basis. 

*Unauthenticated* user (client) is allowed and granted access to resources shared for
everybody. *Authenticated* user (client) is eligible to manage **own** data (settings),
access and share access to own resources. User accounts are managed by *administrator* 
which is the only privelege of administrator role. Administrators have no access to other 
user data (incl. credentials) and resources unless these are explicitly shared by owner.

### Account management

On start the user subsystem will check for active administrator account. If none is 
found the *default* admin account with credentials **admin:admin** (see password reset
state below) will be created. Default account expected to be used on initial setup.

Following rules are applied (behavior expected) for user accounts

- Only administrator can create, enable, disable and delete user accounts.
- User won't be authenticated if account is disabled 
- Only disabled account can be deleted.
- Username is defined on account creation and cannot be changed during account lifetime. 

### Password management

- Initially (on account creation) and after password reset the password is same as username.
- Password can be changed by owner only.
- Paccword can be reset by administrator only.




