<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.pawnshop.model.Account" %>
<%
    Account currentAccount = (Account) session.getAttribute("account");
    if (currentAccount == null || currentAccount.getRole() != Account.Role.ADMIN) {
        response.sendRedirect(request.getContextPath() + "/login.jsp?error=Access denied");
        return;
    }

    String action = request.getParameter("action");
    String employeeId = request.getParameter("id");
    boolean isCreateMode = "create".equals(action);
    boolean isEditMode = "edit".equals(action) && employeeId != null;
%>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Employee Management - Pawnshop</title>
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
            transition: all 0.3s;
        }

        .btn-back:hover {
            background: rgba(255, 255, 255, 0.3);
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

        .card {
            background: white;
            border-radius: 12px;
            padding: 32px;
            box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
            margin-bottom: 24px;
        }

        .form-grid {
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
            gap: 20px;
        }

        .form-group {
            margin-bottom: 20px;
        }

        .form-group label {
            display: block;
            color: #333;
            font-weight: 600;
            margin-bottom: 8px;
            font-size: 14px;
        }

        .form-group input,
        .form-group select {
            width: 100%;
            padding: 12px 16px;
            border: 2px solid #e1e8ed;
            border-radius: 8px;
            font-size: 14px;
            transition: all 0.3s;
        }

        .form-group input:focus,
        .form-group select:focus {
            outline: none;
            border-color: #667eea;
            box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.1);
        }

        .form-actions {
            display: flex;
            gap: 12px;
            margin-top: 24px;
        }

        .btn {
            padding: 12px 24px;
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

        .btn-primary:hover {
            transform: translateY(-2px);
            box-shadow: 0 4px 12px rgba(102, 126, 234, 0.4);
        }

        .btn-secondary {
            background: #f0f0f0;
            color: #333;
        }

        .btn-secondary:hover {
            background: #e0e0e0;
        }

        .btn-success {
            background: #28a745;
            color: white;
        }

        .btn-success:hover {
            background: #218838;
        }

        .btn-danger {
            background: #dc3545;
            color: white;
        }

        .btn-danger:hover {
            background: #c82333;
        }

        .btn-warning {
            background: #ffc107;
            color: #333;
        }

        .btn-warning:hover {
            background: #e0a800;
        }

        .btn-sm {
            padding: 6px 12px;
            font-size: 12px;
        }

        .alert {
            padding: 14px 18px;
            border-radius: 8px;
            margin-bottom: 20px;
            font-size: 14px;
        }

        .alert-success {
            background-color: #d4edda;
            color: #155724;
            border: 1px solid #c3e6cb;
        }

        .alert-error {
            background-color: #f8d7da;
            color: #721c24;
            border: 1px solid #f5c6cb;
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

        .loading {
            text-align: center;
            padding: 40px;
            color: #666;
        }

        .action-buttons {
            display: flex;
            gap: 6px;
        }

        .required {
            color: #dc3545;
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
        <h1>👨‍💼 Employee Management</h1>
        <% if (!isCreateMode && !isEditMode) { %>
        <button onclick="showCreateForm()" class="btn btn-primary">
            ➕ Add New Employee
        </button>
        <% } %>
    </div>

    <% if (isCreateMode || isEditMode) { %>
    <!-- Create/Edit Form -->
    <div class="card">
        <h2 style="margin-bottom: 24px; color: #333;">
            <%= isEditMode ? "Edit Employee" : "Add New Employee" %>
        </h2>

        <div id="message"></div>

        <form id="employeeForm">
            <% if (isCreateMode) { %>
            <!-- Account Information (Only for new employees) -->
            <h3 style="margin-bottom: 16px; color: #667eea;">Account Information</h3>
            <div class="form-grid">
                <div class="form-group">
                    <label for="username">Username <span class="required">*</span></label>
                    <input type="text" id="username" name="username" required>
                </div>

                <div class="form-group">
                    <label for="password">Password <span class="required">*</span></label>
                    <input type="password" id="password" name="password" required minlength="6">
                </div>
            </div>
            <% } %>

            <!-- Personal Information -->
            <h3 style="margin: 24px 0 16px; color: #667eea;">Personal Information</h3>
            <div class="form-grid">
                <div class="form-group">
                    <label for="fullName">Full Name <span class="required">*</span></label>
                    <input type="text" id="fullName" name="fullName" required maxlength="100">
                </div>

                <div class="form-group">
                    <label for="citizenNumber">Citizen Number</label>
                    <input type="text" id="citizenNumber" name="citizenNumber" maxlength="20">
                </div>

                <div class="form-group">
                    <label for="dob">Date of Birth</label>
                    <input type="date" id="dob" name="dob">
                </div>

                <div class="form-group">
                    <label for="phoneNumber">Phone Number</label>
                    <input type="tel" id="phoneNumber" name="phoneNumber" placeholder="+84 123 456 789">
                </div>

                <div class="form-group">
                    <label for="email">Email</label>
                    <input type="email" id="email" name="email">
                </div>
            </div>

            <!-- Employment Information -->
            <h3 style="margin: 24px 0 16px; color: #667eea;">Employment Information</h3>
            <div class="form-grid">
                <div class="form-group">
                    <label for="hireDate">Hire Date</label>
                    <input type="date" id="hireDate" name="hireDate">
                </div>

                <div class="form-group">
                    <label for="salary">Salary (VND)</label>
                    <input type="number" id="salary" name="salary" step="0.01" min="0">
                </div>
            </div>

            <div class="form-actions">
                <button type="submit" class="btn btn-primary" id="submitBtn">
                    <%= isEditMode ? "Update Employee" : "Create Employee" %>
                </button>
                <button type="button" class="btn btn-secondary" onclick="cancelForm()">
                    Cancel
                </button>
            </div>
        </form>
    </div>

    <script>
        <% if (isEditMode) { %>
        // Load employee data for editing
        loadEmployeeData(<%= employeeId %>);
        <% } %>

        document.getElementById('employeeForm').addEventListener('submit', async function(e) {
            e.preventDefault();

            const formData = {
                fullName: document.getElementById('fullName').value,
                citizenNumber: document.getElementById('citizenNumber').value || null,
                dob: document.getElementById('dob').value || null,
                phoneNumber: document.getElementById('phoneNumber').value || null,
                email: document.getElementById('email').value || null,
                hireDate: document.getElementById('hireDate').value || null,
                salary: document.getElementById('salary').value || null
            };

            <% if (isCreateMode) { %>
            // Add account credentials for new employee
            formData.username = document.getElementById('username').value;
            formData.password = document.getElementById('password').value;
            <% } %>

            const submitBtn = document.getElementById('submitBtn');
            submitBtn.disabled = true;
            submitBtn.textContent = '<%= isEditMode ? "Updating..." : "Creating..." %>';

            try {
                const url = '<%= isEditMode
                        ? request.getContextPath() + "/api/employees/" + employeeId
                        : request.getContextPath() + "/api/employees/with-account" %>';

                const method = '<%= isEditMode ? "PUT" : "POST" %>';

                const response = await fetch(url, {
                    method: method,
                    headers: {
                        'Content-Type': 'application/json'
                    },
                    body: JSON.stringify(formData)
                });

                const result = await response.json();

                if (result.success) {
                    showMessage('<%= isEditMode ? "Employee updated" : "Employee created" %> successfully!', 'success');
                    setTimeout(() => {
                        location.href = 'employees.jsp';
                    }, 1500);
                } else {
                    showMessage(result.error || 'Operation failed', 'error');
                    submitBtn.disabled = false;
                    submitBtn.textContent = '<%= isEditMode ? "Update Employee" : "Create Employee" %>';
                }
            } catch (error) {
                showMessage('Network error occurred', 'error');
                submitBtn.disabled = false;
                submitBtn.textContent = '<%= isEditMode ? "Update Employee" : "Create Employee" %>';
            }
        });

        <% if (isEditMode) { %>
        async function loadEmployeeData(employeeId) {
            try {
                const response = await fetch('${pageContext.request.contextPath}/api/employees/' + employeeId);
                const result = await response.json();

                if (result.success && result.data) {
                    const emp = result.data;
                    document.getElementById('fullName').value = emp.fullName || '';
                    document.getElementById('citizenNumber').value = emp.citizenNumber || '';
                    document.getElementById('dob').value = emp.dob || '';
                    document.getElementById('phoneNumber').value = emp.phoneNumber || '';
                    document.getElementById('email').value = emp.email || '';
                    document.getElementById('hireDate').value = emp.hireDate || '';
                    document.getElementById('salary').value = emp.salary || '';
                }
            } catch (error) {
                showMessage('Failed to load employee data', 'error');
            }
        }
        <% } %>

        function showMessage(message, type) {
            const messageDiv = document.getElementById('message');
            messageDiv.innerHTML = `<div class="alert alert-${type}">${message}</div>`;
        }

        function cancelForm() {
            location.href = 'employees.jsp';
        }
    </script>

    <% } else { %>
    <!-- Employee List -->
    <div class="toolbar">
        <div class="search-box">
            <input type="text" id="searchInput" placeholder="Search by name, phone, email..."
                   onkeyup="handleSearch()">
            <span class="search-icon">🔍</span>
        </div>
        <button class="btn btn-secondary" onclick="loadEmployees()">
            🔄 Refresh
        </button>
    </div>

    <div class="card">
        <div id="employeeTable">
            <div class="loading">Loading employees...</div>
        </div>
    </div>

    <script>
        let allEmployees = [];

        async function loadEmployees() {
            try {
                const response = await fetch('${pageContext.request.contextPath}/api/employees/');
                const result = await response.json();

                if (result.success && result.data) {
                    allEmployees = result.data;
                    displayEmployees(allEmployees);
                } else {
                    document.getElementById('employeeTable').innerHTML =
                        '<div class="alert alert-error">Failed to load employees</div>';
                }
            } catch (error) {
                document.getElementById('employeeTable').innerHTML =
                    '<div class="alert alert-error">Network error occurred</div>';
            }
        }

        function displayEmployees(employees) {
            if (employees.length === 0) {
                document.getElementById('employeeTable').innerHTML =
                    '<p style="text-align: center; color: #666;">No employees found</p>';
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
                                <th>Hire Date</th>
                                <th>Salary</th>
                                <th>Status</th>
                                <th>Actions</th>
                            </tr>
                        </thead>
                        <tbody>
                `;

            employees.forEach(emp => {
                const hireDate = emp.hireDate ? new Date(emp.hireDate).toLocaleDateString() : '-';
                const salary = emp.salary ? new Intl.NumberFormat('vi-VN').format(emp.salary) + ' VND' : '-';
                const statusBadge = emp.active
                    ? '<span class="badge badge-active">Active</span>'
                    : '<span class="badge badge-inactive">Inactive</span>';

                html += `
                        <tr>
                            <td>${emp.employeeId}</td>
                            <td><strong>${emp.fullName}</strong></td>
                            <td>${emp.username || '-'}</td>
                            <td>${emp.phoneNumber || '-'}</td>
                            <td>${emp.email || '-'}</td>
                            <td>${hireDate}</td>
                            <td>${salary}</td>
                            <td>${statusBadge}</td>
                            <td>
                                <div class="action-buttons">
                                    <button class="btn btn-warning btn-sm" onclick="editEmployee(${emp.employeeId})">
                                        ✏️ Edit
                                    </button>
                                    ${emp.active
                                        ? `<button class="btn btn-danger btn-sm" onclick="deactivateEmployee(${emp.employeeId})">🚫 Deactivate</button>`
                                        : `<button class="btn btn-success btn-sm" onclick="activateEmployee(${emp.employeeId})">✅ Activate</button>`
                                    }
                                </div>
                            </td>
                        </tr>
                    `;
            });

            html += `
                        </tbody>
                    </table>
                `;

            document.getElementById('employeeTable').innerHTML = html;
        }

        function handleSearch() {
            const keyword = document.getElementById('searchInput').value.toLowerCase();

            if (!keyword) {
                displayEmployees(allEmployees);
                return;
            }

            const filtered = allEmployees.filter(emp =>
                emp.fullName.toLowerCase().includes(keyword) ||
                (emp.phoneNumber && emp.phoneNumber.includes(keyword)) ||
                (emp.email && emp.email.toLowerCase().includes(keyword)) ||
                (emp.citizenNumber && emp.citizenNumber.includes(keyword))
            );

            displayEmployees(filtered);
        }

        function showCreateForm() {
            location.href = 'employees.jsp?action=create';
        }

        function editEmployee(employeeId) {
            location.href = 'employees.jsp?action=edit&id=' + employeeId;
        }

        async function deactivateEmployee(employeeId) {
            if (!confirm('Are you sure you want to deactivate this employee?')) {
                return;
            }

            try {
                const response = await fetch(
                    '${pageContext.request.contextPath}/api/employees/deactivate?id=' + employeeId,
                    { method: 'PUT' }
                );

                const result = await response.json();

                if (result.success) {
                    alert('Employee deactivated successfully');
                    loadEmployees();
                } else {
                    alert('Failed: ' + result.error);
                }
            } catch (error) {
                alert('Network error occurred');
            }
        }

        async function activateEmployee(employeeId) {
            if (!confirm('Are you sure you want to activate this employee?')) {
                return;
            }

            try {
                const response = await fetch(
                    '${pageContext.request.contextPath}/api/employees/activate?id=' + employeeId,
                    { method: 'PUT' }
                );

                const result = await response.json();

                if (result.success) {
                    alert('Employee activated successfully');
                    loadEmployees();
                } else {
                    alert('Failed: ' + result.error);
                }
            } catch (error) {
                alert('Network error occurred');
            }
        }

        // Load employees on page load
        window.addEventListener('DOMContentLoaded', loadEmployees);
    </script>
    <% } %>
</div>
</body>
</html>