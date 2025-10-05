<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.pawnshop.model.Account" %>
<%
    Account currentAccount = (Account) session.getAttribute("account");
    if (currentAccount == null || currentAccount.getRole() != Account.Role.ADMIN) {
        response.sendRedirect(request.getContextPath() + "/login.jsp?error=Access denied");
        return;
    }
%>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Revenue Reports - Pawnshop</title>
    <script src="https://cdn.jsdelivr.net/npm/chart.js@4.4.0/dist/chart.umd.min.js"></script>
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
            margin-bottom: 32px;
        }

        .page-header h1 {
            font-size: 32px;
            color: #333;
            margin-bottom: 8px;
        }
    </style>
</head>
<body>
<div class="navbar">
    <a href="/" class="navbar-brand">Pawnshop Admin</a>
    <a href="dashboard.jsp" class="btn-back">&#8592; Back to Dashboard</a>
</div>
<div class="container">
    <div class="page-header">
        <h1>Revenue Reports</h1>
        <p>View and analyze the shop's revenue over time.</p>
    </div>
    <canvas id="revenueChart" width="1200" height="400"></canvas>
</div>
<script>
    // Example data, replace with dynamic data as needed
    const labels = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul'];
    const data = {
        labels: labels,
        datasets: [{
            label: 'Revenue (VND)',
            backgroundColor: '#667eea',
            borderColor: '#764ba2',
            data: [12000000, 15000000, 18000000, 14000000, 20000000, 17000000, 22000000],
            tension: 0.3,
            fill: true
        }]
    };

    const config = {
        type: 'line',
        data: data,
        options: {
            responsive: true,
            plugins: {
                legend: {
                    display: true,
                    position: 'top'
                },
                title: {
                    display: true,
                    text: 'Monthly Revenue'
                }
            },
            scales: {
                y: {
                    beginAtZero: true,
                    ticks: {
                        callback: function(value) {
                            return value.toLocaleString('vi-VN') + ' ₫';
                        }
                    }
                }
            }
        }
    };

    new Chart(
        document.getElementById('revenueChart'),
        config
    );
</script>
</body>
</html>
