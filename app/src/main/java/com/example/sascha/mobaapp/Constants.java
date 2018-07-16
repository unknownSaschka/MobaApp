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
    public static final String SERVER_HTTP_EVENT_NAME_COMMAND = "server.http.event.command";
    public static final String SERVER_HTTP_COMMAND = "server.http.command";
    public static final String SERVER_HTTP_START = "server.http.command.start";
    public static final String SERVER_HTTP_STOP = "server.http.command.stop";
    public static final String SERVER_HTTP_IS_RUNNING_TRUE = "server.http.status.running.true";
    public static final String SERVER_HTTP_IS_RUNNING_FALSE = "server.http.status.running.false";

    //Used for starting and Stopping the capturing.
    public static final String CAPTURE_EVENT_NAME_COMMAND = "server.capture.event.command";
    public static final String CAPTURE_COMMAND = "server.capture.command";
    public static final String CAPTURE_INIT = "server.capture.command.init";
    public static final String CAPTURE_MEDIA_GRANTING_TOKEN_INTENT = "capture.token.intent";
    public static final String CAPTURE_START = "server.capture.command.start";
    public static final String CAPTURE_STOP = "server.capture.command.stop";

    //Used for Http-Server
    public static final int INET_PERMS = 1;
    public static int HTTP_SERVER_PORT = 8080;

    //Used for starting Capturing
    public static final int REQUEST_CODE_SCREEN_CAPTURE = 69;

    //Used for QR Code generator
    public static final String QR_CODE_EVENT = "qr.code.event";
    public static final String QR_CODE_DATA = "qr.code.data";

    //Used for XML
    public static final String XML_Filename = "settings.xml";


    //_JPEGOutputStream.toByteArray()
    //Bitmap blarg = BitmapFactory.decodeByteArray(_JPEGOutputStream.toByteArray(), 0, _JPEGOutputStream.toByteArray().length);
}
