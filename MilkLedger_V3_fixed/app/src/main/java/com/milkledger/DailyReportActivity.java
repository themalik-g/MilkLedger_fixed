package com.milkledger;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class DailyReportActivity extends AppCompatActivity {
    private TextView tvReport;
    private Button btnShare, btnBack;
    private DatabaseHelper db;
    private SharedPreferences prefs;
    private long sessionId;
    private String currentDate;
    private boolean reportGenerated = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report_view);

        db = new DatabaseHelper(this);
        prefs = getSharedPreferences("MilkLedgerPrefs", MODE_PRIVATE);
        sessionId = prefs.getLong("current_session_id", -1);

        tvReport = findViewById(R.id.tvReport);
        btnShare = findViewById(R.id.btnShare);
        btnBack = findViewById(R.id.btnBack);

        currentDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Calendar.getInstance().getTime());

        generateReport();

        btnShare.setOnClickListener(v -> shareReport());
        btnBack.setOnClickListener(v -> finish());
    }

    private void generateReport() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== MILK LEDGER - DAILY REPORT ===
");
        sb.append("Date: ").append(currentDate).append("

");

        List<Entry> entries = db.getEntriesForDate(currentDate, sessionId);

        double totalMorningLiters = 0, totalMorningAmount = 0;
        double totalEveningLiters = 0, totalEveningAmount = 0;

        sb.append("--- MORNING ---
");
        boolean hasMorning = false;
        for (Entry e : entries) {
            if ("morning".equals(e.getTimeOfDay())) {
                sb.append(String.format(Locale.getDefault(), "%s %s: %.2fL @ %.2f = %.2f
",
                        e.getSellerNumber(), e.getSellerName(), e.getLiters(), e.getRate(), e.getAmount()));
                totalMorningLiters += e.getLiters();
                totalMorningAmount += e.getAmount();
                hasMorning = true;
            }
        }
        if (!hasMorning) sb.append("No morning entries
");
        sb.append(String.format(Locale.getDefault(), "Morning Total: %.2fL, Amount: %.2f

", totalMorningLiters, totalMorningAmount));

        sb.append("--- EVENING ---
");
        boolean hasEvening = false;
        for (Entry e : entries) {
            if ("evening".equals(e.getTimeOfDay())) {
                sb.append(String.format(Locale.getDefault(), "%s %s: %.2fL @ %.2f = %.2f
",
                        e.getSellerNumber(), e.getSellerName(), e.getLiters(), e.getRate(), e.getAmount()));
                totalEveningLiters += e.getLiters();
                totalEveningAmount += e.getAmount();
                hasEvening = true;
            }
        }
        if (!hasEvening) sb.append("No evening entries
");
        sb.append(String.format(Locale.getDefault(), "Evening Total: %.2fL, Amount: %.2f

", totalEveningLiters, totalEveningAmount));

        sb.append("--- COMBINED ---
");
        sb.append(String.format(Locale.getDefault(), "Total Liters: %.2f
", totalMorningLiters + totalEveningLiters));
        sb.append(String.format(Locale.getDefault(), "Total Amount: %.2f

", totalMorningAmount + totalEveningAmount));

        double cashPaid = db.getTotalCashPaidForDate(currentDate, sessionId);
        double cashReceived = db.getTotalCashReceivedForDate(currentDate, sessionId);
        sb.append(String.format(Locale.getDefault(), "Cash Paid: %.2f
", cashPaid));
        sb.append(String.format(Locale.getDefault(), "Cash Received: %.2f
", cashReceived));
        sb.append(String.format(Locale.getDefault(), "Net Cash: %.2f
", cashReceived - cashPaid));

        tvReport.setText(sb.toString());
        reportGenerated = true;
    }

    private void shareReport() {
        if (!reportGenerated || tvReport.getText().toString().isEmpty()) {
            Toast.makeText(this, "No report to share", Toast.LENGTH_SHORT).show();
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
