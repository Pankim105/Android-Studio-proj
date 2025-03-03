package com.example.tnote.Utils.Interface;
// Session.java - 会话通用接口
public interface Session {
    void start(OutputListener listener);
    boolean executeCommand(String command);
    void terminate();
    boolean isAlive();

    public interface OutputListener {
        void onOutputReceived(String output);
        void onError(String error);
    }
}