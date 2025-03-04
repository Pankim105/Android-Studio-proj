package com.example.tnote.Utils.EditorUtils.HilightStrategy;

import android.content.Context;
import android.widget.EditText;
import java.util.HashMap;
import java.util.Map;

import com.example.tnote.Utils.EditorUtils.EditorUIUtils;
import com.example.tnote.Utils.Interfaces.SyntaxHighlightStrategy;
/**
 * 语法高亮管理类，负责：
 * - 根据文件类型选择高亮策略
 * - 协调高亮过程
 */
public class SyntaxHighlightManager {
    private final Map<String, SyntaxHighlightStrategy> strategies = new HashMap<>();
    private final Context context;

    public SyntaxHighlightManager(Context context) {
        this.context = context;
        initializeStrategies();
    }

    private void initializeStrategies() {
        strategies.put("py", new PythonSyntaxStrategy(context));
        strategies.put("md", new MarkdownSyntaxStrategy(context));
    }

    /**
     * 应用语法高亮
     * @param editor 目标编辑器
     * @param fileName 当前文件名（用于判断类型）
     */
    public void applyHighlight(EditText editor, String fileName) {
        String extension = getFileExtension(fileName);
        SyntaxHighlightStrategy strategy = strategies.get(extension);

        if (strategy != null) {
            EditorUIUtils.preserveEditorState(editor, () ->
                    strategy.highlight(editor.getEditableText())
            );
        }
    }

    private String getFileExtension(String fileName) {
        int lastDot = fileName.lastIndexOf('.');
        return (lastDot == -1) ? "" : fileName.substring(lastDot + 1).toLowerCase();
    }
}
// 该文件作为语法高亮入口，根据扩展名路由到具体策略
