package com.atakmap.android.QRSend;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Base64;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.atak.plugins.impl.PluginLayoutInflater;
import com.atakmap.android.QRSend.plugin.QRScan;
import com.atakmap.android.QRSend.plugin.R;
import com.atakmap.android.cot.CotMapComponent;
import com.atakmap.android.dropdown.DropDown.OnStateListener;
import com.atakmap.android.dropdown.DropDownReceiver;
import com.atakmap.android.importexport.CotEventFactory;
import com.atakmap.android.maps.MapEvent;
import com.atakmap.android.maps.MapEventDispatcher;
import com.atakmap.android.maps.MapItem;
import com.atakmap.android.maps.MapTouchController;
import com.atakmap.android.maps.MapView;
import com.atakmap.comms.CommsMapComponent;
import com.atakmap.coremap.cot.event.CotEvent;
import com.atakmap.coremap.log.Log;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import gov.tak.platform.graphics.Color;


public class QRSendDropDownReceiver extends DropDownReceiver implements OnStateListener {


    private static final String TAG = "QRSendDropDownRcvr";
    public static final String SHOW_LAYOUT = "com.atakmap.android.QRSend.SHOW_LAYOUT";
    private static final String XML_DECL = "<?xml version='1.0' encoding='UTF-8' standalone='yes'?>";
    private final View QRView;

    private final MapView mapView;


    private String compressString(String data) {
        if (data == null || data.length() == 0) {
            return data;
        }
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream(data.length());
            GZIPOutputStream gzip = new GZIPOutputStream(bos);
            gzip.write(data.getBytes());
            gzip.close();
            byte[] compressed = bos.toByteArray();
            bos.close();
            Log.d(TAG, "compressed length: " + compressed.length);
            return Base64.encodeToString(compressed, Base64.DEFAULT);
        } catch (IOException e) {
            Log.e(TAG, "Error compressing string: " + e.getMessage());
            return null;
        }
    }

    private String decompressString(String compressedData) {
        if (compressedData == null || compressedData.length() == 0) {
            return compressedData;
        }
        try {
            byte[] compressed = Base64.decode(compressedData, Base64.DEFAULT);
            ByteArrayInputStream bis = new ByteArrayInputStream(compressed);
            GZIPInputStream gis = new GZIPInputStream(bis);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();

            byte[] buffer = new byte[1024];
            int len;
            while ((len = gis.read(buffer)) != -1) {
                bos.write(buffer, 0, len);
            }

            String out = bos.toString();
            return XML_DECL+out;
        } catch (IOException e) {
            Log.e(TAG, "Error decompressing string: " + e.getMessage());
            return null;
        }
    }

    private void generateAndDisplayQRCode(CotEvent cotmsg, String mapItemTitle) {
        try {
            String text = cotmsg.toString().replace(XML_DECL,"");
            String regexForEmptyAttributes = "\\s+\\w+=''";
            text = text.replaceAll(regexForEmptyAttributes, "");
            text = compressString(text);

            DisplayMetrics displayMetrics = new DisplayMetrics();
            mapView.getDisplay().getMetrics(displayMetrics);
            int width = displayMetrics.widthPixels;
            int height = displayMetrics.heightPixels;

            int qrCodeSize = Math.min(width, height) ;

            TextView tvMapItemTitle = QRView.findViewById(R.id.tvMapItemTitle);
            tvMapItemTitle.setText(mapItemTitle);

            QRCodeWriter writer = new QRCodeWriter();
            Bitmap bitmap = toBitmap(writer.encode(text, BarcodeFormat.QR_CODE, qrCodeSize, qrCodeSize));
            ImageView qrImageView = QRView.findViewById(R.id.qrCodeImageView);
            qrImageView.setImageBitmap(bitmap);

        } catch (WriterException e) {
            Log.e(TAG, "Error generating QR code: " + e.getMessage());
        }
    }

    private Bitmap toBitmap(BitMatrix matrix) {
        int height = matrix.getHeight();
        int width = matrix.getWidth();
        Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
        for (int x = 0; x < width; x++){
            for (int y = 0; y < height; y++){
                bmp.setPixel(x, y, matrix.get(x, y) ? Color.BLACK : Color.WHITE);
            }
        }
        return bmp;
    }

       private void toast(final String toast) {
                Toast.makeText(QRView.getContext(), toast,
                        Toast.LENGTH_SHORT).show();
            }


    protected QRSendDropDownReceiver(MapView mapView, Context context) {
        super(mapView);
        this.mapView = mapView;
        Log.d(TAG, "QRSendDropDownReceiver ctr");
        QRView = PluginLayoutInflater.inflate(context,
                R.layout.qrsend_layout, null);

        final Button btn_qr= QRView.findViewById(R.id.button_create_qr);
        final TextView tvbtnQr = QRView.findViewById(R.id.tvButton1);
        final Button btnScanQr = QRView.findViewById(R.id.button_scan_qr);
        final TextView tvbtnScanQr = QRView.findViewById(R.id.tvButton2);
        final Button btnClose = QRView.findViewById(R.id.btnClose);



        btnScanQr.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                BroadcastReceiver qrScanReceiver = new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        if (QRScan.QR_INFO.equals(intent.getAction())) {
                            String scannedData = intent.getStringExtra("SCANNED_DATA");
                            CotEvent cot_received = CotEvent.parse(decompressString(scannedData));

                            Bundle extras = new Bundle();
                            extras.putString("from", "QRSend");
                            extras.putString("fromClass",
                                    QRSendComponent.class.getName());
                            extras.putBoolean("visible", true);
                            CommsMapComponent.ImportResult res = CotMapComponent.getInstance()
                                    .processCotEvent(cot_received, extras);
                            if (res == CommsMapComponent.ImportResult.SUCCESS) {
                                MapTouchController.goTo(mapView.getMapItem(cot_received.getUID()), true);
                            }

                        }
                    }
                };

                IntentFilter filter = new IntentFilter(QRScan.QR_INFO);
                context.registerReceiver(qrScanReceiver, filter);
                Intent intent = new Intent();
                intent.setClassName("com.atakmap.android.QRSend.plugin",
                        "com.atakmap.android.QRSend.plugin.QRScan");
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                getMapView().getContext().startActivity(intent);

            }
        });




        btn_qr.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Toast.makeText(mapView.getContext(),
                                "Select object on the map", Toast.LENGTH_SHORT)
                        .show();

                mapView.getMapEventDispatcher().addMapEventListener(
                        MapEvent.ITEM_CLICK, new MapEventDispatcher.MapEventDispatchListener() {

                            @Override
                            public void onMapEvent(MapEvent event) {

                                MapItem mi = event.getItem();
                                if (mi != null) {
                                    CotEvent cotevent = CotEventFactory.createCotEvent(mi);


                                    showDropDown(QRView, FULL_WIDTH, FULL_HEIGHT, FULL_WIDTH, FULL_HEIGHT, true, QRSendDropDownReceiver.this);

                                    btnClose.setVisibility(View.VISIBLE);

                                    btn_qr.setVisibility(View.GONE);
                                    tvbtnQr.setVisibility(View.GONE);
                                    btnScanQr.setVisibility(View.GONE);
                                    tvbtnScanQr.setVisibility(View.GONE);

                                    generateAndDisplayQRCode(cotevent, mi.getTitle());
                                }

                                // clean up the listener
                                mapView.getMapEventDispatcher()
                                        .removeMapEventListener(
                                                MapEvent.ITEM_CLICK, this);
                            }
                        });

                Button btnClose = QRView.findViewById(R.id.btnClose);

                btnClose.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {


                        TextView tvMapItemTitle = QRView.findViewById(R.id.tvMapItemTitle);
                        tvMapItemTitle.setVisibility(View.GONE);

                        ImageView qrImageView = QRView.findViewById(R.id.qrCodeImageView);
                        qrImageView.setImageBitmap(null);


                        // Close the drop-down
                        closeDropDown();
                        btn_qr.setVisibility(View.VISIBLE);
                        tvbtnQr.setVisibility(View.VISIBLE);
                        btnScanQr.setVisibility(View.VISIBLE);
                        tvbtnScanQr.setVisibility(View.VISIBLE);
                        btnClose.setVisibility(View.GONE);
                    }
                });

            }
        });

       }



    @Override
    protected void disposeImpl() {

    }

    @Override
    public void onReceive(Context context, Intent intent) {
        final String action = intent.getAction();
        Log.d(TAG, "onReceive: " +action);
        if (action == null)
            return;

        // Show drop-down
        if (SHOW_LAYOUT.equals(action)) {
            showDropDown(QRView, 0.4, FULL_HEIGHT,
                    0.5, HALF_HEIGHT, false, this);
            setAssociationKey("QRSendPreferences");
        }
    }

    @Override
    public void onDropDownSelectionRemoved() {

    }

    @Override
    public void onDropDownClose() {

    }

    @Override
    public void onDropDownSizeChanged(double v, double v1) {
        Log.d(TAG, "onDropDownSizeChanged");
    }

    @Override
    public void onDropDownVisible(boolean b) {

    }


}
