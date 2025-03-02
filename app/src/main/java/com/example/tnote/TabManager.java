package com.example.tnote;

import android.widget.ImageButton;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.example.tnote.filebrowser.FileBrowserFragment;

import java.util.EnumMap;
import java.util.Map;

public class TabManager {
    public enum TabType { TERMINAL, FILE_BROWSER }
    public final Map<TabType, Fragment> fragmentMap = new EnumMap<>(TabType.class);
    private final ImageButton btnTerminal;
    private final  ImageButton btnFileBrowser;
    private final FragmentManager fragmentManager;
    private final int leftContainerId;
    private final int rightContainerId;

    private boolean isFileBroswerON;

    public TabManager( ImageButton btnTerminal,
                       ImageButton btnFileBrowser,
                      FragmentManager fragmentManager,
                      int leftContainerId,
                      int rightContainerId) {
        this.btnTerminal = btnTerminal;
        this.btnFileBrowser = btnFileBrowser;
        this.fragmentManager = fragmentManager;
        this.leftContainerId = leftContainerId;
        this.rightContainerId = rightContainerId;
        this.isFileBroswerON = false;


        setupButtonListeners();
        switchTab(TabType.TERMINAL); // 初始化默认Fragment
    }

    private void setupButtonListeners() {
        btnTerminal.setOnClickListener(v -> {
                switchTab(TabType.TERMINAL);
        });

        btnFileBrowser.setOnClickListener(v -> {
            switchTab(TabType.FILE_BROWSER);
            this.isFileBroswerON = !this.isFileBroswerON;
        });
    }

    public void switchTab(TabType tabType) {
        FragmentTransaction transaction = fragmentManager.beginTransaction();

        // 处理TERMINAL：始终显示
        if (tabType == TabType.TERMINAL) {
            Fragment terminalFragment = fragmentMap.get(tabType);
            if (terminalFragment == null) {
                terminalFragment = createFragmentForTab(tabType);
                fragmentMap.put(tabType, terminalFragment);
                transaction.add(leftContainerId, terminalFragment, tabType.name());
            }
            transaction.show(terminalFragment);
        }

        // 处理FILE_BROWSER：根据状态显示/隐藏
        if (tabType == TabType.FILE_BROWSER) {
            Fragment fileBrowserFragment = fragmentMap.get(tabType);
            if (fileBrowserFragment == null) {
                fileBrowserFragment = createFragmentForTab(tabType);
                fragmentMap.put(tabType, fileBrowserFragment);
                transaction.add(rightContainerId, fileBrowserFragment, tabType.name());
            }
            if (this.isFileBroswerON) {
                transaction.hide(fileBrowserFragment);
                this.isFileBroswerON = false;
            } else {
                //((FileBrowserFragment) fileBrowserFragment).resetRootNodeWhenClick();//重置目录根节点
                transaction.show(fileBrowserFragment);
                this.isFileBroswerON = true;
            }
        }

        transaction.commit();
    }






    private Fragment createFragmentForTab(TabType tabType) {
        switch (tabType) {
            case TERMINAL:
                return new com.example.tnote.terminal.TerminalFragment();
            case FILE_BROWSER:
                return new FileBrowserFragment();
            default:
                throw new IllegalArgumentException("未知选项卡类型: " + tabType);
        }
    }



    public void cleanup() {
        btnTerminal.setOnClickListener(null);
        btnFileBrowser.setOnClickListener(null);
    }
}