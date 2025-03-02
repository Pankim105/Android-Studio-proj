
package com.example.tnote.terminal;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.example.tnote.MainActivity;
import com.example.tnote.filebrowser.FileBrowserFragment;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
public class TerminalSession {
    private static final String TAG = "TerminalSession";
    private String appDir;
    private String currentDirectory;
    private Process process;
    private WeakReference<MainActivity> activityRef;
    private Thread inputThread;
    private Thread outputThread;
    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    // 输入输出流
    private OutputStream processOut;
    private InputStream processIn;
    private InputStream processErr;
    private boolean isShell;

    public TerminalSession(MainActivity activity,String appDir) {
        this.activityRef = new WeakReference<>(activity);
        this.appDir = appDir;
        this.currentDirectory = appDir;
        this.isShell = true;
    }
    public interface OutputListener {
        void onOutputReceived(String output);
        void onError(String error);
    }

    public void start(OutputListener listener) {
        try {
            process = Runtime.getRuntime().exec("/system/bin/sh");
            processOut = process.getOutputStream();
            processIn = process.getInputStream();
            processErr = process.getErrorStream();

            isRunning.set(true);

            // 启动标准输出监听线程
            outputThread = new Thread(() -> readStream(processIn, listener, false));
            outputThread.start();

            // 启动错误输出监听线程
            new Thread(() -> readStream(processErr, listener, true)).start();

        } catch (IOException e) {
            Log.e(TAG, "Failed to start shell: ", e);
            listener.onError("Failed to initialize terminal: " + e.getMessage());
        }
    }

    private void readStream(InputStream stream, OutputListener listener, boolean isError) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream))) {
            char[] buffer = new char[1024];
            int bytesRead;
            while (isRunning.get() && (bytesRead = reader.read(buffer)) != -1) {
                String output = new String(buffer, 0, bytesRead);
                if (isError) {
                    listener.onError(output);
                } else {
                    listener.onOutputReceived(output);
                }
            }
        } catch (IOException e) {
            if (isRunning.get()) {
                listener.onError("IO Error: " + e.getMessage());
            }
        }
    }

    public synchronized boolean executeCommand(String command) {
        if(isShell) return shellExec(command);
        else return true;
    }
    private boolean shellExec(String command){
        if (!isRunning.get() || processOut == null) return true;
        if (command.equals("cd ~")){
            command="cd "+appDir;
        }

        if (command.startsWith("cd ..")){
            String relativeDir = command.split("/",2)[1];
            Log.println(Log.INFO,"../dir",relativeDir);
            command="cd "+ Objects.requireNonNull((new File(appDir)).getParentFile()).getAbsolutePath()+"/"+relativeDir;

        }


        if(command.startsWith("python")||command.startsWith("python3")) return true;
        try {
            processOut.write((command + "\n").getBytes());
            processOut.flush();
        } catch (IOException e) {
            Log.e(TAG, "Command execution failed: ", e);
        }
        finally {;
            if(command.startsWith("cd")&&!command.equals("cd ~")) {
                handleCdCommand(command);
            }
        }
        return true;
    }

    private void handleCdCommand(String command) {
        try {
            String targetPath = command.substring(3).trim();
            File newDir = new File(targetPath).getCanonicalFile();
            if (newDir.isDirectory()) {
                Log.println(Log.INFO, "cd", "cd start");
                currentDirectory = newDir.getAbsolutePath();
                notifyDirectoryChanged(currentDirectory);
                Log.println(Log.INFO, "cd", "cd finish");
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void notifyDirectoryChanged(String newPath) {
        mainHandler.post(() -> {
            MainActivity activity = activityRef.get();
            if (activity != null && !activity.isDestroyed()) {
                FileBrowserFragment fragment = activity.getFileBrowserFragment();
                if (fragment != null) {
                    fragment.setCurrentDirectory(newPath);

                }
            }
        });
    }
    public void terminate() {
        isRunning.set(false);

        try {
            if (process != null) {
                process.destroy();
                processOut.close();
                processIn.close();
                processErr.close();
            }
        } catch (Exception e) {
            Log.w(TAG, "Termination error: ", e);
        }
        if (inputThread != null) {
            inputThread.interrupt();
        }
        if (outputThread != null) {
            outputThread.interrupt();
        }
    }
    public boolean isAlive() {
        return isRunning.get() && process != null && process.isAlive();
    }
}