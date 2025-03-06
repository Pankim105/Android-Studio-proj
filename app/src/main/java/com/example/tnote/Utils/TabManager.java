package com.example.tnote.Utils;

import android.annotation.SuppressLint;
import android.widget.ImageButton;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.example.tnote.Editor.EditorFragment;
import com.example.tnote.FileBrowser.FileBrowserFragment;
import com.example.tnote.MainActivity;
import com.example.tnote.Terminal.TerminalFragment;

import java.io.File;
import java.util.EnumMap;
import java.util.Map;

/**
 * 选项卡管理类，负责管理终端、文件浏览器和编辑器三个面板的切换逻辑
 * 使用 ReentrantLock 和 Condition 实现线程安全的 Fragment 操作
 */
public class TabManager {
    /** 选项卡类型枚举 */
    public enum TabType { TERMINAL, FILE_BROWSER, EDITOR }

    /** Fragment 缓存映射表：存储已创建的 Fragment 实例 */
    public final Map<TabType, Fragment> fragmentMap = new EnumMap<>(TabType.class);

    // UI 组件
    private final ImageButton btnTerminal;      // 终端切换按钮
    private final ImageButton btnFileBrowser;   // 文件浏览器切换按钮

    // Fragment 管理相关
    private final FragmentManager fragmentManager; // Fragment 管理器
    private final int leftContainerId;          // 左侧容器资源 ID
    private final int rightContainerId;         // 右侧容器资源 ID

    /** 当前显示的 Fragment 实例 */
    private Fragment leftPaneFragment;   // 左侧面板当前显示的 Fragment
    private Fragment rightPaneFragment;  // 右侧面板当前显示的 Fragment

    /** 状态标志 */
    private boolean isFileBroswerON;     // 文件浏览器显示状态标识
    private int numberOfEditors = 0;     // 已打开的编辑器计数器（最大 5 个）

    /**
     * 构造函数
     * @param btnTerminal 终端按钮控件
     * @param btnFileBrowser 文件浏览器按钮控件
     * @param fragmentManager Fragment 管理器实例
     * @param leftContainerId 左侧容器资源 ID（用于显示终端和编辑器）
     * @param rightContainerId 右侧容器资源 ID（用于显示文件浏览器）
     */
    public TabManager(ImageButton btnTerminal,
                      ImageButton btnFileBrowser,
                      FragmentManager fragmentManager,
                      int leftContainerId,
                      int rightContainerId) throws InterruptedException {
        // 初始化组件引用
        this.btnTerminal = btnTerminal;
        this.btnFileBrowser = btnFileBrowser;
        this.fragmentManager = fragmentManager;
        this.leftContainerId = leftContainerId;
        this.rightContainerId = rightContainerId;
        this.isFileBroswerON = false;

        // 设置按钮监听并初始化默认视图
        setupButtonListeners();
        switchTab(TabType.TERMINAL); // 默认显示终端面板
    }

    /** 初始化按钮点击事件监听器 */
    private void setupButtonListeners() {
        // 终端按钮点击事件：切换到终端面板
        btnTerminal.setOnClickListener(v -> {
            try {
                switchTab(TabType.TERMINAL);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("终端切换操作被中断", e);
            }
        });

        // 文件浏览器按钮点击事件：切换文件浏览器的显示状态
        btnFileBrowser.setOnClickListener(v -> {
            try {
                switchTab(TabType.FILE_BROWSER);
                this.isFileBroswerON = !this.isFileBroswerON; // 反转显示状态
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("文件浏览器切换操作被中断", e);
            }
        });
    }

    /**
     * 核心方法：执行选项卡切换操作
     * @param tabType 要切换的面板类型
     * @throws InterruptedException 当线程被中断时抛出
     */
    @SuppressLint("CommitTransaction")
    public void switchTab(TabType tabType) throws InterruptedException {
        FragmentTransaction transaction = fragmentManager.beginTransaction();

        // 处理终端面板切换（左侧容器）
        if (tabType == TabType.TERMINAL) {
            Fragment terminalFragment = fragmentMap.get(tabType);

            // 延迟初始化：首次使用时创建 Fragment
            if (terminalFragment == null) {
                terminalFragment = createFragmentForTab(tabType);

                // 初始化左侧面板引用
                if (leftPaneFragment == null) {
                    leftPaneFragment = terminalFragment;
                }
                fragmentMap.put(tabType, leftPaneFragment);

                // 将 Fragment 添加到左侧容器
                transaction.add(leftContainerId, leftPaneFragment, tabType.name());
                transaction.commit();
            }

            // 隐藏当前左侧面板内容（如果可见）
            if (leftPaneFragment != null && leftPaneFragment.isVisible()) {
                hideFromLeftPane(leftPaneFragment);
            }

            // 显示终端面板并更新状态
            showInLeftPane(terminalFragment);
            leftPaneFragment = terminalFragment;
        }

        // 处理文件浏览器面板（右侧容器）
        if (tabType == TabType.FILE_BROWSER) {
            Fragment fileBrowserFragment = fragmentMap.get(tabType);

            // 延迟初始化
            if (fileBrowserFragment == null) {
                fileBrowserFragment = createFragmentForTab(tabType);

                // 初始化右侧面板引用
                if (rightPaneFragment == null) {
                    rightPaneFragment = fileBrowserFragment;
                }
                fragmentMap.put(tabType, rightPaneFragment);

                // 添加到右侧容器
                transaction.add(rightContainerId, rightPaneFragment, tabType.name());
                transaction.commit();
            }

            // 根据当前状态切换显示/隐藏（使用线程安全方法）
            if (this.isFileBroswerON) {
                hideFromRightPane(fileBrowserFragment);
                this.isFileBroswerON = false;
            } else {
                showInRightPane(fileBrowserFragment);
                this.isFileBroswerON = true;
            }
        }

        // 处理编辑器面板（左侧容器）
        if (tabType == TabType.EDITOR) {
            Fragment editorFragment = fragmentMap.get(tabType);

            // 限制最大打开数量为 5 个
            if (numberOfEditors <= 5) {
                editorFragment = createFragmentForTab(tabType);
                fragmentMap.put(tabType, editorFragment);

                // 使用唯一标签添加到容器
                transaction.add(leftContainerId, editorFragment,
                        tabType.name() + numberOfEditors);
                transaction.commit();
                numberOfEditors += 1;

                // 多个编辑器时显示标签栏（TODO：待实现）
                if (numberOfEditors > 1) addEditorsTagsViewer();
            }

            // 隐藏当前终端面板（如果可见）
            if (fragmentMap.get(TabType.TERMINAL) != null &&
                    fragmentMap.get(TabType.TERMINAL).isVisible()) {
                hideFromLeftPane(fragmentMap.get(TabType.TERMINAL));
            }

            // 显示编辑器面板
            showInLeftPane(editorFragment);
        }
    }

    /**
     * （待实现）添加编辑器标签视图
     * 用于在多个编辑器实例之间切换
     */
    private void addEditorsTagsViewer() {
        // 规划功能：当打开多个编辑器时，在顶部显示可切换的标签栏
        // 预期实现方案：使用 TabLayout+ViewPager2 管理多个编辑器实例
    }

    /**
     * 打开指定文件的编辑器（重载方法）
     * @param tabType 必须为 EDITOR 类型
     * @param file 要编辑的文件对象
     */
    public void switchTab(TabType tabType, File file) throws InterruptedException {
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        if (tabType == TabType.EDITOR) {
            Fragment editorFragment = fragmentMap.get(tabType);

            // 创建带文件参数的编辑器实例
            if (editorFragment == null) {
                editorFragment = createFragmentForTab(tabType, file);
                fragmentMap.put(tabType, editorFragment);

                // 添加到左侧容器
                transaction.add(leftContainerId, editorFragment, tabType.name());
                transaction.commit();
            }

            // 隐藏当前终端
            if (fragmentMap.get(TabType.TERMINAL) != null &&
                    fragmentMap.get(TabType.TERMINAL).isVisible()) {
                hideFromLeftPane(fragmentMap.get(TabType.TERMINAL));
            }

            // 显示编辑器
            showInLeftPane(editorFragment);
        }
    }

    /**
     * 创建指定类型的 Fragment 实例
     * @param tabType 要创建的面板类型
     * @return 对应的 Fragment 实例
     * @throws IllegalArgumentException 传入非法类型时抛出
     */
    private Fragment createFragmentForTab(TabType tabType) {
        switch (tabType) {
            case TERMINAL:
                return new TerminalFragment();    // 终端实例
            case FILE_BROWSER:
                return new FileBrowserFragment(); // 文件浏览器实例
            case EDITOR:
                return new EditorFragment();      // 空白编辑器实例
            default:
                throw new IllegalArgumentException("不支持的选项卡类型: " + tabType);
        }
    }

    /**
     * 创建带文件参数的编辑器实例（工厂方法）
     * @param tabType 必须为 EDITOR 类型
     * @param file 需要编辑的目标文件
     */
    private Fragment createFragmentForTab(TabType tabType, File file) {
        if (tabType == TabType.EDITOR) {
            return new EditorFragment(file); // 创建带文件参数的编辑器
        } else {
            throw new IllegalArgumentException("非编辑器类型不支持文件参数: " + tabType);
        }
    }

    /** 清理资源，解除按钮监听 */
    public void cleanup() {
        btnTerminal.setOnClickListener(null);
        btnFileBrowser.setOnClickListener(null);
    }

    // region 同步控制方法（使用 ReentrantLock 和 Condition 保证线程安全）

    /**
     * 在左侧容器显示指定 Fragment（线程安全）
     * @param fragment 要显示的 Fragment
     * @throws InterruptedException 当线程被中断时抛出
     */
    private void showInLeftPane(Fragment fragment) throws InterruptedException {
        MainActivity.leftPaneLock.lock(); // 获取左侧面板锁
        try {
            // 等待当前 Fragment 隐藏
            while (leftPaneFragment != null && leftPaneFragment.isVisible()) {
                MainActivity.leftPaneCondition.await();
            }

            // 执行显示操作
            FragmentTransaction transaction = fragmentManager.beginTransaction();
            transaction.show(fragment);
            transaction.commit(); // 提交事务

            // 更新当前 Fragment 引用
            leftPaneFragment = fragment;
        } finally {
            MainActivity.leftPaneLock.unlock(); // 释放锁
        }
    }

    /**
     * 隐藏左侧容器的指定 Fragment（线程安全）
     * @param fragment 要隐藏的 Fragment
     */
    private void hideFromLeftPane(Fragment fragment) {
        MainActivity.leftPaneLock.lock(); // 获取左侧面板锁
        try {
            // 执行隐藏操作
            FragmentTransaction transaction = fragmentManager.beginTransaction();
            transaction.hide(fragment);
            transaction.commit();

            // 清空当前 Fragment 引用
            if (leftPaneFragment == fragment) {
                leftPaneFragment = null;
            }

            // 唤醒等待线程
            if (leftPaneFragment != null && !leftPaneFragment.isVisible()) {
                MainActivity.leftPaneCondition.signalAll();
            }
        } finally {
            MainActivity.leftPaneLock.unlock(); // 释放锁
        }
    }

    /**
     * 在右侧容器显示指定 Fragment（线程安全）
     * @param fragment 要显示的 Fragment
     * @throws InterruptedException 当线程被中断时抛出
     */
    private void showInRightPane(Fragment fragment) throws InterruptedException {
        MainActivity.rightPaneLock.lock(); // 获取右侧面板锁
        try {
            // 等待当前 Fragment 隐藏
            while (rightPaneFragment != null && rightPaneFragment.isVisible()) {
                MainActivity.rightPaneCondition.await();
            }

            // 执行显示操作
            FragmentTransaction transaction = fragmentManager.beginTransaction();
            transaction.show(fragment);
            transaction.commit();
            rightPaneFragment = fragment; // 更新状态
        } finally {
            MainActivity.rightPaneLock.unlock(); // 释放锁
        }
    }

    /**
     * 隐藏右侧容器的指定 Fragment（线程安全）
     * @param fragment 要隐藏的 Fragment
     */
    private void hideFromRightPane(Fragment fragment) {
        MainActivity.rightPaneLock.lock(); // 获取右侧面板锁
        try {
            // 执行隐藏操作
            FragmentTransaction transaction = fragmentManager.beginTransaction();
            transaction.hide(fragment);
            transaction.commit();
            if (rightPaneFragment == fragment) {
                rightPaneFragment = null; // 清空状态
            }

            // 唤醒等待线程
            if (rightPaneFragment != null && !rightPaneFragment.isVisible()) {
                MainActivity.rightPaneCondition.signalAll();
            }
        } finally {
            MainActivity.rightPaneLock.unlock(); // 释放锁
        }
    }
    // endregion
}