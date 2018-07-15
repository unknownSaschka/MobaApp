package com.example.sascha.mobaapp;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.media.Image;
import android.media.ImageReader;
import android.util.Log;

import java.io.ByteArrayOutputStream;

public class OnNewImageReadyListener implements ImageReader.OnImageAvailableListener {
    private CaptureService _parent;
    private ByteArrayOutputStream _JPEGOutputStream = new ByteArrayOutputStream();
    private int _JPEGQuality = 50;
    private Matrix _resizeMatrix = new Matrix();

    //Holds the correct Image Data after cleanup as long the image is still in process.
    private int _width = 0;
    private int _height = 0;

    public OnNewImageReadyListener(CaptureService parent) {
        _parent = parent;
    }

    @Override
    public synchronized void onImageAvailable(ImageReader _ImageReader) {
        synchronized (_parent) {
            Image tempImage = tryToGetLatestImage(_ImageReader);
            _resizeMatrix.setScale(0.5f, 0.5f);

            if (tempImage == null) {
                return;
            }
            Bitmap cleanBitmap = getCleanedBitmap(tempImage);
            Bitmap resizedBitmap = getResizedBitmap(cleanBitmap, tempImage);

            compressAsJPEG(resizedBitmap);

            //Do some Cleanup! .close is needed!
            tempImage.close();
            cleanBitmap.recycle();
            resizedBitmap.recycle();

            sendStreamAsByteArray();

            _width = 0;
            _height = 0;
        }
    }

    /**
     * Tries to get the latest Image from the ImageReader
     *
     * @param _ImageReader The Reader, that has the Image
     * @return May be null otherwise the latest Image.
     */
    private Image tryToGetLatestImage(ImageReader _ImageReader) throws IllegalStateException {
        Image tempImage;
        try {
            tempImage = _ImageReader.acquireLatestImage();
            return tempImage;
        } catch (Exception e) {
            if (Debug.InDebugging) {
                Log.d("ImageReader", "Could not read Image.");
            }
            return null;
        }
    }

    /**
     * Using Voodomagic to clean the raw Image of padding.
     * Basicinfo: Stride ~= Image width + padding.
     * => Padding at the right of the image gets in the mid of the bytestream.
     * Link: https://www.collabora.com/news-and-blog/blog/2016/02/16/a-programmers-view-on-digital-images-the-essentials/
     *
     * @param rawImage Image to clear. Must not be null!
     * @return cleared Bitmap without padding.
     */
    private Bitmap getCleanedBitmap(Image rawImage) throws IllegalStateException {
        Bitmap cleanedBitmap;

        //We are uniplanar. Like an unicorn!
        Image.Plane rawPlane = rawImage.getPlanes()[0];
        //RowStride ~= size of one pixelrow (in Byte)
        //PixelStride ~= size of an pixeldata (in Byte)
        int pixelPerRow = rawPlane.getRowStride() / rawPlane.getPixelStride();

        //Is there Padding in the raw data?
        //Yes? Then make bigger Image.
        if (pixelPerRow > rawImage.getWidth()) {
            Bitmap tempBitmap = Bitmap.createBitmap(pixelPerRow, rawImage.getHeight(), Bitmap.Config.ARGB_8888);

            tempBitmap.copyPixelsFromBuffer(rawPlane.getBuffer());
            cleanedBitmap = Bitmap.createBitmap(tempBitmap, 0, 0, rawImage.getWidth(), rawImage.getHeight());
        } else {
            cleanedBitmap = Bitmap.createBitmap(rawImage.getWidth(), rawImage.getHeight(), Bitmap.Config.ARGB_8888);
            cleanedBitmap.copyPixelsFromBuffer(rawPlane.getBuffer());
        }
        _width = rawImage.getWidth();
        _height = rawImage.getHeight();
        return cleanedBitmap;
    }

    private Bitmap getResizedBitmap(Bitmap cleanBitmap, Image rawImage) throws IllegalStateException {
        Bitmap resizedBitmap;

        resizedBitmap = Bitmap.createBitmap(cleanBitmap, 0, 0, rawImage.getWidth(), rawImage.getHeight(), _resizeMatrix, false);

        return resizedBitmap;
    }

    /**
     * Compresses the bitmap as JPEG to the class member output.
     *
     * @param bitmapToCompress The bitmap to compress.
     */
    private void compressAsJPEG(Bitmap bitmapToCompress) {
        _JPEGOutputStream.reset();
        bitmapToCompress.compress(Bitmap.CompressFormat.JPEG, _JPEGQuality, _JPEGOutputStream);
    }

    /**
     * asks the parent to send the outputstream casted to bytearray.
     */
    private void sendStreamAsByteArray() {
        Intent intentToSend = new Intent(Constants.IMAGE_EVENT_NAME);
        intentToSend.putExtra(Constants.IMAGE_WIDTH, _width);
        intentToSend.putExtra(Constants.IMAGE_HEIGHT, _height);
        intentToSend.putExtra(Constants.IMAGE_DATA_NAME, _JPEGOutputStream.toByteArray());

        _parent.sendImage(intentToSend);
    }
}
