package com.example.tnote.Utils.EditorUtils;

import java.util.Stack;

/**
 * EditorStateManager 用于管理文本编辑器的状态，包括撤销、重做和修改标记。
 */
public class EditorStateManager {
    // 标记文档是否被修改
    private boolean isModified;

    // 撤销栈，用于存储历史状态
    private final Stack<CharSequence> undoStack = new Stack<>();

    // 重做栈，用于存储被撤销的状态
    private final Stack<CharSequence> redoStack = new Stack<>();

    /**
     * 标记当前文档为已修改
     */
    public void markModified() {
        isModified = true;
    }

    /**
     * 清除修改标记
     */
    public void clearModified() {
        isModified = false;
    }

    /**
     * 保存编辑状态到撤销栈
     *
     * @param text 当前文本状态
     */
    public void saveState(CharSequence text) {
        undoStack.push(text);
        // 限制撤销栈的大小为100，防止内存占用过高
        if (undoStack.size() > 100) {
            undoStack.remove(0); // 移除最旧的状态
        }
    }

    /**
     * 撤销上一步的操作
     *
     * @return 撤销后的文本状态，若栈为空则返回 null
     */
    public CharSequence undo() {
        if (!undoStack.isEmpty()) {
            CharSequence state = undoStack.pop(); // 移除并获取最新的状态
            redoStack.push(state); // 将撤销的状态推入重做栈
            return state;
        }
        return null; // 如果撤销栈为空，返回 null
    }

    /**
     * 重做上一步撤销的操作
     *
     * @return 重做后的文本状态，若重做栈为空则返回 null
     */
    public CharSequence redo() {
        if (!redoStack.isEmpty()) {
            CharSequence state = redoStack.pop(); // 移除并获取最新的重做状态
            undoStack.push(state); // 将重做的状态推回撤销栈
            return state;
        }
        return null; // 如果重做栈为空，返回 null
    }

    /**
     * 重置编辑器状态，清空撤销和重做栈，并清除修改标记
     */
    public void reset() {
        undoStack.clear(); // 清空撤销栈
        redoStack.clear(); // 清空重做栈
        clearModified(); // 清除修改标记
    }
}