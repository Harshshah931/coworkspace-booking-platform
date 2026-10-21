# CoWorkSpace — Frontend (wired to backend)

This folder contains your Stitch-designed pages, now wired to the real Spring
Boot backend (the one you built earlier in IntelliJ). No build step, no
framework — just static HTML + one shared `api.js`.

## Files

| File | Purpose |
|---|---|
| `login.html` | Choose Login or Sign Up; use email/password or phone OTP |
| `browse.html` | Browse all spaces (requires login) |
| `space-detail.html` | Seat map, availability check, booking |
| `my-bookings.html` | View / cancel your bookings |
| `admin.html` | Admin dashboard: manage spaces, seat units, view all bookings |
| `api.js` | Shared helper — JWT storage + all `fetch()` calls to the backend |

## How to run

### 1. Start the backend first
Open the `coworkspace` Spring Boot project in IntelliJ, activate the `dev`
Spring profile, and run `CoworkspaceApplication.java`. The local JWT and
administrator seed defaults are defined only in that profile. Confirm it's up by visiting
`http://localhost:8080/swagger-ui.html`.

For a production run, activate only the `prod` profile. Set `JWT_SECRET` and
`OTP_HMAC_SECRET` to different strong secrets of at least 32 characters.
`OTP_PROVIDER` defaults to `TWILIO` in production, which also requires
`TWILIO_ACCOUNT_SID`, `TWILIO_AUTH_TOKEN`, `TWILIO_PHONE_NUMBER`, and
`TWILIO_WHATSAPP_NUMBER`. Administrator seeding is optional; set both
`ADMIN_EMAIL` and a strong `ADMIN_PASSWORD` only if it is needed. Missing
administrator credentials safely skip that seed.

### 2. Serve this frontend folder
You can't just double-click the HTML files (fetch calls need a real origin,
not `file://`). From inside this folder, run a simple local server:

```bash
# Python 3 (most common)
python3 -m http.server 5500
```

or, if you have Node:

```bash
npx serve .
```

Then open **http://localhost:5500/login.html** in your browser.

### 3. Log in
- **Admin:** `admin@coworkspace.com` / `admin123` (seeded automatically by the backend)
- **Regular user:** choose "Sign Up" on the login page to create an account

## How the pieces connect

- `api.js` defines `API_BASE = "http://localhost:8080/api"` — change this if
  your backend runs on a different port.
- On login/signup, the JWT is stored in `localStorage` and sent as
  `Authorization: Bearer <token>` on every subsequent request.
- Pages that require login (`browse.html`, `space-detail.html`,
  `my-bookings.html`) call `Auth.requireLogin()` on load and redirect to
  `login.html` if there's no valid session.
- `admin.html` calls `Auth.requireAdmin()` — a non-admin user gets redirected
  straight back to login.
- If a protected API call returns 401/403 (expired/invalid token), the session
  is cleared and the user is bounced to `login.html`; public auth errors are
  displayed on the login form.

## What's real vs. what's still visual-only

**Fully wired to the backend:**
- Email/password registration and login
- Phone OTP send/verification and token-based phone registration
- Browse spaces (with client-side name/location filter)
- Seat map availability (calls the real overlap-detection endpoint)
- Booking creation, cancellation, "my bookings" list
- Admin: create/delete spaces, add seat units, view all bookings

**Not available in this build:**
- Google/GitHub social login and password reset (the backend has no corresponding endpoints)
- "Payment Methods" and "Profile" sidebar links on My Bookings
- "Memberships" nav link and its footer promo banner

The development OTP provider defaults to `MOCK`; it does not send real SMS or
WhatsApp messages. To retrieve a generated code for local manual testing, start
the backend from PowerShell with the `dev` profile and the explicit opt-in flag:

```powershell
$env:SPRING_PROFILES_ACTIVE = "dev"
$env:OTP_PROVIDER = "MOCK"
$env:OTP_MOCK_EXPOSE_CODE = "true"
.\mvnw.cmd spring-boot:run
```

After requesting a code in the login page, select **Show local mock OTP**. The
endpoint is available only under the `dev` profile, with MOCK selected, and when
`OTP_MOCK_EXPOSE_CODE=true`; otherwise OTP values are never returned by the API.
Keep this flag disabled outside local development. The phone field accepts a
10-digit Indian mobile number, normalized to E.164 with the `+91` country code,
or an international number in E.164 form with its country code.

Real SMS/WhatsApp delivery is not enabled by default. It requires selecting
`OTP_PROVIDER=TWILIO` and configuring `TWILIO_ACCOUNT_SID`,
`TWILIO_AUTH_TOKEN`, `TWILIO_PHONE_NUMBER`, and (for WhatsApp)
`TWILIO_WHATSAPP_NUMBER` in the environment. Never put provider credentials in
source control.

## Troubleshooting

- **"Could not load spaces" / network errors:** make sure the backend is
  running on `localhost:8080` and that you started the frontend via a local
  server (not by opening the HTML file directly).
- **CORS errors in the browser console:** the backend's `SecurityConfig`
  allows `http://localhost:*` and `http://127.0.0.1:*`. If you're serving the
  frontend on a different host, update `corsConfigurationSource()` in
  `SecurityConfig.java`.
- **Logged in but immediately bounced to login:** your JWT may have expired
  (default 24h) — just log in again.
