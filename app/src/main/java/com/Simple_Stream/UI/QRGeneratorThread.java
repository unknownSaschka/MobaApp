package com.Simple_Stream.UI;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.support.v4.content.LocalBroadcastManager;
import android.widget.ImageView;

import com.Simple_Stream.Constants;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

public class QRGeneratorThread extends Thread {

    private String _ip_shadow;
    private String _HttpServerPort_shadow;
    private LocalBroadcastManager _localBroadcaster;
    private ImageView qr;
    private Context context;

    public QRGeneratorThread(String ip, String port, ImageView qr, Context context) {
        _ip_shadow = ip;
        this.qr = qr;
        this.context = context;
        _HttpServerPort_shadow = port;
        initLocalBroadcaster();
    }

    @Override
    public void run() {
        int width = qr.getWidth();
        int height = qr.getHeight();

        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        BitMatrix bitMatrix = null;
        try {
            bitMatrix = qrCodeWriter.encode("http://" + _ip_shadow + ":" + _HttpServerPort_shadow, BarcodeFormat.QR_CODE, width, height);
        } catch (WriterException e) {
            e.printStackTrace();
        }
        if (bitMatrix == null) return;
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                bitmap.setPixel(i, j, bitMatrix.get(i, j) ? Color.BLACK : Color.WHITE);
            }
        }
        Intent toSend = new Intent(Constants.QR_CODE_EVENT);
        toSend.putExtra(Constants.QR_CODE_DATA, bitmap);
        _localBroadcaster.sendBroadcast(toSend);
        cleanLocalBroadcaster();
    }

    private void initLocalBroadcaster() {
        if (_localBroadcaster == null) {
            _localBroadcaster = LocalBroadcastManager.getInstance(context);
        }
    }

    private void cleanLocalBroadcaster() {
        if (_localBroadcaster != null) {
            _localBroadcaster = null;
        }
    }
}
