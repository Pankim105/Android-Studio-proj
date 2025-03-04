package com.example.tnote.Utils.EditorUtils.HilightStrategy;

import android.content.Context;
import android.text.Editable;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import androidx.core.content.ContextCompat;

import com.example.tnote.Utils.Interfaces.SyntaxHighlightStrategy;
import com.example.tnote.R;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Python语法高亮策略实现：
 * - 注释（#）
 * - 字符串（"" 或 ''）
 * - 关键字（def, class等）
 */
public class PythonSyntaxStrategy implements SyntaxHighlightStrategy {
    // 匹配模式
    private static final Pattern PATTERN = Pattern.compile(
            "(#.*)|(\".*?\"|'.*?')|\\b(and|as|assert|break|class|continue|def|del|elif|else|except|" +
                    "finally|for|from|global|if|import|in|is|lambda|nonlocal|not|or|pass|raise|return|try|while|with|yield)\\b"
    );

    // 颜色资源
    private final int commentColor;
    private final int stringColor;
    private final int keywordColor;

    public PythonSyntaxStrategy(Context context) {
        commentColor = ContextCompat.getColor(context, R.color.syntax_comment);
        stringColor = ContextCompat.getColor(context, R.color.syntax_string);
        keywordColor = ContextCompat.getColor(context, R.color.syntax_keyword);
    }

    @Override
    public void highlight(Editable editable) {
        clearExistingHighlights(editable);
        applySyntaxColors(editable);
    }

    private void clearExistingHighlights(Editable editable) {
        ForegroundColorSpan[] spans = editable.getSpans(0, editable.length(), ForegroundColorSpan.class);
        for (ForegroundColorSpan span : spans) {
            editable.removeSpan(span);
        }
    }

    private void applySyntaxColors(Editable editable) {
        Matcher matcher = PATTERN.matcher(editable);
        while (matcher.find()) {
            applyColorGroup(editable, matcher, 1, commentColor); // 注释
            applyColorGroup(editable, matcher, 2, stringColor);  // 字符串
            applyColorGroup(editable, matcher, 3, keywordColor); // 关键字
        }
    }

    private void applyColorGroup(Editable editable, Matcher matcher, int group, int color) {
        int start = matcher.start(group);
        int end = matcher.end(group);
        if (start != -1 && end != -1) {
            editable.setSpan(new ForegroundColorSpan(color), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
    }
}
// 该文件实现Python语法的高亮规则