<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.pawnshop.model.Account" %>
<%
    Account currentAccount = (Account) session.getAttribute("account");
    if (currentAccount == null) {
        response.sendRedirect(request.getContextPath() + "/login.jsp?error=Please login");
        return;
    }

    boolean isAdmin = currentAccount.getRole() == Account.Role.ADMIN;
    boolean isEmployee = currentAccount.getRole() == Account.Role.EMPLOYEE;

    if (!isAdmin && !isEmployee) {
        response.sendRedirect(request.getContextPath() + "/dashboard.jsp?error=Access denied");
        return;
    }
%>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Customer Management - Pawnshop</title>
    <style>
        * {
            margin: 0;
            padding: 0;
            box-sizing: border-box;
        }

        body {
            font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
            background-color: #f5f7fa;
        }

        .navbar {
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            color: white;
            padding: 16px 24px;
            display: flex;
            justify-content: space-between;
            align-items: center;
            box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
        }

        .navbar-brand {
            font-size: 22px;
            font-weight: 700;
            text-decoration: none;
            color: white;
        }

        .btn-back {
            background: rgba(255, 255, 255, 0.2);
            color: white;
            border: 1px solid rgba(255, 255, 255, 0.3);
            padding: 8px 16px;
            border-radius: 6px;
            text-decoration: none;
            font-size: 14px;
        }

        .container {
            max-width: 1400px;
            margin: 0 auto;
            padding: 32px 24px;
        }

        .page-header {
            display: flex;
            justify-content: space-between;
            align-items: center;
            margin-bottom: 32px;
        }

        .page-header h1 {
            font-size: 32px;
            color: #333;
        }

        .toolbar {
            display: flex;
            gap: 12px;
            margin-bottom: 24px;
            flex-wrap: wrap;
        }

        .search-box {
            flex: 1;
            max-width: 400px;
            position: relative;
        }

        .search-box input {
            width: 100%;
            padding: 10px 40px 10px 16px;
            border: 2px solid #e1e8ed;
            border-radius: 8px;
            font-size: 14px;
        }

        .search-box input:focus {
            outline: none;
            border-color: #667eea;
        }

        .search-icon {
            position: absolute;
            right: 12px;
            top: 50%;
            transform: translateY(-50%);
            color: #666;
        }

        .filter-buttons {
            display: flex;
            gap: 8px;
        }

        .btn {
            padding: 10px 20px;
            border-radius: 8px;
            border: none;
            font-size: 14px;
            font-weight: 600;
            cursor: pointer;
            transition: all 0.3s;
        }

        .btn-primary {
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            color: white;
        }

        .btn-secondary {
            background: #f0f0f0;
            color: #333;
        }

        .btn-sm {
            padding: 6px 12px;
            font-size: 12px;
        }

        .btn-info {
            background: #17a2b8;
            color: white;
        }

        .card {
            background: white;
            border-radius: 12px;
            padding: 24px;
            box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
            margin-bottom: 24px;
        }

        .stats-grid {
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
            gap: 20px;
            margin-bottom: 24px;
        }

        .stat-card {
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            color: white;
            padding: 20px;
            border-radius: 12px;
            box-shadow: 0 4px 12px rgba(102, 126, 234, 0.3);
        }

        .stat-card h3 {
            font-size: 14px;
            opacity: 0.9;
            margin-bottom: 8px;
        }

        .stat-card .value {
            font-size: 32px;
            font-weight: 700;
        }

        table {
            width: 100%;
            border-collapse: collapse;
        }

        th, td {
            padding: 14px;
            text-align: left;
            border-bottom: 1px solid #e1e8ed;
        }

        th {
            background-color: #f8f9fa;
            font-weight: 600;
            color: #333;
            font-size: 14px;
        }

        td {
            font-size: 14px;
            color: #555;
        }

        tr:hover {
            background-color: #f8f9fa;
        }

        .badge {
            padding: 4px 12px;
            border-radius: 12px;
            font-size: 12px;
            font-weight: 600;
        }

        .badge-active {
            background-color: #d4edda;
            color: #155724;
        }

        .badge-inactive {
            background-color: #f8d7da;
            color: #721c24;
        }

        .badge-contracts {
            background-color: #cce5ff;
            color: #004085;
        }

        .loading {
            text-align: center;
            padding: 40px;
            color: #666;
        }

        .modal {
            display: none;
            position: fixed;
            top: 0;
            left: 0;
            width: 100%;
            height: 100%;
            background: rgba(0, 0, 0, 0.5);
            z-index: 1000;
            align-items: center;
            justify-content: center;
        }

        .modal.show {
            display: flex;
        }

        .modal-content {
            background: white;
            border-radius: 12px;
            padding: 32px;
            max-width: 600px;
            width: 90%;
            max-height: 80vh;
            overflow-y: auto;
        }

        .modal-header {
            display: flex;
            justify-content: space-between;
            align-items: center;
            margin-bottom: 24px;
        }

        .modal-header h2 {
            color: #333;
            font-size: 24px;
        }

        .close-btn {
            background: none;
            border: none;
            font-size: 24px;
            cursor: pointer;
            color: #666;
        }

        .info-grid {
            display: grid;
            grid-template-columns: 140px 1fr;
            gap: 16px;
        }

        .info-label {
            font-weight: 600;
            color: #666;
        }

        .info-value {
            color: #333;
        }
    </style>
</head>
<body>
<nav class="navbar">
    <a href="${pageContext.request.contextPath}/dashboard.jsp" class="navbar-brand">
        🏦 Pawnshop Management
    </a>
    <a href="${pageContext.request.contextPath}/dashboard.jsp" class="btn-back">
        ← Back to Dashboard
    </a>
</nav>

<div class="container">
    <div class="page-header">
        <h1>👥 Customer Management</h1>
    </div>

    <div class="stats-grid" id="statsGrid">
        <div class="stat-card">
            <h3>Total Customers</h3>
            <div class="value" id="totalCustomers">0</div>
        </div>
        <div class="stat-card">
            <h3>Active Customers</h3>
            <div class="value" id="activeCustomers">0</div>
        </div>
        <div class="stat-card">
            <h3>New This Month</h3>
            <div class="value" id="newCustomers">0</div>
        </div>
    </div>

    <div class="toolbar">
        <div class="search-box">
            <input type="text" id="searchInput" placeholder="Search by name, phone, email, citizen number..."
                   onkeyup="handleSearch()">
            <span class="search-icon">🔍</span>
        </div>
        <div class="filter-buttons">
            <button class="btn btn-secondary" onclick="filterCustomers('all')" id="btnAll">
                All
            </button>
            <button class="btn btn-secondary" onclick="filterCustomers('active')" id="btnActive">
                Active Only
            </button>
            <button class="btn btn-secondary" onclick="loadCustomers()">
                🔄 Refresh
            </button>
        </div>
    </div>

    <div class="card">
        <div id="customerTable">
            <div class="loading">Loading customers...</div>
        </div>
    </div>
</div>

<!-- Customer Detail Modal -->
<div id="customerModal" class="modal">
    <div class="modal-content">
        <div class="modal-header">
            <h2>Customer Details</h2>
            <button class="close-btn" onclick="closeModal()">&times;</button>
        </div>
        <div id="customerDetails"></div>
    </div>
</div>

<script>
    let allCustomers = [];
    let currentFilter = 'all';

    async function loadCustomers() {
        try {
            const url = currentFilter === 'active'
                ? '${pageContext.request.contextPath}/api/customers/?active=true'
                : '${pageContext.request.contextPath}/api/customers/';

            const response = await fetch(url);
            const result = await response.json();

            if (result.success && result.data) {
                allCustomers = result.data;
                displayCustomers(allCustomers);
                updateStats(allCustomers);
            } else {
                document.getElementById('customerTable').innerHTML =
                    '<div class="alert alert-error">Failed to load customers</div>';
            }
        } catch (error) {
            document.getElementById('customerTable').innerHTML =
                '<div class="alert alert-error">Network error occurred</div>';
        }
    }

    function displayCustomers(customers) {
        if (customers.length === 0) {
            document.getElementById('customerTable').innerHTML =
                '<p style="text-align: center; color: #666;">No customers found</p>';
            return;
        }

        let html = `
                <table>
                    <thead>
                        <tr>
                            <th>ID</th>
                            <th>Full Name</th>
                            <th>Username</th>
                            <th>Phone</th>
                            <th>Email</th>
                            <th>Citizen Number</th>
                            <th>Contracts</th>
                            <th>Status</th>
                            <th>Actions</th>
                        </tr>
                    </thead>
                    <tbody>
            `;

        customers.forEach(cust => {
            const statusBadge = cust.active
                ? '<span class="badge badge-active">Active</span>'
                : '<span class="badge badge-inactive">Inactive</span>';

            const contractBadge = cust.contractCount > 0
                ? `<span class="badge badge-contracts">${cust.contractCount} contracts</span>`
                : '-';

            html += `
                    <tr>
                        <td>${cust.customerId}</td>
                        <td><strong>${cust.fullName}</strong></td>
                        <td>${cust.username || '-'}</td>
                        <td>${cust.phoneNumber || '-'}</td>
                        <td>${cust.email || '-'}</td>
                        <td>${cust.citizenNumber || '-'}</td>
                        <td>${contractBadge}</td>
                        <td>${statusBadge}</td>
                        <td>
                            <button class="btn btn-info btn-sm" onclick="viewCustomer(${cust.customerId})">
                                👁️ View
                            </button>
                        </td>
                    </tr>
                `;
        });

        html += `
                    </tbody>
                </table>
            `;

        document.getElementById('customerTable').innerHTML = html;
    }

    function updateStats(customers) {
        const total = customers.length;
        const active = customers.filter(c => c.active).length;

        const now = new Date();
        const firstDayOfMonth = new Date(now.getFullYear(), now.getMonth(), 1);
        const newThisMonth = customers.filter(c => {
            const createdDate = new Date(c.createdAt);
            return createdDate >= firstDayOfMonth;
        }).length;

        document.getElementById('totalCustomers').textContent = total;
        document.getElementById('activeCustomers').textContent = active;
        document.getElementById('newCustomers').textContent = newThisMonth;
    }

    function handleSearch() {
        const keyword = document.getElementById('searchInput').value.toLowerCase();

        if (!keyword) {
            displayCustomers(allCustomers);
            return;
        }

        const filtered = allCustomers.filter(cust =>
            cust.fullName.toLowerCase().includes(keyword) ||
            (cust.phoneNumber && cust.phoneNumber.includes(keyword)) ||
            (cust.email && cust.email.toLowerCase().includes(keyword)) ||
            (cust.citizenNumber && cust.citizenNumber.includes(keyword)) ||
            (cust.address && cust.address.toLowerCase().includes(keyword))
        );

        displayCustomers(filtered);
    }

    function filterCustomers(filter) {
        currentFilter = filter;

        // Update button states
        document.getElementById('btnAll').className = filter === 'all' ? 'btn btn-primary' : 'btn btn-secondary';
        document.getElementById('btnActive').className = filter === 'active' ? 'btn btn-primary' : 'btn btn-secondary';

        loadCustomers();
    }

    async function viewCustomer(customerId) {
        try {
            const response = await fetch('${pageContext.request.contextPath}/api/customers/' + customerId);
            const result = await response.json();

            if (result.success && result.data) {
                displayCustomerDetails(result.data);
                document.getElementById('customerModal').classList.add('show');
            } else {
                alert('Failed to load customer details');
            }
        } catch (error) {
            alert('Network error occurred');
        }
    }

    function displayCustomerDetails(customer) {
        const statusBadge = customer.active
            ? '<span class="badge badge-active">Active</span>'
            : '<span class="badge badge-inactive">Inactive</span>';

        const dob = customer.dob ? new Date(customer.dob).toLocaleDateString() : 'Not provided';
        const createdAt = new Date(customer.createdAt).toLocaleString();

        const html = `
                <div class="info-grid">
                    <div class="info-label">Customer ID:</div>
                    <div class="info-value"><strong>#${customer.customerId}</strong></div>

                    <div class="info-label">Full Name:</div>
                    <div class="info-value"><strong>${customer.fullName}</strong></div>

                    <div class="info-label">Username:</div>
                    <div class="info-value">${customer.username || 'N/A'}</div>

                    <div class="info-label">Account Status:</div>
                    <div class="info-value">${statusBadge}</div>

                    <div class="info-label">Citizen Number:</div>
                    <div class="info-value">${customer.citizenNumber || 'Not provided'}</div>

                    <div class="info-label">Phone Number:</div>
                    <div class="info-value">${customer.phoneNumber || 'Not provided'}</div>

                    <div class="info-label">Email:</div>
                    <div class="info-value">${customer.email || 'Not provided'}</div>

                    <div class="info-label">Address:</div>
                    <div class="info-value">${customer.address || 'Not provided'}</div>

                    <div class="info-label">Date of Birth:</div>
                    <div class="info-value">${dob}</div>

                    <div class="info-label">Total Contracts:</div>
                    <div class="info-value"><span class="badge badge-contracts">${customer.contractCount || 0} contracts</span></div>

                    <div class="info-label">Registered:</div>
                    <div class="info-value">${createdAt}</div>
                </div>
            `;

        document.getElementById('customerDetails').innerHTML = html;
    }

    function closeModal() {
        document.getElementById('customerModal').classList.remove('show');
    }

    // Close modal when clicking outside
    window.onclick = function(event) {
        const modal = document.getElementById('customerModal');
        if (event.target === modal) {
            closeModal();
        }
    }

    // Load customers on page load
    window.addEventListener('DOMContentLoaded', loadCustomers);
</script>
</body>
</html>