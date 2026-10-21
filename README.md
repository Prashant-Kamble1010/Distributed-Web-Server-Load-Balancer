# Distributed Web Server Load Balancer Simulator
### Final Year B.Tech Information Technology &bull; Distributed Computing Systems (DCS) Mini-Project

A fully functional, educational web application simulating a **Distributed Web Server Load Balancer**. This application demonstrates deterministic Round-Robin request scheduling, real-time node health monitoring, automatic fault tolerance / server skipping, thread-safe request counting, and graceful degradation across three simulated backend web servers (**Server A**, **Server B**, and **Server C**).

> **Academic Note / Disclaimer:**  
> This project is designed for educational simulation. The backend nodes (Server A, Server B, Server C) are simulated as isolated, thread-safe state machines within a single Spring Boot runtime rather than physically distinct distributed servers across a physical network. This ensures zero external infrastructure overhead (no Docker, Kubernetes, cloud subscriptions, or external databases required) while faithfully demonstrating core distributed systems algorithms.

---

## 1. Project Abstract
Modern web applications handle millions of simultaneous user requests. Distributing this traffic equitably across a cluster of backend servers is vital to avoid node saturation, optimize throughput, minimize response latency, and eliminate single points of failure. This project implements a software-level Load Balancer Simulator using Spring Boot and Java. It visualizes the cyclic Round-Robin distribution of incoming HTTP requests, dynamically tracks server health states, automatically isolates offline nodes, routes around failures, and gracefully rejects requests with standard HTTP status codes (`HTTP 503 Service Unavailable`) when all backend nodes fail.

---

## 2. Problem Statement
In distributed computing, uncoordinated or random traffic distribution leads to uneven server utilization, request bottlenecks, and cascading node failure. When backend servers crash or undergo maintenance, a naive dispatcher continues sending traffic to dead nodes, degrading quality of service (QoS). There is a critical need for an interactive educational tool that visualizes:
1. Deterministic cyclic request dispatching (**Round-Robin**).
2. Health-aware dynamic routing without race conditions.
3. Fault detection, automatic failover, and self-healing when nodes recover.

---

## 3. Project Objectives
- **Round-Robin Scheduling:** Dispatch sequential HTTP requests across available servers in a cyclic sequence ($A \to B \to C \to A$).
- **Health Monitoring & Failover:** Dynamically detect offline servers and skip them without breaking rotation order.
- **Fault Tolerance:** Maintain seamless system availability as long as at least one backend node remains online.
- **Graceful Degradation:** Safely reject requests with `HTTP 503 Service Unavailable` when all servers are offline, without corrupting internal counters.
- **Thread Safety:** Ensure concurrency control using synchronized blocks and atomic primitives (`AtomicLong`) so concurrent multi-threaded requests do not corrupt routing state.
- **Interactive Visualization:** Provide a modern Bootstrap 5 dark-blue dashboard with live cards, Chart.js metrics, and an audit table.

---

## 4. Technologies Used
- **Core Platform:** Java 17 / 21 / 25 compatible.
- **Framework:** Spring Boot (MVC, REST, Embedded Tomcat).
- **Build System:** Apache Maven.
- **Templating Engine:** Thymeleaf (Server-Side Rendering).
- **Frontend Stack:** HTML5, CSS3, JavaScript (ES6+), Bootstrap 5, Bootstrap Icons, Chart.js.
- **Testing:** JUnit 5, Spring Boot Test (`@SpringBootTest`).
- **Persistence:** In-Memory thread-safe data structures (`ConcurrentHashMap`, `ConcurrentLinkedDeque`, `AtomicLong`).

---

## 5. System Architecture

```
                   +----------------------------------+
                   |       Web Browser Client         |
                   |  (Dashboard UI / AJAX / REST)    |
                   +-----------------+----------------+
                                     | HTTP Requests (Port 8080)
                                     v
                   +----------------------------------+
                   |  DashboardController / RestApi   |
                   +-----------------+----------------+
                                     |
                                     v
                   +----------------------------------+
                   |      LoadBalancerService         |
                   |   (Round-Robin Pointer Engine)   |
                   +--------+----------------+--------+
                            |                |
             Health Checks  |                | Request Audit
                            v                v
            +--------------------+   +---------------------+
            | ServerHealthService|   |RequestHistoryService|
            +---------+----------+   +---------------------+
                      |
        +-------------+-------------+
        |                           |
        v                           v
+-----------------+         +-----------------+         +-----------------+
|    Server A     |         |    Server B     |         |    Server C     |
| (127.0.0.1:8081)|         | (127.0.0.1:8082)|         | (127.0.0.1:8083)|
| Status: ONLINE  |         | Status: ONLINE  |         | Status: ONLINE  |
+-----------------+         +-----------------+         +-----------------+
```

### Module Descriptions:
1. **`LoadBalancerService`**: Implements the synchronized Round-Robin scheduling algorithm, maintains the cyclic pointer, handles batch dispatches, and coordinates failover.
2. **`ServerHealthService`**: Tracks operational statuses (`ONLINE`/`OFFLINE`) of Server A, Server B, and Server C, calculates load distribution percentages, and evaluates overall system health.
3. **`RequestHistoryService`**: Maintains bounded FIFO request logs (last 100 entries) and atomic counters for total, successful, and failed requests.
4. **`LoadBalancerRestController`**: Exposes RESTful endpoints for single/batch request dispatch, health toggles, and metrics retrieval.
5. **`DashboardController`**: Serves the responsive Thymeleaf web dashboard.

---

## 6. Round-Robin Algorithm Specification

### Mathematical Formulation
Given an ordered list of $N$ servers $S = [S_0, S_1, \dots, S_{N-1}]$ and a rotation pointer $P$ (initialized to $-1$):
When request $R$ arrives:
1. Let $i = 0$.
2. Compute candidate index:  
   $$C = (P + 1 + i) \pmod N$$
3. If $S_C$ is `ONLINE`:
   - Set $P \leftarrow C$.
   - Increment $S_C.\text{requestCount}$.
   - Return $S_C$ with `HTTP 200 SUCCESS`.
4. Else, if $i < N - 1$, increment $i \leftarrow i + 1$ and repeat from Step 2.
5. If all $N$ servers have been examined and none are online:
   - Keep pointer $P$ unchanged.
   - Return `HTTP 503 FAILED` (Service Unavailable).

### Example Execution:
- **All Servers Online:**  
  Request 1 &rarr; Server A  
  Request 2 &rarr; Server B  
  Request 3 &rarr; Server C  
  Request 4 &rarr; Server A  
  Request 5 &rarr; Server B  
  Request 6 &rarr; Server C  
- **Server B Offline:**  
  Request 1 &rarr; Server A  
  Request 2 &rarr; Server C *(B skipped)*  
  Request 3 &rarr; Server A  
  Request 4 &rarr; Server C  
- **Server B Restored Online:**  
  Request 5 &rarr; Server A  
  Request 6 &rarr; Server B *(restored node reintegrated)*  
  Request 7 &rarr; Server C  

---

## 7. Installation and Execution Steps

### Prerequisites
- Java JDK 17, 21, or 25 installed (`java -version`).
- Maven (or use the provided `mvn.bat` / `mvn.ps1` helper scripts).

### Execution Commands:

1. **Clone or Navigate to the Project Directory:**
   ```powershell
   cd c:\Users\Prashant\OneDrive\Desktop\Projects\DCS
   ```

2. **Run the Automated Test Suite (15 Tests):**
   ```powershell
   .\mvn.bat test
   ```
   *(or `.\mvn.ps1 test`)*

3. **Build the Standalone JAR:**
   ```powershell
   .\mvn.bat package -DskipTests
   ```

4. **Run the Application:**
   ```powershell
   .\mvn.bat spring-boot:run
   ```
   *Alternatively, run the packaged JAR directly:*
   ```powershell
   java -jar target\load-balancer-simulator-1.0.0.jar
   ```

5. **Access the Dashboard:**
   Open your browser and navigate to:  
   **`http://localhost:8080/`**

---

## 8. REST API Endpoint Documentation

| Method | Endpoint | Description | Status Code |
|---|---|---|---|
| `GET` | `/api/servers` | Retrieves all server states, request counts, and load percentages. | `200 OK` |
| `POST` | `/api/requests` | Dispatches a single request through the Round-Robin balancer. | `200 OK` or `503 Service Unavailable` |
| `POST` | `/api/requests/batch` | Dispatches multiple sequential requests (e.g. `{"count": 6}`). | `200 OK` or `503 Service Unavailable` |
| `POST` | `/api/servers/{id}/offline`| Takes a server offline (`server-a`, `server-b`, `server-c`). | `200 OK` or `404 Not Found` |
| `POST` | `/api/servers/{id}/online` | Restores a server to online status. | `200 OK` or `404 Not Found` |
| `GET` | `/api/stats` | Retrieves aggregate metrics (total, success, failed, health). | `200 OK` |
| `GET` | `/api/requests/history` | Retrieves recent request audit records (supports `?limit=50`). | `200 OK` |
| `POST` | `/api/requests/history/clear`| Clears the audit table while retaining metrics. | `200 OK` |
| `POST` | `/api/reset` | Resets all servers to online, zeroes counters, resets pointer. | `200 OK` |

---

## 9. Automated Testing Scenarios Verified

The project contains 15 JUnit 5 unit and integration tests covering:
1. **Scenario 1:** Sequential routing with 3 online servers ($A \to B \to C \to A \to B \to C$).
2. **Scenario 2:** Skipping offline nodes (Server B offline &rarr; strictly routes $A \to C \to A \to C$).
3. **Scenario 3:** Dynamic server recovery and seamless reintegration into the active rotation.
4. **Scenario 4:** Cluster-wide failure (all servers offline &rarr; returns 503, no node counter incremented).
5. **Scenario 5:** Isolation of counters (only the handling server increments its counter).
6. **Scenario 6:** Full simulation reset (all servers restored online, counters zeroed, pointer reset).
7. **Scenario 7:** Concurrency stress test (300 concurrent requests across 10 threads without race conditions).
8. **Scenario 8:** Batch request dispatching.
9. **Scenario 9:** Input validation (rejection of batch counts $< 1$ or $> 100$).
10. **Scenarios 10-15:** REST API and Thymeleaf Web MVC integration tests.

---

## 10. DCS Syllabus Mapping

| DCS Syllabus Topic | Project Implementation & Mapping |
|---|---|
| **Process Synchronization & Mutual Exclusion** | Synchronized routing method preventing critical section race conditions between concurrent client requests. |
| **Scheduling Algorithms** | Deterministic cyclic Round-Robin scheduling algorithm. |
| **Fault Detection & Health Probing** | Active status monitoring, cluster health state calculation (`HEALTHY`, `DEGRADED`, `CRITICAL`). |
| **Fault Tolerance & Graceful Degradation** | Dynamic failover routing around offline nodes; returning HTTP 503 when quorum/cluster is exhausted. |
| **System Scalability & Load Balancing** | Horizontal distribution of computational load to optimize throughput and prevent node starvation. |
| **Audit Trails & Observability** | Bounded audit logging of request metadata, cluster state snapshots, and simulated latencies. |

---

## 11. Limitations and Future Enhancements
- **Limitations:** Backend nodes are simulated in memory within one JVM process. Network partitions and packet loss are simulated programmatically rather than physical Ethernet disconnects.
- **Future Enhancements:**
  1. Add alternative scheduling algorithms: Weighted Round-Robin, Least Connections, and IP-Hash.
  2. Implement simulated server response delays and dynamic latency-based routing.
  3. Support dynamic registration of new servers at runtime ($N > 3$).
