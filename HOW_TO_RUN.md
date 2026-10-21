# CoWorkSpace — How to Run Locally

## Prerequisites

- Java JDK 21
- Python 3
- Git
- VS Code

## 1. Start the Backend

Open the first VS Code terminal and run:

```powershell
cd "C:\Users\Harsh Shah\Desktop\IP MP\coworkspace"
```

If Java 21 is installed through IntelliJ IDEA, configure it for this terminal:

```powershell
$java21 = "C:\Program Files\JetBrains\IntelliJ IDEA Community Edition 2025.1.3\jbr"
$env:JAVA_HOME = $java21
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
```

Set a local development JWT secret:

```powershell
$env:JWT_SECRET = "replace-with-a-local-secret-at-least-32-characters-long"
```

Run the backend:

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=dev"
```

Wait for Spring Boot to start successfully. Keep this terminal open.

Backend API base URL:

http://localhost:8080/api

## 2. Start the Frontend

Open a second VS Code terminal. Run:

```powershell
cd "C:\Users\Harsh Shah\Desktop\IP MP\coworkspace-frontend"
python -m http.server 5500
```

Keep this terminal open.

Open the application in your browser:

http://localhost:5500/login.html

The workspace browsing page is available at:

http://localhost:5500/browse.html

## 3. Run Backend Tests

Open another terminal:

```powershell
cd "C:\Users\Harsh Shah\Desktop\IP MP\coworkspace"
.\mvnw.cmd test
```

The previously tested backend suite completed with 27 tests, zero failures, zero errors and zero skipped tests.

Run the tests again after future code changes.

## 4. Stop the Application

Press `Ctrl + C` in each terminal to stop the corresponding server.

## Important Notes

- Start the backend before testing frontend API functionality.
- Keep both server terminals running while using the application.
- The application currently does not process real online payments.
- Booking dates support daily and monthly reservations.
- Never commit real passwords, API keys, JWT secrets or `.env` files to GitHub.
- Use a strong, private JWT secret outside local development.
- Do not delete the local database to solve unrelated application errors.