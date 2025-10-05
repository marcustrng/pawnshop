package com.pawnshop.servlet;

import com.google.gson.Gson;
import com.pawnshop.dto.ApiResponse;
import com.pawnshop.dto.MonthlyRevenueDTO;
import com.pawnshop.dto.RevenueSummaryDTO;
import com.pawnshop.service.RevenueService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;

@WebServlet("/api/revenue/*")
public class RevenueServlet extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(RevenueServlet.class);
    private final Gson gson = new Gson();
    private RevenueService revenueService;

    @Override
    public void init() throws ServletException {
        revenueService = new RevenueService();
        logger.info("RevenueServlet initialized");
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String pathInfo = request.getPathInfo();

        try {
            if ("/monthly".equals(pathInfo)) {
                handleGetMonthlyRevenue(request, response);
            } else if ("/year".equals(pathInfo)) {
                handleGetRevenueByYear(request, response);
            } else if ("/range".equals(pathInfo)) {
                handleGetRevenueByRange(request, response);
            } else if ("/summary".equals(pathInfo)) {
                handleGetSummary(request, response);
            } else {
                sendErrorResponse(response, HttpServletResponse.SC_NOT_FOUND, "Endpoint not found");
            }
        } catch (Exception e) {
            logger.error("Error in GET request", e);
            sendErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Internal server error");
        }
    }

    private void handleGetMonthlyRevenue(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        String monthsParam = request.getParameter("months");
        int months = monthsParam != null ? Integer.parseInt(monthsParam) : 12;

        if (months < 1 || months > 60) {
            sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST,
                    "Months must be between 1 and 60");
            return;
        }

        try {
            List<MonthlyRevenueDTO> revenueList = revenueService.getMonthlyRevenue(months);
            sendSuccessResponse(response, HttpServletResponse.SC_OK, null, revenueList);
        } catch (RevenueService.ServiceException e) {
            sendErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    e.getMessage());
        }
    }

    private void handleGetRevenueByYear(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        String yearParam = request.getParameter("year");

        if (yearParam == null || yearParam.isEmpty()) {
            sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "Year is required");
            return;
        }

        try {
            int year = Integer.parseInt(yearParam);
            List<MonthlyRevenueDTO> revenueList = revenueService.getRevenueByYear(year);
            sendSuccessResponse(response, HttpServletResponse.SC_OK, null, revenueList);
        } catch (NumberFormatException e) {
            sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "Invalid year format");
        } catch (RevenueService.ServiceException e) {
            sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        }
    }

    private void handleGetRevenueByRange(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        String startDate = request.getParameter("start");
        String endDate = request.getParameter("end");

        if (startDate == null || endDate == null) {
            sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST,
                    "Start date and end date are required");
            return;
        }

        try {
            List<MonthlyRevenueDTO> revenueList = revenueService.getRevenueByDateRange(startDate, endDate);
            sendSuccessResponse(response, HttpServletResponse.SC_OK, null, revenueList);
        } catch (RevenueService.ServiceException e) {
            sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        }
    }

    private void handleGetSummary(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        String yearParam = request.getParameter("year");
        String monthsParam = request.getParameter("months");

        try {
            RevenueSummaryDTO summary;

            if (yearParam != null && !yearParam.isEmpty()) {
                int year = Integer.parseInt(yearParam);
                summary = revenueService.getRevenueSummaryByYear(year);
            } else {
                int months = monthsParam != null ? Integer.parseInt(monthsParam) : 12;
                summary = revenueService.getRevenueSummary(months);
            }

            sendSuccessResponse(response, HttpServletResponse.SC_OK, null, summary);
        } catch (NumberFormatException e) {
            sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "Invalid parameter format");
        } catch (RevenueService.ServiceException e) {
            sendErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    e.getMessage());
        }
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