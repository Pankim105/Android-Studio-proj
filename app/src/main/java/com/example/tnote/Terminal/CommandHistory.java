package com.example.tnote.Terminal;
import java.util.ArrayList;
import java.util.List;

public class CommandHistory {
    private final List<String> history = new ArrayList<>();
    private int position = 0;

    public void add(String command) {
        history.add(command);
        position = history.size(); // 重置位置到最新
    }

    public String getPrevious() {
        if (history.isEmpty()) return "";
        position = Math.max(position - 1, 0);
        return history.get(position);
    }

    public String getNext() {
        if (history.isEmpty()) return "";
        position = Math.min(position + 1, history.size());
        return position < history.size() ? history.get(position) : "";
    }
}
