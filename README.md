# Radion

AI That Organizes Your Life

## Prerequisites
- Java 17+
- Node.js 18+
- Docker (for PostgreSQL)

## Startup Instructions
1. **Start Database:** Run `docker-compose up -d` in the root folder.
2. **Start Backend:** 
   - Navigate to `/backend`
   - Run `./mvnw spring-boot:run`
   - Backend runs on `http://localhost:8080`
3. **Start Frontend:**
   - Navigate to `/frontend`
   - Run `npm install`
   - Run `npm run dev`
   - Frontend runs on `http://localhost:3000`