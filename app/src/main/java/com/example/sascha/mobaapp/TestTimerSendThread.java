package com.example.sascha.mobaapp;

/**
 * Created by Sascha on 09.07.2018.
 */

public class TestTimerSendThread extends Thread{

    ImageSendService service;

    public TestTimerSendThread(ImageSendService service){
        this.service = service;
    }

    @Override
    public void run(){

        for(int i = 0; i < 200; i++){
            service.sendTestPictures();
            try {
                Thread.sleep(4);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
