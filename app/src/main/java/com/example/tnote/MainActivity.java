package com.example.tnote;

import android.os.Bundle;

import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.FragmentManager;

import com.example.tnote.Editor.EditorFragment;
import com.example.tnote.Utils.AnimGuideline;
import com.example.tnote.FileBrowser.FileBrowserFragment;
import com.example.tnote.Utils.TabManager;

import java.io.File;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class MainActivity extends AppCompatActivity {



    // 界面组件
    public TabManager tabManager;
    private View divider;  // 分割线视图
    private AnimGuideline guideline;  // 自定义Guideline
    private ConstraintLayout dualPaneContainer;  // 双窗格容器
    private FrameLayout rightPane;  // 右侧窗格
    private AtomicBoolean isRightPaneVisible = new AtomicBoolean(false);  // 右侧窗格可见状态
    private static volatile File appDir;

    //同步
    //guidline调整同步
    public static Lock guidelineLock = new ReentrantLock();
    public static Condition guidelineCondition = guidelineLock.newCondition();

    //左右窗口Fragment同步
    public static Lock leftPaneLock = new ReentrantLock();
    public static Condition leftPaneCondition = leftPaneLock.newCondition();
    public static Lock rightPaneLock = new ReentrantLock();
    public static Condition rightPaneCondition = rightPaneLock.newCondition();

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //初始化app目录位置
        appDir = getFilesDir();

        // 初始化视图组件
        dualPaneContainer = findViewById(R.id.dual_pane_container);
        divider = findViewById(R.id.divider);
        rightPane = findViewById(R.id.right_pane);

        initialGuildeline();  // 初始化Guideline
        try {
            initializeTabManager(savedInstanceState);  // 初始化标签管理器
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        setupButtonListeners();  // 设置按钮监听
    }

    /**
     * 初始化自定义Guideline及其相关配置
     */
    private void initialGuildeline() {
        guideline = findViewById(R.id.guideline);
        // 设置触摸区域
        guideline.setTouchArea(findViewById(R.id.touch_area));
        // 设置状态监听器
        guideline.setGuidelineListener(new AnimGuideline.GuidelineListener() {
            @Override
            public void onDragStart() {
                updateDividerStyle(true);  // 拖动开始时更新分割线样式
            }

            @Override
            public void onDragEnd(float finalPercent) {
                updateDividerStyle(false);  // 拖动结束恢复分割线样式
            }

            @Override
            public void onVisibilityChanged(boolean visible) {
                divider.setVisibility(visible ? View.VISIBLE : View.GONE);
                updatePaneVisibility(isRightPaneVisible.get());
            }
        });
        guideline.setupDragBehavior();  // 启用拖动手势
    }

    /**
     * 更新分割线样式
     * @param isDragging 是否处于拖动状态
     */

    private void updateDividerStyle(boolean isDragging) {
        int colorRes = isDragging ? R.color.guideline_active : R.color.guideline_inactive;
        int widthDp = isDragging ? 4: 2;
        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams)divider.getLayoutParams();
        params.gravity = Gravity.END;
        params.width = dpToPx(widthDp);
        divider.setBackgroundColor(getResources().getColor(colorRes));
    }

    /**
     * 更新右侧窗格可见性
     * @param visible 是否可见
     */
    private void updatePaneVisibility(boolean visible) {
        isRightPaneVisible.set(visible);
        rightPane.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    /**
     * 获取文件浏览器Fragment实例
     */
    public FileBrowserFragment getFileBrowserFragment() {
        return (FileBrowserFragment) getSupportFragmentManager()
                .findFragmentByTag(TabManager.TabType.FILE_BROWSER.name());
    }

    /**
     * dp转px单位换算
     */
    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }

    /**
     * 初始化标签管理器
     */
    private void initializeTabManager(Bundle savedInstanceState) throws InterruptedException {
        tabManager = new TabManager(
                findViewById(R.id.btn_terminal),
                findViewById(R.id.btn_filebrowser),
                getSupportFragmentManager(),
                R.id.left_pane,
                R.id.right_pane
        );

        // 初次加载默认标签
        if (savedInstanceState == null) {
            tabManager.switchTab(TabManager.TabType.TERMINAL);
        }
    }

    /**
     * 设置底部按钮点击监听
     */
    private void setupButtonListeners() {
        // 终端按钮
        findViewById(R.id.btn_terminal).setOnClickListener(v ->{
            try {
                tabManager.switchTab(TabManager.TabType.TERMINAL);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            EditorFragment editorFragment = (EditorFragment) getSupportFragmentManager().findFragmentById(R.id.editor);
        });

        // 文件浏览器按钮
        findViewById(R.id.btn_filebrowser).setOnClickListener(v -> {
            try {
                tabManager.switchTab(TabManager.TabType.FILE_BROWSER);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            if (!isRightPaneVisible.get()) {
                divider.setVisibility(View.VISIBLE);
                guideline.toggleVisibility(true, 0.5f);  // 显示并设置到50%位置
                updateDividerStyle(false);
                updatePaneVisibility(true);
            } else {
                isRightPaneVisible.set(false);
                guideline.toggleVisibility(true, 1.0f);  // 隐藏到100%位置
                divider.setVisibility(View.GONE);
                updatePaneVisibility(false);
            }
        });
    }
    public static File getAppDir(){
        return appDir;
    }
    public void setGuideLinePosition(float percentage){
        guidelineLock.lock();
        guideline.toggleVisibility(true,percentage);
        guidelineLock.unlock();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        // 保存状态（如有需要可扩展）
    }

    @Override
    protected void onDestroy() {
        // 清理资源
        if (tabManager != null) {
            tabManager.cleanup();
        }
        super.onDestroy();
    }
}