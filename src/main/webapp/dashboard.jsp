<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.pawnshop.model.Account" %>
<%
    Account account = (Account) session.getAttribute("account");
    if (account == null) {
        response.sendRedirect(request.getContextPath() + "/login.jsp");
        return;
    }

    boolean isAdmin = account.getRole() == Account.Role.ADMIN;
    boolean isEmployee = account.getRole() == Account.Role.EMPLOYEE;
    boolean isCustomer = account.getRole() == Account.Role.CUSTOMER;
%>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Dashboard - Pawnshop Management System</title>
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
        }

        .navbar-user {
            display: flex;
            align-items: center;
            gap: 20px;
        }

        .user-info {
            text-align: right;
        }

        .user-name {
            font-weight: 600;
            font-size: 15px;
        }

        .user-role {
            font-size: 12px;
            opacity: 0.9;
            text-transform: capitalize;
        }

        .btn-logout {
            background: rgba(255, 255, 255, 0.2);
            color: white;
            border: 1px solid rgba(255, 255, 255, 0.3);
            padding: 8px 20px;
            border-radius: 6px;
            cursor: pointer;
            font-size: 14px;
            transition: all 0.3s;
        }

        .btn-logout:hover {
            background: rgba(255, 255, 255, 0.3);
        }

        .container {
            max-width: 1200px;
            margin: 0 auto;
            padding: 32px 24px;
        }

        .page-header {
            margin-bottom: 32px;
        }

        .page-header h1 {
            font-size: 32px;
            color: #333;
            margin-bottom: 8px;
        }

        .page-header p {
            color: #666;
            font-size: 16px;
        }

        .card-grid {
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
            gap: 24px;
            margin-bottom: 32px;
        }

        .card {
            background: white;
            border-radius: 12px;
            padding: 24px;
            box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
            transition: transform 0.3s, box-shadow 0.3s;
            cursor: pointer;
        }

        .card:hover {
            transform: translateY(-4px);
            box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
        }

        .card-icon {
            font-size: 36px;
            margin-bottom: 12px;
        }

        .card-title {
            font-size: 18px;
            font-weight: 600;
            color: #333;
            margin-bottom: 8px;
        }

        .card-description {
            font-size: 14px;
            color: #666;
            line-height: 1.5;
        }

        .quick-actions {
            background: white;
            border-radius: 12px;
            padding: 24px;
            box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
        }

        .quick-actions h2 {
            font-size: 20px;
            color: #333;
            margin-bottom: 20px;
        }

        .action-buttons {
            display: flex;
            flex-wrap: wrap;
            gap: 12px;
        }

        .btn {
            padding: 10px 20px;
            border-radius: 8px;
            border: none;
            font-size: 14px;
            font-weight: 600;
            cursor: pointer;
            transition: all 0.3s;
            text-decoration: none;
            display: inline-block;
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
    </style>
</head>
<body>
<nav class="navbar">
    <div class="navbar-brand">
        🏦 Pawnshop Management
    </div>
    <div class="navbar-user">
        <div class="user-info">
            <div class="user-name"><%= account.getUsername() %></div>
            <div class="user-role"><%= account.getRole().getValue() %></div>
        </div>
        <form action="${pageContext.request.contextPath}/auth/logout" method="POST" style="margin: 0;">
            <button type="submit" class="btn-logout">Logout</button>
        </form>
    </div>
</nav>

<div class="container">
    <div class="page-header">
        <h1>Welcome, <%= account.getUsername() %>!</h1>
        <p>Manage your pawnshop operations efficiently</p>
    </div>

    <div class="card-grid">
        <% if (isAdmin || isEmployee) { %>
        <div class="card" onclick="location.href='${pageContext.request.contextPath}/pawn-contracts.jsp'">
            <div class="card-icon">📝</div>
            <div class="card-title">Pawn Contracts</div>
            <div class="card-description">Manage active pawn contracts and track due dates</div>
        </div>

        <div class="card" onclick="location.href='${pageContext.request.contextPath}/products.jsp'">
            <div class="card-icon">💎</div>
            <div class="card-title">Products</div>
            <div class="card-description">View and manage pawned products inventory</div>
        </div>
        <% } %>

        <% if (isCustomer) { %>
        <div class="card" onclick="location.href='${pageContext.request.contextPath}/my-contracts.jsp'">
            <div class="card-icon">📋</div>
            <div class="card-title">My Contracts</div>
            <div class="card-description">View your pawn contracts and payment status</div>
        </div>
        <% } %>

        <% if (isAdmin || isEmployee) { %>
        <div class="card" onclick="location.href='${pageContext.request.contextPath}/customers.jsp'">
            <div class="card-icon">👥</div>
            <div class="card-title">Customers</div>
            <div class="card-description">Manage customer information and history</div>
        </div>

        <div class="card" onclick="location.href='${pageContext.request.contextPath}/liquidations.jsp'">
            <div class="card-icon">💰</div>
            <div class="card-title">Liquidations</div>
            <div class="card-description">Process and track product liquidations</div>
        </div>
        <% } %>

        <% if (isAdmin) { %>
        <div class="card" onclick="location.href='${pageContext.request.contextPath}/accounts.jsp'">
            <div class="card-icon">⚙️</div>
            <div class="card-title">User Accounts</div>
            <div class="card-description">Manage system users and permissions</div>
        </div>

        <div class="card" onclick="location.href='${pageContext.request.contextPath}/reports.jsp'">
            <div class="card-icon">📊</div>
            <div class="card-title">Reports</div>
            <div class="card-description">View business analytics and revenue reports</div>
        </div>

        <div class="card" onclick="location.href='${pageContext.request.contextPath}/employees.jsp'">
            <div class="card-icon">👨‍💼</div>
            <div class="card-title">Employees</div>
            <div class="card-description">Manage employee information and access</div>
        </div>
        <% } %>
    </div>

    <% if (isAdmin) { %>
    <div class="quick-actions">
        <h2>Quick Actions</h2>
        <div class="action-buttons">
            <a href="${pageContext.request.contextPath}/accounts.jsp?action=register" class="btn btn-primary">
                ➕ Register New User
            </a>
            <a href="${pageContext.request.contextPath}/pawn-contracts.jsp?action=create" class="btn btn-primary">
                📝 New Pawn Contract
            </a>
            <a href="${pageContext.request.contextPath}/reports.jsp" class="btn btn-secondary">
                📊 View Reports
            </a>
        </div>
    </div>
    <% } %>
</div>
</body>
</html>