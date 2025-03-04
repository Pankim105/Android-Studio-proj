package com.example.tnote.Utils.EditorUtils;

import android.view.KeyEvent;
import android.widget.EditText;

public class KeyBindingHandler {
    public boolean handleKeyEvent(int keyCode, KeyEvent event, EditText editor) {
        if (keyCode == KeyEvent.KEYCODE_TAB && event.getAction() == KeyEvent.ACTION_DOWN) {
            insertTab(editor);
            return true;
        }
        return false;
    }

    private void insertTab(EditText editor) {
        int start = editor.getSelectionStart();
        int end = editor.getSelectionEnd();
        editor.getText().replace(start, end, "    ");
        editor.setSelection(start + 4);
    }
}