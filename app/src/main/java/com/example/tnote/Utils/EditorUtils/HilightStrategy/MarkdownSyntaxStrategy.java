package com.example.tnote.Utils.EditorUtils.HilightStrategy;

import android.content.Context;
import android.text.Editable;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import androidx.core.content.ContextCompat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.example.tnote.Utils.Interfaces.SyntaxHighlightStrategy;
import com.example.tnote.R;
/**
 * Markdown 语法高亮策略实现：
 * - 标题（#）
 * - 粗体（** 或 __）
 * - 斜体（* 或 _）
 * - 链接（[text](url)）
 */
public class MarkdownSyntaxStrategy implements SyntaxHighlightStrategy {
    // 匹配模式
    private static final Pattern PATTERN = Pattern.compile(
            "(#{1,6}\\s.*?$)|" + // 标题
                    "(\\*\\*(.*?)\\*\\*|__(.*?)__)|" + // 粗体
                    "(\\*(.*?)\\*|_(.*?)_)|" + // 斜体
                    "(\\[(.*?)\\]\\((.*?)\\))" // 链接
    );

    // 颜色资源
    private final int headerColor;
    private final int boldColor;
    private final int italicColor;
    private final int linkColor;

    public MarkdownSyntaxStrategy(Context context) {
        headerColor = ContextCompat.getColor(context, R.color.syntax_header);
        boldColor = ContextCompat.getColor(context, R.color.syntax_bold);
        italicColor = ContextCompat.getColor(context, R.color.syntax_italic);
        linkColor = ContextCompat.getColor(context, R.color.syntax_link);
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
        StyleSpan[] styleSpans = editable.getSpans(0, editable.length(), StyleSpan.class);
        for (StyleSpan span : styleSpans) {
            editable.removeSpan(span);
        }
    }

    private void applySyntaxColors(Editable editable) {
        Matcher matcher = PATTERN.matcher(editable);
        while (matcher.find()) {
            applyColorGroup(editable, matcher, 1, headerColor); // 标题
            applyColorGroup(editable, matcher, 2, boldColor);   // 粗体
            applyColorGroup(editable, matcher, 3, italicColor); // 斜体
            applyLinkGroup(editable, matcher, 4, linkColor);     // 链接
        }
    }

    private void applyColorGroup(Editable editable, Matcher matcher, int group, int color) {
        if (matcher.group(group) != null) {
            int start = matcher.start(group);
            int end = matcher.end(group);
            editable.setSpan(new ForegroundColorSpan(color), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
    }

    private void applyLinkGroup(Editable editable, Matcher matcher, int group, int color) {
        if (matcher.group(group) != null) {
            int start = matcher.start(group);
            int end = matcher.end(group);
            editable.setSpan(new ForegroundColorSpan(color), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            // 可以根据需求添加其他样式，比如下划线
        }
    }
}
// 该文件实现Markdown语法的高亮规则