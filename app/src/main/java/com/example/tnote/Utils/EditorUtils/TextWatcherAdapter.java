package com.example.tnote.Utils.EditorUtils;

import android.text.TextWatcher;

public abstract class TextWatcherAdapter implements TextWatcher {
    @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
    @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
}
