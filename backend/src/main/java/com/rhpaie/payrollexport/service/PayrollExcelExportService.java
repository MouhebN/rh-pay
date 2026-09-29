package com.rhpaie.payrollexport.service;

import com.rhpaie.payrollexport.model.BankTransferRow;
import com.rhpaie.payrollexport.model.PayrollJournalRow;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

@Service
public class PayrollExcelExportService {

    public byte[] payrollJournal(Integer month, Integer year, String companyName, List<PayrollJournalRow> rows) {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Journal de paie");
            Styles styles = new Styles(workbook);

            title(sheet, styles, "%s - Journal de paie - %02d/%d".formatted(companyName, month, year), 22);

            String[] headers = {
                    "Matricule", "Nom complet", "Email", "Département", "Poste", "Mois", "Année",
                    "Salaire de base", "Taux journalier", "Taux horaire", "Absences sans solde",
                    "Retenue absences", "Retards minutes", "Retenue retards", "Heures supplémentaires minutes",
                    "Montant heures supplémentaires", "Prime", "Avance", "Retenue manuelle",
                    "Salaire brut", "Total retenues", "Salaire net", "Statut"
            };
            header(sheet, styles, 2, headers);

            int rowIndex = 3;
            for (PayrollJournalRow row : rows) {
                Row excelRow = sheet.createRow(rowIndex++);
                int col = 0;
                text(excelRow, col++, row.matricule(), styles.text);
                text(excelRow, col++, row.employeeFullName(), styles.text);
                text(excelRow, col++, row.email(), styles.text);
                text(excelRow, col++, row.departmentName(), styles.text);
                text(excelRow, col++, row.jobPositionName(), styles.text);
                number(excelRow, col++, row.month(), styles.number);
                number(excelRow, col++, row.year(), styles.number);
                money(excelRow, col++, row.baseSalary(), styles.money);
                money(excelRow, col++, row.dailyRate(), styles.money);
                money(excelRow, col++, row.hourlyRate(), styles.money);
                decimal(excelRow, col++, row.unpaidAbsenceDays(), styles.decimal);
                money(excelRow, col++, row.unpaidAbsenceDeduction(), styles.money);
                number(excelRow, col++, row.lateMinutes(), styles.number);
                money(excelRow, col++, row.lateDeduction(), styles.money);
                number(excelRow, col++, row.overtimeMinutes(), styles.number);
                money(excelRow, col++, row.overtimeAmount(), styles.money);
                money(excelRow, col++, row.bonusAmount(), styles.money);
                money(excelRow, col++, row.advanceAmount(), styles.money);
                money(excelRow, col++, row.deductionAmount(), styles.money);
                money(excelRow, col++, row.grossSalary(), styles.money);
                money(excelRow, col++, row.totalDeductions(), styles.money);
                money(excelRow, col++, row.netSalary(), styles.money);
                text(excelRow, col, row.status().name(), styles.text);
            }

            Row total = sheet.createRow(rowIndex);
            text(total, 0, "TOTAL", styles.total);
            money(total, 7, sum(rows.stream().map(PayrollJournalRow::baseSalary).toList()), styles.totalMoney);
            money(total, 15, sum(rows.stream().map(PayrollJournalRow::overtimeAmount).toList()), styles.totalMoney);
            money(total, 16, sum(rows.stream().map(PayrollJournalRow::bonusAmount).toList()), styles.totalMoney);
            money(total, 17, sum(rows.stream().map(PayrollJournalRow::advanceAmount).toList()), styles.totalMoney);
            money(total, 19, sum(rows.stream().map(PayrollJournalRow::grossSalary).toList()), styles.totalMoney);
            money(total, 20, sum(rows.stream().map(PayrollJournalRow::totalDeductions).toList()), styles.totalMoney);
            money(total, 21, sum(rows.stream().map(PayrollJournalRow::netSalary).toList()), styles.totalMoney);

            autosize(sheet, headers.length);
            workbook.write(output);
            return output.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to generate payroll journal export", exception);
        }
    }

    public byte[] bankTransfer(Integer month, Integer year, String companyName, List<BankTransferRow> rows) {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Virements");
            Styles styles = new Styles(workbook);

            title(sheet, styles, "%s - Fichier de virement - %02d/%d".formatted(companyName, month, year), 9);

            String[] headers = {
                    "Matricule", "Nom complet", "RIB", "Banque", "Méthode de paiement",
                    "Montant net à payer", "Mois", "Année", "Référence", "Alerte"
            };
            header(sheet, styles, 2, headers);

            int rowIndex = 3;
            for (BankTransferRow row : rows) {
                Row excelRow = sheet.createRow(rowIndex++);
                int col = 0;
                text(excelRow, col++, row.matricule(), styles.text);
                text(excelRow, col++, row.employeeFullName(), styles.text);
                text(excelRow, col++, row.rib(), styles.text);
                text(excelRow, col++, row.bankName(), styles.text);
                text(excelRow, col++, row.paymentMethod() == null ? "" : row.paymentMethod().name(), styles.text);
                money(excelRow, col++, row.netSalary(), styles.money);
                number(excelRow, col++, row.month(), styles.number);
                number(excelRow, col++, row.year(), styles.number);
                text(excelRow, col++, row.reference(), styles.text);
                text(excelRow, col, row.warning(), row.warning().isBlank() ? styles.text : styles.warning);
            }

            Row total = sheet.createRow(rowIndex);
            text(total, 0, "TOTAL NET A PAYER", styles.total);
            money(total, 5, sum(rows.stream().map(BankTransferRow::netSalary).toList()), styles.totalMoney);

            autosize(sheet, headers.length);
            workbook.write(output);
            return output.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to generate bank transfer export", exception);
        }
    }

    private void title(Sheet sheet, Styles styles, String title, int lastColumn) {
        Row row = sheet.createRow(0);
        Cell cell = row.createCell(0);
        cell.setCellValue(title);
        cell.setCellStyle(styles.title);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, lastColumn));
    }

    private void header(Sheet sheet, Styles styles, int rowIndex, String[] headers) {
        Row row = sheet.createRow(rowIndex);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = row.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(styles.header);
        }
    }

    private void text(Row row, int column, String value, CellStyle style) {
        Cell cell = row.createCell(column);
        cell.setCellValue(value == null ? "" : value);
        cell.setCellStyle(style);
    }

    private void number(Row row, int column, Integer value, CellStyle style) {
        Cell cell = row.createCell(column);
        cell.setCellValue(value == null ? 0 : value);
        cell.setCellStyle(style);
    }

    private void decimal(Row row, int column, BigDecimal value, CellStyle style) {
        Cell cell = row.createCell(column);
        cell.setCellValue(value == null ? 0 : value.doubleValue());
        cell.setCellStyle(style);
    }

    private void money(Row row, int column, BigDecimal value, CellStyle style) {
        decimal(row, column, value, style);
    }

    private BigDecimal sum(List<BigDecimal> values) {
        return values.stream().filter(value -> value != null).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private void autosize(Sheet sheet, int columns) {
        for (int i = 0; i < columns; i++) {
            sheet.autoSizeColumn(i);
            sheet.setColumnWidth(i, Math.min(sheet.getColumnWidth(i) + 700, 9000));
        }
    }

    private static class Styles {
        final CellStyle title;
        final CellStyle header;
        final CellStyle text;
        final CellStyle number;
        final CellStyle decimal;
        final CellStyle money;
        final CellStyle total;
        final CellStyle totalMoney;
        final CellStyle warning;

        Styles(Workbook workbook) {
            DataFormat format = workbook.createDataFormat();
            Font titleFont = workbook.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 15);

            Font boldFont = workbook.createFont();
            boldFont.setBold(true);

            title = workbook.createCellStyle();
            title.setFont(titleFont);

            header = workbook.createCellStyle();
            header.setFont(boldFont);
            header.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            header.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            header.setBorderBottom(BorderStyle.THIN);

            text = workbook.createCellStyle();

            number = workbook.createCellStyle();
            number.setDataFormat(format.getFormat("0"));

            decimal = workbook.createCellStyle();
            decimal.setDataFormat(format.getFormat("0.00"));

            money = workbook.createCellStyle();
            money.setDataFormat(format.getFormat("#,##0.00"));

            total = workbook.createCellStyle();
            total.setFont(boldFont);
            total.setFillForegroundColor(IndexedColors.LIGHT_YELLOW.getIndex());
            total.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            totalMoney = workbook.createCellStyle();
            totalMoney.cloneStyleFrom(total);
            totalMoney.setDataFormat(format.getFormat("#,##0.00"));

            warning = workbook.createCellStyle();
            warning.setFillForegroundColor(IndexedColors.LIGHT_ORANGE.getIndex());
            warning.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        }
    }
}
