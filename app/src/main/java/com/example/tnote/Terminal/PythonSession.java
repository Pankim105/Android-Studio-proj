// PythonSession.java - Python交互会话实现
package com.example.tnote.Terminal;

import android.os.Handler;
import android.util.Log;
import com.chaquo.python.PyObject;
import com.chaquo.python.Python;
import com.chaquo.python.android.AndroidPlatform;
import com.example.tnote.MainActivity;
import com.example.tnote.Utils.Interfaces.Session;
import java.lang.ref.WeakReference;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Python交互会话实现类，使用ChaQuo Python库执行Python代码
 */
public class PythonSession implements Session {
    private final Handler mainHandler;         // 主线程Handler用于UI更新
    private OutputListener outputListener;     // 输出监听器
    private final ExecutorService executor = Executors.newSingleThreadExecutor(); // 单线程执行器
    private final AtomicBoolean isAlive = new AtomicBoolean(false); // 会话存活状态
    private WeakReference<MainActivity> activityRef; // 对Activity的弱引用，防止内存泄漏

    public PythonSession(MainActivity activity, Handler mainHandler) {
        this.mainHandler = mainHandler;
        this.activityRef = new WeakReference<>(activity);
    }

    @Override
    public void start(OutputListener listener) {
        this.outputListener = listener;
        this.isAlive.set(true);
        sendSystemMessage("Python Session Started");
    }

    @Override
    public boolean executeCommand(String command) {
        if (!isAlive.get()) return false;

        executor.execute(() -> {
            try {
                // 初始化Python环境
                if (!Python.isStarted()) {
                    Python.start(new AndroidPlatform(activityRef.get()));
                }
                Python py = Python.getInstance();

                // 设置工作目录到应用文件目录
                PyObject os = py.getModule("os");
                os.get("chdir").call(PyObject.fromJava(activityRef.get().getFilesDir().getAbsolutePath()));

                // 调用Python执行模块
                PyObject module = py.getModule("executor");
                Log.i("PythonSession", "command_exec: " + command);
                PyObject result = module.callAttr("execute_code", command);

                // 处理执行结果
                String output = String.valueOf(result);
                mainHandler.post(() -> {
                    if (!output.isEmpty()) {
                        outputListener.onOutputReceived(output + "\n");
                    }
                });
            } catch (Exception e) {
                mainHandler.post(() -> outputListener.onError("Exec Error: " + e.getMessage()));
            }
        });
        return true;
    }

    @Override
    public void terminate() {
        executor.shutdownNow(); // 立即关闭执行器
        isAlive.set(false);
        sendSystemMessage("Python Session Ended!");
    }

    @Override
    public boolean isAlive() {
        return isAlive.get();
    }

    /**
     * 发送系统消息到输出界面
     * @param message 系统消息内容
     */
    private void sendSystemMessage(String message) {
        mainHandler.post(() -> outputListener.onOutputReceived("\nNotify:" + message + "\n"));
    }
}