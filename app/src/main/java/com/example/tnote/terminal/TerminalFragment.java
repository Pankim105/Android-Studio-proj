package com.example.tnote.terminal;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Spanned;
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

public class TerminalFragment extends Fragment {
    private TerminalSession terminalSession;
    private MainActivity mainActivity;
    private TextView tvOutput;
    private EditText etInput;
    private ScrollView svOutput;
    private Button execButton;
    private String inputCache;
    private CommandHistory commandHistory = new CommandHistory();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private boolean isExecuting = false;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_terminal, container, false);
        bindViews(view);
        initializeTerminal();
        setupInputListener();
        if (inputCache != null) {
            etInput.setText(inputCache);
            etInput.setSelection(inputCache.length()); // 光标定位到末尾
        }
        return view;
    }


    private void bindViews(View view) {
        tvOutput = view.findViewById(R.id.tv_terminal_output);
        etInput = view.findViewById(R.id.et_command_input);
        svOutput = view.findViewById(R.id.sv_output);
        execButton = view.findViewById(R.id.btn_send);
    }

    private void initializeTerminal() {
        terminalSession = new TerminalSession(mainActivity,requireContext().getFilesDir().getAbsolutePath());
        terminalSession.start(new TerminalSession.OutputListener() {
            @Override
            public void onOutputReceived(String output) {
                handleTerminalOutput(output, false);
            }

            @Override
            public void onError(String error) {
                handleTerminalOutput(error, true);
            }
        });
        Context context = requireContext();
        String appDir = context.getFilesDir().getAbsolutePath();   // /data/data/<包名>/files
        terminalSession.executeCommand("cd "+appDir);
    }

    private void setupInputListener() {
        etInput.setOnKeyListener((v, keyCode, event) -> {
            if (event.getAction() == KeyEvent.ACTION_DOWN) {
                return handleKeyPress(keyCode);
            }
            return false;
        });
        execButton.setOnClickListener(v -> executeCurrentCommand());
    }

    private boolean handleKeyPress(int keyCode) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_ENTER:
                executeCurrentCommand();
                return true;
            case KeyEvent.KEYCODE_DPAD_UP:
                recallHistory(-1);
                return true;
            case KeyEvent.KEYCODE_DPAD_DOWN:
                recallHistory(1);
                return true;
        }
        return false;
    }

    private void executeCurrentCommand() {
        if (isExecuting) return;

        String command = etInput.getText().toString().trim();
        if (!command.isEmpty()) {
            isExecuting = true;
            appendToOutput("> " + command + "\n");
            commandHistory.add(command);

            terminalSession.executeCommand(command);

            mainHandler.postDelayed(() -> {
                etInput.setText("");
                isExecuting = false;
            }, 100);
        }
    }

    private void handleTerminalOutput(String text, boolean isError) {
        mainHandler.post(() -> {
            Spanned formatted = AnsiColorHelper.convertAnsiToSpanned(text);
            appendToOutput(formatted);
            scrollToBottom();
        });
    }

    private void recallHistory(int direction) {
        String historyCommand = (direction == -1) ?
                commandHistory.getPrevious() : commandHistory.getNext();
        etInput.setText(historyCommand);
        etInput.setSelection(historyCommand.length());
    }

    private void appendToOutput(CharSequence text) {
        tvOutput.append(text);
    }

    private void scrollToBottom() {
        svOutput.post(() -> svOutput.fullScroll(ScrollView.FOCUS_DOWN));
    }

    @Override
    public void onDestroy() {
        terminalSession.terminate();
        mainHandler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }
    @Override
    public void onDestroyView() {
        // 保留输入内容等视图状态
        if (etInput != null) {
            inputCache = etInput.getText().toString();
        }
        super.onDestroyView();
    }


    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        // 检查 Context 是否是 MainActivity 实例
        if (context instanceof MainActivity) {
            mainActivity = (MainActivity) context;
        } else {
            throw new IllegalStateException("TerminalFragment 必须附加到 MainActivity");
        }
    }

}