package com.example.sascha.mobaapp;

import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.support.v4.content.LocalBroadcastManager;
import android.widget.ImageView;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

public class QRGeneratorThread extends Thread{

    private String ip;
    private ImageView qr;
    private Context context;
    private LocalBroadcastManager _localBroadcaster;

    public QRGeneratorThread(String ip, ImageView qr, Context context){
        this.ip = ip;
        this.qr = qr;
        this.context = context;
        initLocalBroadcaster();
    }

    @Override
    public void run(){
        int width = qr.getWidth();
        int height = qr.getHeight();

        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        BitMatrix bitMatrix = null;
        try {
            bitMatrix = qrCodeWriter.encode("http://" + ip + ":" + Constants.HTTP_SERVER_PORT, BarcodeFormat.QR_CODE, width, height);
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
