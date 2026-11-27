# TeamFlow Hub

Team management web app: projects, tasks with a review workflow, team chat (live over WebSocket),
notifications and a performance dashboard.

- **Backend:** Java 17, Spring Boot 3.3, Spring Security + JWT, Spring Data JPA, MySQL 8, WebSocket (STOMP)
- **Frontend:** Angular 18

## What you need installed

| Tool | Version |
|------|---------|
| JDK | 17 or newer |
| Node.js | 18.19+ or 20+ (22 works) |
| MySQL | 8.x (MySQL Workbench is fine to inspect it), or Docker |

## Run it locally

### 1. Database
Have MySQL running on `localhost:3306`. The app creates the `teamflow_hub` database and all tables by itself.
By default it connects as `root` with password `root`. If your MySQL password is different, set it (step 2).

No MySQL installed? Run `docker compose up -d` in this folder (it starts MySQL with the settings above).

### 2. Backend (port 8080)
```bash
cd backend
./mvnw spring-boot:run          # Windows: mvnw.cmd spring-boot:run
```
Wrong MySQL password? Either edit `backend/src/main/resources/application.yml`, or set environment variables:
`DB_USER`, `DB_PASSWORD`, `DB_HOST`, `DB_PORT`, `DB_NAME`.
In IntelliJ: open the `backend` folder as a Maven project and run `HubApplication`
(add `DB_PASSWORD=yourpassword` under Run > Edit Configurations > Environment variables).

### 3. Frontend (port 4200)
```bash
cd frontend
npm install
npm start
```
Open http://localhost:4200. (VS Code: open the `frontend` folder and run the commands in its terminal.)
If the backend is not on port 8080, change `API_URL` in `frontend/src/app/core/config.ts`.

### 4. Log in
A manager account is created on first start:

- email: `admin@teamflow.local`
- password: `Admin@123`

Change it in Profile. You can also sign up new users (as Manager or Team member).

## Roles and rules
- **Manager (ADMIN):** creates projects (and is that project's manager), adds members or whole teams, creates and assigns tasks,
  uploads guidelines, posts announcements, reviews tasks.
- **Member:** sees only projects they belong to, works on assigned tasks, chats, uploads deliverables.

Task lifecycle: `Pending -> In progress -> Reviewing` (assignee) then the manager either `Accepts` it (rating 1-5 required)
or rejects it back to `In progress` with feedback (required). `Accepted -> Done` is closed by the manager.
Project progress = (Accepted + Done tasks) / all tasks.

Chat channels: direct messages, project group chat (everyone in the project), announcements (only the project manager can post).

## Forgot password
No mail server is configured. The reset token is shown on the page (development only) and printed in the backend console.
Plug in an email sender in `AuthService.forgotPassword` for production.

## Tests
`cd backend && ./mvnw test` runs an end-to-end test of the API (login, roles, task lifecycle, chat, files, password reset)
on an in-memory database.

## Project layout
```
backend/    Spring Boot API (model, repo, service, web, security, config)
frontend/   Angular app (pages, core services)
database/   init.sql (optional manual DB creation)
docs/       Analysis review: what was fixed in the ERD and business logic
```
