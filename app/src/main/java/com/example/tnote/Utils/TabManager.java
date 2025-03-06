package com.example.tnote.Utils;

import android.annotation.SuppressLint;
import android.widget.ImageButton;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.example.tnote.Editor.EditorFragment;
import com.example.tnote.FileBrowser.FileBrowserFragment;
import com.example.tnote.Terminal.TerminalFragment;

import java.io.File;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

public class TabManager {
    public enum TabType { TERMINAL, FILE_BROWSER, EDITOR }
    public final Map<TabType, Fragment> fragmentMap = new EnumMap<>(TabType.class);
    private final ImageButton btnTerminal;
    private final  ImageButton btnFileBrowser;
    private final FragmentManager fragmentManager;
    private final int leftContainerId;
    private final int rightContainerId;

    private boolean isFileBroswerON;

    private int numberOfEditors = 0;

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

    @SuppressLint("CommitTransaction")
    public void switchTab(TabType tabType) {
        FragmentTransaction transaction = fragmentManager.beginTransaction();

        // 处理TERMINAL,放在左边
        if (tabType == TabType.TERMINAL) {
            Fragment terminalFragment = fragmentMap.get(tabType);
            if (terminalFragment == null) {
                terminalFragment = createFragmentForTab(tabType);
                fragmentMap.put(tabType, terminalFragment);
                transaction.add(leftContainerId, terminalFragment, tabType.name());
            }
            if(fragmentMap.get(TabType.EDITOR)!=null&& fragmentMap.get(TabType.EDITOR).isVisible()){
                transaction.hide(fragmentMap.get(TabType.EDITOR));
            }
            transaction.show(terminalFragment);
        }

        // 处理FILE_BROWSER：根据状态显示/隐藏，出现在右边
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

        if (tabType == TabType.EDITOR){
            Fragment editorFragment = fragmentMap.get(tabType);
            if (numberOfEditors <= 5) {
                editorFragment = createFragmentForTab(tabType);
                fragmentMap.put(tabType, editorFragment);
                transaction.add(leftContainerId, editorFragment, tabType.name()+ numberOfEditors);
                numberOfEditors +=1;
                if(numberOfEditors>1) addEditorsTagsViewer();
            }
            if(fragmentMap.get(TabType.TERMINAL)!=null&& fragmentMap.get(TabType.TERMINAL).isVisible()){
                System.out.println("NULL check:???????");
                System.out.println(fragmentMap.get(TabType.TERMINAL)!=null);
                System.out.println(fragmentMap.get(TabType.TERMINAL).isVisible());
                transaction.hide(fragmentMap.get(TabType.TERMINAL));
            }
            transaction.show(editorFragment);
        }
        transaction.commit();
    }
    private void addEditorsTagsViewer(){


    };
    public  void switchTab(TabType tabType, File file){
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        if (tabType == TabType.EDITOR){
            Fragment editorFragment = fragmentMap.get(tabType);
            if (editorFragment == null) {
                editorFragment = createFragmentForTab(tabType,file);
                fragmentMap.put(tabType, editorFragment);
                transaction.add(leftContainerId, editorFragment, tabType.name());
            }
            if(fragmentMap.get(TabType.TERMINAL)!=null&& fragmentMap.get(TabType.TERMINAL).isVisible()){
                System.out.println("NULL check:???????");
                System.out.println(fragmentMap.get(TabType.TERMINAL)!=null);
                System.out.println(fragmentMap.get(TabType.TERMINAL).isVisible());
                transaction.hide(fragmentMap.get(TabType.TERMINAL));
            }
            transaction.show(editorFragment);
        }
        transaction.commit();
    }






    private Fragment createFragmentForTab(TabType tabType) {
        switch (tabType) {
            case TERMINAL:
                return new TerminalFragment();
            case FILE_BROWSER:
                return new FileBrowserFragment();
            case EDITOR:
                return new EditorFragment();
            default:
                throw new IllegalArgumentException("未知选项卡类型: " + tabType);
        }
    }
    private Fragment createFragmentForTab(TabType tabType,File file) {
            if (tabType==TabType.EDITOR) return new EditorFragment(file);
            else{
                throw new IllegalArgumentException("未知选项卡类型: " + tabType);
            }
    }



    public void cleanup() {
        btnTerminal.setOnClickListener(null);
        btnFileBrowser.setOnClickListener(null);
    }
}