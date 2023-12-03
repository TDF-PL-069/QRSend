package com.atakmap.android.QRSend.plugin;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;

import com.journeyapps.barcodescanner.CompoundBarcodeView;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class QRScan extends Activity {
    private CompoundBarcodeView barcodeScannerView;
    public static final String QR_INFO = "com.atakmap.android.QRSend.QRSCAN";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qrscan);

        if (ContextCompat.checkSelfPermission(this.getApplicationContext(), Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    666);
        }

        barcodeScannerView = findViewById(R.id.barcode_scanner);
        barcodeScannerView.decodeSingle(result -> {
            if (result != null) {
                sendResult(result.getText());
            } else {
                finish();
            }
        });
    }

    private void sendResult(String scannedData) {
        Intent i = new Intent(QR_INFO);
        i.putExtra("SCANNED_DATA", scannedData);
        sendBroadcast(i);
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        barcodeScannerView.resume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        barcodeScannerView.pause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        barcodeScannerView.pause();
    }
}
