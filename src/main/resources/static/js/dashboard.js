/**
 * Distributed Web Server Load Balancer Simulator
 * Frontend Client Controller
 */

let loadChart = null;

document.addEventListener("DOMContentLoaded", function () {
    initChart();
    refreshAll();

    // Event listener for single request button
    document.getElementById("btn-send-single").addEventListener("click", sendSingleRequest);

    // Event listener for batch request button
    document.getElementById("btn-send-batch").addEventListener("click", sendBatchRequests);

    // Event listener for reset simulation button
    document.getElementById("btn-confirm-reset").addEventListener("click", confirmResetSimulation);

    // Event listener for clear history button
    document.getElementById("btn-confirm-clear-history").addEventListener("click", confirmClearHistory);

    // Auto-refresh stats and servers every 5 seconds
    setInterval(function () {
        fetchServers();
        fetchStats();
    }, 5000);
});

/**
 * Refresh all dashboard components.
 */
function refreshAll() {
    fetchServers();
    fetchStats();
    fetchHistory();
}

/**
 * Quick batch selection chips.
 */
function setBatchSize(val) {
    document.getElementById("batch-count-input").value = val;
}

/**
 * Fetch and update backend server cards.
 */
function fetchServers() {
    fetch("/api/servers")
        .then(response => response.json())
        .then(servers => {
            renderServerCards(servers);
            updateChart(servers);
        })
        .catch(err => console.error("Error fetching servers:", err));
}

/**
 * Render Server A, Server B, Server C cards dynamically.
 */
function renderServerCards(servers) {
    servers.forEach(server => {
        const prefix = server.id; // "server-a", "server-b", "server-c"

        // Update status badge
        const statusBadge = document.getElementById(prefix + "-status-badge");
        const cardElem = document.getElementById(prefix + "-card");
        const btnOffline = document.getElementById(prefix + "-btn-offline");
        const btnOnline = document.getElementById(prefix + "-btn-online");

        if (server.status === "ONLINE") {
            if (statusBadge) {
                statusBadge.className = "badge bg-success";
                statusBadge.innerHTML = '<span class="status-indicator-dot dot-online"></span>ONLINE';
            }
            if (cardElem) {
                cardElem.classList.remove("server-offline");
                cardElem.classList.add("server-online");
            }
            if (btnOffline) btnOffline.disabled = false;
            if (btnOnline) btnOnline.disabled = true;
        } else {
            if (statusBadge) {
                statusBadge.className = "badge bg-danger";
                statusBadge.innerHTML = '<span class="status-indicator-dot dot-offline"></span>OFFLINE';
            }
            if (cardElem) {
                cardElem.classList.remove("server-online");
                cardElem.classList.add("server-offline");
            }
            if (btnOffline) btnOffline.disabled = true;
            if (btnOnline) btnOnline.disabled = false;
        }

        // Update request counts and percentage
        const countElem = document.getElementById(prefix + "-count");
        if (countElem) countElem.innerText = server.requestCount;

        const pctElem = document.getElementById(prefix + "-pct");
        if (pctElem) pctElem.innerText = server.percentage + "%";

        const progressBar = document.getElementById(prefix + "-progress");
        if (progressBar) {
            progressBar.style.width = server.percentage + "%";
            progressBar.setAttribute("aria-valuenow", server.percentage);
        }

        const lastActiveElem = document.getElementById(prefix + "-last-active");
        if (lastActiveElem) lastActiveElem.innerText = server.lastActiveTimestamp || "-";
    });
}

/**
 * Fetch aggregate statistics and update summary cards.
 */
function fetchStats() {
    fetch("/api/stats")
        .then(response => response.json())
        .then(stats => {
            document.getElementById("stat-total-req").innerText = stats.totalRequests;
            document.getElementById("stat-success-req").innerText = stats.successfulRequests;
            document.getElementById("stat-failed-req").innerText = stats.failedRequests;
            document.getElementById("stat-online-servers").innerText = stats.onlineServers;
            document.getElementById("stat-offline-servers").innerText = stats.offlineServers;

            // System health badge
            const healthBadge = document.getElementById("system-health-badge");
            if (healthBadge) {
                healthBadge.className = "badge px-3 py-2 ";
                if (stats.systemHealth === "HEALTHY") {
                    healthBadge.classList.add("system-badge-healthy");
                    healthBadge.innerHTML = '<i class="bi bi-shield-check me-1"></i>HEALTHY (All Online)';
                } else if (stats.systemHealth === "DEGRADED") {
                    healthBadge.classList.add("system-badge-degraded");
                    healthBadge.innerHTML = '<i class="bi bi-exclamation-triangle me-1"></i>DEGRADED (' + stats.onlineServers + '/' + stats.totalServers + ' Online)';
                } else {
                    healthBadge.classList.add("system-badge-critical");
                    healthBadge.innerHTML = '<i class="bi bi-x-octagon me-1"></i>CRITICAL (All Offline)';
                }
            }

            // Pointer label
            const pointerElem = document.getElementById("current-rotation-pointer");
            if (pointerElem) {
                pointerElem.innerText = stats.currentRotationPointer;
            }
        })
        .catch(err => console.error("Error fetching stats:", err));
}

/**
 * Fetch and render recent request history.
 */
function fetchHistory() {
    fetch("/api/requests/history?limit=50")
        .then(response => response.json())
        .then(records => {
            renderHistoryTable(records);
        })
        .catch(err => console.error("Error fetching history:", err));
}

/**
 * Render table rows for request history.
 */
function renderHistoryTable(records) {
    const tbody = document.getElementById("history-table-body");
    if (!tbody) return;

    if (!records || records.length === 0) {
        tbody.innerHTML = '<tr><td colspan="6" class="text-center text-muted py-4">No requests processed yet. Use the Simulator above to dispatch requests.</td></tr>';
        return;
    }

    let rowsHtml = "";
    records.forEach(r => {
        const isSuccess = r.result === "SUCCESS";
        const badgeClass = isSuccess ? "badge-req-success" : "badge-req-failed";
        const serverLabel = r.selectedServerName ? r.selectedServerName : '<span class="text-muted">None</span>';
        const statusBadge = isSuccess
            ? '<span class="' + badgeClass + '"><i class="bi bi-check-circle me-1"></i>200 OK</span>'
            : '<span class="' + badgeClass + '"><i class="bi bi-x-circle me-1"></i>503 Service Unavailable</span>';

        rowsHtml += `
            <tr>
                <td class="fw-bold text-dark font-monospace">${r.requestId}</td>
                <td class="text-muted small">${r.timestamp}</td>
                <td><span class="badge bg-light text-dark border">${serverLabel}</span></td>
                <td>${statusBadge}</td>
                <td class="small text-truncate" style="max-width: 220px;" title="${r.clusterStateSnapshot}">${r.clusterStateSnapshot}</td>
                <td class="small text-secondary">${r.message} <span class="badge bg-secondary-subtle text-secondary ms-1">${r.latencyMs}ms</span></td>
            </tr>
        `;
    });

    tbody.innerHTML = rowsHtml;
}

/**
 * Send a single request through the Round-Robin load balancer.
 */
function sendSingleRequest() {
    const btn = document.getElementById("btn-send-single");
    btn.disabled = true;
    btn.innerHTML = '<span class="spinner-border spinner-border-sm me-1" role="status"></span>Routing...';

    fetch("/api/requests", {
        method: "POST",
        headers: { "Content-Type": "application/json" }
    })
    .then(async response => {
        const data = await response.json();
        displayResult(data, response.ok);
        refreshAll();
    })
    .catch(err => {
        displayErrorResult("Request failed to dispatch: " + err);
    })
    .finally(() => {
        btn.disabled = false;
        btn.innerHTML = '<i class="bi bi-send-fill me-1"></i>Send Request';
    });
}

/**
 * Send a batch of multiple sequential requests.
 */
function sendBatchRequests() {
    const input = document.getElementById("batch-count-input");
    const count = parseInt(input.value, 10);

    if (isNaN(count) || count < 1 || count > 100) {
        alert("Please enter a valid batch size between 1 and 100.");
        return;
    }

    const btn = document.getElementById("btn-send-batch");
    btn.disabled = true;
    btn.innerHTML = '<span class="spinner-border spinner-border-sm me-1" role="status"></span>Processing Batch...';

    fetch("/api/requests/batch", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ count: count })
    })
    .then(async response => {
        const data = await response.json();
        displayBatchResult(data, response.ok);
        refreshAll();
    })
    .catch(err => {
        displayErrorResult("Batch request failed: " + err);
    })
    .finally(() => {
        btn.disabled = false;
        btn.innerHTML = '<i class="bi bi-layers-fill me-1"></i>Send Multiple Requests';
    });
}

/**
 * Display outcome of a single request in the result panel.
 */
function displayResult(record, isSuccessResponse) {
    const resultBox = document.getElementById("result-display-panel");
    const isSuccess = record.result === "SUCCESS";

    resultBox.className = "result-display-box " + (isSuccess ? "result-success" : "result-failure");

    resultBox.innerHTML = `
        <div class="d-flex justify-content-between align-items-center mb-1">
            <strong><i class="bi ${isSuccess ? 'bi-check-circle-fill text-success' : 'bi-exclamation-octagon-fill text-danger'} me-1"></i>${record.requestId} [HTTP ${record.httpStatusCode}]</strong>
            <span class="badge ${isSuccess ? 'bg-success' : 'bg-danger'}">${record.result}</span>
        </div>
        <div><strong>Selected Target:</strong> ${record.selectedServerName || 'None (Cluster Offline)'}</div>
        <div><strong>Diagnosis:</strong> ${record.message}</div>
        <div class="mt-1 small text-muted"><i class="bi bi-clock me-1"></i>${record.timestamp} | Simulated Latency: ${record.latencyMs}ms</div>
    `;
}

/**
 * Display outcome of a batch execution.
 */
function displayBatchResult(result, isSuccessResponse) {
    const resultBox = document.getElementById("result-display-panel");
    const allSuccessful = result.failedCount === 0;

    resultBox.className = "result-display-box " + (allSuccessful ? "result-success" : "result-failure");

    let serverDistribution = {};
    if (result.records) {
        result.records.forEach(r => {
            const name = r.selectedServerName || "None";
            serverDistribution[name] = (serverDistribution[name] || 0) + 1;
        });
    }

    let distSummary = Object.entries(serverDistribution)
        .map(([k, v]) => `<strong>${k}</strong>: ${v}`)
        .join(" | ");

    resultBox.innerHTML = `
        <div class="d-flex justify-content-between align-items-center mb-1">
            <strong><i class="bi bi-layers me-1"></i>Batch Execution Complete: ${result.requestedCount} Requests</strong>
            <span class="badge bg-primary">Success: ${result.successfulCount} | Failed: ${result.failedCount}</span>
        </div>
        <div><strong>Distribution:</strong> ${distSummary}</div>
        <div class="mt-1 small text-muted">All requests dispatched sequentially via Round-Robin load balancer.</div>
    `;
}

function displayErrorResult(message) {
    const resultBox = document.getElementById("result-display-panel");
    resultBox.className = "result-display-box result-failure";
    resultBox.innerHTML = `<strong><i class="bi bi-x-circle-fill text-danger me-1"></i>Error:</strong> ${message}`;
}

/**
 * Toggle a server offline or online.
 */
function toggleServer(serverId, targetStatus) {
    const endpoint = `/api/servers/${serverId}/${targetStatus}`;
    fetch(endpoint, { method: "POST" })
        .then(response => {
            if (!response.ok) {
                throw new Error("Failed to change server status.");
            }
            return response.json();
        })
        .then(server => {
            refreshAll();
        })
        .catch(err => {
            alert("Error updating server status: " + err.message);
        });
}

/**
 * Confirm and trigger simulation reset.
 */
function confirmResetSimulation() {
    fetch("/api/reset", { method: "POST" })
        .then(response => response.json())
        .then(data => {
            const modalElem = document.getElementById("resetModal");
            const modal = bootstrap.Modal.getInstance(modalElem);
            if (modal) modal.hide();

            // Clear result box
            const resultBox = document.getElementById("result-display-panel");
            resultBox.className = "result-display-box";
            resultBox.innerHTML = '<span class="text-muted"><i class="bi bi-info-circle me-1"></i>Simulation reset. Counters cleared and all servers restored online.</span>';

            refreshAll();
        })
        .catch(err => alert("Error resetting simulation: " + err));
}

/**
 * Confirm and trigger clear history.
 */
function confirmClearHistory() {
    fetch("/api/requests/history/clear", { method: "POST" })
        .then(response => response.json())
        .then(data => {
            const modalElem = document.getElementById("clearHistoryModal");
            const modal = bootstrap.Modal.getInstance(modalElem);
            if (modal) modal.hide();

            fetchHistory();
        })
        .catch(err => alert("Error clearing history: " + err));
}

/**
 * Initialize Chart.js visualization.
 */
function initChart() {
    const ctx = document.getElementById("loadChart");
    if (!ctx) return;

    if (typeof Chart === "undefined") {
        console.warn("Chart.js not loaded. Falling back to progress bars.");
        return;
    }

    loadChart = new Chart(ctx, {
        type: 'bar',
        data: {
            labels: ['Server A', 'Server B', 'Server C'],
            datasets: [{
                label: 'Requests Handled',
                data: [0, 0, 0],
                backgroundColor: [
                    'rgba(37, 99, 235, 0.75)',
                    'rgba(16, 185, 129, 0.75)',
                    'rgba(14, 165, 233, 0.75)'
                ],
                borderColor: [
                    'rgb(37, 99, 235)',
                    'rgb(16, 185, 129)',
                    'rgb(14, 165, 233)'
                ],
                borderWidth: 2,
                borderRadius: 8
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            scales: {
                y: {
                    beginAtZero: true,
                    ticks: {
                        stepSize: 1,
                        precision: 0
                    },
                    grid: {
                        color: 'rgba(226, 232, 240, 0.6)'
                    }
                },
                x: {
                    grid: {
                        display: false
                    }
                }
            },
            plugins: {
                legend: {
                    display: false
                },
                tooltip: {
                    callbacks: {
                        label: function (context) {
                            return ` Handled: ${context.raw} requests`;
                        }
                    }
                }
            }
        }
    });
}

/**
 * Update Chart.js dataset with fresh server numbers.
 */
function updateChart(servers) {
    if (!loadChart) return;

    const counts = servers.map(s => s.requestCount);
    loadChart.data.datasets[0].data = counts;
    loadChart.update();
}
