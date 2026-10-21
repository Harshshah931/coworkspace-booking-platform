# CoWorkSpace

CoWorkSpace is a coworking-space discovery and reservation application. Customers can create an account, browse spaces, check desk/cabin availability, make daily or monthly reservations, manage their bookings and profile. Administrators can manage spaces, add bookable units, and review platform reservations.

**Authentication uses email and password only. This version does not include OTP verification or actual online payment processing.** Booking reservations are recorded immediately after the signed-in customer confirms, provided the selected unit is still available.

## Technology

- Backend: Java 17+, Spring Boot 3.3.2, Spring Security, JWT, Spring Data JPA and Maven.
- Development database: H2 file database, persisted between restarts.
- Production database: PostgreSQL.
- Frontend: static HTML, CSS and JavaScript; Tailwind CDN is still used by some legacy management pages.

## Requirements

- JDK 17 or 21 recommended.
- Windows PowerShell for the commands below (macOS/Linux can use `./mvnw`).
- Internet access is needed the first time Maven downloads the project's dependencies.

## Run the backend on Windows

From the repository root:

```powershell
cd coworkspace
$env:JWT_SECRET = "replace-with-a-local-secret-at-least-32-characters-long"
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=dev"
```

The backend runs at `http://localhost:8080`. The development profile seeds an administrator on an empty database using `ADMIN_EMAIL` and `ADMIN_PASSWORD`. Its local defaults are `admin@coworkspace.com` and `admin123`; change them before sharing or deploying the app.

The H2 console, when enabled, is at `http://localhost:8080/h2-console`. Development database files are under `coworkspace/data/` and must not be deleted if you want to preserve local users and reservations.

## Run the frontend

Open a second PowerShell window at the repository root:

```powershell
cd coworkspace-frontend
python -m http.server 5500
```

Then visit `http://localhost:5500/login.html`. You can also use VS Code Live Server. Keep the backend running in the other terminal. The frontend API helper calls `http://localhost:8080/api` by default. For a hosted setup, define `window.COWORKSPACE_API_BASE` before loading `api.js` on each page to point at the deployed API base URL.

## Customer journey

1. Register using a name, email and password, or sign in.
2. Browse and filter workspace listings.
3. Open a space, select daily or monthly booking and a date range, and check live unit availability.
4. Select an available desk/cabin and confirm the reservation. The server recalculates the price, validates the dates and uses a seat-unit lock to reduce concurrent double-booking.
5. View reservations under **My Bookings** and cancel your own active reservation.
6. Edit your name/contact phone and change your password under **Profile**.

## Administrator journey

Sign in with an administrator account, then open **Manage**. The dashboard uses actual workspace and booking records. Administrators can create/update/delete spaces where database constraints allow, add seat units and review all reservations. All `/api/admin/**` endpoints and workspace write endpoints are protected by server-side role checks.

To create an administrator from environment variables before the first run:

```powershell
$env:ADMIN_EMAIL = "admin@your-domain.example"
$env:ADMIN_PASSWORD = "choose-a-strong-local-admin-password"
```

Self-registration always creates a `USER` role; it cannot set itself to `ADMIN`.

## Run automated tests

From the repository root:

```powershell
cd coworkspace
.\mvnw.cmd test
```

Tests use an isolated in-memory H2 configuration. They must never be pointed at the persistent development database. If Maven reports that it cannot download dependencies, check your network/proxy settings and rerun the command; do not delete `coworkspace/data/` to solve a build problem.

## Production configuration

Activate the `prod` profile and configure `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`, `JWT_SECRET`, and optionally `ADMIN_EMAIL`/`ADMIN_PASSWORD`. Production uses PostgreSQL and `spring.jpa.hibernate.ddl-auto=validate`; prepare the PostgreSQL schema before starting the application. Never commit real secrets.

## Scope notes

- Reservations do not charge a card. The payment preferences page is a local browser preference only, and displays actual reservation history.
- Email confirmation integration is not implemented in this version.
- Phone numbers are optional contact information; they are not used for authentication or booking verification.
- Daily and monthly booking plans are supported. Hourly bookings are not implemented.
- Visual assets on the browse page use remote image URLs and require an internet connection; booking and pricing data come from the backend.
