package com.pawnshop.filter;

import com.google.gson.Gson;
import com.pawnshop.dto.ApiResponse;
import com.pawnshop.model.Account;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@WebFilter("/*")
public class AuthenticationFilter implements Filter {
    private static final Logger logger = LoggerFactory.getLogger(AuthenticationFilter.class);
    private static final Gson gson = new Gson();

    // Public endpoints that don't require authentication
    private static final List<String> PUBLIC_ENDPOINTS = Arrays.asList(
            "/login.jsp",
            "/auth/login",
            "/api/auth/login",
            "/api/health",
            "/css/",
            "/js/",
            "/images/",
            "/error/"
    );

    // Admin-only API endpoints
    private static final List<String> ADMIN_API_ENDPOINTS = Arrays.asList(
            "/api/accounts/register",
            "/api/accounts/all",
            "/api/accounts/update",
            "/api/accounts/delete",
            "/api/accounts/deactivate"
    );

    // Admin-only JSP pages
    private static final List<String> ADMIN_JSP_PAGES = Arrays.asList(
            "/accounts.jsp",
            "/employees.jsp",
            "/reports.jsp"
    );

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        logger.info("AuthenticationFilter initialized");
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        // Set CORS headers for API requests
        httpResponse.setHeader("Access-Control-Allow-Origin", "*");
        httpResponse.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        httpResponse.setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization");
        httpResponse.setHeader("Access-Control-Allow-Credentials", "true");

        // Handle preflight requests
        if ("OPTIONS".equalsIgnoreCase(httpRequest.getMethod())) {
            httpResponse.setStatus(HttpServletResponse.SC_OK);
            return;
        }

        String path = httpRequest.getRequestURI().substring(httpRequest.getContextPath().length());
        boolean isApiRequest = path.startsWith("/api/");

        // Allow public endpoints
        if (isPublicEndpoint(path)) {
            chain.doFilter(request, response);
            return;
        }

        // Check authentication
        HttpSession session = httpRequest.getSession(false);
        if (session == null || session.getAttribute("account") == null) {
            if (isApiRequest) {
                sendUnauthorizedResponse(httpResponse, "Authentication required");
            } else {
                httpResponse.sendRedirect(httpRequest.getContextPath() +
                        "/login.jsp?error=Please login to access this page");
            }
            return;
        }

        Account account = (Account) session.getAttribute("account");

        // Check if account is active
        if (!account.isActive()) {
            session.invalidate();
            if (isApiRequest) {
                sendUnauthorizedResponse(httpResponse, "Account is inactive");
            } else {
                httpResponse.sendRedirect(httpRequest.getContextPath() +
                        "/login.jsp?error=Your account has been deactivated");
            }
            return;
        }

        // Check admin-only access
        if ((isAdminApiEndpoint(path) || isAdminJspPage(path)) &&
                account.getRole() != Account.Role.ADMIN) {
            if (isApiRequest) {
                sendForbiddenResponse(httpResponse, "Admin access required");
            } else {
                httpResponse.sendRedirect(httpRequest.getContextPath() +
                        "/dashboard.jsp?error=Access denied - Admin privileges required");
            }
            return;
        }

        // Set account in request attribute for downstream use
        httpRequest.setAttribute("currentAccount", account);

        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {
        logger.info("AuthenticationFilter destroyed");
    }

    private boolean isPublicEndpoint(String path) {
        return PUBLIC_ENDPOINTS.stream().anyMatch(path::startsWith);
    }

    private boolean isAdminApiEndpoint(String path) {
        return ADMIN_API_ENDPOINTS.stream().anyMatch(path::startsWith);
    }

    private boolean isAdminJspPage(String path) {
        return ADMIN_JSP_PAGES.stream().anyMatch(path::equals);
    }

    private void sendUnauthorizedResponse(HttpServletResponse response, String message)
            throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        ApiResponse<?> apiResponse = ApiResponse.error(message);
        response.getWriter().write(gson.toJson(apiResponse));
    }

    private void sendForbiddenResponse(HttpServletResponse response, String message)
            throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        ApiResponse<?> apiResponse = ApiResponse.error(message);
        response.getWriter().write(gson.toJson(apiResponse));
    }
}