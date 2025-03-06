package com.example.tnote.Terminal;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Spanned;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.example.tnote.MainActivity;
import com.example.tnote.R;
import com.example.tnote.Utils.Interfaces.Session;
import com.example.tnote.Utils.AnsiColorHelper;

import java.util.concurrent.atomic.AtomicBoolean;

public class TerminalFragment extends Fragment {
    // UI组件
    private TextView tvOutput;
    private EditText etInput;
    private ScrollView svOutput;
    private Button execButton;

    // 会话管理
    private Session currentSession;
    private ShellSession shellSession;
    private PythonSession pythonSession;

    // 状态管理
    private String inputCache;
    private String pythonCommandCache = "";
    private final CommandHistory commandHistory = new CommandHistory();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final AtomicBoolean isExecuting = new AtomicBoolean(false);
    private MainActivity mainActivity;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_terminal, container, false);
        bindViews(view);
        initializeSessions();
        setupInputHandlers();
        restoreInputCache();
        return view;
    }

    private void bindViews(View view) {
        tvOutput = view.findViewById(R.id.tv_terminal_output);
        etInput = view.findViewById(R.id.et_command_input);
        svOutput = view.findViewById(R.id.sv_output);
        execButton = view.findViewById(R.id.btn_send);
    }

    private void initializeSessions() {
        // 初始化Shell会话
        String appDir = requireContext().getFilesDir().getAbsolutePath();
        shellSession = new ShellSession(mainActivity, appDir);

        // 初始化Python会话
        pythonSession = new PythonSession(mainActivity,mainHandler);

        // 默认启动Shell会话
        switchToSession(shellSession, "=== Terminal Shell Session===");
        shellSession.executeCommand("cd " + appDir); // 进入应用目录
    }

    private void setupInputHandlers() {
        // 执行按钮监听
        execButton.setOnClickListener(v -> executeCurrentCommand());

        // 输入框监听
        etInput.setOnKeyListener((v, keyCode, event) -> {
            if (event.getAction() == KeyEvent.ACTION_DOWN) {
                return handleKeyPress(keyCode);
            }
            return false;
        });
    }

    // region 核心功能方法
    private void switchToSession(Session newSession, String message) {
        // 终止当前会话
        if (currentSession != null) {
            currentSession.terminate();
        }

        // 启动新会话
        currentSession = newSession;
        currentSession.start(new Session.OutputListener() {
            @Override
            public void onOutputReceived(String output) {
                appendOutput(output, false);
            }

            @Override
            public void onError(String error) {
                appendOutput(error, true);
            }
        });

        appendOutput(message + "\n", false);
    }

    private void executeCurrentCommand() {
        boolean success = false;
        if (isExecuting.get()) return;

        String command = etInput.getText().toString();

        // 处理特殊命令
        if (handleSessionSwitch(command)) {
            clearInput();
            return;
        }
        // python 指令处理 多行指令缓存
        if(currentSession==pythonSession){
            Log.println(Log.INFO,"command is:", String.valueOf(command.isEmpty()));
            if (command.isEmpty()){
                success = submmitCommand(pythonCommandCache);
                pythonCommandCache = "";
            }
            else {
                pythonCommandCache = pythonCommandCache + command+"\n";
                appendOutput(">>> " + command+"..."+"\n",false);
                clearInput();
            }
        }
        // 记录历史命令
        if (currentSession==shellSession){
            commandHistory.add(command);
            if (command.startsWith("clear")) {
                tvOutput.setText("") ;
                success = true;
            }
            else {
                appendOutput("> " + command + "\n", false);
                success = submmitCommand(command);
            }
        }
        if (success) {
            clearInput();
        }
        isExecuting.set(false);
    }
    private boolean submmitCommand(String command){
        // 执行常规命令
        isExecuting.set(true);
        boolean success = currentSession.executeCommand(command);

        return true;
    }

    private boolean handleSessionSwitch(String command) {
        if (currentSession == shellSession && ("python".equalsIgnoreCase(command))||"python3".equalsIgnoreCase(command)) {
            switchToSession(pythonSession, "=== Python Shell Session ===\nEnter exit() Back To Shell\n\n"+getString(R.string.python_info));
            return true;
        } else if (currentSession == pythonSession && "exit()".equals(command)) {
            switchToSession(shellSession, "=== Shell Session Return ===");
            return true;
        }
        return false;
    }
    // endregion

    // region 辅助方法
    private void appendOutput(CharSequence text, boolean isError) {
        mainHandler.post(() -> {
            Spanned formatted = isError ?
                    AnsiColorHelper.formatRed(text) :
                    AnsiColorHelper.convertAnsiToSpanned(text.toString());
            tvOutput.append(formatted);
            scrollToBottom();
        });
    }

    private void clearInput() {
        mainHandler.post(() -> etInput.setText(""));
    }

    private void scrollToBottom() {
        svOutput.post(() -> svOutput.fullScroll(ScrollView.FOCUS_DOWN));
    }

    private boolean handleKeyPress(int keyCode) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_ENTER:
                executeCurrentCommand();
                return true;
            case KeyEvent.KEYCODE_DPAD_UP:
                etInput.setText(commandHistory.getPrevious());
                return true;
            case KeyEvent.KEYCODE_DPAD_DOWN:
                etInput.setText(commandHistory.getNext());
                return true;
        }
        return false;
    }

    private void restoreInputCache() {
        if (inputCache != null) {
            etInput.setText(inputCache);
            etInput.setSelection(inputCache.length());
        }
    }
    // endregion

    // region 生命周期管理
    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d("TerminalFragment", "onDestroy()");
        if (shellSession != null) shellSession.terminate();
        if (pythonSession != null) pythonSession.terminate();
    }

    @Override
    public void onDestroyView() {
        Log.d("TerminalFragment", "onDestroyView()");
        inputCache = etInput.getText().toString();
        super.onDestroyView();
    }

    @Override
    public void onDetach() {
        Log.d("TerminalFragment", "onDetach()");
        if (currentSession != null) {
            currentSession.terminate();
            currentSession = null;
        }
        mainHandler.removeCallbacksAndMessages(null);
        super.onDetach();
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof MainActivity) {
            mainActivity = (MainActivity) context;
        } else {
            throw new IllegalStateException("必须附加到MainActivity");
        }
    }
    // endregion
}