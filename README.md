# BidMesh

BidMesh is a high-performance, real-time bidding and auction platform. It features a microservice-inspired architecture using Spring Boot, a React frontend, Spring Cloud Gateway for secure routing, and real-time WebSockets backed by Redis and Kafka.

---

## 🚀 Prerequisites

Before you begin, ensure you have the following installed:
* **Java 17+** and **Maven**
* **Node.js** (v16+) and **npm** or **yarn**
* **PostgreSQL** (Running on port `5432`)
* **Redis** (Running on port `6379`)
* **Apache Kafka** (Running on port `9092`)

*(Note: You can use Docker to quickly spin up Postgres, Redis, and Kafka if you don't want to install them locally).*

---

## ☁️ Cloudinary Setup (Image Hosting)

BidMesh uses Cloudinary to host auction item images securely on the cloud. You must set this up before running the backend.

1. Go to [Cloudinary](https://cloudinary.com/) and create a free account.
2. Navigate to your Cloudinary Dashboard and locate your **Product Environment Credentials**:
   * Cloud Name
   * API Key
   * API Secret
3. Open the backend properties file: `src/main/resources/application.properties`
4. Scroll to the bottom and paste your keys:
   ```properties
   # Cloudinary Setup
   cloudinary.cloud_name=YOUR_CLOUD_NAME
   cloudinary.api_key=YOUR_API_KEY
   cloudinary.api_secret=YOUR_API_SECRET
   ```

---

## 🛠️ Project Setup Instructions

The BidMesh platform requires three separate servers to run simultaneously: The Backend API, the API Gateway, and the React Frontend.

### 1. Start the Backend API (Port 8080)
The core backend handles databases, security, Kafka events, and Cloudinary uploads.
1. Open a terminal in the root directory (`BidMesh/`).
2. Ensure your PostgreSQL server is running and a database named `bidmesh` exists (Update credentials in `application.properties` if needed).
3. Ensure Redis and Kafka are running.
4. Run the Spring Boot application:
   ```bash
   mvn clean spring-boot:run
   ```

### 2. Start the API Gateway (Port 9000)
The Gateway securely routes frontend requests to the backend and handles CORS.
1. Open a **new** terminal in the gateway directory (`BidMesh/gateway/`).
2. Run the Gateway application:
   ```bash
   mvn clean spring-boot:run
   ```

### 3. Start the React Frontend (Port 3000)
The user interface for Admins and regular Bidders.
1. Open a **new** terminal in the UI directory (`BidMesh/bidmesh-ui/`).
2. Install the necessary dependencies:
   ```bash
   npm install
   ```
3. Start the development server:
   ```bash
   npm start
   ```

---

## 🔑 Default Roles & Authentication

The platform uses JWT-based authentication. 

* **Admin Role (`ROLE_ADMIN`):** Has exclusive access to create auctions, upload/edit images via Cloudinary, and end or delete auctions early.
* **User Role (`ROLE_USER`):** Can browse active auctions, view their personal bid history, and place live bids via WebSockets.

---

## ⚡ Load Testing (Artillery)

BidMesh includes a robust concurrency testing suite designed to prove that the backend (backed by Redis distributed locks) is immune to race conditions during high-traffic bidding wars. 

The provided Artillery test simulates **75 virtual users** simultaneously placing bids on a single auction item over a 15-second window.

### How to run the Load Test:
1. Install Artillery globally on your machine:
   ```bash
   npm install -g artillery
   ```
2. Log into the BidMesh frontend and grab your active JWT Token from the browser's Developer Tools (Application -> Local Storage -> `jwtToken`).
3. Configure the test files with your active data:
   * In `load-test.yml`, replace `YOUR_AUCTION_ID` with an active auction ID (e.g., `1`).
   * In `load-test.yml`, replace `YOUR_JWT_TOKEN_HERE` with your copied JWT token.
   * In `helpers.js`, change `currentBid = 0;` to a number higher than your auction's current price.
4. Open a terminal in the `load-testing/` directory and unleash the test:
   ```bash
   artillery run load-test.yml
   ```
*If successful, you will see the backend mathematically reject conflicting concurrent bids with HTTP `400 Bad Request` while perfectly processing valid bids with `200 OK`, all with zero server crashes.*
