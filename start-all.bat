@echo off
echo ========================================================
echo        Starting BidMesh Event-Driven Microservices
echo ========================================================

echo 1. Starting Infrastructure (Docker: Postgres, Redis, Kafka)...
docker compose up -d

timeout /t 5 /nobreak >nul

echo Ensuring isolated databases exist (bidmesh_auth, bidmesh_catalog, bidmesh_bidding)...
docker exec bidmesh-postgres psql -U user -d postgres -c "CREATE DATABASE bidmesh_auth;" 2>nul
docker exec bidmesh-postgres psql -U user -d postgres -c "CREATE DATABASE bidmesh_catalog;" 2>nul
docker exec bidmesh-postgres psql -U user -d postgres -c "CREATE DATABASE bidmesh_bidding;" 2>nul

echo 2. Starting Auth Service (Port 8081 - DB: bidmesh_auth)...
start "BidMesh Auth Service (:8081)" cmd /k "cd /d D:\Newfolder\BidMesh\bidmesh-auth-service && "D:\Program Files\apache-maven-3.9.6\bin\mvn.cmd" spring-boot:run"

echo 3. Starting Catalog Service (Port 8082 - DB: bidmesh_catalog)...
start "BidMesh Catalog Service (:8082)" cmd /k "cd /d D:\Newfolder\BidMesh\bidmesh-catalog-service && "D:\Program Files\apache-maven-3.9.6\bin\mvn.cmd" spring-boot:run"

echo 4. Starting Bidding Engine Service (Port 8083 - DB: bidmesh_bidding)...
start "BidMesh Bidding Service (:8083)" cmd /k "cd /d D:\Newfolder\BidMesh\bidmesh-bidding-service && "D:\Program Files\apache-maven-3.9.6\bin\mvn.cmd" spring-boot:run"

echo 5. Starting API Gateway (Port 9000)...
start "BidMesh Gateway (:9000)" cmd /k "cd /d D:\Newfolder\BidMesh\gateway && "D:\Program Files\apache-maven-3.9.6\bin\mvn.cmd" spring-boot:run"

echo 6. Starting React Frontend (Port 3000)...
start "BidMesh UI (:3000)" cmd /k "cd /d D:\Newfolder\BidMesh\bidmesh-ui && npm run dev"

echo ========================================================
echo All pure microservices are running with isolated databases!
echo UI: http://localhost:3000
echo Gateway: http://localhost:9000
echo ========================================================
