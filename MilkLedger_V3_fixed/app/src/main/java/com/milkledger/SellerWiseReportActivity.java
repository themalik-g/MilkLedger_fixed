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

import java.util.List;
import java.util.Locale;

public class SellerWiseReportActivity extends AppCompatActivity {
    private Spinner spinnerSeller;
    private TextView tvReport;
    private Button btnGenerate, btnShare, btnBack;
    private DatabaseHelper db;
    private SharedPreferences prefs;
    private long sessionId;
    private boolean reportGenerated = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_seller_wise_report);

        db = new DatabaseHelper(this);
        prefs = getSharedPreferences("MilkLedgerPrefs", MODE_PRIVATE);
        sessionId = prefs.getLong("current_session_id", -1);

        spinnerSeller = findViewById(R.id.spinnerSeller);
        tvReport = findViewById(R.id.tvReport);
        btnGenerate = findViewById(R.id.btnGenerate);
        btnShare = findViewById(R.id.btnShare);
        btnBack = findViewById(R.id.btnBack);

        loadSellers();

        btnGenerate.setOnClickListener(v -> generateReport());
        btnShare.setOnClickListener(v -> shareReport());
        btnBack.setOnClickListener(v -> finish());
    }

    private void loadSellers() {
        List<Seller> sellers = db.getAllSellers(sessionId, false);
        ArrayAdapter<Seller> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, sellers);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSeller.setAdapter(adapter);
    }

    private void generateReport() {
        Seller seller = (Seller) spinnerSeller.getSelectedItem();
        if (seller == null) return;

        List<Entry> entries = db.getEntriesForSeller(seller.getId());
        List<CashTransaction> cashList = db.getCashTransactionsForSeller(seller.getId());

        StringBuilder sb = new StringBuilder();
        sb.append("=== SELLER WISE REPORT ===\n");
        sb.append(String.format("Seller: %s - %s\n", seller.getNumber(), seller.getName()));
        sb.append(String.format("Contact: %s\n\n", seller.getContact()));

        double totalLiters = 0, totalAmount = 0;
        for (Entry e : entries) {
            sb.append(String.format("%s: %.2fL @ %.2f = %.2f (%s)\n",
                    e.getDate(), e.getLiters(), e.getRate(), e.getAmount(), e.getTimeOfDay()));
            totalLiters += e.getLiters();
            totalAmount += e.getAmount();
        }

        sb.append(String.format("\nTotal Milk: %.2fL\n", totalLiters));
        sb.append(String.format("Total Amount: %.2f\n\n", totalAmount));

        double cashPaid = 0, cashReceived = 0;
        for (CashTransaction c : cashList) {
            sb.append(String.format("%s: %.2f (%s) %s\n",
                    c.getDate(), c.getAmount(), c.getType(), c.getNotes()));
            if ("paid".equals(c.getType())) cashPaid += c.getAmount();
            else cashReceived += c.getAmount();
        }

        double balance = db.getSellerBalance(seller.getId());
        sb.append(String.format("\nCash Paid: %.2f\n", cashPaid));
        sb.append(String.format("Cash Received: %.2f\n", cashReceived));
        sb.append(String.format("BALANCE: %.2f (%s)\n", Math.abs(balance), balance >= 0 ? "THEY OWE YOU" : "YOU OWE THEM"));

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
