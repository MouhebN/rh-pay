package com.rhpaie.payrollexport.service;

import com.rhpaie.payrollexport.model.BankTransferRow;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Service
public class PayrollCsvExportService {

    public byte[] bankTransfer(List<BankTransferRow> rows) {
        StringBuilder csv = new StringBuilder();
        csv.append("Matricule,Nom complet,RIB,Banque,Methode de paiement,Montant net a payer,Mois,Annee,Reference,Alerte\n");
        for (BankTransferRow row : rows) {
            csv.append(value(row.matricule())).append(',')
                    .append(value(row.employeeFullName())).append(',')
                    .append(value(row.rib())).append(',')
                    .append(value(row.bankName())).append(',')
                    .append(value(row.paymentMethod() == null ? "" : row.paymentMethod().name())).append(',')
                    .append(value(money(row.netSalary()))).append(',')
                    .append(row.month()).append(',')
                    .append(row.year()).append(',')
                    .append(value(row.reference())).append(',')
                    .append(value(row.warning())).append('\n');
        }
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    private String money(BigDecimal value) {
        return value == null ? "0.00" : value.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private String value(String raw) {
        String value = raw == null ? "" : raw;
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }
}
