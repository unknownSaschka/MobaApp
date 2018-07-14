package com.example.sascha.mobaapp;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.media.Image;
import android.media.ImageReader;
import android.util.Log;

import java.io.ByteArrayOutputStream;

public class OnNewImageReadyListener implements ImageReader.OnImageAvailableListener {
    private Bitmap _reusedBitmap;
    private ByteArrayOutputStream _JPEGOutputStream = new ByteArrayOutputStream();
    private int _JPEGQuality = 30;
    private Matrix resizeMatrix = new Matrix();

    @Override
    public synchronized void onImageAvailable(ImageReader _ImageReader) {
        Image tempImage = tryToGetLatestImage(_ImageReader);
        resizeMatrix.setScale(0.5f, 0.5f);

        if(tempImage == null){
            return;
        }

        Bitmap cleanBitmap = getCleanedBitmap(tempImage);
        Bitmap resizedBitmap = getResizedBitmap(cleanBitmap, tempImage);
        compressAsJPEG(resizedBitmap);

        //Do some Cleanup!
        tempImage.close();
        cleanBitmap.recycle();
        resizedBitmap.recycle();

        sendStreamAsByteArray();

    }

    /**
     * Tries to get the latest Image from the ImageReader
     * @param _ImageReader The Reader, that has the Image
     * @return May be null otherwise the latest Image.
     */
    private Image tryToGetLatestImage(ImageReader _ImageReader){
        Image tempImage;
        try{
            tempImage = _ImageReader.acquireLatestImage();
            return tempImage;
        }
        catch(Exception e){
            if(Debug.InDebugging){
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
     * @param rawImage Image to clear. Must not be null!
     * @return cleared Bitmap without padding.
     */
    private Bitmap getCleanedBitmap(Image rawImage){
        Bitmap cleanedBitmap;

        //We are uniplanar. Like an unicorn!
        Image.Plane rawPlane = rawImage.getPlanes()[0];
        //RowStride ~= size of one pixelrow (in Byte)
        //PixelStride ~= size of an pixeldata (in Byte)
        int pixelPerRow = rawPlane.getRowStride()/rawPlane.getPixelStride();

        //Is there Padding in the raw data?
        //Yes? Then cut of padding.
        if(pixelPerRow > rawImage.getWidth()){
            if(_reusedBitmap == null){
                _reusedBitmap = Bitmap.createBitmap(pixelPerRow,rawImage.getHeight(), Bitmap.Config.ARGB_8888);
            }
            _reusedBitmap.copyPixelsFromBuffer(rawPlane.getBuffer());
            cleanedBitmap = Bitmap.createBitmap(_reusedBitmap, 0, 0, rawImage.getWidth(), rawImage.getHeight());
        }else{
            cleanedBitmap = Bitmap.createBitmap(rawImage.getWidth(), rawImage.getHeight(), Bitmap.Config.ARGB_8888);
            cleanedBitmap.copyPixelsFromBuffer(rawPlane.getBuffer());
        }
        return cleanedBitmap;
    }

    private Bitmap getResizedBitmap(Bitmap cleanBitmap, Image rawImage){
        Bitmap resizedBitmap;

        resizedBitmap = Bitmap.createBitmap(cleanBitmap, 0, 0, rawImage.getWidth(), rawImage.getHeight(), resizeMatrix, false);

        return resizedBitmap;
    }

    private void compressAsJPEG(Bitmap bitmapToCompress){
        _JPEGOutputStream.reset();
        bitmapToCompress.compress(Bitmap.CompressFormat.JPEG, _JPEGQuality, _JPEGOutputStream);
    }

    private void sendStreamAsByteArray(){
        //TODO: sending Bytearray
        //_JPEGOutputStream.toByteArray()
    }
}
