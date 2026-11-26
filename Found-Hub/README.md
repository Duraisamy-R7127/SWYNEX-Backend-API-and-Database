<<<<<<< HEAD
# Lost & Found Hub – Backend (Spring Boot REST API)

Find what you lost. Return what you found.

Lost & Found Hub lets users report **LOST** / **FOUND** items with photos, search and filter them, send **ownership claim requests**, and lets an **admin** verify claims and mark items as returned. This repository is the **backend and REST API only** (no frontend).

## Technology stack

| Part | Technology |
|---|---|
| Language / build | Java 17+, Maven |
| Framework | Spring Boot 3.5.x (Spring Web, Spring Data JPA, Spring Security, Validation) |
| Database | MySQL 8 (H2 in-memory is used only by the automated tests) |
| Security | JWT (jjwt 0.12.6), BCrypt, role based (`USER`, `ADMIN`) |
| Upload | Multipart file upload, files saved in `uploads/` |
| Tests | JUnit 5, Mockito, MockMvc (full-stack integration tests) |

No Lombok is used, so there is nothing to configure in IntelliJ (no annotation-processing setup).

## Project structure

```
Found-Hub
├── pom.xml
├── api-tests.http              IntelliJ HTTP Client requests
├── http-client.env.json        baseUrl + admin login for api-tests.http
├── sample-images/wallet.png    image used by api-tests.http
├── uploads/                    created at runtime (uploaded images)
└── src
    ├── main/java/com/lostfoundhub
    │   ├── LostAndFoundHubApplication.java
    │   ├── config       SecurityConfig, CorsConfig, FileStorageConfig, FileStorageProperties, DataInitializer
    │   ├── controller   Auth, User, Item, Claim, Admin, Notification controllers
    │   ├── dto          request/response objects (entities are never returned directly)
    │   ├── enums        UserRole, ItemType, ItemStatus, ClaimStatus
    │   ├── exception    GlobalExceptionHandler + custom exceptions
    │   ├── model        User, Item, Claim, Notification (JPA entities)
    │   ├── repository   Spring Data repositories (+ ItemSpecifications for search)
    │   ├── security     JwtService, JwtAuthenticationFilter, CustomUserDetails(+Service), TokenBlacklistService, 401/403 handlers
    │   ├── service      business logic (Auth, User, Item, Claim, Admin, Notification)
    │   └── util         FileStorageUtil, PageUtil
    ├── main/resources/application.properties
    └── test
        ├── java/com/lostfoundhub  service / util / security unit tests + integration test
        └── resources/application-test.properties
```

## Database setup

1. Install MySQL 8 and start it.
2. Nothing else is required: the schema `lostfoundhub` is created automatically (`createDatabaseIfNotExist=true`) and the 4 tables (`users`, `items`, `claims`, `notifications`) are created/updated by Hibernate (`ddl-auto=update`), including primary keys, foreign keys, indexes and the unique constraint on `users.email`.

Relationships: `users 1—* items`, `users 1—* claims`, `items 1—* claims`, `users 1—* notifications`.

## Configuration

Everything is in `src/main/resources/application.properties` and can be overridden with **environment variables** (no real secrets are stored in the code):

| Variable | Default (placeholder) | Meaning |
|---|---|---|
| `DB_HOST`, `DB_PORT`, `DB_NAME` | `localhost`, `3306`, `lostfoundhub` | MySQL location |
| `DB_USERNAME` | `root` | MySQL user |
| `DB_PASSWORD` | `your_mysql_password` | **Set this to your MySQL password** |
| `JWT_SECRET` | placeholder | **Set a random secret of at least 32 characters** |
| `JWT_EXPIRATION_MS` | `86400000` (24 h) | Token lifetime |
| `UPLOAD_DIR` | `uploads` | Image folder (outside `src`) |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:3000,http://localhost:5173,http://localhost:8080` | Allowed frontend origins |
| `ADMIN_NAME`, `ADMIN_EMAIL`, `ADMIN_PASSWORD` | `System Admin`, `admin@lostfoundhub.com`, `Admin@12345` | First admin account, created at startup if the email does not exist. **Change the password.** |

**In IntelliJ:** open *Run → Edit Configurations → LostAndFoundHubApplication → Environment variables* and add e.g. `DB_PASSWORD=mypass;JWT_SECRET=some-long-random-secret-at-least-32-chars`.

## How to run

```bash
# from the project folder
mvn clean test          # unit + integration tests (uses in-memory H2, no MySQL needed)
mvn clean package       # builds target/Found-Hub-0.0.1-SNAPSHOT.jar
mvn spring-boot:run     # or: run LostAndFoundHubApplication in IntelliJ
```

The API starts on **http://localhost:8080**. Then open `api-tests.http` in IntelliJ, pick the `dev` environment and run the requests top to bottom.

## Business rules enforced

* Login is required to post items, claim, view own data; browsing/searching items and downloading images is public.
* Passwords are stored only as BCrypt hashes; emails are unique (case-insensitive).
* A user cannot claim their own item, cannot send two PENDING claims for the same item, and cannot claim an item that is CLAIMED or RETURNED.
* Only ADMIN can approve/reject claims. Rejecting requires remarks.
* Approving a claim (one transaction, with row locks): claim → APPROVED, item → CLAIMED, every other PENDING claim for that item → REJECTED (and those users are notified).
* Admin can mark an item RETURNED (pending claims are closed). A RETURNED item can never change status again.
* Images: JPG/JPEG/PNG only, max 5 MB, content type **and** file signature are checked; only a generated file name is stored in MySQL, never the image bytes.
* Normal users can edit/delete only their own OPEN items; admin can modify any item.
* Logout revokes the JWT (kept in memory – restarting the server clears the revoked list).

## API documentation

**Base URL:** `http://localhost:8080`

**Every response has this shape** (`data` is `null` for messages without data):

```json
{ "success": true, "message": "Item fetched", "data": { }, "timestamp": "2026-01-15T10:30:00" }
```

**Errors** use the same shape with `success:false`; validation errors put field messages in `data`:

```json
{ "success": false, "message": "Validation failed", "data": { "email": "Email must be valid" }, "timestamp": "..." }
```

| Status | Meaning |
|---|---|
| 200 / 201 | OK / Created |
| 400 | Invalid input, invalid image, business rule violated, bad query parameter |
| 401 | Missing / invalid / expired / logged-out JWT, wrong email or password |
| 403 | Logged in but not allowed (not admin, not owner) |
| 404 | Item / claim / user / notification / endpoint not found |
| 409 | Duplicate email or duplicate pending claim, database constraint violation |
| 413 | Uploaded file larger than 5 MB |
| 500 | Unexpected/database error (generic message, details only in server log) |

**Auth header** for protected endpoints: `Authorization: Bearer <token>`.
**Roles:** 🌐 public · 🔒 any logged-in user · 👑 ADMIN only.

### 1. Authentication

#### POST `/api/auth/register` 🌐
Headers: `Content-Type: application/json`

```json
{ "name": "Arun Kumar", "email": "arun@example.com", "password": "Passw0rd123", "phone": "9876543210" }
```
Password: 8–64 chars with at least one letter and one digit. `phone` is optional.

Response **201**:
```json
{ "success": true, "message": "User registered successfully",
  "data": { "id": 2, "name": "Arun Kumar", "email": "arun@example.com", "phone": "9876543210", "role": "USER", "createdAt": "2026-01-15T10:30:00" } }
```
Errors: 400 validation, 409 email already registered.

#### POST `/api/auth/login` 🌐
Headers: `Content-Type: application/json`
```json
{ "email": "arun@example.com", "password": "Passw0rd123" }
```
Response **200**:
```json
{ "success": true, "message": "Login successful",
  "data": { "token": "eyJhbGciOi...", "tokenType": "Bearer", "expiresInMs": 86400000,
            "user": { "id": 2, "name": "Arun Kumar", "email": "arun@example.com", "phone": "9876543210", "role": "USER", "createdAt": "..." } } }
```
Errors: 400 validation, 401 wrong email/password.

#### POST `/api/auth/logout` 🔒
Headers: `Authorization: Bearer <token>` → **200** `{ "success": true, "message": "Logged out successfully" }`. The token is rejected (401) afterwards.

### 2. Users

| Method & URL | Auth | Description | Success |
|---|---|---|---|
| GET `/api/users/me` | 🔒 | Own profile | 200 `UserResponse` |
| PUT `/api/users/me` | 🔒 | Update own name/phone | 200 `UserResponse` |
| GET `/api/users/me/items?page=0&size=10` | 🔒 | Items I posted (paged, newest first) | 200 `PageResponse<ItemResponse>` |
| GET `/api/users/me/claims` | 🔒 | Claims I submitted | 200 `ClaimResponse[]` |
| GET `/api/users/{id}` | 🔒 | User details – own id only (admin: any) | 200 / 403 / 404 |

PUT `/api/users/me` body (`Content-Type: application/json`):
```json
{ "name": "Arun Kumar S", "phone": "9876543210" }
```

### 3. Items

`ItemResponse`:
```json
{ "id": 12, "itemName": "Black Leather Wallet", "category": "Wallet", "description": "Black wallet with a student ID card",
  "imagePath": "3f2c...a1.png", "imageUrl": "/api/items/images/3f2c...a1.png", "location": "Main Library",
  "eventDate": "2026-01-15", "itemType": "FOUND", "status": "OPEN", "postedById": 2, "postedByName": "Arun Kumar",
  "createdAt": "2026-01-15T10:35:00" }
```

#### POST `/api/items` 🔒 → **201**
Headers: `Authorization: Bearer <token>`, `Content-Type: multipart/form-data`

Form fields: `itemName`*, `category`*, `description`*, `location`*, `eventDate`* (`yyyy-MM-dd`, not in the future), `itemType`* (`LOST`|`FOUND`), and optional file field `image` (JPG/JPEG/PNG, ≤ 5 MB). Errors: 400 (validation / bad image), 401, 413.

#### GET `/api/items` 🌐 → **200** `PageResponse<ItemResponse>`
Query parameters (all optional): `keyword` (name contains), `category` (exact, case-insensitive), `location` (contains), `type` (`LOST`|`FOUND`), `status` (`OPEN`|`CLAIMED`|`RETURNED`), `page` (0), `size` (10, max 50), `sortBy` (`createdAt`|`itemName`|`eventDate`|`category`|`location`|`status`|`itemType`), `direction` (`asc`|`desc`).

```json
{ "success": true, "message": "Items fetched",
  "data": { "content": [ { "...ItemResponse..." } ], "page": 0, "size": 10, "totalElements": 1, "totalPages": 1, "last": true } }
```

#### GET `/api/items/{id}` 🌐 → 200 `ItemResponse` / 404

#### PUT `/api/items/{id}` 🔒 (owner or admin) → 200
Same multipart form as create. If no `image` is sent the old image is kept; send `removeImage=true` to delete it. Errors: 400 (also when a non-admin edits a CLAIMED/RETURNED item), 403, 404.

#### DELETE `/api/items/{id}` 🔒 (owner or admin) → 200
Also deletes the item's claims and image file. Non-admin users can delete only OPEN items.

#### GET `/api/items/images/{filename}` 🌐 → 200 image bytes (`image/png` or `image/jpeg`) / 404
Use the `imageUrl` returned by the item APIs.

### 4. Claims

`ClaimResponse`:
```json
{ "id": 5, "itemId": 12, "itemName": "Black Leather Wallet", "itemStatus": "OPEN", "claimantId": 3, "claimantName": "Priya Devi",
  "proofDescription": "The ID card inside has my name", "status": "PENDING", "adminRemarks": null,
  "createdAt": "2026-01-16T09:00:00", "reviewedAt": null }
```

#### POST `/api/claims` 🔒 → **201**
Headers: `Authorization`, `Content-Type: application/json`
```json
{ "itemId": 12, "proofDescription": "The ID card inside has my name Priya Devi and roll number 21CS045" }
```
`proofDescription`: 10–2000 characters. Errors: 400 (own item / item CLAIMED or RETURNED / validation), 404 item, 409 duplicate pending claim.

| Method & URL | Auth | Description | Success |
|---|---|---|---|
| GET `/api/claims/my` | 🔒 | My claims | 200 `ClaimResponse[]` |
| GET `/api/claims/{id}` | 🔒 | Claim details (claimant, item owner or admin only) | 200 / 403 / 404 |
| GET `/api/claims/{id}/status` | 🔒 | `{claimId, status, itemStatus, adminRemarks, reviewedAt}` | 200 |

### 5. Admin (all 👑, otherwise 401/403)

| Method & URL | Description | Success |
|---|---|---|
| GET `/api/admin/dashboard` | Statistics | 200 |
| GET `/api/admin/users?page&size` | All users | 200 `PageResponse<UserResponse>` |
| GET `/api/admin/items?...` | All items (same filters as `/api/items`) | 200 |
| GET `/api/admin/claims?status=&page&size` | All claims, optional status filter | 200 |
| GET `/api/admin/claims/pending?page&size` | Pending claims | 200 |
| PATCH `/api/admin/claims/{id}/verify` | Approve / reject a claim | 200 |
| PATCH `/api/admin/items/{id}/returned` | Mark item RETURNED | 200 |
| PATCH `/api/admin/items/{id}/status` | Set item status | 200 |

Dashboard `data`:
```json
{ "totalUsers": 5, "totalItems": 12, "lostItems": 5, "foundItems": 7, "openItems": 8, "claimedItems": 2, "returnedItems": 2,
  "totalClaims": 6, "pendingClaims": 2, "approvedClaims": 2, "rejectedClaims": 2 }
```

PATCH `/api/admin/claims/{id}/verify` (`Content-Type: application/json`):
```json
{ "status": "APPROVED", "adminRemarks": "ID card details verified" }
```
or `{ "status": "REJECTED", "adminRemarks": "Roll number does not match" }` (remarks required for REJECTED). Errors: 400 (claim already decided, item not available, missing remarks), 404.

PATCH `/api/admin/items/{id}/status`: `{ "status": "OPEN" }` (`OPEN`|`CLAIMED`|`RETURNED`). `RETURNED` behaves like `/returned`; a RETURNED item cannot be changed again (400).

### 6. Notifications 🔒

Notifications are created when a claim is submitted (claimant, item owner, all admins), approved (claimant, item owner), rejected (claimant, including automatic rejection of competing claims), and when an item is returned.

| Method & URL | Description | Success |
|---|---|---|
| GET `/api/notifications?page=0&size=20` | My notifications, newest first | 200 `PageResponse<NotificationResponse>` |
| GET `/api/notifications/unread-count` | `{ "unreadCount": 3 }` | 200 |
| PATCH `/api/notifications/{id}/read` | Mark one as read (own only, else 404) | 200 |
| PATCH `/api/notifications/read-all` | `{ "updated": 3 }` | 200 |

`NotificationResponse`: `{ "id": 9, "message": "Your claim for 'Wallet' was APPROVED.", "read": false, "createdAt": "..." }`

## Tests

* `AuthServiceTest`, `ClaimServiceTest`, `AdminServiceTest` – Mockito unit tests of the business rules.
* `FileStorageUtilTest` – image validation (type, content, size, path traversal).
* `JwtServiceTest` – token creation, tampering, expiry.
* `LostFoundHubIntegrationTest` – real Spring context + H2 + MockMvc: registration/login/logout, admin-only access, item upload/search/filter/pagination, claim → approve → returned workflow, notifications.

Run them with `mvn test`.

## Notes for production

Use a long random `JWT_SECRET`, change the default admin password, set `spring.jpa.hibernate.ddl-auto=validate` (with migrations), and keep the revoked-token list in Redis/DB if you run more than one server instance.
=======
1. Problem Statement:
In colleges, schools, offices, and public places, people frequently lose personal belongings such as mobile phones, ID cards, wallets, bags, books, and other valuable items. Finding these lost items and contacting their owners is difficult because there is no centralized platform to report or search for them.
Traditional methods of informing others through notices or word of mouth are time-consuming and often ineffective. There is also no proper verification process to ensure that lost items are returned to their rightful owners.
Therefore, a centralized web-based Lost & Found Hub is needed to simplify reporting, searching, claiming, and verifying lost and found items.


2. Proposed Solution:
The Lost & Found Hub is a full-stack web application designed to connect people who have lost their belongings with those who have found them.

     The system provides the following features:
   
1. User Registration and Login: Users can securely create accounts and log in to the application.
2. Upload Lost and Found Items: Users can upload item images along with names, descriptions, categories, dates, and locations.
3. Search and Filter: Users can easily search for items and filter them based on categories, locations, and item types.
4. Claim Request: Users can submit claim requests with ownership details to recover their lost belongings.
5. Admin Verification: The administrator can review claim requests, verify ownership information, and approve or reject claims.
6. Item Return Management: Once a claim is approved, the item can be marked as returned.
This application provides an efficient, reliable, and user-friendly platform for managing lost and found belongings.


3. Tools and Technologies Used :
Category	Technology
IDE	IntelliJ IDEA Community Edition
Frontend	HTML
Styling	CSS
Frontend Scripting	JavaScript
Backend	Java
Framework	Spring Boot
Database	MySQL
ORM	Spring Data JPA / Hibernate
Security	Spring Security Crypto (BCrypt password hashing)
Build Tool	Apache Maven
Server	Embedded Apache Tomcat
Image Upload	Multipart File Upload
Browser	Google Chrome

4. What We Are Going to Do:
The project will be developed entirely using IntelliJ IDEA.
- Develop an attractive and responsive user interface.
- Create a secure user registration and login system.
- Implement lost and found item image uploads.
- Connect the application with the MySQL database.
- Develop search and filtering functionality.
- Implement the claim request system.
- Create an admin dashboard for verification.
- Test all functionalities and ensure proper data management.
Expected Outcome: A complete full-stack web application that makes it easy to report, search, identify, claim, verify, and recover lost belongings through a single centralized platform.
>>>>>>> 99788963d987d91522f8ea5c03ce089bc9f87e0e
