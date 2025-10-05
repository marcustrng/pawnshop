package com.pawnshop.servlet;

import com.google.gson.Gson;
import com.pawnshop.dto.ApiResponse;
import com.pawnshop.dto.CustomerRequestDTO;
import com.pawnshop.dto.CustomerResponseDTO;
import com.pawnshop.service.CustomerService;
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

@WebServlet("/api/customers/*")
public class CustomerServlet extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(CustomerServlet.class);
    private final Gson gson = new Gson();
    private CustomerService customerService;

    @Override
    public void init() throws ServletException {
        customerService = new CustomerService();
        logger.info("CustomerServlet initialized");
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String pathInfo = request.getPathInfo();
        String searchKeyword = request.getParameter("search");
        String activeOnly = request.getParameter("active");

        try {
            if (pathInfo == null || pathInfo.equals("/")) {
                if (searchKeyword != null && !searchKeyword.trim().isEmpty()) {
                    handleSearchCustomers(request, response, searchKeyword);
                } else if ("true".equals(activeOnly)) {
                    handleGetActiveCustomers(request, response);
                } else {
                    handleGetAllCustomers(request, response);
                }
            } else if (pathInfo.matches("/\\d+")) {
                String customerId = pathInfo.substring(1);
                handleGetCustomerById(request, response, customerId);
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
                handleCreateCustomer(request, response);
            } else if ("/with-account".equals(pathInfo)) {
                handleCreateCustomerWithAccount(request, response);
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
                String customerId = pathInfo.substring(1);
                handleUpdateCustomer(request, response, customerId);
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
                String customerId = pathInfo.substring(1);
                handleDeleteCustomer(request, response, customerId);
            } else {
                sendErrorResponse(response, HttpServletResponse.SC_NOT_FOUND, "Endpoint not found");
            }
        } catch (Exception e) {
            logger.error("Error in DELETE request", e);
            sendErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Internal server error");
        }
    }

    private void handleCreateCustomer(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        CustomerRequestDTO dto = readRequestBody(request, CustomerRequestDTO.class);

        if (dto == null) {
            sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "Invalid request body");
            return;
        }

        try {
            CustomerResponseDTO result = customerService.createCustomer(dto);
            sendSuccessResponse(response, HttpServletResponse.SC_CREATED,
                    "Customer created successfully", result);
        } catch (CustomerService.ServiceException e) {
            sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        }
    }

    private void handleCreateCustomerWithAccount(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        CustomerRequestDTO dto = readRequestBody(request, CustomerRequestDTO.class);

        if (dto == null) {
            sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "Invalid request body");
            return;
        }

        try {
            CustomerResponseDTO result = customerService.createCustomerWithAccount(dto);
            sendSuccessResponse(response, HttpServletResponse.SC_CREATED,
                    "Customer and account created successfully", result);
        } catch (CustomerService.ServiceException e) {
            sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        }
    }

    private void handleGetAllCustomers(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        try {
            List<CustomerResponseDTO> customers = customerService.getAllCustomers();
            sendSuccessResponse(response, HttpServletResponse.SC_OK, null, customers);
        } catch (CustomerService.ServiceException e) {
            sendErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    e.getMessage());
        }
    }

    private void handleGetActiveCustomers(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        try {
            List<CustomerResponseDTO> customers = customerService.getActiveCustomers();
            sendSuccessResponse(response, HttpServletResponse.SC_OK, null, customers);
        } catch (CustomerService.ServiceException e) {
            sendErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    e.getMessage());
        }
    }

    private void handleSearchCustomers(HttpServletRequest request, HttpServletResponse response,
                                       String keyword) throws IOException {

        try {
            List<CustomerResponseDTO> customers = customerService.searchCustomers(keyword);
            sendSuccessResponse(response, HttpServletResponse.SC_OK, null, customers);
        } catch (CustomerService.ServiceException e) {
            sendErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    e.getMessage());
        }
    }

    private void handleGetCustomerById(HttpServletRequest request, HttpServletResponse response,
                                       String customerIdStr) throws IOException {

        try {
            Integer customerId = Integer.parseInt(customerIdStr);
            CustomerResponseDTO customer = customerService.getCustomerById(customerId);
            sendSuccessResponse(response, HttpServletResponse.SC_OK, null, customer);
        } catch (NumberFormatException e) {
            sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "Invalid customer ID");
        } catch (CustomerService.ServiceException e) {
            sendErrorResponse(response, HttpServletResponse.SC_NOT_FOUND, e.getMessage());
        }
    }

    private void handleUpdateCustomer(HttpServletRequest request, HttpServletResponse response,
                                      String customerIdStr) throws IOException {

        try {
            Integer customerId = Integer.parseInt(customerIdStr);
            CustomerRequestDTO dto = readRequestBody(request, CustomerRequestDTO.class);

            if (dto == null) {
                sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST,
                        "Invalid request body");
                return;
            }

            CustomerResponseDTO result = customerService.updateCustomer(customerId, dto);
            sendSuccessResponse(response, HttpServletResponse.SC_OK,
                    "Customer updated successfully", result);
        } catch (NumberFormatException e) {
            sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "Invalid customer ID");
        } catch (CustomerService.ServiceException e) {
            sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        }
    }

    private void handleDeleteCustomer(HttpServletRequest request, HttpServletResponse response,
                                      String customerIdStr) throws IOException {

        try {
            Integer customerId = Integer.parseInt(customerIdStr);
            customerService.deleteCustomer(customerId);
            sendSuccessResponse(response, HttpServletResponse.SC_OK,
                    "Customer deleted successfully", null);
        } catch (NumberFormatException e) {
            sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "Invalid customer ID");
        } catch (CustomerService.ServiceException e) {
            sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        }
    }

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