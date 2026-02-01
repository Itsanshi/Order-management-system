package com.restaurantback;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailService;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailServiceClientBuilder;
import com.amazonaws.services.simpleemail.model.*;
import com.restaurantback.dto.report.SalesPerformanceDTO;
import com.restaurantback.dto.report.StaffPerformanceDTO;
import com.restaurantback.services.EmailService;
import com.restaurantback.services.ExcelService;
import com.restaurantback.services.SalesReportsService;
import com.restaurantback.services.StaffReportsService;
import com.syndicate.deployment.annotations.environment.EnvironmentVariable;
import com.syndicate.deployment.annotations.environment.EnvironmentVariables;
import com.syndicate.deployment.annotations.events.RuleEventSource;
import com.syndicate.deployment.annotations.lambda.LambdaHandler;
import com.syndicate.deployment.model.DeploymentRuntime;
import com.syndicate.deployment.model.RetentionSetting;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@LambdaHandler(
        lambdaName = "reports_sender_handler",
        roleName = "reports_sender_handler-role",
        runtime = DeploymentRuntime.JAVA21,
        isPublishVersion = true,
        aliasName = "${lambdas_alias_name}",
        logsExpiration = RetentionSetting.SYNDICATE_ALIASES_SPECIFIED
)

@RuleEventSource(targetRule = "${weekly_event_rule}")

@EnvironmentVariables(value = {
        @EnvironmentVariable(key = "senderEmail", value = "${sender_email}"),
        @EnvironmentVariable(key = "recipientEmail", value = "${recipient_email}"),
        @EnvironmentVariable(key = "waiterReportTable", value = "${waiter_report_table}"),
        @EnvironmentVariable(key = "locationReportTable", value = "${location_report_table}")
})

public class ReportsSenderHandler implements RequestHandler<Object, Map<String, Object>> {

    private final SQSApplication context = DaggerSQSApplication.create();

    private final String senderEmail = System.getenv("senderEmail");
    private final String recipientEmail = System.getenv("recipientEmail");

    private final SalesReportsService salesReportsService = context.getSalesReportsService();
    private final StaffReportsService staffReportsService = context.getStaffReportsService();
    private final ExcelService excelService = context.getExcelService();
    private final EmailService emailService = context.getEmailService();

    public Map<String, Object> handleRequest(Object request, Context context) {

        Map<String, Object> resultMap = new HashMap<String, Object>();

        try {
            LocalDate currDate = LocalDate.now(ZoneId.of("Asia/Kolkata"));
            context.getLogger().log("curr date: " + currDate);

            LocalDate endDate = currDate.minusDays(1);
            LocalDate startDate = endDate.minusDays(7);

            String reportFrom = startDate.toString();
            String reportTo = endDate.toString();

            context.getLogger().log("processing reports for sales and staff...");
            List<SalesPerformanceDTO> salesPerformanceDTOS = salesReportsService.generateSalesPerformanceReport(reportFrom, reportTo);
            List<StaffPerformanceDTO> staffPerformanceDTOS = staffReportsService.generateStaffPerformanceReport(reportFrom, reportTo);

            context.getLogger().log("creating excel sheet");
//            byte[] report = excelService.generateExcelReport(staffPerformanceDTOS, salesPerformanceDTOS);
            byte[] waiterReport = excelService.generateWaiterReport(staffPerformanceDTOS);
            byte[] locationReport = excelService.generateLocationReport(salesPerformanceDTOS);

            String reportPeriod = reportFrom + " to " + reportTo;

            context.getLogger().log("report period: " + reportPeriod);
//            emailService.sentReportEmail(report, senderEmail, recipientEmail, reportPeriod);
            emailService.sentReportEmail(waiterReport, locationReport, senderEmail, recipientEmail, reportPeriod);

            resultMap.put("statusCode", 200);
            resultMap.put("body", "Mail sent");
        } catch (Exception e) {
            context.getLogger().log(e.toString());
            resultMap.put("statusCode", 500);
            resultMap.put("body", e.getMessage());
        }
        return resultMap;
    }
}
