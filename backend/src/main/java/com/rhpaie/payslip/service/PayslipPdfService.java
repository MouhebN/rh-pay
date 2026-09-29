package com.rhpaie.payslip.service;

import com.rhpaie.employee.entity.Employee;
import com.rhpaie.payroll.entity.PayrollCalculation;
import com.rhpaie.settings.entity.AppSettings;
import com.rhpaie.settings.service.SettingsService;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
public class PayslipPdfService {

    private static final float PAGE_WIDTH = PDRectangle.A4.getWidth();
    private static final float MARGIN = 42;
    private static final float CONTENT_WIDTH = PAGE_WIDTH - (MARGIN * 2);
    private static final float LEFT_COL = 58;
    private static final float RIGHT_COL = 318;

    private final SettingsService settingsService;

    public byte[] generate(PayrollCalculation calculation) {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
                Employee employee = calculation.getEmployee();
                AppSettings settings = settingsService.getCurrentSettings();

                drawHeader(stream, calculation, settings);
                drawEmployeeCards(stream, employee, calculation, settings);
                drawPayrollTable(stream, calculation, settings);
                drawNetSalaryBand(stream, calculation, settings);
                drawFooter(stream, settings);
            }

            document.save(output);
            return output.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to generate payslip PDF", exception);
        }
    }

    private void drawHeader(PDPageContentStream stream, PayrollCalculation calculation, AppSettings settings) throws IOException {
        setRgb(stream, 23, 23, 23);
        stream.addRect(0, 760, PAGE_WIDTH, 82);
        stream.fill();

        setRgb(stream, 230, 98, 57);
        stream.addRect(0, 760, 9, 82);
        stream.fill();

        setRgb(stream, 255, 255, 255);
        writeText(stream, settings.getCompanyName(), MARGIN, 808, 20, true);
        writeText(stream, companyLine(settings), MARGIN, 790, 10, false);
        writeText(stream, "BULLETIN DE PAIE", 360, 806, 18, true);
        writeText(stream, "Periode : " + formatPeriod(calculation), 360, 787, 11, false);
        writeText(stream, "Genere le : " + LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")), 360, 772, 9, false);
        setRgb(stream, 0, 0, 0);
    }

    private void drawEmployeeCards(PDPageContentStream stream, Employee employee, PayrollCalculation calculation, AppSettings settings) throws IOException {
        drawCard(stream, MARGIN, 675, CONTENT_WIDTH, 64);
        writeText(stream, "INFORMATIONS EMPLOYE", 58, 718, 10, true);
        writeMeta(stream, LEFT_COL, 700, "Nom complet", employee.getFirstName() + " " + employee.getLastName());
        writeMeta(stream, LEFT_COL, 684, "Email", employee.getEmail());
        writeMeta(stream, RIGHT_COL, 700, "Departement", employee.getDepartment() == null ? "-" : employee.getDepartment().getName());
        writeMeta(stream, RIGHT_COL, 684, "Poste", employee.getJobPosition() == null ? "-" : employee.getJobPosition().getTitle());

        drawCard(stream, MARGIN, 600, CONTENT_WIDTH, 54);
        writeText(stream, "BASE DE CALCUL", 58, 633, 10, true);
        writeMeta(stream, LEFT_COL, 616, "Salaire de base", money(calculation.getBaseSalary(), settings));
        writeMeta(stream, RIGHT_COL, 616, "Taux journalier", money(calculation.getDailyRate(), settings));
        writeMeta(stream, RIGHT_COL, 600, "Taux horaire", money(calculation.getHourlyRate(), settings));
    }

    private void drawPayrollTable(PDPageContentStream stream, PayrollCalculation c, AppSettings settings) throws IOException {
        float y = 552;
        writeText(stream, "DETAILS DU CALCUL", MARGIN, y, 12, true);
        y -= 18;

        drawTableHeader(stream, y);
        y -= 24;

        y = drawLine(stream, y, "Salaire de base", "Base contractuelle", c.getBaseSalary(), true, settings);
        y = drawLine(stream, y, "Prime", "Element variable manuel", c.getBonusAmount(), true, settings);
        y = drawLine(stream, y, "Heures supplementaires", c.getOvertimeMinutes() + " min", c.getOvertimeAmount(), true, settings);
        y = drawLine(stream, y, "Absences sans solde", c.getUnpaidAbsenceDays() + " jour(s)", c.getUnpaidAbsenceDeduction(), false, settings);
        y = drawLine(stream, y, "Avance", "Avance sur salaire", c.getAdvanceAmount(), false, settings);
        y = drawLine(stream, y, "Retenue manuelle", "Retenue RH", c.getDeductionAmount(), false, settings);
        y = drawLine(stream, y, "Retard", c.getLateMinutes() + " min", c.getLateDeduction(), false, settings);

        y -= 8;
        drawTotalRow(stream, y, "Salaire brut", c.getGrossSalary(), settings);
        y -= 24;
        drawTotalRow(stream, y, "Total retenues", c.getTotalDeductions(), settings);
    }

    private void drawTableHeader(PDPageContentStream stream, float y) throws IOException {
        setRgb(stream, 245, 246, 248);
        stream.addRect(MARGIN, y - 5, CONTENT_WIDTH, 24);
        stream.fill();
        setRgb(stream, 82, 82, 82);
        writeText(stream, "Libelle", 58, y + 3, 9, true);
        writeText(stream, "Base / reference", 260, y + 3, 9, true);
        writeText(stream, "Gain", 398, y + 3, 9, true);
        writeText(stream, "Retenue", 485, y + 3, 9, true);
        setRgb(stream, 0, 0, 0);
    }

    private float drawLine(PDPageContentStream stream, float y, String label, String basis, BigDecimal amount, boolean earning, AppSettings settings) throws IOException {
        drawHorizontalLine(stream, y - 8);
        setRgb(stream, 38, 38, 38);
        writeText(stream, label, 58, y, 9, false);
        setRgb(stream, 115, 115, 115);
        writeText(stream, basis, 260, y, 9, false);
        setRgb(stream, 23, 23, 23);
        if (earning) {
            writeText(stream, money(amount, settings), 398, y, 9, false);
            writeText(stream, "-", 500, y, 9, false);
        } else {
            writeText(stream, "-", 420, y, 9, false);
            writeText(stream, money(amount, settings), 485, y, 9, false);
        }
        return y - 22;
    }

    private void drawTotalRow(PDPageContentStream stream, float y, String label, BigDecimal amount, AppSettings settings) throws IOException {
        setRgb(stream, 250, 250, 250);
        stream.addRect(MARGIN, y - 7, CONTENT_WIDTH, 22);
        stream.fill();
        setRgb(stream, 23, 23, 23);
        writeText(stream, label, 58, y, 10, true);
        writeText(stream, money(amount, settings), 455, y, 10, true);
    }

    private void drawNetSalaryBand(PDPageContentStream stream, PayrollCalculation calculation, AppSettings settings) throws IOException {
        setRgb(stream, 230, 98, 57);
        stream.addRect(MARGIN, 145, CONTENT_WIDTH, 56);
        stream.fill();
        setRgb(stream, 255, 255, 255);
        writeText(stream, "SALAIRE NET A PAYER", 62, 178, 13, true);
        writeText(stream, "Montant net apres retenues", 62, 160, 9, false);
        writeText(stream, money(calculation.getNetSalary(), settings), 380, 168, 20, true);
        setRgb(stream, 0, 0, 0);
    }

    private void drawFooter(PDPageContentStream stream, AppSettings settings) throws IOException {
        setRgb(stream, 82, 82, 82);
        writeText(stream, "Document genere automatiquement par le systeme " + settings.getCompanyName() + ".", MARGIN, 92, 9, false);
        writeText(stream, "Ce bulletin est fourni a titre academique.", MARGIN, 78, 9, false);
        drawHorizontalLine(stream, 65);
        writeText(stream, "Signature RH", 72, 43, 9, true);
        writeText(stream, "Signature employe", 390, 43, 9, true);
        setRgb(stream, 0, 0, 0);
    }

    private void drawCard(PDPageContentStream stream, float x, float y, float width, float height) throws IOException {
        setRgb(stream, 255, 255, 255);
        stream.addRect(x, y, width, height);
        stream.fill();
        setRgb(stream, 229, 229, 229);
        stream.addRect(x, y, width, height);
        stream.stroke();
        setRgb(stream, 0, 0, 0);
    }

    private void writeMeta(PDPageContentStream stream, float x, float y, String label, String value) throws IOException {
        setRgb(stream, 115, 115, 115);
        writeText(stream, label + " :", x, y, 8, true);
        setRgb(stream, 23, 23, 23);
        writeText(stream, value, x + 82, y, 9, false);
    }

    private void drawHorizontalLine(PDPageContentStream stream, float y) throws IOException {
        setRgb(stream, 230, 230, 230);
        stream.moveTo(MARGIN, y);
        stream.lineTo(MARGIN + CONTENT_WIDTH, y);
        stream.stroke();
        setRgb(stream, 0, 0, 0);
    }

    private void writeText(PDPageContentStream stream, String text, float x, float y, int size, boolean bold) throws IOException {
        stream.beginText();
        stream.setFont(new PDType1Font(bold ? Standard14Fonts.FontName.HELVETICA_BOLD : Standard14Fonts.FontName.HELVETICA), size);
        stream.newLineAtOffset(x, y);
        stream.showText(sanitize(text));
        stream.endText();
    }

    private void setRgb(PDPageContentStream stream, int red, int green, int blue) throws IOException {
        stream.setNonStrokingColor(red / 255f, green / 255f, blue / 255f);
        stream.setStrokingColor(red / 255f, green / 255f, blue / 255f);
    }

    private String money(BigDecimal value, AppSettings settings) {
        return value.setScale(2, RoundingMode.HALF_UP).toPlainString() + " " + settings.getCurrency();
    }

    private String companyLine(AppSettings settings) {
        String contact = String.join(" - ",
                java.util.stream.Stream.of(settings.getCompanyAddress(), settings.getCompanyEmail(), settings.getCompanyPhone())
                        .filter(value -> value != null && !value.isBlank())
                        .toList());
        return contact.isBlank() ? "Gestion RH et paie" : contact;
    }

    private String formatPeriod(PayrollCalculation calculation) {
        return "%02d/%d".formatted(calculation.getMonth(), calculation.getYear());
    }

    private String sanitize(String text) {
        return text == null ? "-" : text
                .replace("é", "e")
                .replace("è", "e")
                .replace("ê", "e")
                .replace("à", "a")
                .replace("ç", "c")
                .replace("’", "'");
    }
}
