package com.restaurantback.services;

import com.restaurantback.dto.report.SalesPerformanceDTO;
import com.restaurantback.dto.report.StaffPerformanceDTO;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

public class ExcelService {

    public byte[] generateWaiterReport(List<StaffPerformanceDTO> staffPerformanceDTOList) {
        try (Workbook workbook = new XSSFWorkbook()) {

            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle titleStyle = createTitleStyle(workbook);
            CellStyle dataStyle = createDataStyle(workbook);

            // Create a sheet for waiter report
            Sheet sheet = workbook.createSheet("Waiter Performance Report");

            // Set column widths
            for (int i = 0; i < 11; i++) {
                sheet.setColumnWidth(i, 4000);
            }

            int rowNum = 0;

            // Add report title
            Row titleRow = sheet.createRow(rowNum++);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("Waiter Performance Report");
            titleCell.setCellStyle(titleStyle);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 10));

            rowNum++; // Add empty row for spacing

            // Create header row for staff performance
            Row staffHeaderRow = sheet.createRow(rowNum++);
            String[] staffHeaders = {
                    "Location", "Waiter", "Waiter's e-mail", "Report period start", "Report period end",
                    "Waiter working hours", "Waiter Orders processed", "Delta of Waiter Orders processed to previous period in %",
                    "Average Service Feedback Waiter (1 to 5)", "Minimum Service Feedback Waiter (1 to 5)",
                    "Delta of Average Service Feedback Waiter to previous period in %"
            };

            for (int i = 0; i < staffHeaders.length; i++) {
                Cell cell = staffHeaderRow.createCell(i);
                cell.setCellValue(staffHeaders[i]);
                cell.setCellStyle(headerStyle);
            }

            // Add staff performance data
            for (StaffPerformanceDTO staff : staffPerformanceDTOList) {
                Row row = sheet.createRow(rowNum++);

                row.createCell(0).setCellValue(staff.getLocationId());
                row.createCell(1).setCellValue(staff.getStaffName());
                row.createCell(2).setCellValue(staff.getStaffEmail());
                row.createCell(3).setCellValue(staff.getReportFrom());
                row.createCell(4).setCellValue(staff.getReportTo());
                row.createCell(5).setCellValue(staff.getWorkingHours());
                row.createCell(6).setCellValue(staff.getOrderProcessed());

                Cell orderDeltaCell = row.createCell(7);
                String deltaValue = (staff.getDeltaOfOrderProcessedToPreviousPeriod() >= 0 ? "+" : "") +
                        String.format("%.0f%%", staff.getDeltaOfOrderProcessedToPreviousPeriod());
                orderDeltaCell.setCellValue(deltaValue);

                row.createCell(8).setCellValue(staff.getAverageServiceFeedback());
                row.createCell(9).setCellValue(staff.getMinimumServiceFeedback());

                Cell serviceDeltaCell = row.createCell(10);
                String serviceDeltaValue = (staff.getDeltaOfAverageServiceFeedbackToPreviousPeriod() >= 0 ? "+" : "") +
                        String.format("%.0f%%", staff.getDeltaOfAverageServiceFeedbackToPreviousPeriod());
                serviceDeltaCell.setCellValue(serviceDeltaValue);

                // Apply data style to all cells
                for (int i = 0; i < 11; i++) {
                    if (row.getCell(i) != null) {
                        row.getCell(i).setCellStyle(dataStyle);
                    }
                }
            }

            // Convert workbook to byte array
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            return outputStream.toByteArray();

        } catch (IOException e) {
            throw new RuntimeException("Failed to generate Waiter Excel report", e);
        }
    }

    public byte[] generateLocationReport(List<SalesPerformanceDTO> salesPerformanceDTOList) {
        try (Workbook workbook = new XSSFWorkbook()) {

            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle titleStyle = createTitleStyle(workbook);
            CellStyle dataStyle = createDataStyle(workbook);
            CellStyle currencyStyle = createCurrencyStyle(workbook);

            // Create a sheet for location report
            Sheet sheet = workbook.createSheet("Location Performance Report");

            // Set column widths
            for (int i = 0; i < 10; i++) {
                sheet.setColumnWidth(i, 4000);
            }

            int rowNum = 0;

            // Add report title
            Row titleRow = sheet.createRow(rowNum++);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("Location Performance Report");
            titleCell.setCellStyle(titleStyle);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 9));

            rowNum++; // Add empty row for spacing

            // Create header row for sales performance
            Row salesHeaderRow = sheet.createRow(rowNum++);
            String[] salesHeaders = {
                    "Location", "Report period start", "Report period end",
                    "Orders processed within location", "Delta of orders processed within location to previous period (in %)",
                    "Average cuisine Feedback by Restaurant location (1 to 5)",
                    "Minimum cuisine Feedback by Restaurant location (1 to 5)",
                    "Delta of average cuisine Feedback by Restaurant location to previous period (in %)",
                    "Revenue for orders within reported period (USD)",
                    "Delta of revenue for orders to previous period %"
            };

            for (int i = 0; i < salesHeaders.length; i++) {
                Cell cell = salesHeaderRow.createCell(i);
                cell.setCellValue(salesHeaders[i]);
                cell.setCellStyle(headerStyle);
            }

            // Add sales performance data
            for (SalesPerformanceDTO sales : salesPerformanceDTOList) {
                Row row = sheet.createRow(rowNum++);

                row.createCell(0).setCellValue(sales.getLocationId());
                row.createCell(1).setCellValue(sales.getReportFrom());
                row.createCell(2).setCellValue(sales.getReportTo());
                row.createCell(3).setCellValue(sales.getOrderProcessedWithinLocation());

                Cell orderDeltaCell = row.createCell(4);
                String deltaValue = (sales.getDeltaOfOrderProcessedToPreviousPeriod() >= 0 ? "+" : "") +
                        String.format("%.0f%%", sales.getDeltaOfOrderProcessedToPreviousPeriod());
                orderDeltaCell.setCellValue(deltaValue);

                row.createCell(5).setCellValue(sales.getAverageCuisineFeedback());
                row.createCell(6).setCellValue(sales.getMinimumCuisineFeedback());

                Cell cuisineDeltaCell = row.createCell(7);
                String cuisineDeltaValue = (sales.getDeltaOfAverageCuisineFeedbackToPreviousPeriod() >= 0 ? "+" : "") +
                        String.format("%.0f%%", sales.getDeltaOfAverageCuisineFeedbackToPreviousPeriod());
                cuisineDeltaCell.setCellValue(cuisineDeltaValue);

                Cell revenueCell = row.createCell(8);
                revenueCell.setCellValue(sales.getRevenueForOrders());
                revenueCell.setCellStyle(currencyStyle);

                Cell revenueDeltaCell = row.createCell(9);
                String revenueDeltaValue = (sales.getDeltaOfRevenueForOrdersToPreviousPeriod() >= 0 ? "+" : "") +
                        String.format("%.0f%%", sales.getDeltaOfRevenueForOrdersToPreviousPeriod());
                revenueDeltaCell.setCellValue(revenueDeltaValue);

                // Apply data style to all cells
                for (int i = 0; i < 10; i++) {
                    if (row.getCell(i) != null && i != 8) { // Skip cell 8 which already has currency style
                        row.getCell(i).setCellStyle(dataStyle);
                    }
                }
            }

            // Convert workbook to byte array
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            return outputStream.toByteArray();

        } catch (IOException e) {
            throw new RuntimeException("Failed to generate Location Excel report", e);
        }
    }

    public byte[] generateExcelReport(List<StaffPerformanceDTO> staffPerformanceDTOList, List<SalesPerformanceDTO> salesPerformanceDTOList) {
        try (Workbook workbook = new XSSFWorkbook()) {

            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle titleStyle = createTitleStyle(workbook);
            CellStyle dataStyle = createDataStyle(workbook);
            CellStyle percentStyle = createPercentStyle(workbook);
            CellStyle currencyStyle = createCurrencyStyle(workbook);

            // Create a single sheet for both reports
            Sheet sheet = workbook.createSheet("Performance Report");

            // Set column widths
            for (int i = 0; i < 10; i++) {
                sheet.setColumnWidth(i, 4000);
            }

            int rowNum = 0;

            // Add report title and date
            Row titleRow = sheet.createRow(rowNum++);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("Performance Report");
            titleCell.setCellStyle(titleStyle);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 9));

            rowNum++; // Add empty row for spacing

            // 1. Staff Performance Table
            Row staffTitleRow = sheet.createRow(rowNum++);
            Cell staffTitleCell = staffTitleRow.createCell(0);
            staffTitleCell.setCellValue("Staff Performance");
            staffTitleCell.setCellStyle(titleStyle);
            sheet.addMergedRegion(new CellRangeAddress(rowNum-1, rowNum-1, 0, 9));

            // Create header row for staff performance
            Row staffHeaderRow = sheet.createRow(rowNum++);
            String[] staffHeaders = {
                    "Location", "Waiter", "Waiter's e-mail", "Report period start", "Report period end",
                    "Waiter working hours", "Waiter Orders processed", "Delta of Waiter Orders processed to previous period in %",
                    "Average Service Feedback Waiter (1 to 5)", "Minimum Service Feedback Waiter (1 to 5)",
                    "Delta of Average Service Feedback Waiter to previous period in %"
            };

            for (int i = 0; i < staffHeaders.length; i++) {
                Cell cell = staffHeaderRow.createCell(i);
                cell.setCellValue(staffHeaders[i]);
                cell.setCellStyle(headerStyle);
            }

            // Add staff performance data
            for (StaffPerformanceDTO staff : staffPerformanceDTOList) {
                Row row = sheet.createRow(rowNum++);

                row.createCell(0).setCellValue(staff.getLocationId());
                row.createCell(1).setCellValue(staff.getStaffName());
                row.createCell(2).setCellValue(staff.getStaffEmail());
                row.createCell(3).setCellValue(staff.getReportFrom());
                row.createCell(4).setCellValue(staff.getReportTo());
                row.createCell(5).setCellValue(staff.getWorkingHours());
                row.createCell(6).setCellValue(staff.getOrderProcessed());

                Cell orderDeltaCell = row.createCell(7);
                String deltaValue = (staff.getDeltaOfOrderProcessedToPreviousPeriod() >= 0 ? "+" : "-") +
                        String.format("%.0f%%", staff.getDeltaOfOrderProcessedToPreviousPeriod());
                orderDeltaCell.setCellValue(deltaValue);

                row.createCell(8).setCellValue(staff.getAverageServiceFeedback());
                row.createCell(9).setCellValue(staff.getMinimumServiceFeedback());

                Cell serviceDeltaCell = row.createCell(10);
                String serviceDeltaValue = (staff.getDeltaOfAverageServiceFeedbackToPreviousPeriod() >= 0 ? "+" : "-") +
                        String.format("%.0f%%", staff.getDeltaOfAverageServiceFeedbackToPreviousPeriod());
                serviceDeltaCell.setCellValue(serviceDeltaValue);

                // Apply data style to all cells
                for (int i = 0; i < 11; i++) {
                    if (row.getCell(i) != null) {
                        row.getCell(i).setCellStyle(dataStyle);
                    }
                }
            }

            rowNum += 3; // Add empty rows for spacing

            // 2. Sales Performance Table
            Row salesTitleRow = sheet.createRow(rowNum++);
            Cell salesTitleCell = salesTitleRow.createCell(0);
            salesTitleCell.setCellValue("Sales Performance");
            salesTitleCell.setCellStyle(titleStyle);
            sheet.addMergedRegion(new CellRangeAddress(rowNum-1, rowNum-1, 0, 9));

            // Create header row for sales performance
            Row salesHeaderRow = sheet.createRow(rowNum++);
            String[] salesHeaders = {
                    "Location", "Report period start", "Report period end",
                    "Orders processed within location", "Delta of orders processed within location to previous period (in %).",
                    "Average cuisine Feedback by Restaurant location (1 to 5)",
                    "Minimum cuisine Feedback by Restaurant location (1 to 5)",
                    "Delta of average cuisine Feedback by Restaurant location to previous period (in %).",
                    "Revenue for orders within reported period (USD)",
                    "Delta of revenue for orders to previous period %"
            };

            for (int i = 0; i < salesHeaders.length; i++) {
                Cell cell = salesHeaderRow.createCell(i);
                cell.setCellValue(salesHeaders[i]);
                cell.setCellStyle(headerStyle);
            }

            // Add sales performance data
            for (SalesPerformanceDTO sales : salesPerformanceDTOList) {
                Row row = sheet.createRow(rowNum++);

                row.createCell(0).setCellValue(sales.getLocationId());
                row.createCell(1).setCellValue(sales.getReportFrom());
                row.createCell(2).setCellValue(sales.getReportTo());
                row.createCell(3).setCellValue(sales.getOrderProcessedWithinLocation());

                Cell orderDeltaCell = row.createCell(4);
                String deltaValue = (sales.getDeltaOfOrderProcessedToPreviousPeriod() >= 0 ? "+" : "-") +
                        String.format("%.0f%%", sales.getDeltaOfOrderProcessedToPreviousPeriod());
                orderDeltaCell.setCellValue(deltaValue);

                row.createCell(5).setCellValue(sales.getAverageCuisineFeedback());
                row.createCell(6).setCellValue(sales.getMinimumCuisineFeedback());

                Cell cuisineDeltaCell = row.createCell(7);
                String cuisineDeltaValue = (sales.getDeltaOfAverageCuisineFeedbackToPreviousPeriod() >= 0 ? "+" : "-") +
                        String.format("%.0f%%", sales.getDeltaOfAverageCuisineFeedbackToPreviousPeriod());
                cuisineDeltaCell.setCellValue(cuisineDeltaValue);

                row.createCell(8).setCellValue(sales.getRevenueForOrders());

                Cell revenueDeltaCell = row.createCell(9);
                String revenueDeltaValue = (sales.getDeltaOfRevenueForOrdersToPreviousPeriod() >= 0 ? "+" : "-") +
                        String.format("%.0f%%", sales.getDeltaOfRevenueForOrdersToPreviousPeriod());
                revenueDeltaCell.setCellValue(revenueDeltaValue);

                // Apply data style to all cells
                for (int i = 0; i < 10; i++) {
                    if (row.getCell(i) != null) {
                        row.getCell(i).setCellStyle(dataStyle);
                    }
                }
            }

            // Convert workbook to byte array
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            return outputStream.toByteArray();

        } catch (IOException e) {
            throw new RuntimeException("Failed to generate Excel report", e);
        }
    }

    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setWrapText(true);
        return style;
    }

    private CellStyle createTitleStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        return style;
    }

    private CellStyle createDataStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    private CellStyle createPercentStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    private CellStyle createCurrencyStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

}
