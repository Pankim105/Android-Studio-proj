package com.example.tnote;

import android.annotation.SuppressLint;
import android.os.Bundle;

import android.view.View;
import android.widget.FrameLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import android.widget.EditText;
import com.example.tnote.Editor.EditorFragment;
import com.example.tnote.Utils.AnimGuideline;

import java.util.concurrent.atomic.AtomicBoolean;

public class testActivity extends AppCompatActivity {


    // 界面组件
    private TabManager tabManager;
    private View divider;  // 分割线视图
    private AnimGuideline guideline;  // 自定义Guideline
    private ConstraintLayout dualPaneContainer;  // 双窗格容器
    private FrameLayout rightPane;  // 右侧窗格
    private AtomicBoolean isRightPaneVisible = new AtomicBoolean(false);  // 右侧窗格可见状态
    private EditText editor;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 初始化视图组件
        dualPaneContainer = findViewById(R.id.dual_pane_container);
        divider = findViewById(R.id.divider);
        rightPane = findViewById(R.id.right_pane);
    }
}