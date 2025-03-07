package com.example.tnote.Utils;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * 文件操作工具类，提供：
 * - 异步文件读取
 * - 异步文件写入
 * - 线程安全管理
 */
public class FileIOUtils {
    private static final String TAG = "FileIOUtils";
    private static final ExecutorService executor = Executors.newSingleThreadExecutor();
    private static Future<?> currentWriteTask = null;
    public interface FileReadCallback {
        void onComplete(String content);
    }

    public interface FileWriteCallback {
        void onComplete(boolean success);
    }

    /**
     * 异步读取文件内容
     * @param file 目标文件
     * @param callback 读取完成回调
     */
    public static void readFileAsync(File file, FileReadCallback callback) {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                String content = new String(Files.readAllBytes(file.toPath()));
                postToMainThread(() -> callback.onComplete(content));
            } catch (IOException e) {
                postToMainThread(() -> callback.onComplete(""));
            }
        });
    }

    /**
     * 异步写入文件内容
     * @param file 目标文件
     * @param content 待写入内容
     * @param callback 写入完成回调
     * @return 是否成功启动写入任务
     */
    public static boolean writeFile(File file, String content, FileWriteCallback callback) {

        if (currentWriteTask != null && !currentWriteTask.isDone()) {
            currentWriteTask.cancel(true); // 取消之前的任务
        }
        currentWriteTask = executor.submit(() -> {
            boolean success = false;
            try {
                Files.write(file.toPath(), content.getBytes());
                success = true;
            } catch (IOException e) {
                Log.e(TAG, "Error writing file: " + e.getMessage());
            }
            boolean finalSuccess = success;
            postToMainThread(() -> {
                Log.e(TAG, "Error writing file: " +"writeback!!!!!!" );
                callback.onComplete(finalSuccess);
            });
        });
        return true;
    }

    private static void postToMainThread(Runnable action) {
        new Handler(Looper.getMainLooper()).post(action);
    }
}
// 该文件封装所有文件IO操作，确保线程安全
