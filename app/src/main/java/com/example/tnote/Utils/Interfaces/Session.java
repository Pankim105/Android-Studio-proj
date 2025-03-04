// Session.java - 定义终端会话通用接口
package com.example.tnote.Utils.Interfaces;

/**
 * 会话接口，定义终端会话的基本操作
 */
public interface Session {
    /**
     * 启动会话
     * @param listener 输出监听器，用于接收会话输出
     */
    void start(OutputListener listener);

    /**
     * 执行命令
     * @param command 要执行的命令字符串
     * @return 命令是否成功提交执行
     */
    boolean executeCommand(String command);

    /**
     * 终止会话
     */
    void terminate();

    /**
     * 检查会话是否存活
     * @return 会话是否正在运行
     */
    boolean isAlive();

    /**
     * 输出监听器接口，用于处理会话的输出和错误信息
     */
    interface OutputListener {
        /**
         * 接收标准输出
         * @param output 输出内容
         */
        void onOutputReceived(String output);

        /**
         * 接收错误输出
         * @param error 错误信息
         */
        void onError(String error);
    }
}