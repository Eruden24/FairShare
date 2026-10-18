# FairShare

Split shared expenses within groups: who paid, who owes what, and the fewest transfers to settle up.

| Folder | What | Docs |
|---|---|---|
| [`backend/`](backend/) | REST API: Java 25, Spring Boot 4, PostgreSQL | [backend/README.md](backend/README.md) |
| [`frontend/`](frontend/) | Web app: React 19, TypeScript, Vite | [frontend/README.md](frontend/README.md) |
| [`postman/`](postman/) | Postman collection and environment for the API | Import both files into Postman |

## Quick start

Requirements: JDK 25, Docker and Node 20.19+.

```bash
# 1. Database and API (http://localhost:8080)
cd backend
cp .env.example .env            # then fill in your own values
docker compose up -d
./mvnw spring-boot:run

# 2. Web app (http://localhost:5173), in another terminal
cd frontend
npm install
npm run dev
```

The web app forwards `/api` calls to the API on port 8080.
