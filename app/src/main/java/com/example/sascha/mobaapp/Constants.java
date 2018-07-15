package com.example.sascha.mobaapp;

public abstract class Constants {
    //Used for Image broadcasting
    public static final String IMAGE_EVENT_NAME = "image";
    public static final String IMAGE_WIDTH = "image_width";
    public static final String IMAGE_HEIGHT = "image_height";
    public static final String IMAGE_DATA_NAME = "image_data";

    //Used for IP requesting
    public static final String IP_REQUEST = "ip_request";
    public static final String IP_ANSWER = "ip_request_answer";
    public static final String IP_ANSWER_ADDRESS = "ip_answer_address";
    public static final String IP_ANSWER_FLAG_RUN = "ip_answer_flag_run";

    //Used for Starting and Stopping the Http Server.
    public static final String SERVER_HTTP_EVENT_NAME_COMMAND = "server.event.command";
    public static final String SERVER_HTTP_COMMAND = "server.command";
    public static final String SERVER_HTTP_START = "server.command.start";
    public static final String SERVER_HTTP_STOP = "server.command.stop";
    public static final String SERVER_HTTP_ISRUNNING_TRUE = "server.status.running.true";
    public static final String SERVER_HTTP_ISRUNNING_FALSE = "server.status.running.false";


    //Used for Http-Server
    public static final int INET_PERMS = 1;
    public static final int HTTP_SERVER_PORT = 8080;

    //Used for starting Capturing
    public static final int REQUEST_CODE_SCREEN_CAPTURE = 69;

    //_JPEGOutputStream.toByteArray()
    //Bitmap blarg = BitmapFactory.decodeByteArray(_JPEGOutputStream.toByteArray(), 0, _JPEGOutputStream.toByteArray().length);
}
