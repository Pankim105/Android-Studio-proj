package com.example.tnote.filebrowser;

import android.os.Bundle;
import android.os.Handler;
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

/**
 * 文件浏览器Fragment，显示树形目录结构并支持展开/折叠操作
 * 使用自动刷新机制定期更新目录内容
 */
public class FileBrowserFragment extends Fragment {
    // 视图组件
    boolean showed = false;
    private RecyclerView recyclerView;
    // 适配器
    private FileBrowserAdapter adapter;
    // 所有节点的完整树结构
    private List<FileBrowserAdapter.TreeNode> allNodes = new ArrayList<>();
    //应用文件夹
    public File appDir;
    // 当前目录路径
    public File currentDirectory;
    // 自动刷新处理器
    private final Handler refreshHandler = new Handler();
    // 自动刷新任务
    private Runnable refreshRunnable;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // 初始化视图
        View view = inflater.inflate(R.layout.fragment_filebrowser, container, false);
        appDir = requireContext().getFilesDir();
        initRootNode();
        // 初始化视图组件
        initViews(view);
        // 加载目录内容
        loadDirectory(currentDirectory);
        return view;
    }

    public void initRootNode(){
        if(currentDirectory==null) currentDirectory = appDir;
    }

    public void resetRootNodeWhenClick(){
        currentDirectory = appDir;
    }
    public void setCurrentDirectory(String newPath) {
        // 校验路径有效性
        File targetDir = new File(newPath);
        if (!targetDir.isDirectory()) {
            System.out.println("Wrong path!!!!!!!!!!!!!!!!!!");
            return;
        }
        System.out.println("path change?????????????????????????????????");
        currentDirectory = new File(newPath);
        showed = false;
    }
    @Override
    public void onResume() {
        super.onResume();
        startAutoRefresh(); // 启动自动刷新
    }

    @Override
    public void onPause() {
        super.onPause();
        stopAutoRefresh(); // 停止自动刷新
    }

    /**
     * 初始化视图组件
     * @param view 根视图
     */
    private void initViews(View view) {
        recyclerView = view.findViewById(R.id.rv_file_list);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // 初始化适配器并设置点击监听
        adapter = new FileBrowserAdapter(new ArrayList<>(), node -> {
            if (node.getFile().isDirectory()) {
                if (node.getFile().listFiles() == null) return;

                // 切换展开/折叠状态
                if(node.getLevel()==999){
                    currentDirectory = node.getFile();
                }
                if (node.isExpanded()) {
                    collapseNode(node);
                } else {
                    expandNode(node);
                }
                // 更新可见节点
                adapter.updateData(getVisibleNodes(allNodes));
            }
        });

        recyclerView.setAdapter(adapter);
    }

    /**
     * 启动自动刷新（每秒刷新一次）
     */
    private void startAutoRefresh() {
        refreshRunnable = new Runnable() {
            @Override
            public void run() {
                loadDirectory(currentDirectory); // 重新加载目录
                refreshHandler.postDelayed(this, 1000); // 设置下次执行
            }
        };
        refreshHandler.postDelayed(refreshRunnable, 1000);
    }

    /**
     * 停止自动刷新
     */
    private void stopAutoRefresh() {
        refreshHandler.removeCallbacks(refreshRunnable);
    }

    /**
     * 加载指定目录内容
     * @param directory 要加载的目录
     */
    private void loadDirectory(File directory) {

        if (!directory.canRead()) {
            AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
            builder.setTitle("警告")
                    .setMessage("目标目录无权限访问！")
                    .setPositiveButton("确定", (dialog, which) -> {
                    });
            if(showed == false){
                builder.show();
                showed = true;
            }

            return;
        }
        List<FileBrowserAdapter.TreeNode> newNodes = new ArrayList<>();
        // 添加父目录节点（用于返回上级）
        newNodes.add(new FileBrowserAdapter.TreeNode(
             currentDirectory.getParentFile(), 999){});
        // 构建树结构并保留原有展开状态
        buildTreeWithOldState(directory, 0, newNodes, allNodes);
        // 当结构变化时更新数据
        if (!isStructureSame(newNodes, allNodes)) {
            allNodes = newNodes;
            adapter.updateData(getVisibleNodes(allNodes));
        }
    }

    /**
     * 递归构建树结构，保留原有展开状态
     * @param dir 当前目录
     * @param level 当前层级
     * @param newNodes 新节点列表（输出参数）
     * @param oldNodes 旧节点列表（用于状态保留）
     */
    private void buildTreeWithOldState(File dir, int level,
                                       List<FileBrowserAdapter.TreeNode> newNodes,
                                       List<FileBrowserAdapter.TreeNode> oldNodes) {

        File[] files = dir.listFiles();
        if (files == null) return;

        // 构建旧节点映射表用于快速查找
        Map<String, FileBrowserAdapter.TreeNode> oldNodeMap = new HashMap<>();
        for (FileBrowserAdapter.TreeNode oldNode : oldNodes) {
            oldNodeMap.put(oldNode.getFile().getName(), oldNode);
        }

        // 遍历目录内容
        for (File file : files) {
            String fileName = file.getName();
            // 查找对应的旧节点
            FileBrowserAdapter.TreeNode oldNode = oldNodeMap.get(fileName);
            // 保留原有展开状态
            boolean isExpanded = oldNode != null && oldNode.isExpanded();

            // 创建新节点
            FileBrowserAdapter.TreeNode newNode = new FileBrowserAdapter.TreeNode(file, level);
            newNode.setExpanded(isExpanded);
            newNodes.add(newNode);

            // 递归处理子目录
            if (file.isDirectory()) {
                List<FileBrowserAdapter.TreeNode> oldChildren =
                        (oldNode != null) ? oldNode.getChildren() : new ArrayList<>();
                buildTreeWithOldState(file, level + 1, newNode.getChildren(), oldChildren);
            }
        }
    }

    /**
     * 比较两个节点列表结构是否相同
     */
    private boolean isStructureSame(List<FileBrowserAdapter.TreeNode> list1,
                                    List<FileBrowserAdapter.TreeNode> list2) {
        if (list1.size() != list2.size()) return false;

        for (int i = 0; i < list1.size(); i++) {
            FileBrowserAdapter.TreeNode node1 = list1.get(i);
            FileBrowserAdapter.TreeNode node2 = list2.get(i);

            if (!isNodeSame(node1, node2)) return false;
        }
        return true;
    }

    /**
     * 比较两个节点是否相同（包含子结构比较）
     */
    private boolean isNodeSame(FileBrowserAdapter.TreeNode node1,
                               FileBrowserAdapter.TreeNode node2) {
        // 比较文件和层级
        if (!node1.getFile().equals(node2.getFile()) ||
                node1.getLevel() != node2.getLevel()) {
            return false;
        }

        // 如果是目录，递归比较子节点
        if (node1.getFile().isDirectory()) {
            return isStructureSame(node1.getChildren(), node2.getChildren());
        }
        return true;
    }

    /**
     * 获取所有可见节点列表（展开的节点）
     */
    private List<FileBrowserAdapter.TreeNode> getVisibleNodes(List<FileBrowserAdapter.TreeNode> nodes) {
        List<FileBrowserAdapter.TreeNode> result = new ArrayList<>();
        for (FileBrowserAdapter.TreeNode node : nodes) {
            result.add(node);
            // 递归添加展开的子节点
            if (node.isExpanded() && !node.getChildren().isEmpty()) {
                result.addAll(getVisibleNodes(node.getChildren()));
            }
        }
        return result;
    }

    /**
     * 展开指定节点
     */
    private void expandNode(FileBrowserAdapter.TreeNode node) {
        node.setExpanded(true);
        // 延迟加载子节点
        if (node.getChildren().isEmpty()) {
            File dir = node.getFile();
            buildTreeWithOldState(dir, node.getLevel() + 1, node.getChildren(), new ArrayList<>());
        }
    }

    /**
     * 折叠指定节点
     */
    private void collapseNode(FileBrowserAdapter.TreeNode node) {
        node.setExpanded(false);
    }

}