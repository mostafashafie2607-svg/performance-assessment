# Performance Engineering Assessment

This repository contains a performance testing assessment implemented using **Gatling (Java DSL)** targeting the **DummyJSON authentication workflow**.

---

## 📁 Repository Structure

- `gatling-assessment/`  
  Contains the Maven-based Gatling project, including simulations and test data.

- `Plan&Reports/`  
  Contains the final deliverables:
  - Performance Testing Strategy (PDF)
  - Final Report (PDF)
  - README (PDF)
  - Supporting screenshots

---

## 🧪 Test Scenarios

### 1. Smoke Test
- 1 concurrent user  
- Purpose: validate correctness of the script  
- Ensures authentication flow works without errors  

### 2. Main Steady-State Test
- 10 concurrent users  
- Ramp-up: 20 seconds  
- Steady duration: 560 seconds  
- Ramp-down: 20 seconds  
- Total duration: 10 minutes  
- Pacing: 20 seconds  

### 3. Negative Test (Token Handling)
- Calls protected endpoint without token  
- Validates authentication/session enforcement  
- Expected result: HTTP 401 / 403  

### 4. Stress Test
- Ramp from 0 → 20 users (30 sec)  
- Then ramp from 20 → 50 users (60 sec)  
- Pacing: 10 seconds  
- Purpose: identify system breaking point  

**Stop Criteria (evaluation):**
- Error rate > 20%  
- Response time > 3000 ms  

---

## 🔄 Test Flow

- Login  
- Get Current User  
- Refresh Session  
- Get Current User (after refresh)  

---

## 📊 Data Handling

- CSV feeders used for parameterization  
- Circular feeder used for continuous load  
- Multiple unique users added to avoid session conflicts  

---

## ▶️ How to Run

Navigate to the project:

```bash
cd gatling-assessment




Smoke Test:
mvn -Dgatling.simulationClass=simulations.SmokeAuthSimulation gatling:test

Main Steady-State Test:
mvn -Dgatling.simulationClass=simulations.SteadyAuthSimulation gatling:test

Negative Test (Token Handling):
mvn -Dgatling.simulationClass=simulations.NegativeTokenAuthSimulation gatling:test

Stress Test:
mvn -Dgatling.simulationClass=simulations.StressAuthSimulation gatling:test


 Reports:

After execution, reports are generated in:

gatling-assessment/target/gatling/

Open the generated index.html file in a browser.


 Assumptions & Limitations:
Target API is a public demo service (DummyJSON)
Environment is shared and not controlled
Limited number of valid users available
Results may vary between executions


 Key Findings:
System is stable under moderate load
Login endpoint is slightly sensitive under concurrency
Read endpoints (/auth/me, /auth/refresh) are highly stable
Test data design significantly impacts results
