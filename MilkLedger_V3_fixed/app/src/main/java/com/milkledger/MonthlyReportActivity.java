package com.milkledger;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class MonthlyReportActivity extends AppCompatActivity {
    private Spinner spinnerMonth, spinnerYear;
    private TextView tvReport;
    private Button btnGenerate, btnShare, btnBack;
    private DatabaseHelper db;
    private SharedPreferences prefs;
    private long sessionId;
    private boolean reportGenerated = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_monthly_report);

        db = new DatabaseHelper(this);
        prefs = getSharedPreferences("MilkLedgerPrefs", MODE_PRIVATE);
        sessionId = prefs.getLong("current_session_id", -1);

        spinnerMonth = findViewById(R.id.spinnerMonth);
        spinnerYear = findViewById(R.id.spinnerYear);
        tvReport = findViewById(R.id.tvReport);
        btnGenerate = findViewById(R.id.btnGenerate);
        btnShare = findViewById(R.id.btnShare);
        btnBack = findViewById(R.id.btnBack);

        String[] months = {"January", "February", "March", "April", "May", "June",
                "July", "August", "September", "October", "November", "December"};
        spinnerMonth.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, months));

        List<String> years = new ArrayList<>();
        int currentYear = Calendar.getInstance().get(Calendar.YEAR);
        for (int y = currentYear - 5; y <= currentYear + 5; y++) years.add(String.valueOf(y));
        spinnerYear.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, years));
        spinnerYear.setSelection(years.indexOf(String.valueOf(currentYear)));

        btnGenerate.setOnClickListener(v -> generateReport());
        btnShare.setOnClickListener(v -> shareReport());
        btnBack.setOnClickListener(v -> finish());
    }

    private void generateReport() {
        int month = spinnerMonth.getSelectedItemPosition() + 1;
        int year = Integer.parseInt((String) spinnerYear.getSelectedItem());

        List<Entry> entries = db.getEntriesForMonth(month, year, sessionId);
        List<CashTransaction> cashList = db.getCashTransactionsForMonth(month, year, sessionId);

        StringBuilder sb = new StringBuilder();
        sb.append("=== MONTHLY REPORT ===\n");
        sb.append(String.format(Locale.getDefault(), "Month: %02d/%d\n\n", month, year));

        double totalLiters = 0, totalAmount = 0;
        for (Entry e : entries) {
            sb.append(String.format("%s: %s %s - %.2fL @ %.2f = %.2f (%s)\n",
                    e.getDate(), e.getSellerNumber(), e.getSellerName(),
                    e.getLiters(), e.getRate(), e.getAmount(), e.getTimeOfDay()));
            totalLiters += e.getLiters();
            totalAmount += e.getAmount();
        }

        sb.append(String.format("\nTotal Milk: %.2fL\n", totalLiters));
        sb.append(String.format("Total Amount: %.2f\n\n", totalAmount));

        double cashPaid = 0, cashReceived = 0;
        for (CashTransaction c : cashList) {
            sb.append(String.format("%s: %s - %.2f (%s) %s\n",
                    c.getDate(), c.getSellerName(), c.getAmount(), c.getType(), c.getNotes()));
            if ("paid".equals(c.getType())) cashPaid += c.getAmount();
            else cashReceived += c.getAmount();
        }

        sb.append(String.format("\nCash Paid: %.2f\n", cashPaid));
        sb.append(String.format("Cash Received: %.2f\n", cashReceived));

        tvReport.setText(sb.toString());
        reportGenerated = true;
    }

    private void shareReport() {
        if (!reportGenerated || tvReport.getText().toString().isEmpty()) {
            Toast.makeText(this, "Generate report first", Toast.LENGTH_SHORT).show();
            return;
        }
        String text = tvReport.getText().toString();
        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT, text);
        sendIntent.setType("text/plain");
        sendIntent.setPackage("com.whatsapp");

        try {
            startActivity(sendIntent);
        } catch (Exception e) {
            sendIntent.setPackage(null);
            startActivity(Intent.createChooser(sendIntent, "Share via"));
        }
    }
}
