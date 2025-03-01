package com.example.tnote.terminal;

import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;

public class AnsiColorHelper {
    // ANSI颜色代码到Android颜色的映射
    private static final int[] ANSI_COLORS = {
            0xFF000000, // 30: Black
            0xFFCC0000, // 31: Red
            0xFF4E9A06, // 32: Green
            0xFFC4A000, // 33: Yellow
            0xFF3465A4, // 34: Blue
            0xFF75507B, // 35: Magenta
            0xFF06989A, // 36: Cyan
            0xFFD3D7CF  // 37: White
    };

    public static Spanned convertAnsiToSpanned(String text) {
        SpannableStringBuilder builder = new SpannableStringBuilder();
        String[] parts = text.split("\u001B\\[");
        builder.append(parts[0]);

        for (int i = 1; i < parts.length; i++) {
            int mIndex = parts[i].indexOf('m');
            if (mIndex == -1) continue;

            String codeSection = parts[i].substring(0, mIndex);
            String content = parts[i].substring(mIndex + 1);

            // 解析颜色代码
            for (String code : codeSection.split(";")) {
                try {
                    int colorCode = Integer.parseInt(code);
                    if (colorCode >= 30 && colorCode <= 37) {
                        int start = builder.length();
                        builder.append(content);
                        builder.setSpan(new ForegroundColorSpan(ANSI_COLORS[colorCode - 30]),
                                start, builder.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    }
                } catch (NumberFormatException ignored) {}
            }
        }
        return builder;
    }
}
