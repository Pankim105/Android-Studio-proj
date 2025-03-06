package com.example.tnote.Utils;

import static android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE;

import android.graphics.Color;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AnsiColorHelper {
    private static final int[] ANSI_COLORS = {
            0xFF000000, 0xFFCC0000, 0xFF4E9A06, 0xFFC4A000,
            0xFF3465A4, 0xFF75507B, 0xFF06989A, 0xFFD3D7CF
    };

    public static Spanned convertAnsiToSpanned(String text) {
        SpannableStringBuilder builder = new SpannableStringBuilder();
        Pattern ansiPattern = Pattern.compile("\u001B\\[([0-9;]*)m");
        Matcher matcher = ansiPattern.matcher(text);

        int lastAppendIndex = 0;
        while (matcher.find()) {
            // 添加 ANSI 代码之前的文本
            builder.append(text.substring(lastAppendIndex, matcher.start()));

            // 解析 ANSI 代码
            String codeSection = matcher.group(1);
            int start = builder.length();

            // 添加 ANSI 代码之后的文本
            int nextAnsiIndex = text.indexOf("\u001B\\[", matcher.end());
            if (nextAnsiIndex == -1) {
                builder.append(text.substring(matcher.end()));
            } else {
                builder.append(text.substring(matcher.end(), nextAnsiIndex));
            }

            // 应用颜色
            for (String code : codeSection.split(";")) {
                try {
                    int colorCode = Integer.parseInt(code);
                    if (colorCode >= 30 && colorCode <= 37) {
                        builder.setSpan(new ForegroundColorSpan(ANSI_COLORS[colorCode - 30]),
                                start, builder.length(), SPAN_EXCLUSIVE_EXCLUSIVE);
                    }
                } catch (NumberFormatException ignored) {}
            }

            lastAppendIndex = (nextAnsiIndex == -1) ? text.length() : nextAnsiIndex;
        }

        // 添加最后一个 ANSI 代码之后的文本
        if (lastAppendIndex < text.length()) {
            builder.append(text.substring(lastAppendIndex));
        }

        return builder;
    }

    public static Spanned formatRed(CharSequence text) {
        SpannableString spannable = new SpannableString(text);
        spannable.setSpan(new ForegroundColorSpan(Color.RED),
                0, text.length(), SPAN_EXCLUSIVE_EXCLUSIVE);
        return spannable;
    }
}