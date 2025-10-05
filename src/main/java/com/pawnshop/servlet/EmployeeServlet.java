package com.pawnshop.servlet;

import com.google.gson.Gson;
import com.pawnshop.dto.ApiResponse;
import com.pawnshop.dto.EmployeeRequestDTO;
import com.pawnshop.dto.EmployeeResponseDTO;
import com.pawnshop.service.EmployeeService;
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

@WebServlet("/api/employees/*")
public class EmployeeServlet extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(EmployeeServlet.class);
    private final Gson gson = new Gson();
    private EmployeeService employeeService;

    @Override
    public void init() throws ServletException {
        employeeService = new EmployeeService();
        logger.info("EmployeeServlet initialized");
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String pathInfo = request.getPathInfo();
        String searchKeyword = request.getParameter("search");
        String activeOnly = request.getParameter("active");

        try {
            if (pathInfo == null || pathInfo.equals("/")) {
                // GET /api/employees or /api/employees?search=keyword
                if (searchKeyword != null && !searchKeyword.trim().isEmpty()) {
                    handleSearchEmployees(request, response, searchKeyword);
                } else if ("true".equals(activeOnly)) {
                    handleGetActiveEmployees(request, response);
                } else {
                    handleGetAllEmployees(request, response);
                }
            } else if (pathInfo.matches("/\\d+")) {
                // GET /api/employees/{id}
                String employeeId = pathInfo.substring(1);
                handleGetEmployeeById(request, response, employeeId);
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
            if (pathInfo == null || pathInfo.equals("/")) {
                // POST /api/employees - Create employee with existing account
                handleCreateEmployee(request, response);
            } else if ("/with-account".equals(pathInfo)) {
                // POST /api/employees/with-account - Create employee with new account
                handleCreateEmployeeWithAccount(request, response);
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
                // PUT /api/employees/{id}
                String employeeId = pathInfo.substring(1);
                handleUpdateEmployee(request, response, employeeId);
            } else if ("/deactivate".equals(pathInfo)) {
                // PUT /api/employees/deactivate?id={id}
                String employeeId = request.getParameter("id");
                handleDeactivateEmployee(request, response, employeeId);
            } else if ("/activate".equals(pathInfo)) {
                // PUT /api/employees/activate?id={id}
                String employeeId = request.getParameter("id");
                handleActivateEmployee(request, response, employeeId);
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
                // DELETE /api/employees/{id}
                String employeeId = pathInfo.substring(1);
                handleDeleteEmployee(request, response, employeeId);
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
    private void handleCreateEmployee(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        EmployeeRequestDTO dto = readRequestBody(request, EmployeeRequestDTO.class);

        if (dto == null) {
            sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "Invalid request body");
            return;
        }

        try {
            EmployeeResponseDTO result = employeeService.createEmployee(dto);
            sendSuccessResponse(response, HttpServletResponse.SC_CREATED,
                    "Employee created successfully", result);
        } catch (EmployeeService.ServiceException e) {
            sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        }
    }

    private void handleCreateEmployeeWithAccount(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        EmployeeRequestDTO dto = readRequestBody(request, EmployeeRequestDTO.class);

        if (dto == null) {
            sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "Invalid request body");
            return;
        }

        try {
            EmployeeResponseDTO result = employeeService.createEmployeeWithAccount(dto);
            sendSuccessResponse(response, HttpServletResponse.SC_CREATED,
                    "Employee and account created successfully", result);
        } catch (EmployeeService.ServiceException e) {
            sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        }
    }

    private void handleGetAllEmployees(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        try {
            List<EmployeeResponseDTO> employees = employeeService.getAllEmployees();
            sendSuccessResponse(response, HttpServletResponse.SC_OK, null, employees);
        } catch (EmployeeService.ServiceException e) {
            sendErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    e.getMessage());
        }
    }

    private void handleGetActiveEmployees(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        try {
            List<EmployeeResponseDTO> employees = employeeService.getActiveEmployees();
            sendSuccessResponse(response, HttpServletResponse.SC_OK, null, employees);
        } catch (EmployeeService.ServiceException e) {
            sendErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    e.getMessage());
        }
    }

    private void handleSearchEmployees(HttpServletRequest request, HttpServletResponse response,
                                       String keyword) throws IOException {

        try {
            List<EmployeeResponseDTO> employees = employeeService.searchEmployees(keyword);
            sendSuccessResponse(response, HttpServletResponse.SC_OK, null, employees);
        } catch (EmployeeService.ServiceException e) {
            sendErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    e.getMessage());
        }
    }

    private void handleGetEmployeeById(HttpServletRequest request, HttpServletResponse response,
                                       String employeeIdStr) throws IOException {

        try {
            Integer employeeId = Integer.parseInt(employeeIdStr);
            EmployeeResponseDTO employee = employeeService.getEmployeeById(employeeId);
            sendSuccessResponse(response, HttpServletResponse.SC_OK, null, employee);
        } catch (NumberFormatException e) {
            sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "Invalid employee ID");
        } catch (EmployeeService.ServiceException e) {
            sendErrorResponse(response, HttpServletResponse.SC_NOT_FOUND, e.getMessage());
        }
    }

    private void handleUpdateEmployee(HttpServletRequest request, HttpServletResponse response,
                                      String employeeIdStr) throws IOException {

        try {
            Integer employeeId = Integer.parseInt(employeeIdStr);
            EmployeeRequestDTO dto = readRequestBody(request, EmployeeRequestDTO.class);

            if (dto == null) {
                sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST,
                        "Invalid request body");
                return;
            }

            EmployeeResponseDTO result = employeeService.updateEmployee(employeeId, dto);
            sendSuccessResponse(response, HttpServletResponse.SC_OK,
                    "Employee updated successfully", result);
        } catch (NumberFormatException e) {
            sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "Invalid employee ID");
        } catch (EmployeeService.ServiceException e) {
            sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        }
    }

    private void handleDeactivateEmployee(HttpServletRequest request, HttpServletResponse response,
                                          String employeeIdStr) throws IOException {

        if (employeeIdStr == null || employeeIdStr.isEmpty()) {
            sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST,
                    "Employee ID is required");
            return;
        }

        try {
            Integer employeeId = Integer.parseInt(employeeIdStr);
            employeeService.deactivateEmployee(employeeId);
            sendSuccessResponse(response, HttpServletResponse.SC_OK,
                    "Employee deactivated successfully", null);
        } catch (NumberFormatException e) {
            sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "Invalid employee ID");
        } catch (EmployeeService.ServiceException e) {
            sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        }
    }

    private void handleActivateEmployee(HttpServletRequest request, HttpServletResponse response,
                                        String employeeIdStr) throws IOException {

        if (employeeIdStr == null || employeeIdStr.isEmpty()) {
            sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST,
                    "Employee ID is required");
            return;
        }

        try {
            Integer employeeId = Integer.parseInt(employeeIdStr);
            employeeService.activateEmployee(employeeId);
            sendSuccessResponse(response, HttpServletResponse.SC_OK,
                    "Employee activated successfully", null);
        } catch (NumberFormatException e) {
            sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "Invalid employee ID");
        } catch (EmployeeService.ServiceException e) {
            sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        }
    }

    private void handleDeleteEmployee(HttpServletRequest request, HttpServletResponse response,
                                      String employeeIdStr) throws IOException {

        try {
            Integer employeeId = Integer.parseInt(employeeIdStr);
            employeeService.deleteEmployee(employeeId);
            sendSuccessResponse(response, HttpServletResponse.SC_OK,
                    "Employee deleted successfully", null);
        } catch (NumberFormatException e) {
            sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "Invalid employee ID");
        } catch (EmployeeService.ServiceException e) {
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