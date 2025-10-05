package com.pawnshop.servlet;

import com.pawnshop.dto.LoginRequestDTO;
import com.pawnshop.dto.AccountResponseDTO;
import com.pawnshop.model.Account;
import com.pawnshop.service.AccountService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Servlet for JSP-based authentication
 * Handles form-based login/logout with page redirects
 */
@WebServlet("/auth/*")
public class AuthServlet extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(AuthServlet.class);
    private AccountService accountService;

    @Override
    public void init() throws ServletException {
        accountService = new AccountService();
        logger.info("AuthServletJSP initialized");
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String pathInfo = request.getPathInfo();

        if ("/login".equals(pathInfo)) {
            handleLogin(request, response);
        } else if ("/logout".equals(pathInfo)) {
            handleLogout(request, response);
        } else {
            response.sendRedirect(request.getContextPath() + "/login.jsp?error=Invalid request");
        }
    }

    private void handleLogin(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String username = request.getParameter("username");
        String password = request.getParameter("password");

        // Validate input
        if (username == null || username.trim().isEmpty() ||
                password == null || password.isEmpty()) {
            response.sendRedirect(request.getContextPath() +
                    "/login.jsp?error=Username and password are required");
            return;
        }

        try {
            // Create DTO
            LoginRequestDTO loginDto = new LoginRequestDTO();
            loginDto.setUsername(username.trim());
            loginDto.setPassword(password);

            // Authenticate
            AccountResponseDTO accountDto = accountService.login(loginDto);

            // Create session
            HttpSession session = request.getSession(true);

            // Convert DTO to Account model for session
            Account account = new Account();
            account.setAccountId(accountDto.getAccountId());
            account.setUsername(accountDto.getUsername());
            account.setRole(Account.Role.valueOf(accountDto.getRole().toUpperCase()));
            account.setActive(accountDto.isActive());
            account.setCreatedAt(accountDto.getCreatedAt());
            account.setUpdatedAt(accountDto.getUpdatedAt());

            session.setAttribute("account", account);
            session.setMaxInactiveInterval(3600); // 1 hour

            logger.info("User logged in via JSP: username={}, sessionId={}",
                    username, session.getId());

            // Redirect to dashboard
            response.sendRedirect(request.getContextPath() + "/dashboard.jsp");

        } catch (AccountService.ServiceException e) {
            logger.warn("Login failed for username: {}", username);
            response.sendRedirect(request.getContextPath() +
                    "/login.jsp?error=" + e.getMessage());
        } catch (Exception e) {
            logger.error("Unexpected error during login", e);
            response.sendRedirect(request.getContextPath() +
                    "/login.jsp?error=An unexpected error occurred");
        }
    }

    private void handleLogout(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);

        if (session != null) {
            Account account = (Account) session.getAttribute("account");
            String username = account != null ? account.getUsername() : "unknown";

            session.invalidate();
            logger.info("User logged out via JSP: username={}", username);
        }

        response.sendRedirect(request.getContextPath() +
                "/login.jsp?message=You have been logged out successfully");
    }
}
