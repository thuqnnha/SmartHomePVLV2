package com.example.sh;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.videogo.openapi.bean.EZDeviceRecordFile;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class PlaybackAdapter extends RecyclerView.Adapter<PlaybackAdapter.PlaybackViewHolder> {

    public interface OnItemClickListener {
        void onClick(EZDeviceRecordFile recordFile);
    }

    private List<EZDeviceRecordFile> playbackList;
    private OnItemClickListener listener;
    private SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());

    public PlaybackAdapter(List<EZDeviceRecordFile> playbackList, OnItemClickListener listener) {
        this.playbackList = playbackList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public PlaybackViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_playback, parent, false);
        return new PlaybackViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PlaybackViewHolder holder, int position) {
        EZDeviceRecordFile recordFile = playbackList.get(position);

        // Hiển thị thời gian bắt đầu - kết thúc
        String startTime = timeFormat.format(recordFile.getStartTime().getTime());
        String endTime = timeFormat.format(recordFile.getStopTime().getTime());
        holder.txtTimeRange.setText(startTime + " - " + endTime);

        // TODO: Load thumbnail nếu có API/SDK hỗ trợ - hiện tại đặt màu đen placeholder
        holder.imgThumbnail.setImageResource(R.drawable.ic_video_placeholder);

        // Gán sự kiện click
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onClick(recordFile);
        });
    }

    @Override
    public int getItemCount() {
        return playbackList != null ? playbackList.size() : 0;
    }

    public static class PlaybackViewHolder extends RecyclerView.ViewHolder {
        ImageView imgThumbnail;
        TextView txtTimeRange;

        public PlaybackViewHolder(@NonNull View itemView) {
            super(itemView);
            imgThumbnail = itemView.findViewById(R.id.imgThumbnail);
            txtTimeRange = itemView.findViewById(R.id.txtTimeRange);
        }
    }
}


