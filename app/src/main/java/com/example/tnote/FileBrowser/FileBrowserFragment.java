package com.example.tnote.FileBrowser;
import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.service.controls.actions.FloatAction;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.tnote.Editor.EditorFragment;
import com.example.tnote.MainActivity;
import com.example.tnote.R;
import com.example.tnote.Terminal.TerminalFragment;
import com.example.tnote.Utils.TabManager;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class FileBrowserFragment extends Fragment {
    private RecyclerView recyclerView;
    private FloatingActionButton addFile;
    private CardView cardView;
    private TextView fileName;
    private Button confirmButton;
    private Button cancelButton;
    private FileBrowserAdapter adapter;
    private List<FileBrowserAdapter.TreeNode> currentNodes = new ArrayList<>();
    private final Handler refreshHandler = new Handler();
    private Runnable refreshRunnable;
    private int refreshFrequency = 1;
    private final Map<File, Long> dirModifiedTimes = new HashMap<>(); // 记录目录修改时间

    private volatile static File currentDirectory;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_filebrowser, container, false);
        initRootNode();
        initViews(view);
        setButtonListener();
        loadDirectory(currentDirectory);
        return view;
    }

    private void initRootNode() {
        if (currentDirectory == null) currentDirectory = MainActivity.getAppDir();
    }


    @Override
    public void onResume() {
        super.onResume();
        startAutoRefresh();
    }

    @Override
    public void onPause() {
        super.onPause();
        stopAutoRefresh();
    }

    private void initViews(View view) {
        //文件浏览item
        recyclerView = view.findViewById(R.id.rv_file_list);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new FileBrowserAdapter(new ArrayList<>(), node -> {
            File file = node.getFile();
            if (!file.isDirectory()) return;

//            if (node.getLevel() == 999) { // 处理上级目录
//                currentDirectory = file;
//                loadDirectory(currentDirectory);
//                return;
//            }

            if (node.isExpanded()) {
                collapseNode(node);
            } else {
                expandNode(node);
            }
            adapter.updateData(getVisibleNodes(currentNodes));
        });
        recyclerView.setAdapter(adapter);
        //文件创建fab、确认card
        addFile = view.findViewById(R.id.fab_create_file);
        fileName = view.findViewById(R.id.et_file_name);
        confirmButton = view.findViewById(R.id.btn_confirm);
        cancelButton = view.findViewById(R.id.btn_cancel);
        cardView = view.findViewById(R.id.card_file_name);
    }
    private void setButtonListener(){
        addFile.setOnClickListener(v -> {
            try {
                toggleCardView();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
        confirmButton.setOnClickListener(v -> {
            try {
                creatFile();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }
    public void creatFile() throws IOException {
        String Name = fileName.getText().toString();
        File file = new File(MainActivity.getAppDir(),Name);
        Log.println(Log.INFO,"Check Save Path", file.getAbsolutePath());
        try{
            if (!file.exists()) {
                file.createNewFile();
            }
            FileOutputStream fos = new FileOutputStream(file);
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(fos));
            writer.write("from torch import tensor\nprint(\"This is a python file test\")"); // 写入数据
            writer.flush();
            writer.close();
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        fileName.setText("");
        cardView.setVisibility(View.GONE);
        setGuidline(0.5f);
        if (isAdded() && getContext() != null && getContext() instanceof MainActivity) {
            MainActivity activity = (MainActivity) getContext();
            activity.tabManager.switchTab(TabManager.TabType.EDITOR,file);
        }
    }
    public void toggleCardView() throws InterruptedException {
        if(cardView.getVisibility()==View.GONE) {
            cardView.setVisibility(View.VISIBLE);
            setGuidline(0.23f);
        }
        else{
            setGuidline(0.5f);
            MainActivity.guidelineLock.lock();
            cardView.setVisibility(View.GONE);
            MainActivity.guidelineLock.unlock();
        }

    }
    private void setGuidline(float pos){
        try {
            Activity mainActivity = getActivity();
            assert mainActivity != null;
            ((MainActivity) mainActivity).setGuideLinePosition(pos);
        } catch (Exception e) {
            Log.e("Activity cannot convert!", "Trying convert an activity to mainActivity and setGuideline", e);
        }
    }

    public void loadDirectory(File directory) {
        if (!directory.canRead()) {
            showAccessDeniedDialog();
            currentDirectory = MainActivity.getAppDir();
            loadDirectory(currentDirectory);
            return;
        }

        dirModifiedTimes.put(directory, directory.lastModified());

        List<FileBrowserAdapter.TreeNode> newNodes = new ArrayList<>();
       // 添加父目录节点
//        if (directory.getParentFile().canRead()) {
//            FileBrowserAdapter.TreeNode parentNode = new FileBrowserAdapter.TreeNode(
//                    directory.getParentFile(), 999);
//            parentNode.setHasLoadedChildren(true);
//            newNodes.add(parentNode);
//        }
        // 构建当前层级节点
        buildCurrentLevelNodes(directory, 0, newNodes, currentNodes);
        currentNodes = newNodes;
        adapter.updateData(getVisibleNodes(currentNodes));
    }

    private void buildCurrentLevelNodes(File dir, int level,
                                        List<FileBrowserAdapter.TreeNode> newNodes,
                                        List<FileBrowserAdapter.TreeNode> oldNodes) {
        File[] files = dir.listFiles();
        if (files == null) return;

        Map<String, FileBrowserAdapter.TreeNode> oldNodeMap = new HashMap<>();
        for (FileBrowserAdapter.TreeNode node : oldNodes) {
            oldNodeMap.put(node.getFile().getName(), node);
        }

        for (File file : files) {
            FileBrowserAdapter.TreeNode oldNode = oldNodeMap.get(file.getName());
            FileBrowserAdapter.TreeNode newNode = new FileBrowserAdapter.TreeNode(file,level);


            if (oldNode != null) {
                if (oldNode.getLevel()==999) newNode.setLevel(999);
                newNode.setExpanded(oldNode.isExpanded());
                newNode.setHasLoadedChildren(oldNode.hasLoadedChildren());
            }

            if (file.isDirectory()) {
                // 保留之前的子节点（如果有）
                if (oldNode != null && !oldNode.getChildren().isEmpty()) {
                    newNode.getChildren().addAll(oldNode.getChildren());
                }
            }
            newNodes.add(newNode);
        }
    }

    private List<FileBrowserAdapter.TreeNode> getVisibleNodes(List<FileBrowserAdapter.TreeNode> nodes) {
        List<FileBrowserAdapter.TreeNode> result = new ArrayList<>();
        for (FileBrowserAdapter.TreeNode node : nodes) {
            result.add(node);
            if (node.isExpanded() && node.hasLoadedChildren()) {
                result.addAll(getVisibleNodes(node.getChildren()));
            }
        }
        return result;
    }

    private void expandNode(FileBrowserAdapter.TreeNode node) {
        if (!node.hasLoadedChildren()) {
            loadChildren(node);
            node.setHasLoadedChildren(true);
        }
        node.setExpanded(true);
    }

    private void loadChildren(FileBrowserAdapter.TreeNode node) {
        File dir = node.getFile();
        List<FileBrowserAdapter.TreeNode> children = new ArrayList<>();
        File[] files = dir.listFiles();

        if (files != null) {
            for (File file : files) {
                FileBrowserAdapter.TreeNode child = new FileBrowserAdapter.TreeNode(file, node.getLevel() + 1);
                children.add(child);
            }
        }
        node.getChildren().clear();
        node.getChildren().addAll(children);
        dirModifiedTimes.put(dir, dir.lastModified());
    }

    private void collapseNode(FileBrowserAdapter.TreeNode node) {
        node.setExpanded(false);
    }

    private void startAutoRefresh() {
        refreshRunnable = new Runnable() {
            @Override
            public void run() {
                checkExpandedDirectories();
                refreshHandler.postDelayed(this, refreshFrequency * 1000L);
            }
        };
        refreshHandler.post(refreshRunnable);
    }

    private void checkExpandedDirectories() {
        // 检查当前目录是否发生变化
        Long currentDirLastModified = dirModifiedTimes.get(currentDirectory);
        long currentModified = currentDirectory.lastModified();
        if (currentDirLastModified == null || currentModified != currentDirLastModified) {
            loadDirectory(currentDirectory); // 重新加载当前目录
            dirModifiedTimes.put(currentDirectory, currentModified);
        }

        // 检查所有已展开的目录
        List<FileBrowserAdapter.TreeNode> expandedNodes = new ArrayList<>();
        collectExpandedNodes(currentNodes, expandedNodes);

        for (FileBrowserAdapter.TreeNode node : expandedNodes) {
            File dir = node.getFile();
            Long lastModified = dirModifiedTimes.get(dir);
            long currentModifiedDir = dir.lastModified();

            if (lastModified == null || currentModifiedDir != lastModified) {
                refreshNode(node);
                dirModifiedTimes.put(dir, currentModifiedDir);
            }
        }
    }

    private void collectExpandedNodes(List<FileBrowserAdapter.TreeNode> nodes,
                                      List<FileBrowserAdapter.TreeNode> result) {
        for (FileBrowserAdapter.TreeNode node : nodes) {
            if (node.isExpanded() && node.getFile().isDirectory()) {
                result.add(node);
                collectExpandedNodes(node.getChildren(), result);
            }
        }
    }

    private void refreshNode(FileBrowserAdapter.TreeNode node) {
        loadChildren(node);
        adapter.updateData(getVisibleNodes(currentNodes));
    }

    private void stopAutoRefresh() {
        refreshHandler.removeCallbacks(refreshRunnable);
    }

    private void showAccessDeniedDialog() {
        new AlertDialog.Builder(requireActivity())
                .setTitle(R.string.hint)
                .setMessage("目标目录无权限访问 输入 \"cd ~\" 回到应用目录 ")
                .setPositiveButton("确定", null)
                .show();
    }
    public static File getCurrentDirectory(){
        return currentDirectory;
    }
    public static void setCurrentDirectory(File file){
        currentDirectory = file;
    }
    @Override
    public void onDetach() {
        super.onDetach();
        Log.d("FileBrowserFragment", "onDetach()");
        stopAutoRefresh();
        dirModifiedTimes.clear();
        currentDirectory = null;
    }

}