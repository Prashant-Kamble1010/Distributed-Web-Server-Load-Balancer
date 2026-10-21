# B.Tech Information Technology &bull; Distributed Computing Systems (DCS)
## Project Report Outline & Comprehensive Viva Guide

**Project Title:** Distributed Web Server Load Balancer Simulator  
**Subject:** Distributed Computing Systems (DCS)  
**Academic Level:** Final Year B.Tech (Information Technology)  

---

# Part 1: Mini-Project Report Outline

### 1. Title Page & Certificate
- Project Title, Student Details, Department of Information Technology, Academic Year.
- Certificate of Original Work signed by Guide and Head of Department.

### 2. Abstract
A high-level summary of the motivation, the implementation of the Round-Robin load balancing algorithm, fault-tolerance mechanisms, and the Spring Boot web dashboard.

### 3. Introduction
- **1.1 Background:** The evolution of single-server architectures to distributed clusters.
- **1.2 Motivation:** Why load balancing is necessary to prevent server saturation, bottlenecks, and single points of failure.
- **1.3 Problem Statement:** The challenges of distributing requests equitably and handling sudden server outages.
- **1.4 Project Objectives:** Implementing deterministic round-robin dispatching, health monitoring, automatic failover, and interactive web visualization.

### 4. Theoretical Foundations of Distributed Computing
- **2.1 Load Balancing Concepts:** Layer 4 (Transport) vs Layer 7 (Application) load balancing.
- **2.2 Load Balancing Algorithms:**
  - Round-Robin vs Weighted Round-Robin.
  - Least Connections.
  - IP Hashing.
- **2.3 Fault Tolerance & High Availability:** Active/Passive redundancy, heartbeats, failover mechanics, and graceful degradation (HTTP 503).
- **2.4 Concurrency & Synchronization in Distributed Environments:** Race conditions, thread safety, critical sections, and atomic memory operations.

### 5. System Design & Architecture
- **3.1 High-Level Architecture Diagram:** Client &rarr; Load Balancer Service &rarr; Health Monitor &rarr; Backend Nodes.
- **3.2 Component Details:**
  - `LoadBalancerService`: Central dispatch engine.
  - `ServerHealthService`: Cluster state manager.
  - `RequestHistoryService`: Bounded audit logger.
  - `LoadBalancerRestController`: REST API layer.
  - `DashboardController` & Thymeleaf UI: Real-time visual monitoring interface.
- **3.3 Simulation vs Physical Distributed Systems:** Clarification of simulated in-memory nodes vs multi-host production clusters (NGINX/HAProxy).

### 6. Algorithm Design & Implementation
- **4.1 Detailed Flowchart:** Step-by-step logic for request arrival, cyclic pointer check, status evaluation, dispatching, and error handling.
- **4.2 Mathematical Formulation of Cyclic Pointer:** Pointer modulus arithmetic:  
  $$C = (P + 1 + i) \pmod N$$
- **4.3 Thread-Safety Implementation:** Synchronization of the critical section and lock-free atomic counters.

### 7. Results, Experimental Analysis & Testing
- **5.1 Test Cases & Verification Matrix:**
  - Scenario 1: Normal cyclic operation ($A \to B \to C \to A$).
  - Scenario 2: Single node failure (Server B offline &rarr; $A \to C \to A \to C$).
  - Scenario 3: Node recovery and seamless rotation re-entry.
  - Scenario 4: Total cluster outage (HTTP 503 Service Unavailable).
  - Scenario 5: Multi-threaded concurrency stress test (300 requests over 10 threads).
- **5.2 Load Distribution Graphs:** Bar charts verifying equal percentage share under normal load.

### 8. Conclusion, Limitations & Future Scope
- **6.1 Summary of Outcomes.**
- **6.2 Limitations of In-Memory Simulation.**
- **6.3 Future Work:** Weighted Round-Robin, Least Response Time routing, dynamic autoscaling.

### 9. References & Appendices
- Textbooks: *Distributed Systems: Principles and Paradigms* by Andrew S. Tanenbaum & Maarten van Steen; *Distributed Computing: Principles, Algorithms, and Systems* by Ajay D. Kshemkalyani & Mukesh Singhal.
- Source Code Listings.

---

# Part 2: 17 Essential Viva Questions and Answers

### Q1: What is a Load Balancer and why is it essential in Distributed Systems?
**Answer:**  
A load balancer acts as a reverse proxy and single point of entry for incoming client traffic, distributing client requests across a pool of backend servers. In distributed systems, it is essential because it prevents any single server from becoming a performance bottleneck, optimizes resource utilization, minimizes request response latency, and ensures high availability (HA) by isolating failing servers.

---

### Q2: How does the Round-Robin algorithm work in your project?
**Answer:**  
Round-Robin is a deterministic scheduling algorithm that iterates cyclically through an ordered list of backend servers. In our implementation:
1. The load balancer maintains an integer rotation pointer ($P$) initialized to $-1$.
2. When a request arrives, it inspects candidate servers starting from $(P + 1) \pmod N$.
3. It checks if the candidate server is `ONLINE`.
4. If online, it updates the pointer to this server, increments the server's request counter, and routes the request.
5. If offline, it advances to the next candidate without updating the pointer until a healthy node is found.

---

### Q3: What happens when Server B goes offline? Does it break the Round-Robin order?
**Answer:**  
No, the cyclic order is preserved. If Server B is offline, the load balancer inspects Server B, detects its offline status, automatically skips it, and evaluates Server C. The requests route as:  
`Server A -> Server C -> Server A -> Server C`.  
When Server B is brought back online, it immediately becomes eligible for subsequent routing cycles in its natural rotation position without restarting or corrupting the sequence.

---

### Q4: What happens if all three servers (Server A, Server B, and Server C) go offline?
**Answer:**  
This represents a total cluster outage. The load balancer examines all $N$ servers, determines that zero servers are online, and does not assign the request to any server. It returns an HTTP `503 Service Unavailable` response to the client. Crucially:
- No backend server's handled counter is incremented.
- The rotation pointer remains unchanged.
- The failed request is logged in the audit trail as `FAILED` with status code 503.

---

### Q5: How do you guarantee Thread Safety when multiple clients send requests simultaneously?
**Answer:**  
In a multi-threaded web environment like Spring Boot/Tomcat, simultaneous HTTP worker threads could access the pointer concurrently. We ensure thread safety through two mechanisms:
1. **Critical Section Synchronization:** The `routeRequest()` method in `LoadBalancerService` is marked `synchronized`, ensuring that only one thread can select a server and advance the rotation pointer at any instant.
2. **Atomic Variables:** Server request counters and total metrics utilize Java's `java.util.concurrent.atomic.AtomicLong`, which relies on hardware-level Compare-And-Swap (CAS) operations rather than locking.

---

### Q6: How did you test concurrency and race conditions?
**Answer:**  
We wrote an automated JUnit 5 concurrency test (`testConcurrentRequestsThreadSafety`) using `ExecutorService` and `CountDownLatch`. Ten simultaneous threads dispatched 30 requests each (300 requests total). The test asserted that:
1. The sum of all individual server counters was exactly equal to 300.
2. Each of the three online servers handled exactly 100 requests (300 / 3).
3. Exactly 300 successful requests and 0 failed requests were recorded in history.

---

### Q7: What is the difference between Layer 4 (L4) and Layer 7 (L7) load balancers?
**Answer:**  
- **Layer 4 (Transport Layer):** Operates at TCP/UDP level without inspecting packet payload or HTTP headers. It makes routing decisions purely based on IP addresses and port numbers (e.g., AWS Network Load Balancer).
- **Layer 7 (Application Layer):** Operates at the application level (HTTP/HTTPS/gRPC). It can inspect URLs, HTTP headers, cookies, and payloads to make intelligent routing decisions (e.g., NGINX, HAProxy, AWS ALB).  
Our Spring Boot simulator represents an **Application-Level (L7) Load Balancer** operating over HTTP.

---

### Q8: Clearly explain the difference between your simulated backend nodes and a production server cluster.
**Answer:**  
In a production deployment, backend servers are separate physical machines, virtual machines, or container instances (Docker/Kubernetes pods) running on separate hosts with distinct network interfaces, communicating across physical Ethernet switches.  
In our educational simulator, the three servers are simulated thread-safe Java instances (`BackendServer`) inside a single JVM process. This design demonstrates the exact scheduling logic, state transitions, and health checks cleanly on any laptop without needing external cloud infrastructure, Docker, or external databases.

---

### Q9: Why did you choose Round-Robin over Random routing or Least Connections?
**Answer:**  
Random routing can cause uneven load distributions and burst starvation over short periods. Round-Robin is completely deterministic and guarantees an equitable spread of requests across identical servers. While Least Connections is beneficial when request processing times vary widely, Round-Robin is the foundational scheduling algorithm in distributed systems curricula and is ideal for understanding cyclic scheduling and stateful pointers.

---

### Q10: What is Fault Detection in distributed systems, and how is it simulated here?
**Answer:**  
In distributed systems, fault detection is usually achieved through active heartbeats (periodically pinging `/health` endpoints) or passive detection (detecting TCP connection timeouts or 5xx response codes). In this project, each server node maintains an operational status (`ONLINE`/`OFFLINE`). The load balancer queries `ServerHealthService` before dispatching, simulating active health probing and instant failure detection.

---

### Q11: What HTTP status code is returned when a server cannot handle a request, and why?
**Answer:**  
When all backend servers are offline, our load balancer returns `HTTP 503 (Service Unavailable)`. This RFC-compliant HTTP status indicates that the server is currently unable to handle the request due to temporary overloading or maintenance of upstream nodes.

---

### Q12: How does the application prevent unbounded memory consumption from request history?
**Answer:**  
We maintain an in-memory audit log using a bounded `ConcurrentLinkedDeque` capped at a maximum of 100 entries (`MAX_HISTORY_CAPACITY`). When a 101st request arrives, the oldest record is evicted via `history.pollLast()`, ensuring $O(1)$ memory bounds regardless of how many thousands of requests are simulated.

---

### Q13: What is the role of Spring Boot and Thymeleaf in your project architecture?
**Answer:**  
Spring Boot provides the embedded Tomcat container, dependency injection container, and REST controller framework. Thymeleaf serves as the server-side templating engine that renders the initial HTML structure (`index.html`). Subsequent dashboard interactions (sending requests, batch executions, toggling server statuses) are executed asynchronously via JavaScript `fetch()` calls to Spring `@RestController` endpoints.

---

### Q14: How does the dashboard update its charts and tables without page reloads?
**Answer:**  
The frontend JavaScript (`dashboard.js`) uses the modern asynchronous `fetch()` API. When the user clicks "Send Request", an HTTP `POST` request is sent to `/api/requests`. The response JSON is parsed, updating the DOM dynamically (status badges, counters, history table rows, and Chart.js bar chart heights) without triggering a browser refresh.

---

### Q15: What is High Availability (HA) and how does this project demonstrate it?
**Answer:**  
High Availability is the characteristic of a system to maintain acceptable operational performance and uptime despite component failures. This project demonstrates HA through fault tolerance: even if two out of the three servers go offline (66.6% infrastructure failure), the load balancer continues to serve 100% of client requests successfully via the remaining online server without dropping traffic.

---

### Q16: If you were to extend this project with a database, which one would you choose and why?
**Answer:**  
For an educational simulator, an in-memory H2 database or Redis would be ideal for tracking metrics with zero configuration. For production audit logging, a time-series database like InfluxDB or Prometheus paired with Grafana would be optimal for tracking request latencies, throughput, and error rates over time.

---

### Q17: Mention two potential enhancements you would add to this project.
**Answer:**  
1. **Weighted Round-Robin (WRR):** Allowing servers with higher compute capacity (e.g., Server A with 8 cores vs Server B with 2 cores) to handle a proportional ratio of requests (e.g., 4:1).  
2. **Dynamic Server Discovery:** Enabling new backend servers to dynamically register themselves at runtime using service discovery protocols (like Netflix Eureka or Consul).
