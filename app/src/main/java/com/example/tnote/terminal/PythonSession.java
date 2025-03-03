package com.example.tnote.terminal;

import android.content.Context;
import android.os.Handler;
import android.util.Log;

import com.chaquo.python.PyObject;
import com.chaquo.python.Python;
import com.chaquo.python.android.AndroidPlatform;
import com.example.tnote.MainActivity;
import com.example.tnote.Utils.Interface.Session;


import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
public class PythonSession implements Session {
    private final Handler mainHandler;
    private OutputListener outputListener;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final AtomicBoolean isAlive = new AtomicBoolean(false);
    private WeakReference<MainActivity> activityRef;

    public PythonSession(MainActivity activity, Handler mainHandler) {
        this.mainHandler = mainHandler;
        this.activityRef = new WeakReference<>(activity);
    }

    @Override
    public void start(OutputListener listener) {
        this.outputListener = listener;
        this.isAlive.set(true);
        sendSystemMessage("Python交互模式已激活");
    }

    @Override
    public boolean executeCommand(String command) {
        if (!isAlive.get()) return false;

        executor.execute(() -> {
            try {
                if (!Python.isStarted()){
                    Python.start(new AndroidPlatform(activityRef.get()));
                }
                Python py = Python.getInstance();
                PyObject os = py.getModule("os");
                os.get("chdir").call(PyObject.fromJava(activityRef.get().getFilesDir().getAbsolutePath()));
                PyObject module = py.getModule("executor");
                Log.println(Log.INFO,"input:",command);
                PyObject result = module.callAttr("execute_code", command);
                Log.println(Log.INFO,"output:", String.valueOf(result));
                Log.println(Log.INFO,"output:", String.valueOf(result.type()));
                String outOrErro= String.valueOf(result);
                mainHandler.post(() -> {
                    if (!outOrErro.isEmpty())
                        outputListener.onOutputReceived(outOrErro+ "\n");
                });
            } catch (Exception e) {
                mainHandler.post(() -> outputListener.onError("执行错误: " + e.getMessage()));
            }
        });
        return true;
    }

    @Override
    public void terminate() {
        executor.shutdownNow();
        isAlive.set(false);
        sendSystemMessage("Python会话已终止");
    }

    @Override
    public boolean isAlive() {
        return isAlive.get();
    }

    private void sendSystemMessage(String message) {
        mainHandler.post(() -> outputListener.onOutputReceived("\n[系统] " + message + "\n"));
    }
}