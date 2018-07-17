package com.Simple_Stream.Settings;

import android.content.Context;
import android.util.Log;

import com.Simple_Stream.Constants;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;

public class XMLUtils {

    Context context;
    File xml;

    public XMLUtils(Context context){
        this.context = context;
    }

    public String[] getXMLSettings(){

        xml = new File(context.getFilesDir(), Constants.XML_Filename);
        if(!xml.exists()){
            try {
                xml.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
            createXML(null, null,null);
        }

        return readXML(xml);
    }

    public synchronized void createXML(String httpPort, String quality, String scale){
        if(Constants.InDebugging){
            Log.i("XMLUtils", "Create XML");
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
            toWrite += "0.5" + "\n";
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

    private String[] readXML(File xml){
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

        if(Constants.InDebugging){
            Log.i("XMLUtils", "HTTPPort: " + httpPort + " Quality: " + quality + " Scale: " + scale);
        }

        String[] ret  = new String[3];
        ret[0] = httpPort;
        ret[1] = quality;
        ret[2] = scale;

        return ret;
    }
}
