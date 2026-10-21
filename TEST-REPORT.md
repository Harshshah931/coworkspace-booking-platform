# CoWorkSpace test report

## Checks executed

| Check | Result | Details |
|---|---|---|
| `node --check api.js` | Passed | Shared frontend API helper parses successfully. |
| Inline JavaScript syntax | Passed | 7 inline script blocks across 8 HTML pages passed `node --check`. |
| Static element ID reference check | Passed with expected exceptions | Script ID references were compared against HTML IDs. `app-toast` is created dynamically; `user-name-label` is guarded as optional on the workspace detail page. |
| OTP source-reference scan | Passed | No former OTP controller/service/repository/provider or booking-verification references remain in Java source. The legacy phone verification database field is retained solely for schema compatibility. |
| Maven/JUnit suite | Blocked before test execution | `./mvnw test` could not download Maven 3.9.9 from `https://repo.maven.apache.org/maven2/...`; the wrapper reported `wget: Failed to fetch`. No Java tests are claimed as passing. |
| Browser end-to-end test | Not run | This environment has no installed Playwright, Puppeteer or jsdom package, and the backend could not be built/launched without Maven. |

## Commands for local verification on Windows

Run in PowerShell from the extracted project folder:

```powershell
cd coworkspace
$env:JWT_SECRET = "replace-with-a-local-secret-at-least-32-characters-long"
.\mvnw.cmd test
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=dev"
```

In a second PowerShell window:

```powershell
cd coworkspace-frontend
python -m http.server 5500
```

Open `http://localhost:5500/login.html`.

The Java test suite remains unverified until Maven dependencies can be downloaded and `.\mvnw.cmd test` runs successfully in a working environment. No live SMS, OTP, email or payment provider was contacted by this implementation.
