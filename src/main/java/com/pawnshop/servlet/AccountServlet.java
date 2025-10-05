package com.pawnshop.servlet;

import com.google.gson.Gson;
import com.pawnshop.dto.AccountRegistrationDTO;
import com.pawnshop.dto.AccountResponseDTO;
import com.pawnshop.dto.ApiResponse;
import com.pawnshop.service.AccountService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.List;

@WebServlet("/api/accounts/*")
public class AccountServlet extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(AccountServlet.class);
    private final Gson gson = new Gson();
    private AccountService accountService;

    @Override
    public void init() throws ServletException {
        accountService = new AccountService();
        logger.info("AccountServlet initialized");
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String pathInfo = request.getPathInfo();

        try {
            if (pathInfo == null || pathInfo.equals("/")) {
                // GET /api/accounts - Get all accounts (Admin only)
                handleGetAllAccounts(request, response);
            } else if (pathInfo.matches("/\\d+")) {
                // GET /api/accounts/{id} - Get account by ID
                String accountId = pathInfo.substring(1);
                handleGetAccountById(request, response, accountId);
            } else {
                sendErrorResponse(response, HttpServletResponse.SC_NOT_FOUND, "Endpoint not found");
            }
        } catch (Exception e) {
            logger.error("Error in GET request", e);
            sendErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Internal server error");
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String pathInfo = request.getPathInfo();

        try {
            if ("/register".equals(pathInfo)) {
                // POST /api/accounts/register - Register new account (Admin only)
                handleRegisterAccount(request, response);
            } else {
                sendErrorResponse(response, HttpServletResponse.SC_NOT_FOUND, "Endpoint not found");
            }
        } catch (Exception e) {
            logger.error("Error in POST request", e);
            sendErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Internal server error");
        }
    }

    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String pathInfo = request.getPathInfo();

        try {
            if (pathInfo != null && pathInfo.matches("/\\d+")) {
                // PUT /api/accounts/{id} - Update account (Admin only)
                String accountId = pathInfo.substring(1);
                handleUpdateAccount(request, response, accountId);
            } else if ("/deactivate".equals(pathInfo)) {
                // PUT /api/accounts/deactivate?id={id} - Deactivate account (Admin only)
                String accountId = request.getParameter("id");
                handleDeactivateAccount(request, response, accountId);
            } else {
                sendErrorResponse(response, HttpServletResponse.SC_NOT_FOUND, "Endpoint not found");
            }
        } catch (Exception e) {
            logger.error("Error in PUT request", e);
            sendErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Internal server error");
        }
    }

    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String pathInfo = request.getPathInfo();

        try {
            if (pathInfo != null && pathInfo.matches("/\\d+")) {
                // DELETE /api/accounts/{id} - Delete account (Admin only)
                String accountId = pathInfo.substring(1);
                handleDeleteAccount(request, response, accountId);
            } else {
                sendErrorResponse(response, HttpServletResponse.SC_NOT_FOUND, "Endpoint not found");
            }
        } catch (Exception e) {
            logger.error("Error in DELETE request", e);
            sendErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Internal server error");
        }
    }

    // Handler methods
    private void handleRegisterAccount(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        AccountRegistrationDTO dto = readRequestBody(request, AccountRegistrationDTO.class);

        if (dto == null) {
            sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "Invalid request body");
            return;
        }

        try {
            AccountResponseDTO result = accountService.registerAccount(dto);
            sendSuccessResponse(response, HttpServletResponse.SC_CREATED,
                    "Account registered successfully", result);
        } catch (AccountService.ServiceException e) {
            sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        }
    }

    private void handleGetAllAccounts(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        try {
            List<AccountResponseDTO> accounts = accountService.getAllAccounts();
            sendSuccessResponse(response, HttpServletResponse.SC_OK, null, accounts);
        } catch (AccountService.ServiceException e) {
            sendErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    e.getMessage());
        }
    }

    private void handleGetAccountById(HttpServletRequest request, HttpServletResponse response,
                                      String accountIdStr) throws IOException {

        try {
            Integer accountId = Integer.parseInt(accountIdStr);
            AccountResponseDTO account = accountService.getAccountById(accountId);
            sendSuccessResponse(response, HttpServletResponse.SC_OK, null, account);
        } catch (NumberFormatException e) {
            sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "Invalid account ID");
        } catch (AccountService.ServiceException e) {
            sendErrorResponse(response, HttpServletResponse.SC_NOT_FOUND, e.getMessage());
        }
    }

    private void handleUpdateAccount(HttpServletRequest request, HttpServletResponse response,
                                     String accountIdStr) throws IOException {

        try {
            Integer accountId = Integer.parseInt(accountIdStr);
            AccountRegistrationDTO dto = readRequestBody(request, AccountRegistrationDTO.class);

            if (dto == null) {
                sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST,
                        "Invalid request body");
                return;
            }

            AccountResponseDTO result = accountService.updateAccount(accountId, dto);
            sendSuccessResponse(response, HttpServletResponse.SC_OK,
                    "Account updated successfully", result);
        } catch (NumberFormatException e) {
            sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "Invalid account ID");
        } catch (AccountService.ServiceException e) {
            sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        }
    }

    private void handleDeactivateAccount(HttpServletRequest request, HttpServletResponse response,
                                         String accountIdStr) throws IOException {

        if (accountIdStr == null || accountIdStr.isEmpty()) {
            sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST,
                    "Account ID is required");
            return;
        }

        try {
            Integer accountId = Integer.parseInt(accountIdStr);
            accountService.deactivateAccount(accountId);
            sendSuccessResponse(response, HttpServletResponse.SC_OK,
                    "Account deactivated successfully", null);
        } catch (NumberFormatException e) {
            sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "Invalid account ID");
        } catch (AccountService.ServiceException e) {
            sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        }
    }

    private void handleDeleteAccount(HttpServletRequest request, HttpServletResponse response,
                                     String accountIdStr) throws IOException {

        try {
            Integer accountId = Integer.parseInt(accountIdStr);
            accountService.deleteAccount(accountId);
            sendSuccessResponse(response, HttpServletResponse.SC_OK,
                    "Account deleted successfully", null);
        } catch (NumberFormatException e) {
            sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "Invalid account ID");
        } catch (AccountService.ServiceException e) {
            sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        }
    }

    // Helper methods
    private <T> T readRequestBody(HttpServletRequest request, Class<T> clazz) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = request.getReader()) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        }

        if (sb.length() == 0) {
            return null;
        }

        return gson.fromJson(sb.toString(), clazz);
    }

    private void sendSuccessResponse(HttpServletResponse response, int status,
                                     String message, Object data) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        ApiResponse<Object> apiResponse = message != null
                ? ApiResponse.success(message, data)
                : ApiResponse.success(data);

        response.getWriter().write(gson.toJson(apiResponse));
    }

    private void sendErrorResponse(HttpServletResponse response, int status, String error)
            throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        ApiResponse<?> apiResponse = ApiResponse.error(error);
        response.getWriter().write(gson.toJson(apiResponse));
    }
}