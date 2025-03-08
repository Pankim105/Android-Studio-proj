package com.example.tnote.Utils;

import android.annotation.SuppressLint;
import android.util.Log;
import android.widget.ImageButton;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.example.tnote.Editor.EditorFragment;
import com.example.tnote.FileBrowser.FileBrowserFragment;
import com.example.tnote.MainActivity;
import com.example.tnote.Terminal.TerminalFragment;

import java.io.File;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.function.Consumer;

/**
 * 选项卡管理类，负责管理终端、文件浏览器和编辑器三个面板的切换逻辑
 * 使用 ReentrantLock 和 Condition 实现线程安全的 Fragment 操作
 */
public class TabManager {

    /**
     * 选项卡类型枚举
     */
    public enum TabType {
        TERMINAL,       // 终端选项卡
        FILE_BROWSER,   // 文件浏览器选项卡
        EDITOR          // 编辑器选项卡
    }

    /**
     * Fragment 缓存映射表：存储已创建的 Fragment 实例，key 为选项卡类型
     */
    public final Map<TabType, Fragment> fragmentMap = new EnumMap<>(TabType.class);

    // UI 组件
    private final ImageButton btnTerminal;      // 终端切换按钮
    private final ImageButton btnFileBrowser;   // 文件浏览器切换按钮

    // Fragment 管理相关
    public final FragmentManager fragmentManager; // Fragment 管理器
    private final int leftContainerId;          // 左侧容器资源 ID (用于显示终端和编辑器)
    private final int rightContainerId;         // 右侧容器资源 ID (用于显示文件浏览器)

    /**
     * 当前显示的 Fragment 实例
     */
    public Fragment leftPaneFragment;   // 左侧面板当前显示的 Fragment
    public Fragment rightPaneFragment;  // 右侧面板当前显示的 Fragment

    /**
     * 状态标志
     */
    private boolean isFileBrowserVisible;     // 文件浏览器显示状态标识
    public Semaphore numberOfEditors = new Semaphore(1);     // 已打开的编辑器计数器（最大 5 个）, 使用信号量控制编辑器Fragment的创建数量
    private final ArrayList<String> editorTags = new ArrayList<>(); // 已打开的编辑器在 Manager 中的 tag 列表，用于跟踪和管理编辑器Fragment
    private static final String TAG = "LockUtils";// 测试debug用

    /**
     * 构造函数
     * @param btnTerminal 终端按钮控件
     * @param btnFileBrowser 文件浏览器按钮控件
     * @param fragmentManager Fragment 管理器实例
     * @param leftContainerId 左侧容器资源 ID（用于显示终端和编辑器）
     * @param rightContainerId 右侧容器资源 ID（用于显示文件浏览器）
     * @throws InterruptedException 当线程被中断时抛出
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
        this.isFileBrowserVisible = false;
        switchTab(TabType.TERMINAL); // 默认显示终端面板
    }

    /**
     * 核心方法：执行选项卡切换操作
     * @param tabType 要切换的面板类型
     * @throws InterruptedException 当线程被中断时抛出
     */
    @SuppressLint("CommitTransaction")
    public void switchTab(TabType tabType) throws InterruptedException {
        if (tabType == TabType.TERMINAL) {
            handleTerminalTab();
        } else if (tabType == TabType.FILE_BROWSER) {
            handleFileBrowserTab();
        } else if (tabType == TabType.EDITOR) {
            handleEditorTab();
        }
    }

    /**
     * 处理切换到终端选项卡
     */
    private void handleTerminalTab() {
        if(leftPaneFragment instanceof EditorFragment) Log.println(Log.INFO,"EDITOR TO TERMINAL","DOES EDITOR HIDE?");
        // 如果左侧面板当前显示的Fragment不为空Fragment且可见，则隐藏它
        if (leftPaneFragment == null){
            Log.println(Log.INFO,"LEFTPANE IS","NULL");
        }
        if (leftPaneFragment != null && !leftPaneFragment.isVisible()){
            Log.println(Log.INFO,"LEFTPANE IS","NOT VISIABLE");
        }
        if (leftPaneFragment != null && leftPaneFragment.isVisible()) {
            Log.println(Log.INFO,"TERMINAL HIDE", "TERMINAL HIDE!");
            hideFragment(leftPaneFragment);
        }
        // 初始化或获取终端Fragment
        if(leftPaneFragment!=fragmentMap.get(TabType.TERMINAL)|leftPaneFragment==null) {
            leftPaneFragment = initializeFragment(TabType.TERMINAL, leftContainerId);
            Log.println(Log.INFO, "LEFTPANE：", "TERMINAL");
        }
        Log.println(Log.INFO,"TERMINAL SHOW", "TERMINAL SHOW!");
        // 切换显示
        switchPane(leftPaneFragment, leftContainerId);
    }

    /**
     * 处理切换到文件浏览器选项卡
     */
    private void handleFileBrowserTab() {
        // 初始化或获取文件浏览器Fragment
        rightPaneFragment = initializeFragment(TabType.FILE_BROWSER, rightContainerId);
        // 切换显示/隐藏
        toggleFragmentVisibility(rightPaneFragment, () -> isFileBrowserVisible = !isFileBrowserVisible);
    }

    /**
     * 处理切换到编辑器选项卡
     * @throws InterruptedException 当线程被中断时抛出
     */
    private void handleEditorTab() throws InterruptedException {
        numberOfEditors.acquire(); // 获取编辑器信号量，限制编辑器数量
        try {
            Fragment editorFragment = createFragmentForTab(TabType.EDITOR);
            fragmentMap.put(TabType.EDITOR, editorFragment);
            // 添加Fragment和标签
            addFragmentWithTag(editorFragment, leftContainerId, TabType.EDITOR.name());
            addEditorsTagsViewer(); // 添加编辑器标签视图 (待实现)

            // 切换显示编辑器并隐藏终端
            if (leftPaneFragment != null && leftPaneFragment.isVisible()) {
                    hideFragment(leftPaneFragment);
            };
            switchPane(editorFragment, leftContainerId);
            leftPaneFragment = editorFragment;
            Log.println(Log.INFO,"LEFTPANE：", "EDITOR");

        } catch (Exception e) {
            numberOfEditors.release(); // 发生异常时释放信号量
            throw new RuntimeException(e);
        }
    }

    /**
     * 通用初始化Fragment方法
     * 用于创建或获取指定类型的Fragment实例，并添加到Fragment缓存和容器中
     * @param tabType 要初始化的选项卡类型
     * @param containerId Fragment要添加到的容器ID
     * @return 初始化后的Fragment实例
     */
    private Fragment initializeFragment(TabType tabType, int containerId) {
        Fragment fragment = fragmentMap.get(tabType); // 尝试从缓存中获取Fragment

        if (fragment == null) {
            // 如果缓存中不存在，则创建新的Fragment实例
            fragment = createFragmentForTab(tabType);
            fragmentMap.put(tabType, fragment); // 将Fragment添加到缓存中
            addFragmentToContainer(fragment, containerId, tabType.name()); // 将Fragment添加到容器中
        }
        return fragment; // 返回Fragment实例
    }

    /**
     * 通用Fragment添加方法
     * @param fragment 要添加的Fragment实例
     * @param containerId Fragment要添加到的容器ID
     * @param tag Fragment的标签，用于Fragment管理
     */
    private void addFragmentToContainer(Fragment fragment, int containerId, String tag) {
        executeTransaction(transaction -> transaction.add(containerId, fragment, tag));
    }

    /**
     * 带标签的Fragment添加方法
     * 用于添加编辑器Fragment并记录其标签
     * @param fragment 要添加的Fragment实例
     * @param containerId Fragment要添加到的容器ID
     * @param tag Fragment的标签
     */
    private void addFragmentWithTag(Fragment fragment, int containerId, String tag) {
        editorTags.add(tag); // 将编辑器Fragment的标签添加到列表中
        addFragmentToContainer(fragment, containerId, tag); // 调用通用方法将Fragment添加到容器
    }

    /**
     * 通用面板切换方法
     * 用于执行面板切换的通用逻辑，包括执行切换前的操作、显示新的Fragment、更新当前面板Fragment的引用
     * @param newFragment 要显示的新Fragment实例
     * @param containerId Fragment要显示的容器ID
     */
    private void switchPane(Fragment newFragment, int containerId) {
        executeTransaction(transaction -> {
            transaction.show(newFragment); // 显示新的Fragment
            // 更新当前面板Fragment的引用
            if (containerId == leftContainerId) leftPaneFragment = newFragment;
            if (containerId == rightContainerId) rightPaneFragment = newFragment;
        });
    }

    /**
     * 通用可见性切换方法
     * 用于切换Fragment的可见性（显示/隐藏）
     * @param fragment 要切换可见性的Fragment实例
     * @param postAction 切换可见性后需要执行的操作，Runnable 接口的实现
     */
    private void toggleFragmentVisibility(Fragment fragment, Runnable postAction) {
        executeTransaction(transaction -> {
            // 如果Fragment当前可见，则隐藏；否则显示
            if (fragment.isVisible()) {
                transaction.hide(fragment);
            } else {
                transaction.show(fragment);
            }
            postAction.run(); // 执行切换可见性后的操作
        });
    }

    /**
     * 通用Fragment隐藏方法
     * @param fragment 要隐藏的Fragment实例
     */
    private void hideFragment(Fragment fragment) {
        executeTransaction(transaction -> transaction.hide(fragment));
    }

    /**
     * 通用事务执行方法
     * 封装Fragment事务的执行过程，确保事务的统一处理
     * @param action Consumer 接口，用于执行Fragment事务的具体操作
     */
    private void executeTransaction(Consumer<FragmentTransaction> action) {
        FragmentTransaction transaction = fragmentManager.beginTransaction(); // 开启Fragment事务
        action.accept(transaction); // 执行具体的事务操作
        transaction.commit(); // 提交事务
        Log.println(Log.INFO,"Transaction commit", "DONE!");
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
     * @throws InterruptedException 当线程被中断时抛出
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
                leftPaneFragment = editorFragment;
                Log.println(Log.INFO,"LEFTPANE：","EDITOR");
            }else{
                //处理多个编辑器逻辑 目前的实现是直接替换前编辑器

                remove(leftPaneFragment);
                editorFragment = createFragmentForTab(tabType, file);
                fragmentMap.put(tabType, editorFragment);
                // 添加到左侧容器
                transaction.add(leftContainerId, editorFragment, tabType.name());
                transaction.commit();
                leftPaneFragment = editorFragment;
                Log.println(Log.INFO,"LEFTPANE：","NEW EDITOR");
            }

            // 隐藏当前终端
            Log.println(Log.INFO,"TERMINAL IS","being hiding!!");
            if(fragmentMap.get(TabType.TERMINAL) == null){
                Log.println(Log.INFO,"TERMINAL IS","NOT IN MAP");
            }
            if(!fragmentMap.get(TabType.TERMINAL).isVisible()){
                Log.println(Log.INFO,"TERMINAL IS","NOT VISIABLE");
            }
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
                return new TerminalFragment();    // 创建终端实例
            case FILE_BROWSER:
                return new FileBrowserFragment(); // 创建文件浏览器实例
            case EDITOR:
                return new EditorFragment();      // 创建空白编辑器实例
            default:
                throw new IllegalArgumentException("不支持的选项卡类型: " + tabType);
        }
    }

    /**
     * 创建带文件参数的编辑器实例（工厂方法）
     * @param tabType 必须为 EDITOR 类型
     * @param file 需要编辑的目标文件
     * @return 带文件参数的编辑器 Fragment 实例
     * @throws IllegalArgumentException 当传入的选项卡类型不是 EDITOR 时抛出
     */
    private Fragment createFragmentForTab(TabType tabType, File file) {
        if (tabType == TabType.EDITOR) {
            return new EditorFragment(file); // 创建带文件参数的编辑器
        } else {
            throw new IllegalArgumentException("非编辑器类型不支持文件参数: " + tabType);
        }
    }

    /**
     * 清理资源，解除按钮监听
     */
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
                MainActivity.leftPaneCondition.await(); // 等待左侧面板条件
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
            Log.println(Log.INFO,"HIDE LEFTFRAGMENT","hide!");
            transaction.commit();

            // 清空当前 Fragment 引用
            if (leftPaneFragment == fragment) {
                leftPaneFragment = null;
                Log.println(Log.INFO,"LEFTPANE：", "NULL");
            }

            // 唤醒等待线程
            if (leftPaneFragment != null && !leftPaneFragment.isVisible()) {
                MainActivity.leftPaneCondition.signalAll(); // 通知所有等待左侧面板条件的线程
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
                MainActivity.rightPaneCondition.await(); // 等待右侧面板条件
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
                MainActivity.rightPaneCondition.signalAll(); // 通知所有等待右侧面板条件的线程
            }
        } finally {
            MainActivity.rightPaneLock.unlock(); // 释放锁
        }
    }

    /**
     * 线程安全地移除指定的 Fragment
     * @param fragment 要移除的 Fragment
     */
    public void remove(Fragment fragment) {
        if (fragment == null) return;

        MainActivity.leftPaneLock.lock(); // 获取左侧面板锁
        MainActivity.rightPaneLock.lock(); // 获取右侧面板锁
        try {
            FragmentTransaction transaction = fragmentManager.beginTransaction();
            // 移除 Fragment
            transaction.remove(fragment);
            transaction.commit();

            // 更新 Fragment 引用和 fragmentMap
            if (leftPaneFragment == fragment) {
                leftPaneFragment = null;
                Log.println(Log.INFO,"LEFTPANE：", "NULL");
            }
            if (rightPaneFragment == fragment) {
                rightPaneFragment = null;
            }

            // 从 fragmentMap 中移除
            Iterator<Map.Entry<TabType, Fragment>> iterator = fragmentMap.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<TabType, Fragment> entry = iterator.next();
                if (entry.getValue() == fragment) {
                    iterator.remove();
                    break;
                }
            }

            // 如果是编辑器 Fragment，更新信号量和标签列表
            if (fragment instanceof EditorFragment) {
                numberOfEditors.release(); // 释放信号量
                if (editorTags != null) {
                    String tagToRemove = null;
                    // 查找要移除的 tag，通过 FragmentManager 查找不到的 tag 即为要移除的 tag
                    for (String tag : editorTags) {
                        if (fragmentManager.findFragmentByTag(tag) == null) {
                            tagToRemove = tag;
                            break;
                        }
                    }
                    if (tagToRemove != null) {
                        editorTags.remove(tagToRemove); // 从列表中移除 tag
                    }
                }
            }

            // 唤醒等待线程
            MainActivity.leftPaneCondition.signalAll(); // 通知所有等待左侧面板条件的线程
            MainActivity.rightPaneCondition.signalAll(); // 通知所有等待右侧面板条件的线程
        } finally {
            MainActivity.leftPaneLock.unlock(); // 释放左侧面板锁
            MainActivity.rightPaneLock.unlock(); // 释放右侧面板锁
        }
    }

    // 调试用，已注释

    //    public static void printLockInfo(Lock lock, String lockName) {
    //        ReentrantLock reentrantLock = (ReentrantLock) lock;
    //        if (reentrantLock.getHoldCount() > 0) {
    //            Thread currentThread = Thread.currentThread();
    //            StackTraceElement[] stackTrace = currentThread.getStackTrace();
    //            StringBuilder stackTraceString = new StringBuilder();
    //
    //            for (StackTraceElement element : stackTrace) {
    //                stackTraceString.append(element.toString()).append("\n");
    //            }
    //
    //            Log.d(TAG, "Lock '" + lockName + "' is currently held by thread: " + currentThread.getName() + " (ID: " + currentThread.getId() + ")");
    //            Log.d(TAG, "Stack trace:\n" + stackTraceString.toString());
    //        } else {
    //            Log.d(TAG, "Lock '" + lockName + "' is currently NOT held.");
    //        }
    //    }

    // endregion
}