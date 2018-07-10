package com.example.sascha.mobaapp;

import android.media.Image;
import android.media.ImageReader;
import android.util.Log;

public class OnNewImageReadyListener implements ImageReader.OnImageAvailableListener {
    @Override
    public synchronized void onImageAvailable(ImageReader _ImageReader) {
        Image _Image = null;


        try{
            _Image = _ImageReader.acquireLatestImage();
            _Image.close();
            if(Debug.InDebugging){
                //Log.d("ImageReader", "Here should be an Image.");
            }
        }
        catch(Exception e){
            if(Debug.InDebugging){
                //Log.d("ImageReader", "Could not read Image.");
            }
        }





    }
}
