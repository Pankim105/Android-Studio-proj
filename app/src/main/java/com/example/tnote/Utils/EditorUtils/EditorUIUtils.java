package com.example.tnote.Utils.EditorUtils;

import android.widget.EditText;

public class EditorUIUtils {
    public static void preserveEditorState(EditText editor, Runnable action) {
        int selectionStart = editor.getSelectionStart();
        int selectionEnd = editor.getSelectionEnd();
        int scrollY = editor.getScrollY();

        action.run();

        editor.setSelection(selectionStart, selectionEnd);
        editor.setScrollY(scrollY);
    }
}

