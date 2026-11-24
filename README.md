# BidMesh

BidMesh is a high-performance, real-time bidding and auction platform architected as **Event-Driven Microservices**. It features Spring Boot microservices, an API Gateway for routing and security, an event streaming bus backed by **Apache Kafka**, distributed locking and cache-aside with **Redis/Redisson**, and a reactive React/Vite frontend.

---

## 🏛️ Microservices Architecture

| Module | Service Name | Port | Description |
| :--- | :--- | :---: | :--- |
| `bidmesh-parent` | Maven Parent | - | Root multi-module POM coordinating independent service builds and dependency versions. |
| `bidmesh-auth-service` | Auth & User Service | `8081` | Authentication, registration, JWT issuance, and profile updates. |
| `bidmesh-catalog-service` | Auction Catalog Service | `8082` | Creating auctions, catalog browsing, Redis cache-aside, S3 image uploads, lifecycle controls. |
| `bidmesh-bidding-service` | Bidding Engine Service | `8083` | Owns bids and bid history; validates against its auction projection, uses Redisson locks, Kafka, and WebSockets. |
| `gateway` | API Gateway | `9000` | Routes requests, handles CORS, and validates HS256 JWTs locally using the shared secret. |
| `bidmesh-ui` | Frontend Application | `3000` | React + Vite UI with STOMP/SockJS WebSockets and real-time toast alerts. |
| `lambda-compressor` | Image Compression FaaS | Cloud | AWS Lambda function automatically compressing S3 images to 800px width. |

### Service data ownership

Each backend owns its persistence model and database. Services do not import one another's JPA entities or access another service's database.

| Service | Owned data | Integration |
| :--- | :--- | :--- |
| Auth | Users, credentials, roles (`bidmesh_auth`) | Issues signed JWTs containing the user ID, display name, and role. |
| Catalog | Items and auctions (`bidmesh_catalog`) | Stores creator ID/name as values, not foreign keys to Auth. Publishes auction lifecycle events and consumes bid events. |
| Bidding | Bids and its auction read projection (`bidmesh_bidding`) | Stores bidder/auction IDs as values, consumes catalog lifecycle events, and publishes accepted bids. |

Kafka JSON messages are the cross-service contract; each service maintains its own event types. The Bidding WebSocket consumer atomically deduplicates bid notifications in Redis by `bidId`; its marker TTL (8 days) exceeds the `auction-bids` topic retention (7 days). Redis caches are isolated by service (`6380` for Catalog and `6381` for Bidding). There is no shared domain-model Maven module.

The Gateway verifies JWT signatures locally and does not call Auth for each request. Set the same strong `JWT_SECRET` for Auth, Catalog, Bidding, and Gateway; auth endpoints, auction reads, WebSocket handshakes, health checks, and CORS preflight requests remain public, while other Gateway routes require a valid bearer token.

---

## 🚀 Prerequisites

Before you begin, ensure you have the following installed:
* **Java 17+** (the Maven Wrapper downloads the configured Maven version on first use)
* **Node.js** (v18+) and **npm**
* **Docker & Docker Compose** (for PostgreSQL, Redis, and Apache Kafka)

---

## ☁️ AWS S3 Setup (Image Hosting)

BidMesh uses Amazon Web Services (AWS) S3 to host auction item images securely in the cloud. You must configure your S3 bucket before running the catalog service.

1. Log into your **AWS Console** and create an **S3 Bucket**.
2. **Crucial:** When creating the bucket, uncheck "Block all public access" so images can be viewed on the frontend.
3. Once created, go to the bucket's **Permissions** tab, edit the **Bucket Policy**, and add a policy allowing `s3:GetObject` for `Principal: *`.
4. Create an IAM identity with the minimum S3 permissions required for your bucket and obtain its credentials.
5. Set these environment variables before starting the Catalog service:
   ```text
   AWS_S3_ENABLED=true
   AWS_S3_REGION=ap-south-1
   AWS_S3_BUCKET=your-bucket-name
   AWS_ACCESS_KEY_ID=your-access-key
   AWS_SECRET_ACCESS_KEY=your-secret-key
   ```

S3 is disabled by default, so the Catalog service can run locally without AWS credentials. Image upload requests return `503 Service Unavailable` until S3 is configured.

### ⚡ AWS Lambda Setup (Image Compression)

1. Go to AWS Lambda and create a new **Node.js** function.
2. Under the Code tab, click **Upload from -> .zip file** and upload `lambda-compressor.zip`.
3. Go to Configuration -> Permissions, click the Execution Role, and attach the `AmazonS3FullAccess` policy.
4. Click **+ Add trigger**, select S3, choose your image bucket, and set the **Prefix** exactly to `raw/`.

---

## 🛠️ Project Setup Instructions

### Option A: One-Click Startup (Windows)
Double-click `start-all.bat` in the root folder, or run in PowerShell:
```powershell
.\start-all.bat
```
This automatically boots Docker infrastructure, starts all microservices in separate windows, and starts the UI.

### Option B: Manual Startup

#### 1. Start Infrastructure (Docker)
```powershell
docker compose up -d
```

#### 2. Start Auth Service (Port 8081)
```powershell
.\mvnw.cmd -pl bidmesh-auth-service spring-boot:run
```

#### 3. Start Catalog Service (Port 8082)
```powershell
.\mvnw.cmd -pl bidmesh-catalog-service spring-boot:run
```

#### 4. Start Bidding Engine Service (Port 8083)
```powershell
.\mvnw.cmd -pl bidmesh-bidding-service spring-boot:run
```

#### 5. Start API Gateway (Port 9000)
```powershell
.\mvnw.cmd -pl gateway spring-boot:run
```

#### 6. Start React UI (Port 3000)
```powershell
cd bidmesh-ui
npm install --legacy-peer-deps
npm run dev
```

---

## 🔑 Default Roles & Authentication

The platform uses JWT-based authentication. Initial accounts are seeded upon startup:

* **Admin (`ROLE_ADMIN`):** `admin@bidmesh.com` / `password` (can publish auctions, end/delete auctions, upload photos).
* **Bidder (`ROLE_USER`):** `user1@bidmesh.com` / `password` (can browse active auctions, place bids, view bid history).

---

## ⚡ Load Testing (Artillery)

BidMesh includes a robust concurrency testing suite designed to prove that the Bidding Engine (backed by Redis distributed locks) is immune to race conditions during high-traffic bidding wars. 

The provided Artillery test simulates **75 virtual users** simultaneously placing bids on a single auction item over a 15-second window.

### How to run the Load Test:
1. Install Artillery globally on your machine:
   ```bash
   npm install -g artillery
   ```
2. Log into the BidMesh frontend and grab your active JWT Token from the browser's Developer Tools (Application -> Local Storage -> `jwtToken`).
3. Configure `load-testing/load-test.yml` with your active auction ID and JWT token.
4. Run:
   ```bash
   cd load-testing
   artillery run load-test.yml
   ```
*Conflicting concurrent bids are safely rejected with `400 Bad Request` while valid bids succeed with `200 OK` with zero race conditions.*
