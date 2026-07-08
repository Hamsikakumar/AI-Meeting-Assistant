# AI Meeting Assistant

An AI-powered meeting assistant that transcribes recordings, generates summaries, extracts action items, and more.

**Tech Stack:** React (frontend) + Spring Boot (backend) + PostgreSQL (database) + Whisper/LLM (AI processing, upcoming)

---

## Project Status

- [x] **Phase 1 — Authentication** (complete)
- [ ] Phase 2 — File Upload
- [ ] Phase 3 — Transcription Pipeline (Whisper)
- [ ] Phase 4 — LLM Processing (summaries, action items, deadlines)
- [ ] Phase 5 — Core UI (meeting history, search)
- [ ] Phase 6 — Team & Sharing Features
- [ ] Phase 7 — Polish & Deployment

---

## Prerequisites

Make sure these are installed before running the project:

- **Java 17+** (JDK)
- **Node.js** (for React/Vite)
- **Docker Desktop** (for PostgreSQL)
- **VS Code** with Java Extension Pack + Spring Boot Extension Pack

---

## How to Run This Project

### 1. Start PostgreSQL (Docker)

From the project root:

```bash
docker compose up -d
```

To verify it's running:

```bash
docker ps
```

You should see a container using `postgres:16` with port `5432` mapped.

**If you ever see "connection refused" or "database does not exist" errors**, it usually means the container isn't running or the volume needs to be reset:

```bash
docker compose down -v
docker compose up -d
```

### 2. Start the Backend (Spring Boot)

```bash
cd backend
.\mvnw.cmd spring-boot:run
```

Wait for the log line: `Started BackendApplication in X seconds`

The backend runs on **http://localhost:8080**

**Known Windows quirk:** Java may report the timezone as `Asia/Calcutta` (a legacy name) which Postgres doesn't recognize as `Asia/Kolkata`. This is already fixed in `application.properties` via the JDBC URL parameter `?options=-c%20TimeZone=Asia/Kolkata` — you shouldn't need to do anything extra, but if timezone errors reappear, that's the cause.

### 3. Start the Frontend (React + Vite)

Open a **new terminal** (keep the backend running):

```bash
cd frontend
npm run dev
```

The frontend runs on **http://localhost:5173**

Open that URL in your browser — you should land on the Login page.

---

## What Was Built in Phase 1 — Authentication

### Backend (`backend/src/main/java/com/meetingassistent/backend/`)

| File | Purpose |
|---|---|
| `model/User.java` | User entity (id, name, email, password, role, createdAt) |
| `model/Role.java` | Enum: USER / ADMIN |
| `repository/UserRepository.java` | JPA repository for User, includes `findByEmail` |
| `dto/RegisterRequest.java`, `LoginRequest.java`, `AuthResponse.java` | Request/response shapes for auth endpoints |
| `config/PasswordConfig.java` | BCrypt password encoder bean |
| `security/JwtUtil.java` | Generates and validates JWT tokens |
| `security/JwtAuthFilter.java` | Intercepts requests, validates JWT, sets authenticated user in context |
| `config/SecurityConfig.java` | Configures which routes are public vs protected, wires up the JWT filter, CORS settings |
| `service/AuthService.java` | Business logic for register/login |
| `controller/AuthController.java` | Exposes `POST /api/auth/register` and `POST /api/auth/login` |

**How auth works:**
1. Register: name/email/password → password hashed with BCrypt → user saved to DB → JWT issued
2. Login: email/password → password verified against hash → JWT issued
3. Protected routes: any request must include `Authorization: Bearer <token>` header; `JwtAuthFilter` validates it and identifies the user for that request
4. Tokens expire after 24 hours (configurable in `JwtUtil.java`)

### Frontend (`frontend/src/`)

| File | Purpose |
|---|---|
| `api/auth.js` | Functions that call the backend's register/login endpoints |
| `context/AuthContext.jsx` | Global state for "is a user logged in", stores token + user info in `localStorage` |
| `pages/Login.jsx` | Login form |
| `pages/Register.jsx` | Registration form |
| `pages/Dashboard.jsx` | Placeholder protected page shown after login |
| `components/ProtectedRoute.jsx` | Redirects to `/login` if no authenticated user |
| `App.jsx` | Sets up routing: `/login`, `/register`, `/dashboard` (protected) |

**Verified working:**
- Register → auto-login → redirected to dashboard
- Logout → redirected to login
- Manually visiting `/dashboard` while logged out → redirected to login (route protection actually works, not just hidden UI)
- Re-login with existing credentials works
- Refreshing the page while logged in keeps you logged in (token persists via `localStorage`)

---

## Environment / Config Notes

- **Database name:** `meeting_assistant`
- **Database credentials (local dev only):** `postgres` / `postgres`
- **JWT secret:** stored in `backend/src/main/resources/application.properties` under `jwt.secret` — generated via `openssl rand -base64 32`. **Never commit this to a public repo in a real project** — should eventually move to an environment variable.
- **CORS:** currently only allows requests from `http://localhost:5173` (configured in `SecurityConfig.java`) — update this if the frontend port changes or when deploying.

---

## Troubleshooting Log (Phase 1)

Issues actually encountered and fixed while building this phase, in case they resurface:

1. **`database "meeting_assistant" does not exist`** — caused by an old Docker volume from before `POSTGRES_DB` was set correctly. Fixed with `docker compose down -v && docker compose up -d`.
2. **Orphaned container holding port 5432** — happened after renaming the project folder (Docker Compose names containers after the folder name). Fixed by manually stopping/removing the old container.
3. **`invalid value for parameter "TimeZone": "Asia/Calcutta"`** — Windows + Java reports an outdated timezone alias that Postgres 16 doesn't recognize. Fixed via the JDBC URL parameter (see above).
4. **`cannot find symbol: UsernamePasswordAuthenticationFilter`** — wrong import path. Correct package is `org.springframework.security.web.authentication`, not `org.springframework.security.authentication`.
5. **Got 403 instead of 401 for unauthenticated requests** — expected behavior for this custom JWT setup without a configured `AuthenticationEntryPoint`; not a bug.

---

## Next Steps (Phase 2 Preview)

- Add a `Meeting` entity (id, user, filename, status, timestamps)
- Backend endpoint to accept file uploads (Zoom/Meet/Teams recordings)
- Store uploaded files (local disk for now, cloud storage later)
- Basic "My Meetings" page in React with an upload button