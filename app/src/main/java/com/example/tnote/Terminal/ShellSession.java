// ShellSession.java - 本地Shell会话实现
package com.example.tnote.Terminal;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import com.example.tnote.MainActivity;
import com.example.tnote.Utils.Interfaces.Session;
import com.example.tnote.FileBrowser.FileBrowserFragment;
import java.io.*;
import java.lang.ref.WeakReference;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 本地Shell会话实现类，通过Runtime执行系统命令
 */
public class ShellSession implements Session {
    private static final String TAG = "ShellSession";
    private String appDir;                  // 应用文件目录
    private String currentDirectory;        // 当前工作目录
    private Process process;                // Shell进程
    private WeakReference<MainActivity> activityRef; // Activity弱引用
    private Thread outputThread;            // 输出监听线程
    private final AtomicBoolean isRunning = new AtomicBoolean(false); // 运行状态
    private final Handler mainHandler = new Handler(Looper.getMainLooper()); // 主线程Handler

    // 进程流
    private OutputStream processOut;        // 命令输出流
    private OutputListener outputListener;  // 输出监听器
    private InputStream processIn;          // 标准输入流
    private InputStream processErr;         // 错误输入流
    private boolean isShell;                // 是否为Shell会话标志

    public ShellSession(MainActivity activity, String appDir) {
        this.activityRef = new WeakReference<>(activity);
        this.appDir = appDir;
        this.currentDirectory = appDir; // 初始目录设为应用目录
        this.isShell = true;
    }

    @Override
    public void start(OutputListener listener) {
        try {
            // 启动Shell进程
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
            Log.e(TAG, "启动Shell失败", e);
            listener.onError("终端初始化失败: " + e.getMessage());
        }
    }

    @Override
    public boolean executeCommand(String command) {
        return isShell ? shellExec(command) : true;
    }

    /**
     * 执行Shell命令
     * @param command 命令字符串
     * @return 是否执行成功
     */
    private boolean shellExec(String command) {
        if (!isRunning.get() || processOut == null) return false;

        // 处理特殊命令
        if (command.equals("cd ~")) {
            command = "cd " + appDir;
        } else if (command.startsWith("cd ..")) {
            handleParentDirectory(command);
        }

        try {
            // 写入命令并刷新
            processOut.write((command + "\n").getBytes());
            processOut.flush();
        } catch (IOException e) {
            Log.e(TAG, "命令执行失败: " + command, e);
        } finally {
            // 处理目录变更
            if (command.startsWith("cd") && !command.equals("cd ~")) {
                handleCdCommand(command);
            }
        }
        return true;
    }

    @Override
    public void terminate() {
        isRunning.set(false);
        try {
            if (process != null) {
                process.destroy(); // 销毁进程
                processOut.close();
                processIn.close();
                processErr.close();
            }
        } catch (Exception e) {
            Log.w(TAG, "终止会话时出错", e);
        }
        if (outputThread != null) {
            outputThread.interrupt(); // 中断线程
        }
    }

    @Override
    public boolean isAlive() {
        return isRunning.get() && process != null && process.isAlive();
    }

    /**
     * 读取流数据并转发到监听器
     * @param stream 输入流
     * @param listener 输出监听器
     * @param isError 是否为错误流
     */
    private void readStream(InputStream stream, OutputListener listener, boolean isError) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream))) {
            char[] buffer = new char[1024];
            int bytesRead;
            while (isRunning.get() && (bytesRead = reader.read(buffer)) != -1) {
                String output = new String(buffer, 0, bytesRead);
                // 根据流类型回调
                if (isError) {
                    listener.onError(output);
                } else {
                    listener.onOutputReceived(output);
                }
            }
        } catch (IOException e) {
            if (isRunning.get()) {
                listener.onError("IO错误: " + e.getMessage());
            }
        }
    }

    /**
     * 处理目录切换命令
     * @param command cd命令字符串
     */
    private void handleCdCommand(String command) {
        try {
            String targetPath = command.substring(3).trim();
            File newDir = new File(targetPath).getCanonicalFile();
            if (newDir.isDirectory()) {
                currentDirectory = newDir.getAbsolutePath();
                notifyDirectoryChanged(currentDirectory); // 通知目录变更
            }
        } catch (IOException e) {
            Log.e(TAG, "解析目录失败: " + command, e);
        }
    }

    /**
     * 处理上级目录命令
     * @param command 包含..的命令
     */
    private void handleParentDirectory(String command) {
        String relativeDir = command.split("/", 2)[1];
        Log.i(TAG, "上级目录路径: " + relativeDir);
        File parentDir = new File(appDir).getParentFile();
        if (parentDir != null) {
            command = "cd " + parentDir.getAbsolutePath() + "/" + relativeDir;
        }
    }

    /**
     * 通知文件浏览器目录变更
     * @param newPath 新目录路径
     */
    private void notifyDirectoryChanged(String newPath) {
        mainHandler.post(() -> {
            MainActivity activity = activityRef.get();
            if (activity != null && !activity.isDestroyed()) {
                FileBrowserFragment fragment = activity.getFileBrowserFragment();
                if (fragment != null) {
                    fragment.setCurrentDirectory(newPath); // 更新文件浏览器目录
                }
            }
        });
    }
}