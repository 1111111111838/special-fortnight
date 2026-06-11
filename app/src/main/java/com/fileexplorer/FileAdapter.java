package com.fileexplorer;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class FileAdapter extends RecyclerView.Adapter<FileAdapter.ViewHolder> {

    public interface FileClickListener {
        void onFileClick(FileItem fileItem);
        void onFileLongClick(FileItem fileItem);
    }

    private List<FileItem> files = new ArrayList<>();
    private final Context context;
    private final FileClickListener listener;

    public FileAdapter(Context context, FileClickListener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void setFiles(List<FileItem> files) {
        this.files = files;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_file, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        FileItem item = files.get(position);
        holder.tvName.setText(item.getName());
        holder.tvInfo.setText(item.isDirectory() ? "תיקייה" : item.getFormattedSize() + "  •  " + item.getFormattedDate());
        holder.ivIcon.setImageResource(getIconForType(item.getFileType()));

        holder.itemView.setOnClickListener(v -> listener.onFileClick(item));
        holder.itemView.setOnLongClickListener(v -> {
            listener.onFileLongClick(item);
            return true;
        });
    }

    private int getIconForType(FileItem.FileType type) {
        switch (type) {
            case FOLDER:   return R.drawable.ic_folder;
            case IMAGE:    return R.drawable.ic_image;
            case VIDEO:    return R.drawable.ic_video;
            case AUDIO:    return R.drawable.ic_audio;
            case PDF:      return R.drawable.ic_pdf;
            case ARCHIVE:  return R.drawable.ic_archive;
            case APK:      return R.drawable.ic_apk;
            case TEXT:     return R.drawable.ic_text;
            case DOCUMENT: return R.drawable.ic_document;
            default:       return R.drawable.ic_file;
        }
    }

    @Override
    public int getItemCount() { return files.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivIcon;
        TextView tvName, tvInfo;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivIcon = itemView.findViewById(R.id.ivIcon);
            tvName = itemView.findViewById(R.id.tvName);
            tvInfo = itemView.findViewById(R.id.tvInfo);
        }
    }
}
