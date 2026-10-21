# CoWorkSpace frontend

Static HTML/CSS/JavaScript frontend for the CoWorkSpace Spring Boot API. It does not implement OTP verification and does not process online payments.

## Run locally

From this folder, run `python -m http.server 5500` or use VS Code Live Server, then open `http://localhost:5500/login.html`. Start the backend separately from `../coworkspace` on port 8080.

## Pages

- `login.html`: email/password registration and login.
- `browse.html`: workspace search, type filter and sorting.
- `space-detail.html`: live availability, daily/monthly pricing and direct reservation confirmation.
- `my-bookings.html`: reservation history, summary and cancellation.
- `profile.html`: profile/contact details and password updates.
- `payment-methods.html`: safe, local payment preferences and actual reservation history; no real payment data is collected.
- `admin.html`: protected space management, seat-unit creation and all-reservations view.

The shared `api.js` helper manages JWT requests, error handling, INR formatting and HTML escaping. It defaults to `http://localhost:8080/api`; for deployment, set `window.COWORKSPACE_API_BASE` before `api.js` loads on each page.

- `help.html`: interactive FAQ and navigation to common customer tasks.
