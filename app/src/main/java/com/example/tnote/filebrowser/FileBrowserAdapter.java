package com.example.tnote.filebrowser;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.tnote.R;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * 文件浏览器适配器，管理树形结构的显示
 */
public class FileBrowserAdapter extends RecyclerView.Adapter<FileBrowserAdapter.ViewHolder> {
    // 当前可见节点列表
    private List<TreeNode> visibleNodes;
    // 点击事件监听器
    private final OnItemClickListener listener;

    /**
     * 点击事件接口
     */
    public interface OnItemClickListener {
        void onItemClick(TreeNode node);
    }

    /**
     * 树节点数据结构
     */
    public static class TreeNode {
        private final File file;      // 关联的文件对象
        private final int level;      // 节点层级（从0开始）
        private boolean expanded;     // 是否展开状态
        private final List<TreeNode> children = new ArrayList<>(); // 子节点列表

        public TreeNode(File file, int level) {
            this.file = file;
            this.level = level;
            this.expanded = false;
        }

        // Getter方法
        public File getFile() { return file; }
        public int getLevel() { return level; }
        public boolean isExpanded() { return expanded; }
        public void setExpanded(boolean expanded) { this.expanded = expanded; }
        public List<TreeNode> getChildren() { return children; }
    }

    public FileBrowserAdapter(List<TreeNode> visibleNodes, OnItemClickListener listener) {
        this.visibleNodes = visibleNodes;
        this.listener = listener;
    }

    /**
     * 更新可见数据
     */
    public void updateData(List<TreeNode> newVisibleNodes) {
        this.visibleNodes = newVisibleNodes;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // 加载列表项布局
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_file, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        TreeNode node = visibleNodes.get(position);
        File file = node.getFile();

        // 设置缩进：每级缩进50像素
        if (node.level < 20) {
            ViewGroup.MarginLayoutParams params =
                    (ViewGroup.MarginLayoutParams) holder.itemView.getLayoutParams();
            params.leftMargin = node.getLevel() * 50;
            holder.itemView.setLayoutParams(params);

            // 根据类型设置图标
            if (file.isDirectory()) {
                // 目录：显示箭头和文件夹图标
                holder.arrow.setVisibility(View.VISIBLE);
                holder.arrow.setImageResource(
                        node.isExpanded() ? R.drawable.ic_arrow_down : R.drawable.ic_arrow_right
                );
                holder.icon.setImageResource(R.drawable.ic_folder);
            } else {
                // 文件：隐藏箭头，显示文件图标
                holder.arrow.setVisibility(View.GONE);
                holder.icon.setImageResource(R.drawable.ic_file);
            }
            holder.name.setText(file.getName());
        }
        else{
            holder.arrow.setVisibility(View.GONE);
            holder.icon.setImageResource(R.drawable.ic_folder);
            holder.name.setText((node.getLevel() == 999) ? ".." : file.getName());
        }
        // 设置点击事件
        holder.itemView.setOnClickListener(v -> listener.onItemClick(node));
    }

    @Override
    public int getItemCount() {
        return visibleNodes.size();
    }

    /**
     * 列表项视图容器
     */
    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView arrow;  // 展开/折叠箭头
        ImageView icon;   // 文件/目录图标
        TextView name;    // 名称显示

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            arrow = itemView.findViewById(R.id.iv_arrow);
            icon = itemView.findViewById(R.id.icon);
            name = itemView.findViewById(R.id.name);
        }
    }
}