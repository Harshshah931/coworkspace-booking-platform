# CoWorkSpace — Frontend (wired to backend)

This folder contains your Stitch-designed pages, now wired to the real Spring
Boot backend (the one you built earlier in IntelliJ). No build step, no
framework — just static HTML + one shared `api.js`.

## Files

| File | Purpose |
|---|---|
| `login.html` | Login / Sign up (toggle) |
| `browse.html` | Browse all spaces (requires login) |
| `space-detail.html` | Seat map, availability check, booking |
| `my-bookings.html` | View / cancel your bookings |
| `admin.html` | Admin dashboard: manage spaces, seat units, view all bookings |
| `api.js` | Shared helper — JWT storage + all `fetch()` calls to the backend |

## How to run

### 1. Start the backend first
Open the `coworkspace` Spring Boot project in IntelliJ and run
`CoworkspaceApplication.java`. Confirm it's up by visiting
`http://localhost:8080/swagger-ui.html`.

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
- **Regular user:** click "Sign up" on the login page to create your own account

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
- If any API call returns 401/403 (expired/invalid token), the session is
  cleared and the user is bounced to `login.html` automatically.

## What's real vs. what's still visual-only

**Fully wired to the backend:**
- Register / Login
- Browse spaces (with client-side name/location filter)
- Seat map availability (calls the real overlap-detection endpoint)
- Booking creation, cancellation, "my bookings" list
- Admin: create/delete spaces, add seat units, view all bookings

**Left as visual-only (no backend support needed / out of scope):**
- Google/GitHub social login buttons on the login page (show a message if clicked)
- "Payment Methods" and "Profile" sidebar links on My Bookings
- "Memberships" nav link and its footer promo banner

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
