package com.vexus2.jenkins.chatwork.jenkinschatworkplugin;

public class ChatworkException extends RuntimeException{
    public ChatworkException(){
    }

    public ChatworkException(String message){
        super(message);
    }
}
