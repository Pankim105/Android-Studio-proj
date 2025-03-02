// FileBrowserFragment.java
package com.example.tnote.filebrowser;

import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.tnote.R;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FileBrowserFragment extends Fragment {
    private RecyclerView recyclerView;
    private FileBrowserAdapter adapter;
    private List<FileBrowserAdapter.TreeNode> currentNodes = new ArrayList<>();
    private File appDir;
    private File currentDirectory;
    private final Handler refreshHandler = new Handler();
    private Runnable refreshRunnable;
    private int refreshFrequency = 1;
    private final Map<File, Long> dirModifiedTimes = new HashMap<>(); // 记录目录修改时间

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_filebrowser, container, false);
        appDir = requireContext().getFilesDir();
        initRootNode();
        initViews(view);
        loadDirectory(currentDirectory);
        return view;
    }

    private void initRootNode() {
        if (currentDirectory == null) currentDirectory = appDir;
    }

    public void setCurrentDirectory(String newPath) {
        File targetDir = new File(newPath);
        if (!targetDir.isDirectory()) return;
        currentDirectory = targetDir;
        loadDirectory(currentDirectory);
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
        recyclerView = view.findViewById(R.id.rv_file_list);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new FileBrowserAdapter(new ArrayList<>(), node -> {
            File file = node.getFile();
            if (!file.isDirectory()) return;

            if (node.getLevel() == 999) { // 处理上级目录
                currentDirectory = file;
                loadDirectory(currentDirectory);
                return;
            }

            if (node.isExpanded()) {
                collapseNode(node);
            } else {
                expandNode(node);
            }
            adapter.updateData(getVisibleNodes(currentNodes));
        });

        recyclerView.setAdapter(adapter);
    }

    private void loadDirectory(File directory) {
        if (!directory.canRead()) {
            showAccessDeniedDialog();
            currentDirectory = appDir;
            loadDirectory(currentDirectory);
            return;
        }

        dirModifiedTimes.put(directory, directory.lastModified());

        List<FileBrowserAdapter.TreeNode> newNodes = new ArrayList<>();
        // 添加父目录节点
        if (directory.getParentFile().canRead()) {
            FileBrowserAdapter.TreeNode parentNode = new FileBrowserAdapter.TreeNode(
                    directory.getParentFile(), 999);
            parentNode.setHasLoadedChildren(true);
            parentNode.setExpanded(true);
            newNodes.add(parentNode);
        }
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
            FileBrowserAdapter.TreeNode newNode = new FileBrowserAdapter.TreeNode(file, level);

            if (oldNode != null) {
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

        // 原有逻辑：检查所有已展开的目录
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
}