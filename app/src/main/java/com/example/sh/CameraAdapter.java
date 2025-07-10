package com.example.sh;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.videogo.openapi.EZConstants;
import com.videogo.openapi.EZGlobalSDK;
import com.videogo.openapi.EZPlayer;

import java.util.List;

public class CameraAdapter extends RecyclerView.Adapter<CameraAdapter.CameraViewHolder> {

    private List<Device> cameraList;
    private OnCameraClickListener listener;
    public interface OnCameraClickListener {
        void onClick(Device device);
        void onLongClick(Device device, View view);
    }
    public void setOnCameraClickListener(OnCameraClickListener listener) {
        this.listener = listener;
    }
    public CameraAdapter(List<Device> cameraList) {
        this.cameraList = cameraList;
    }

    @NonNull
    @Override
    public CameraViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_camera, parent, false);
        return new CameraViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CameraViewHolder holder, int position) {
        holder.isCameraReady = false;

        Device camera = cameraList.get(position);
        holder.txtCameraName.setText(camera.getdeviceName());

        String diaChiMAC = camera.getdeviceMacId();
        String[] parts = diaChiMAC.split(" ");

        if (parts.length == 3) {
            holder.deviceSerial = parts[0];
            try {
                holder.cameraNo = Integer.parseInt(parts[1]);
            } catch (NumberFormatException e) {
                Log.e("CameraAdapter", "Lỗi chuyển cameraNo", e);
                holder.cameraNo = 1;
            }
            holder.verifyCode = parts[2];
        } else {
            Log.e("CameraAdapter", "Định dạng MAC không hợp lệ: " + diaChiMAC);
            holder.deviceSerial = null;
            holder.verifyCode = null;
            holder.cameraNo = 1;
        }

        if (holder.isSurfaceCreated) {
            holder.playLiveView();
        }

        //xu li click
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onClick(camera);
        });

        holder.itemView.setOnLongClickListener(v -> {
            if (listener != null) listener.onLongClick(camera, v);
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return cameraList.size();
    }

    public static class CameraViewHolder extends RecyclerView.ViewHolder {
        TextView txtCameraName;
        SurfaceView mSurfaceView;
        SurfaceHolder mHolder;
        EZPlayer mEZPlayer;
        boolean isSurfaceCreated = false;
        boolean isCameraReady = false;

        String deviceSerial;
        int cameraNo;
        String verifyCode;

        public CameraViewHolder(@NonNull View itemView) {
            super(itemView);
            txtCameraName = itemView.findViewById(R.id.txtCameraName);
            mSurfaceView = itemView.findViewById(R.id.surfaceViewCamera);
            mHolder = mSurfaceView.getHolder();

            mHolder.addCallback(new SurfaceHolder.Callback() {
                @Override
                public void surfaceCreated(SurfaceHolder holder) {
                    isSurfaceCreated = true;
                    playLiveView();
                }

                @Override
                public void surfaceDestroyed(SurfaceHolder holder) {
                    isSurfaceCreated = false;
                    if (mEZPlayer != null) {
                        mEZPlayer.stopRealPlay();
                        mEZPlayer.release();
                        mEZPlayer = null;
                    }
                }

                @Override
                public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {}
            });
        }
        private void playLiveView() {
            if (deviceSerial == null || verifyCode == null) {
                isCameraReady = false;
                return;
            }

            if (mEZPlayer != null) {
                mEZPlayer.stopRealPlay();
                mEZPlayer.release();
                mEZPlayer = null;
            }

            mEZPlayer = EZGlobalSDK.getInstance().createPlayer(deviceSerial, cameraNo);
            if (mEZPlayer == null) {
                isCameraReady = false;
                return;
            }

            mEZPlayer.setPlayVerifyCode(verifyCode);

            mEZPlayer.setHandler(new android.os.Handler(msg -> {
                switch (msg.what) {
                    case EZConstants.EZRealPlayConstants.MSG_REALPLAY_PLAY_FAIL:
                        Log.e("EZPlayer", "Phát camera lỗi");
                        isCameraReady = false;
                        break;
                    case EZConstants.EZRealPlayConstants.MSG_REALPLAY_PLAY_SUCCESS:
                        isCameraReady = true;
                        break;
                }
                return true;
            }));

            if (isSurfaceCreated) {
                mEZPlayer.setSurfaceHold(mHolder);
                mEZPlayer.startRealPlay();
            } else {
                isCameraReady = false;
            }
        }
    }
}
