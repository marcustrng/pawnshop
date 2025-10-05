<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.pawnshop.model.Account" %>
<%@ page import="java.util.List" %>
<%
    Account currentAccount = (Account) session.getAttribute("account");
    if (currentAccount == null || currentAccount.getRole() != Account.Role.ADMIN) {
        response.sendRedirect(request.getContextPath() + "/login.jsp?error=Access denied");
        return;
    }

    String action = request.getParameter("action");
    boolean isRegisterMode = "register".equals(action);
%>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>User Accounts - Pawnshop Management</title>
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

        .navbar-user {
            display: flex;
            align-items: center;
            gap: 20px;
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
            max-width: 1200px;
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

        .card {
            background: white;
            border-radius: 12px;
            padding: 32px;
            box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
            margin-bottom: 24px;
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

        .form-row {
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
            gap: 20px;
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
            margin-left: 12px;
        }

        .btn-secondary:hover {
            background: #e0e0e0;
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
            text-transform: capitalize;
        }

        .badge-admin {
            background-color: #e7f3ff;
            color: #0066cc;
        }

        .badge-employee {
            background-color: #fff3e0;
            color: #e65100;
        }

        .badge-customer {
            background-color: #f3e5f5;
            color: #6a1b9a;
        }

        .badge-active {
            background-color: #d4edda;
            color: #155724;
        }

        .badge-inactive {
            background-color: #f8d7da;
            color: #721c24;
        }

        .action-btn {
            padding: 6px 12px;
            margin-right: 6px;
            font-size: 12px;
            border-radius: 6px;
            cursor: pointer;
        }

        .btn-edit {
            background-color: #fff3e0;
            color: #e65100;
            border: 1px solid #ffcc80;
        }

        .btn-deactivate {
            background-color: #ffebee;
            color: #c62828;
            border: 1px solid #ef9a9a;
        }

        .btn-activate {
            background-color: #e8f5e9;
            color: #2e7d32;
            border: 1px solid #a5d6a7;
        }

        .loading {
            text-align: center;
            padding: 40px;
            color: #666;
        }
    </style>
</head>
<body>
<nav class="navbar">
    <a href="${pageContext.request.contextPath}/dashboard.jsp" class="navbar-brand">
        🏦 Pawnshop Management
    </a>
    <div class="navbar-user">
        <a href="${pageContext.request.contextPath}/dashboard.jsp" class="btn-back">
            ← Back to Dashboard
        </a>
    </div>
</nav>

<div class="container">
    <div class="page-header">
        <h1>User Account Management</h1>
        <% if (!isRegisterMode) { %>
        <button onclick="location.href='?action=register'" class="btn btn-primary">
            ➕ Register New User
        </button>
        <% } %>
    </div>

    <% if (isRegisterMode) { %>
    <!-- Registration Form -->
    <div class="card">
        <h2 style="margin-bottom: 24px; color: #333;">Register New User Account</h2>

        <div id="message"></div>

        <form id="registerForm">
            <div class="form-row">
                <div class="form-group">
                    <label for="username">Username *</label>
                    <input type="text" id="username" name="username" required
                           minlength="3" maxlength="50" placeholder="Enter username">
                </div>

                <div class="form-group">
                    <label for="role">Role *</label>
                    <select id="role" name="role" required>
                        <option value="">Select role...</option>
                        <option value="admin">Admin</option>
                        <option value="employee">Employee</option>
                        <option value="customer">Customer</option>
                    </select>
                </div>
            </div>

            <div class="form-row">
                <div class="form-group">
                    <label for="password">Password *</label>
                    <input type="password" id="password" name="password" required
                           minlength="6" placeholder="Minimum 6 characters">
                </div>

                <div class="form-group">
                    <label for="confirmPassword">Confirm Password *</label>
                    <input type="password" id="confirmPassword" name="confirmPassword" required
                           placeholder="Re-enter password">
                </div>
            </div>

            <div style="margin-top: 24px;">
                <button type="submit" class="btn btn-primary" id="submitBtn">
                    Register Account
                </button>
                <button type="button" class="btn btn-secondary"
                        onclick="location.href='accounts.jsp'">
                    Cancel
                </button>
            </div>
        </form>
    </div>

    <script>
        document.getElementById('registerForm').addEventListener('submit', async function(e) {
            e.preventDefault();

            const password = document.getElementById('password').value;
            const confirmPassword = document.getElementById('confirmPassword').value;

            if (password !== confirmPassword) {
                showMessage('Passwords do not match', 'error');
                return;
            }

            const formData = {
                username: document.getElementById('username').value,
                password: password,
                role: document.getElementById('role').value
            };

            const submitBtn = document.getElementById('submitBtn');
            submitBtn.disabled = true;
            submitBtn.textContent = 'Registering...';

            try {
                const response = await fetch('${pageContext.request.contextPath}/api/accounts/register', {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json'
                    },
                    body: JSON.stringify(formData)
                });

                const result = await response.json();

                if (result.success) {
                    showMessage('Account registered successfully!', 'success');
                    setTimeout(() => {
                        location.href = 'accounts.jsp';
                    }, 1500);
                } else {
                    showMessage(result.error || 'Registration failed', 'error');
                    submitBtn.disabled = false;
                    submitBtn.textContent = 'Register Account';
                }
            } catch (error) {
                showMessage('Network error occurred', 'error');
                submitBtn.disabled = false;
                submitBtn.textContent = 'Register Account';
            }
        });

        function showMessage(message, type) {
            const messageDiv = document.getElementById('message');
            messageDiv.innerHTML = `<div class="alert alert-${type}">${message}</div>`;
        }
    </script>

    <% } else { %>
    <!-- Accounts List -->
    <div class="card">
        <h2 style="margin-bottom: 24px; color: #333;">All User Accounts</h2>

        <div id="accountsTable">
            <div class="loading">Loading accounts...</div>
        </div>
    </div>

    <script>
        async function loadAccounts() {
            try {
                const response = await fetch('${pageContext.request.contextPath}/api/accounts/all');
                const result = await response.json();

                if (result.success && result.data) {
                    displayAccounts(result.data);
                } else {
                    document.getElementById('accountsTable').innerHTML =
                        '<div class="alert alert-error">Failed to load accounts</div>';
                }
            } catch (error) {
                document.getElementById('accountsTable').innerHTML =
                    '<div class="alert alert-error">Network error occurred</div>';
            }
        }

        function displayAccounts(accounts) {
            if (accounts.length === 0) {
                document.getElementById('accountsTable').innerHTML =
                    '<p style="text-align: center; color: #666;">No accounts found</p>';
                return;
            }

            let html = `
                    <table>
                        <thead>
                            <tr>
                                <th>ID</th>
                                <th>Username</th>
                                <th>Role</th>
                                <th>Status</th>
                                <th>Created At</th>
                                <th>Actions</th>
                            </tr>
                        </thead>
                        <tbody>
                `;

            accounts.forEach(account => {
                const createdDate = new Date(account.createdAt).toLocaleDateString();
                const statusBadge = account.active
                    ? '<span class="badge badge-active">Active</span>'
                    : '<span class="badge badge-inactive">Inactive</span>';

                const roleBadge = `<span class="badge badge-${account.role}">${account.role}</span>`;

                const actionButtons = account.active
                    ? `<button class="action-btn btn-deactivate" onclick="deactivateAccount(${account.accountId})">Deactivate</button>`
                    : `<button class="action-btn btn-activate" onclick="activateAccount(${account.accountId})">Activate</button>`;

                html += `
                        <tr>
                            <td>${account.accountId}</td>
                            <td><strong>${account.username}</strong></td>
                            <td>${roleBadge}</td>
                            <td>${statusBadge}</td>
                            <td>${createdDate}</td>
                            <td>${actionButtons}</td>
                        </tr>
                    `;
            });

            html += `
                        </tbody>
                    </table>
                `;

            document.getElementById('accountsTable').innerHTML = html;
        }

        async function deactivateAccount(accountId) {
            if (!confirm('Are you sure you want to deactivate this account?')) {
                return;
            }

            try {
                const response = await fetch(
                    '${pageContext.request.contextPath}/api/accounts/deactivate?id=' + accountId,
                    { method: 'PUT' }
                );

                const result = await response.json();

                if (result.success) {
                    alert('Account deactivated successfully');
                    loadAccounts();
                } else {
                    alert('Failed to deactivate account: ' + result.error);
                }
            } catch (error) {
                alert('Network error occurred');
            }
        }

        async function activateAccount(accountId) {
            // For activation, we need to update the account status
            // This would require an additional endpoint or modify the existing one
            alert('Activate functionality would require an additional API endpoint');
        }

        // Load accounts on page load
        window.addEventListener('DOMContentLoaded', loadAccounts);
    </script>
    <% } %>
</div>
</body>
</html>