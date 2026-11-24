@echo off
setlocal
set "PROJECT_DIR=%~dp0"
pushd "%PROJECT_DIR%"

echo ========================================================
echo        Starting BidMesh Event-Driven Microservices
echo ========================================================

echo 1. Starting Infrastructure (Docker: Postgres, Redis, Kafka)...
docker compose up -d

powershell -NoProfile -Command "Start-Sleep -Seconds 5"

echo 2. Starting Auth Service (Port 8081 - DB: bidmesh_auth)...
start "BidMesh Auth Service (:8081)" /D "%PROJECT_DIR%" "%ComSpec%" /k ""%PROJECT_DIR%mvnw.cmd" -pl bidmesh-auth-service spring-boot:run"

echo 3. Starting Catalog Service (Port 8082 - DB: bidmesh_catalog)...
start "BidMesh Catalog Service (:8082)" /D "%PROJECT_DIR%" "%ComSpec%" /k ""%PROJECT_DIR%mvnw.cmd" -pl bidmesh-catalog-service spring-boot:run"

echo 4. Starting Bidding Engine Service (Port 8083 - DB: bidmesh_bidding)...
start "BidMesh Bidding Service (:8083)" /D "%PROJECT_DIR%" "%ComSpec%" /k ""%PROJECT_DIR%mvnw.cmd" -pl bidmesh-bidding-service spring-boot:run"

echo 5. Starting API Gateway (Port 9000)...
start "BidMesh Gateway (:9000)" /D "%PROJECT_DIR%" "%ComSpec%" /k ""%PROJECT_DIR%mvnw.cmd" -pl gateway spring-boot:run"

echo 6. Starting React Frontend (Port 3000)...
start "BidMesh UI (:3000)" /D "%PROJECT_DIR%bidmesh-ui" "%ComSpec%" /k "npm run dev"

echo ========================================================
echo Startup commands sent. Check each service window for readiness.
echo UI: http://localhost:3000
echo Gateway: http://localhost:9000
echo ========================================================
popd
endlocal
