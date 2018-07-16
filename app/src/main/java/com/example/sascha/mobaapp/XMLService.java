package com.example.sascha.mobaapp;

import android.content.Context;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

public class XMLService {

    Context context;

    public XMLService(Context context){
        this.context = context;
    }

    public void handleXML(){

        File xml = new File(context.getFilesDir(), Constants.XML_Filename);
        if(!xml.exists()){
            try {
                xml.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
            createXML(null, null,null, xml);
        }

        readXML(xml);
    }

    private void createXML(String httpPort, String quality, String scale, File xml){
        if(Debug.InDebugging){
            Log.i("XMLService", "Create XML");
        }

        String toWrite = "";
        if(httpPort == null) {
            toWrite += "8080" + "\n";
        }
        else {
            toWrite += httpPort + "\n";
        }
        if(quality == null){
            toWrite += "50" + "\n";
        }
        else{
            toWrite += quality + "\n";
        }
        if(scale == null){
            toWrite += "50" + "\n";
        }
        else{
            toWrite += scale + "\n";
        }

        try {
            FileOutputStream fOut = new FileOutputStream(xml);
            OutputStreamWriter outWriter = new OutputStreamWriter(fOut);
            outWriter.append(toWrite);
            outWriter.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void readXML(File xml){
        String httpPort = null;
        String quality = null;
        String scale = null;
        try {
            FileInputStream in = new FileInputStream(xml);
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            httpPort = reader.readLine();
            quality = reader.readLine();
            scale = reader.readLine();
            reader.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        if(Debug.InDebugging){
            Log.i("XMLService", "HTTPPort: " + httpPort + " Quality: " + quality + " Scale: " + scale);
        }
    }
}
