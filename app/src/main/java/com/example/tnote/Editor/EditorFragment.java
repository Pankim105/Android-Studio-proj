package com.example.tnote.Editor;

import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.example.tnote.R;
import com.example.tnote.Utils.EditorUtils.HilightStrategy.SyntaxHighlightManager;
import com.example.tnote.Utils.EditorUtils.EditorStateManager;
import com.example.tnote.Utils.FileIOUtils;
import com.example.tnote.Utils.EditorUtils.KeyBindingHandler;
import com.example.tnote.Utils.EditorUtils.TextWatcherAdapter;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 文本编辑器核心Fragment，负责协调文件操作、文本编辑和界面交互逻辑
 * 主要功能：
 * - 通过文件路径加载/保存文本内容
 * - 实时语法高亮显示
 * - 编辑状态跟踪（未保存修改标记）
 * - 基础编辑器配置（字体、滚动等）
 */

public class EditorFragment extends Fragment {
    // UI组件
    private EditText editor;  // 核心文本编辑区域

    // 业务逻辑组件
    private File currentFile;                // 当前正在编辑的文件对象
    private String filePath = "file_path";
    private String fileName;
    private EditorStateManager stateManager; // 编辑器状态管理器（跟踪修改状态）
    private SyntaxHighlightManager highlightManager; // 语法高亮处理器
    private KeyBindingHandler keyHandler;    // 快捷键处理器
    private AtomicBoolean isTmpFileSaved;
    public EditorFragment(File file) {
        currentFile = file;
    }


    public EditorFragment() {
        currentFile=null;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            initializeComponents();  // 初始化非UI相关的组件
        } catch (IOException e) {
            Log.e("ERROR","File Initial Failed!",e);
        }
    }

    /**
     * 初始化核心业务组件
     */
    private void initializeComponents() throws IOException {
        // 从参数中获取文件路径并创建File对象
        if(filePath.equals("file_path") && currentFile==null) {
            fileName = UUID.randomUUID().toString();
            currentFile = File.createTempFile(fileName,".py",requireContext().getCacheDir());
            filePath = currentFile.getAbsolutePath();
            writeToFile(currentFile,"#This app created a '.py' temprary file for you. \n#Please save it as what you want.\n print(\"hello,world\")");
            Log.println(Log.INFO,"TEMP FILE CREATING",filePath);

        } else if (currentFile != null) {
            filePath = currentFile.getAbsolutePath();
        }else{
            currentFile = new File("newFile");
        }
        FileReadLog(currentFile);
        // 初始化各功能管理器
        stateManager = new EditorStateManager(); // 跟踪编辑状态（如是否修改）
        highlightManager = new SyntaxHighlightManager(requireContext()); // 需要上下文加载语法规则
        keyHandler = new KeyBindingHandler();    // 预留的快捷键支持
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        // 加载布局文件
        View view = inflater.inflate(R.layout.fragment_code_editor, container, false);
        setupEditor(view);       // 配置编辑器UI属性
        loadFileContent();       // 异步加载文件内容
        return view;
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
        // 释放资源
    }

    /**
     * 配置编辑器基础UI属性
     * @param view 根视图，用于查找编辑器组件
     * @警告 假设布局中必须包含R.id.editor的EditText，否则会抛出空指针异常
     */
    private void setupEditor(View view) {
        editor = view.findViewById(R.id.editor); // 获取编辑器实例
        configureEditorBehavior();  // 设置编辑器显示参数
        setupTextWatcher();         // 注册文本变化监听器
    }

    /**
     * 配置编辑器显示参数
     * - 启用水平滚动（适合长代码行）
     * - 设置为等宽字体（便于代码对齐）
     * - 固定字号为14sp
     */
    private void configureEditorBehavior() {
        editor.setHorizontallyScrolling(true);    // 允许横向滚动
        editor.setTypeface(Typeface.MONOSPACE);   // 使用等宽字体
        editor.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14); // 设置字号为14sp
    }

    /**
     * 设置文本变化监听器，用于：
     * 1. 标记文档为已修改状态
     * 2. 触发实时语法高亮更新
     * @性能注意 每次输入都会触发高亮，大文件可能有性能问题
     */
    private void setupTextWatcher() {
        editor.addTextChangedListener(new TextWatcherAdapter() {
            @Override
            public void afterTextChanged(Editable s) {
                stateManager.markModified();  // 标记内容有未保存修改
                highlightManager.applyHighlight(editor, currentFile.getName()); // 根据文件名后缀应用高亮规则
            }
        });
    }

    /**
     * 异步加载文件内容到编辑器
     * @流程说明：
     * 1. 使用FileIOUtils异步读取文件
     * 2. 成功时更新编辑器内容并重置修改状态
     * 3. 自动应用语法高亮
     * @风险点 未处理文件读取失败的情况
     */
    private void loadFileContent() {
        Log.println(Log.INFO,"read content","loading");
        FileIOUtils.readFileAsync(currentFile, content -> {
            Log.println(Log.INFO,"read content",content);
            // 主线程更新UI（假设回调已在主线程）
            editor.setText(content);          // 填充编辑器内容
            stateManager.reset();             // 重置为未修改状态
            highlightManager.applyHighlight(editor, currentFile.getName()); // 初始高亮
        });
        Log.println(Log.INFO,"read content","loaded");
    }

    /**
     * 执行文件保存操作
     * @return boolean 总是返回true表示已处理保存流程，实际结果通过回调处理
     * @流程说明：
     * 1. 使用FileIOUtils异步写入文件
     * 2. 根据操作结果更新状态和显示提示
     */
    public boolean saveFile() {
        return FileIOUtils.writeFile(currentFile, editor.getText().toString(),
                success -> handleSaveResult(success));
    }

    /**
     * 处理保存结果
     * @param success 保存是否成功
     * @注意 需在主线程执行UI操作
     */
    private void handleSaveResult(boolean success) {
        if (success) {
            stateManager.clearModified();  // 清除修改标记
            showToast(R.string.save_success); // 显示保存成功提示
        } else {
            showToast(R.string.save_failed);  // 显示保存失败提示
        }
    }

    /**
     * 显示短时提示信息
     * @param resId 字符串资源ID
     */
    private void showToast(int resId) {
        Toast.makeText(requireContext(), resId, Toast.LENGTH_SHORT).show();
    }
    public void FileReadLog(File file){
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                Log.println(Log.INFO,"READ LOG",line);
            }
        } catch (IOException e) {
            Log.e("READ EXCEPTION", "Error reading file", e);
        }
    }
    public static void writeToFile(File file, String content) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            writer.write(content);
        }
    }

}

