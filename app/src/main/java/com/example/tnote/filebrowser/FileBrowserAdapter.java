// FileBrowserAdapter.java
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

public class FileBrowserAdapter extends RecyclerView.Adapter<FileBrowserAdapter.ViewHolder> {
    private List<TreeNode> visibleNodes;
    private int nodesLen;
    private final OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(TreeNode node);
    }

    public static class TreeNode {
        private final File file;
        private final int level;
        private boolean expanded;
        private boolean hasLoadedChildren; // 新增加载状态标记
        private final List<TreeNode> children = new ArrayList<>();

        public TreeNode(File file, int level) {
            this.file = file;
            this.level = level;
        }

        public File getFile() { return file; }
        public int getLevel() { return level; }
        public boolean isExpanded() { return expanded; }
        public void setExpanded(boolean expanded) { this.expanded = expanded; }
        public List<TreeNode> getChildren() { return children; }
        public boolean hasLoadedChildren() { return hasLoadedChildren; } // 新增
        public void setHasLoadedChildren(boolean loaded) { this.hasLoadedChildren = loaded; } // 新增
    }

    public FileBrowserAdapter(List<TreeNode> visibleNodes, OnItemClickListener listener) {
        this.visibleNodes = visibleNodes;
        this.listener = listener;
    }

    public void updateData(List<TreeNode> newVisibleNodes) {
        this.visibleNodes = newVisibleNodes;
        this.nodesLen = newVisibleNodes.size();
        notifyDataSetChanged();
    }

    public int getNodesLen() { return this.nodesLen; }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_file, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        TreeNode node = visibleNodes.get(position);
        File file = node.getFile();

        if (node.level < 20) {
            ViewGroup.MarginLayoutParams params =
                    (ViewGroup.MarginLayoutParams) holder.itemView.getLayoutParams();
            params.leftMargin = node.getLevel() * 50;
            holder.itemView.setLayoutParams(params);

            if (file.isDirectory()) {
                holder.arrow.setVisibility(View.VISIBLE);
                holder.arrow.setImageResource(
                        node.isExpanded() ? R.drawable.ic_arrow_down : R.drawable.ic_arrow_right
                );
                holder.icon.setImageResource(R.drawable.ic_folder);
            } else {
                holder.arrow.setVisibility(View.GONE);
                holder.icon.setImageResource(R.drawable.ic_file);
            }
            holder.name.setText(file.getName());
        } else {
            holder.arrow.setVisibility(View.GONE);
            holder.icon.setImageResource(R.drawable.ic_folder);
            holder.name.setText((node.getLevel() == 999) ? ".." : file.getName());
        }
        holder.itemView.setOnClickListener(v -> listener.onItemClick(node));
    }

    @Override
    public int getItemCount() { return visibleNodes.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView arrow, icon;
        TextView name;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            arrow = itemView.findViewById(R.id.iv_arrow);
            icon = itemView.findViewById(R.id.icon);
            name = itemView.findViewById(R.id.name);
        }
    }
}