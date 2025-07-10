package com.example.sh;

import static android.widget.Toast.LENGTH_LONG;
import static android.widget.Toast.LENGTH_SHORT;
import static android.widget.Toast.makeText;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.ParseException;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.provider.MediaStore;
import android.util.Log;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.card.MaterialCardView;
import com.videogo.exception.BaseException;
import com.videogo.exception.ErrorCode;
import com.videogo.openapi.EZConstants;
import com.videogo.openapi.EZGlobalSDK;
import com.videogo.openapi.EZPlayer;
import com.videogo.openapi.bean.EZDeviceRecordFile;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.transition.TransitionManager;
import androidx.transition.ChangeBounds;


public class CameraControllerActivity extends AppCompatActivity {
    private String deviceSerial;
    private int cameraNo;
    private String verifyCode;
    private int ptzSpeed = 2;
    private EZPlayer mEZPlayer;
    private String recordFilePath;
    private SurfaceView mSurfaceView;
    private ImageView imgRecord,imgAudio;
    private ImageButton btnMic;
    private MaterialCardView btnPTZControl, btnFlip, btnAudio,btnCapture,btnRecord,btnSetting;
    //--------------------------------Khai báo biến cờ------------------------------------
    private boolean isRecording = false;
    private boolean isSoundOn = false;
    private boolean isTalking = false;
    private boolean isFullScreen = false;
    private TextView tvDeviceName;
    private ImageButton btnRewind, btnPlayPause, btnForward, btnSelectDate;
    private RecyclerView recyclerPlaybackList;
    private Button btnLive;

    private final Handler mHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(@NonNull Message msg) {
            switch (msg.what) {
                case EZConstants.EZPlaybackConstants.MSG_REMOTEPLAYBACK_PLAY_SUCCUSS:
                    Log.d("EZPlayback", "Playback started successfully");
                    break;

                case EZConstants.EZPlaybackConstants.MSG_REMOTEPLAYBACK_PLAY_FAIL:
                    int errorCode = msg.arg1;
                    Log.e("EZPlayback", "Playback failed with error code: " + errorCode);

                    if (errorCode == ErrorCode.ERROR_INNER_VERIFYCODE_NEED
                            || errorCode == ErrorCode.ERROR_INNER_VERIFYCODE_ERROR) {
                        Toast.makeText(CameraControllerActivity.this,
                                "Yêu cầu mã xác minh hoặc mã sai", Toast.LENGTH_SHORT).show();
                        // Gợi ý: Hiện dialog nhập lại verifyCode, rồi gọi lại:
                        // mEZPlayer.setPlayVerifyCode("123ABC");
                        // mEZPlayer.startPlayback(recordFile);
                    }
                    break;

                case EZConstants.MSG_VIDEO_SIZE_CHANGED:
                    Log.d("EZPlayback", "Video resolution changed");
                    break;
            }
        }
    };

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_camera_controller);

        // Lấy intent từ HomeActivity
        Intent intent = getIntent();

        // Lấy các giá trị truyền qua
        String macid = intent.getStringExtra("cam_macid");

        if (macid != null) {
            String[] parts = macid.split(" ");
            if (parts.length == 3) {
                deviceSerial = parts[0];
                cameraNo = Integer.parseInt(parts[1]);
                verifyCode = parts[2];

                // Gán vào biến hoặc log để kiểm tra
                Log.d("MACID_PARSE", "Serial: " + parts[0] + ", CameraNo: " + parts[1] + ", Verify: " + parts[2]);
            } else {
                Log.e("MACID_PARSE", "Chuỗi cam_macid không hợp lệ: " + macid);
            }
        }

        //------------------------------------Ánh xạ layout--------------------------------------
        mSurfaceView = findViewById(R.id.surfaceView);
        RelativeLayout buttonPanel = findViewById(R.id.buttonPanel);
        FrameLayout cameraFrame = findViewById(R.id.cameraFrame);
        View topBar = findViewById(R.id.topBar);
        View navBar = findViewById(R.id.horizontalNav);
        View ptzPanel = findViewById(R.id.ptzPanel);
        //------------------------------------Ánh xạ nút--------------------------------------
        btnPTZControl = findViewById(R.id.btnPTZControl);
        ImageButton btnUp = findViewById(R.id.btnUp);
        ImageButton btnLeft = findViewById(R.id.btnLeft);
        ImageButton btnRight = findViewById(R.id.btnRight);
        ImageButton btnDown = findViewById(R.id.btnDown);
        btnCapture = findViewById(R.id.btnCapture);
        imgRecord = findViewById(R.id.imgRecord);
        btnRecord = findViewById(R.id.btnRecord);
        btnAudio = findViewById(R.id.btnAudio);
        imgAudio = findViewById(R.id.imgAudio);
        btnFlip = findViewById(R.id.btnFlip);
        btnSetting = findViewById(R.id.btnSetting);
        //------------------------------------Ánh xạ nút hỗ trợ--------------------------------------
        ImageButton btnSound = findViewById(R.id.btnSound);
        ImageButton btnPTZ = findViewById(R.id.btnPTZ);
        btnMic = findViewById(R.id.btnMic);
        ImageButton btnFullScreen = findViewById(R.id.btnFullScreen);
        //
        RelativeLayout ptzPad = findViewById(R.id.ptzPad);
        ImageView ptzDot = ptzPad.findViewById(R.id.ptzDot);
        ImageView ptzPadUp = ptzPad.findViewById(R.id.btnUpp);
        ImageView ptzPadDown = ptzPad.findViewById(R.id.btnDownn);
        ImageView ptzPadRight = ptzPad.findViewById(R.id.btnRightt);
        ImageView ptzPadLeft = ptzPad.findViewById(R.id.btnLeftt);
        btnLive = findViewById(R.id.btnLive);
        tvDeviceName = findViewById(R.id.tvDeviceName);
        btnSelectDate = findViewById(R.id.btnSelectDate);

        recyclerPlaybackList = findViewById(R.id.recyclerPlaybackList);
        recyclerPlaybackList.setLayoutManager(new LinearLayoutManager(this));
        //---------------------------------Cấp quyền--------------------------------------------
        requestPermission();

        mSurfaceView.setDrawingCacheEnabled(true);

        // Cài đặt callback cho SurfaceView
        mSurfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                loadCamera(holder);  // <-- GỌI TẠI ĐÂY KHI Surface SẴN SÀNG
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {}

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                if (mEZPlayer != null) {
                    mEZPlayer.stopRealPlay();
                    mEZPlayer.release();
                    mEZPlayer = null;
                }
            }
        });

        String today = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date());
        loadPlaybackListByDate(today);

//        loadPlaybackList();
        mSurfaceView.setOnClickListener(v -> {
            if (buttonPanel.getVisibility() == View.GONE) {
                buttonPanel.setVisibility(View.VISIBLE);
            } else {
                buttonPanel.setVisibility(View.GONE); // nếu muốn ấn lần 2 để ẩn lại
            }
        });
        //---------------------------------------------Nút điều hướng-------------------------------------------------
        btnPTZControl.setOnClickListener(v -> {
            if (ptzPanel.getVisibility() == View.GONE) {
                ptzPanel.setVisibility(View.VISIBLE);
                recyclerPlaybackList.setVisibility(View.GONE);
            } else {
                ptzPanel.setVisibility(View.GONE);
                recyclerPlaybackList.setVisibility(View.VISIBLE);
            }
        });
        // TouchListener cho nút LÊN
        btnUp.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    v.setPressed(true); // Kích hoạt hiệu ứng nhấn
                    handlePtzMovement(event, EZConstants.EZPTZCommand.EZPTZCommandUp);
                    break;

                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    v.setPressed(false); // Trả lại trạng thái
                    handlePtzMovement(event, EZConstants.EZPTZCommand.EZPTZCommandUp);
                    break;
            }
            return true; // vẫn trả về true để xử lý PTZ
        });
        // TouchListener cho nút XUỐNG
        btnDown.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    v.setPressed(true); // Kích hoạt hiệu ứng nhấn
                    handlePtzMovement(event, EZConstants.EZPTZCommand.EZPTZCommandDown);
                    break;

                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    v.setPressed(false); // Trả lại trạng thái
                    handlePtzMovement(event, EZConstants.EZPTZCommand.EZPTZCommandDown);
                    break;
            }
            return true; // vẫn trả về true để xử lý PTZ
        });
        // TouchListener cho nút TRÁI
        btnLeft.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    v.setPressed(true); // Kích hoạt hiệu ứng nhấn
                    handlePtzMovement(event, EZConstants.EZPTZCommand.EZPTZCommandLeft);
                    break;

                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    v.setPressed(false); // Trả lại trạng thái
                    handlePtzMovement(event, EZConstants.EZPTZCommand.EZPTZCommandLeft);
                    break;
            }
            return true; // vẫn trả về true để xử lý PTZ
        });
        // TouchListener cho nút PHẢI
        btnRight.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    v.setPressed(true); // Kích hoạt hiệu ứng nhấn
                    handlePtzMovement(event, EZConstants.EZPTZCommand.EZPTZCommandRight);
                    break;

                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    v.setPressed(false); // Trả lại trạng thái
                    handlePtzMovement(event, EZConstants.EZPTZCommand.EZPTZCommandRight);
                    break;
            }
            return true; // vẫn trả về true để xử lý PTZ
        });
        //---------------------------------------------Nút chức năng-------------------------------------------------
        btnCapture.setOnClickListener(v -> {
            capManHinh();
        });
        btnRecord.setOnClickListener(v -> {
            quayManHinh();
        });
        btnAudio.setOnClickListener(v -> {
            amThanhHaiChieu();
        });
        btnFlip.setOnClickListener(v -> {
            latAnh();
        });
        //----------------------------------------------------------------------------
        ptzPadUp.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    v.setPressed(true); // Kích hoạt hiệu ứng nhấn
                    // Animate ptzDot lên gần nút Up
                    ptzDot.animate()
                            .translationYBy(-100f) // đi lên khoảng 40px
                            .scaleX(1.2f).scaleY(1.2f)
                            .alpha(1f)
                            .setDuration(150)
                            .start();
                    handlePtzMovement(event, EZConstants.EZPTZCommand.EZPTZCommandUp);
                    break;

                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    v.setPressed(false); // Trả lại trạng thái
                    // Trả ptzDot về giữa
                    ptzDot.animate()
                            .translationX(0f)
                            .translationY(0f)
                            .scaleX(1f)
                            .scaleY(1f)
                            .alpha(0.3f)
                            .setDuration(150)
                            .start();
                    handlePtzMovement(event, EZConstants.EZPTZCommand.EZPTZCommandUp);
                    break;
            }
            return true; // vẫn trả về true để xử lý PTZ
        });
        ptzPadDown.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    v.setPressed(true); // Kích hoạt hiệu ứng nhấn
                    ptzDot.animate()
                            .translationYBy(100f) // đi lên khoảng 40px
                            .scaleX(1.2f)
                            .scaleY(1.2f)
                            .alpha(1f)
                            .setDuration(150)
                            .start();
                    handlePtzMovement(event, EZConstants.EZPTZCommand.EZPTZCommandDown);
                    break;

                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    v.setPressed(false); // Trả lại trạng thái
                    ptzDot.animate()
                            .translationX(0f)
                            .translationY(0f)
                            .scaleX(1f)
                            .scaleY(1f)
                            .alpha(0.3f)
                            .setDuration(150)
                            .start();
                    handlePtzMovement(event, EZConstants.EZPTZCommand.EZPTZCommandDown);
                    break;
            }
            return true; // vẫn trả về true để xử lý PTZ
        });
        ptzPadLeft.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    v.setPressed(true); // Kích hoạt hiệu ứng nhấn
                    ptzDot.animate()
                            .translationXBy(-100f)
                            .scaleX(1.2f).scaleY(1.2f)
                            .alpha(1f)
                            .setDuration(150)
                            .start();
                    handlePtzMovement(event, EZConstants.EZPTZCommand.EZPTZCommandLeft);
                    break;

                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    v.setPressed(false); // Trả lại trạng thái
                    ptzDot.animate()
                            .translationX(0f)
                            .translationY(0f)
                            .scaleX(1f)
                            .scaleY(1f)
                            .alpha(0.3f)
                            .setDuration(150)
                            .start();
                    handlePtzMovement(event, EZConstants.EZPTZCommand.EZPTZCommandLeft);
                    break;
            }
            return true; // vẫn trả về true để xử lý PTZ
        });
        ptzPadRight.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    v.setPressed(true); // Kích hoạt hiệu ứng nhấn
                    ptzDot.animate()
                            .translationXBy(100f)
                            .scaleX(1.2f).scaleY(1.2f)
                            .alpha(1f)
                            .setDuration(150)
                            .start();
                    handlePtzMovement(event, EZConstants.EZPTZCommand.EZPTZCommandRight);
                    break;

                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    v.setPressed(false); // Trả lại trạng thái
                    ptzDot.animate()
                            .translationX(0f)
                            .translationY(0f)
                            .scaleX(1f)
                            .scaleY(1f)
                            .alpha(0.3f)
                            .setDuration(150)
                            .start();
                    handlePtzMovement(event, EZConstants.EZPTZCommand.EZPTZCommandRight);
                    break;
            }
            return true; // vẫn trả về true để xử lý PTZ
        });
        //-------------------------------Xu li nut ho tro-------------------------------
        btnSound.setOnClickListener(v -> {
            try {
                if (!isSoundOn) {
                    mEZPlayer.openSound();
                    isSoundOn = true;
                    btnSound.setImageResource(R.drawable.ic_sound_on);
                    makeText(this, "Âm thanh đã bật", LENGTH_SHORT).show();
                } else {
                    mEZPlayer.closeSound();
                    isSoundOn = false;
                    btnSound.setImageResource(R.drawable.ic_sound_off);
                    makeText(this, "Âm thanh đã tắt", LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                e.printStackTrace(); // Ghi log khi debug
                makeText(this, "Lỗi xử lý âm thanh: " + e.getMessage(), LENGTH_LONG).show();
            }
        });
        btnPTZ.setOnClickListener(v -> {
            if (!isFullScreen) {
                // Chế độ bình thường: bật/tắt ptzPanel
                if (ptzPanel.getVisibility() == View.GONE) {
                    ptzPanel.setVisibility(View.VISIBLE);
                    recyclerPlaybackList.setVisibility(View.GONE);
                    ptzPad.setAlpha(0f);
                    ptzPad.animate().alpha(1f).setDuration(300).start();
                } else {
                    ptzPad.animate().alpha(0f).setDuration(200).withEndAction(() -> {
                        ptzPad.setVisibility(View.GONE);
                        ptzPanel.setVisibility(View.GONE);
                        recyclerPlaybackList.setVisibility(View.VISIBLE);
                    }).start();
                }
            } else {
                if (ptzPad.getVisibility() == View.GONE) {
                    ptzPad.setVisibility(View.VISIBLE);
                } else {
                    ptzPad.setVisibility(View.GONE);
                }
            }
        });

        btnMic.setOnClickListener(v -> {
            amThanhHaiChieu();
        });

        btnFullScreen.setOnClickListener(v -> {
            ptzPad.setVisibility(View.GONE);
            TransitionManager.beginDelayedTransition((ViewGroup) cameraFrame.getParent(), new ChangeBounds());

            if (!isFullScreen) {
                isFullScreen = true;
                // Ẩn UI
                topBar.setVisibility(View.GONE);
                navBar.setVisibility(View.GONE);
                ptzPanel.setVisibility(View.GONE);
                buttonPanel.setVisibility(View.GONE);
                recyclerPlaybackList.setVisibility(View.GONE);

                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                hideSystemUI();

                // Thay đổi chiều cao thành MATCH_PARENT
                ViewGroup.LayoutParams params = cameraFrame.getLayoutParams();
                params.height = ViewGroup.LayoutParams.MATCH_PARENT;
                cameraFrame.setLayoutParams(params);

                // Set padding = 0dp
                cameraFrame.setPadding(0, 0, 0, 0);

                btnFullScreen.setImageResource(R.drawable.ic_fullscreen_exit);
            } else {
                isFullScreen = false;

                // Hiện lại UI
                topBar.setVisibility(View.VISIBLE);
                navBar.setVisibility(View.VISIBLE);
                buttonPanel.setVisibility(View.VISIBLE);
                recyclerPlaybackList.setVisibility(View.VISIBLE);


                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                showSystemUI();

                // Trả lại chiều cao ban đầu là 250dp
                ViewGroup.LayoutParams params = cameraFrame.getLayoutParams();
                params.height = (int) TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP, 250,
                        getResources().getDisplayMetrics()
                );
                cameraFrame.setLayoutParams(params);

                // Set padding = 8dp
                int paddingInPx = (int) TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP, 8,
                        getResources().getDisplayMetrics()
                );
                cameraFrame.setPadding(paddingInPx, paddingInPx, paddingInPx, paddingInPx);

                btnFullScreen.setImageResource(R.drawable.ic_fullscreen);
            }
        });
        //nút xem trực tiếp
        btnLive.setOnClickListener(v -> {
            if (mEZPlayer != null) {
                mEZPlayer.stopPlayback();
                mEZPlayer.release();
                mEZPlayer = null;
            }

            loadCamera(mSurfaceView.getHolder());

            loadPlaybackListByDate(today);

            btnLive.setVisibility(View.GONE);
            btnFlip.setVisibility(View.VISIBLE);
            btnAudio.setVisibility(View.VISIBLE);
            btnPTZControl.setVisibility(View.VISIBLE);
        });

        btnSelectDate.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            DatePickerDialog datePickerDialog = new DatePickerDialog(
                    this,
                    (view, year, month, dayOfMonth) -> {
                        String selectedDate = String.format("%02d/%02d/%04d", dayOfMonth, month + 1, year);

                        // Gọi load danh sách phát lại
                        loadPlaybackListByDate(selectedDate);
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
            );
            datePickerDialog.show();
        });


    }
    private void loadPlaybackListByDate(String selectedDateStr) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        Calendar startTime = Calendar.getInstance();
        Calendar endTime = Calendar.getInstance();

        try {
            Date date = sdf.parse(selectedDateStr);
            if (date == null) return;

            startTime.setTime(date);
            startTime.set(Calendar.HOUR_OF_DAY, 0);
            startTime.set(Calendar.MINUTE, 0);
            startTime.set(Calendar.SECOND, 0);

            endTime.setTime(date);
            endTime.set(Calendar.HOUR_OF_DAY, 23);
            endTime.set(Calendar.MINUTE, 59);
            endTime.set(Calendar.SECOND, 59);
        } catch (ParseException e) {
            e.printStackTrace();
            return;
        } catch (java.text.ParseException e) {
            throw new RuntimeException(e);
        }

        new Thread(() -> {
            try {
                List<EZDeviceRecordFile> recordList = EZGlobalSDK.getInstance()
                        .searchRecordFileFromDevice(deviceSerial, cameraNo, startTime, endTime);

                runOnUiThread(() -> {
                    recyclerPlaybackList.setAdapter(new PlaybackAdapter(recordList, recordFile -> {
                        playPlayback(recordFile); // <- gọi phát lại
                        btnFlip.setVisibility(View.GONE);
                        btnAudio.setVisibility(View.GONE);
                        btnPTZControl.setVisibility(View.GONE);
                    }));
                    recyclerPlaybackList.setVisibility(View.VISIBLE);

                    // Cập nhật TextView thời gian
                    TextView tvDate = findViewById(R.id.tvSelectedDate);
                    tvDate.setText(selectedDateStr);
                });

            } catch (BaseException e) {
                e.printStackTrace();
            }
        }).start();
    }

//    private void loadPlaybackList(){
//        Calendar startTime = Calendar.getInstance();
//        startTime.set(Calendar.HOUR_OF_DAY, 0);
//        startTime.set(Calendar.MINUTE, 0);
//        startTime.set(Calendar.SECOND, 0);
//
//        Calendar endTime = Calendar.getInstance();
//
//        new Thread(() -> {
//            try {
//                List<EZDeviceRecordFile> recordList = EZGlobalSDK.getInstance()
//                        .searchRecordFileFromDevice(deviceSerial, cameraNo, startTime, endTime);
//
//                runOnUiThread(() -> {
//                    recyclerPlaybackList.setAdapter(new PlaybackAdapter(recordList, recordFile -> {
//                        playPlayback(recordFile); // <- gọi phát lại
//                    }));
//                    recyclerPlaybackList.setVisibility(View.VISIBLE); // hoặc bật khi người dùng ấn nút "Playback"
//                });
//
//            } catch (BaseException e) {
//                e.printStackTrace();
//            }
//        }).start();
//
//    }
    private void playPlayback(EZDeviceRecordFile recordFile) {
        if (mEZPlayer != null) {
            mEZPlayer.stopRealPlay();
            mEZPlayer.stopPlayback(); // <-- thêm dòng này
            mEZPlayer.release();
            mEZPlayer = null;
        }

        mEZPlayer = EZGlobalSDK.getInstance().createPlayer(deviceSerial, cameraNo);
        mEZPlayer.setSurfaceHold(mSurfaceView.getHolder());
        mEZPlayer.setPlayVerifyCode(verifyCode);
        mEZPlayer.setHandler(mHandler);

        boolean started = mEZPlayer.startPlayback(recordFile);
        if (started) {
            Log.d("Playback", "Playback started");
            runOnUiThread(() -> {
                recyclerPlaybackList.setVisibility(View.GONE);
                btnLive.setVisibility(View.VISIBLE);
            });
        } else {
            Log.e("Playback", "Playback failed to start");
        }
    }

    private void hideSystemUI() {
        WindowInsetsController insetsController = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            insetsController = getWindow().getInsetsController();
        }
        if (insetsController != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                insetsController.hide(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                insetsController.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            getWindow().setDecorFitsSystemWindows(false);
        }
    }

    private void showSystemUI() {
        WindowInsetsController insetsController = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            insetsController = getWindow().getInsetsController();
        }
        if (insetsController != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                insetsController.show(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            getWindow().setDecorFitsSystemWindows(true);
        }
    }

    private void loadCamera(SurfaceHolder holder) {
        mEZPlayer = EZGlobalSDK.getInstance().createPlayer(deviceSerial, cameraNo);
        if (mEZPlayer != null) {
            mEZPlayer.setPlayVerifyCode(verifyCode);
            mEZPlayer.setSurfaceHold(holder);
            mEZPlayer.startRealPlay();
            Log.d("CameraDebug", "Camera started");
        } else {
            Log.e("CameraDebug", "mEZPlayer is null");
        }
    }

    private void handlePtzMovement(MotionEvent event, EZConstants.EZPTZCommand command) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                // Bắt đầu di chuyển khi nhấn nút
                controlPTZ(command, EZConstants.EZPTZAction.EZPTZActionSTART);
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                // Dừng di chuyển khi thả nút
                controlPTZ(command, EZConstants.EZPTZAction.EZPTZActionSTOP);
                break;
        }
    }
    private void controlPTZ(EZConstants.EZPTZCommand command, EZConstants.EZPTZAction action) {
        new Thread(() -> {
            try {
                boolean result = EZGlobalSDK.getInstance().controlPTZ(
                        deviceSerial,
                        1,
                        command,
                        action,
                        ptzSpeed
                );

                Log.d("PTZ_CONTROL", "Command: " + command + " | Action: " + action + " | Speed: " + ptzSpeed + " | Result: " + result);

            } catch (Exception e) {
                e.printStackTrace();

                this.runOnUiThread(() -> {
                    String message = "Lỗi điều khiển: " + e.getMessage();

                    if (e instanceof BaseException) {
                        String msg = e.getMessage(); // ví dụ: "The PTZ rotation reaches the left limit"

                        if (msg != null) {
                            if (msg.contains("left limit")) {
                                message = "Đã đạt giới hạn quay trái";
                            } else if (msg.contains("right limit")) {
                                message = "Đã đạt giới hạn quay phải";
                            } else if (msg.contains("upper-limit") || msg.contains("up limit")) {
                                message = "Đã đạt giới hạn quay lên";
                            } else if (msg.contains("bottom limit") || msg.contains("down limit")) {
                                message = "Đã đạt giới hạn quay xuống";
                            }
                        }
                    }

                    makeText(this, message, LENGTH_SHORT).show();
                });
            }
        }).start();
    }
    private void capManHinh()
    {
        Bitmap capturedBitmap = mEZPlayer.capturePicture();
        if (capturedBitmap != null) {
            saveBitmapToGallery(capturedBitmap);
        } else {
            makeText(this, "Chụp màn hình thất bại", LENGTH_SHORT).show();
        }
    }
    private void saveBitmapToGallery(Bitmap bitmap) {
        ContentValues values = new ContentValues();
        String fileName = "SCREENSHOT_" + System.currentTimeMillis() + ".jpg";

        values.put(MediaStore.Images.Media.DISPLAY_NAME, fileName);
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");

        // Xử lý đường dẫn theo version Android
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES+"/EZVIZ");
            values.put(MediaStore.Images.Media.IS_PENDING, 1);
        } else {
            values.put(MediaStore.Images.Media.DATA,
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES+"/EZVIZ") + fileName);
        }

        ContentResolver resolver = this.getContentResolver();
        Uri uri = null;

        try {
            uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

            if (uri == null) {
                throw new IOException("Failed to create MediaStore entry");
            }

            try (OutputStream outputStream = resolver.openOutputStream(uri)) {
                if (outputStream == null) {
                    throw new IOException("Failed to open output stream");
                }

                if (!bitmap.compress(Bitmap.CompressFormat.JPEG, 95, outputStream)) {
                    throw new IOException("Failed to compress bitmap");
                }
            }

            // Cập nhật trạng thái cho Android 10+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear();
                values.put(MediaStore.Images.Media.IS_PENDING, 0);
                resolver.update(uri, values, null, null);
            }

            // Thông báo thành công
            makeText(this, "Ảnh đã lưu vào thư viện", LENGTH_SHORT).show();

        } catch (IOException e) {
            e.printStackTrace();

            // Xử lý lỗi chi tiết
            if (uri != null) {
                resolver.delete(uri, null, null);
            }
            makeText(this, "Lỗi khi lưu ảnh: " + e.getMessage(), LENGTH_LONG).show();
        } finally {
            bitmap.recycle(); // Giải phóng bộ nhớ
        }
    }
    private void quayManHinh()
    {
        if (!isRecording) {
            imgRecord.setImageResource(R.drawable.ic_videocam);
            // Tạo thư mục lưu file video
            String folderPath = Environment.getExternalStorageDirectory().getPath() + "/EzvizVideos";
            File folder = new File(folderPath);
            if (!folder.exists()) {
                folder.mkdirs();
            }

            recordFilePath = folderPath + "/record_" + System.currentTimeMillis() + ".mp4";

            boolean result = mEZPlayer.startLocalRecordWithFile(recordFilePath);
            if (result) {
                isRecording = true;
                makeText(this, "Đang quay video...", LENGTH_SHORT).show();
            } else {
                makeText(this, "Không thể bắt đầu quay video.", LENGTH_SHORT).show();
            }
        } else {
            mEZPlayer.stopLocalRecord();
            isRecording = false;
            imgRecord.setImageResource(R.drawable.ic_videocam_off);
            makeText(this, "Video đã lưu tại:\n" + recordFilePath, LENGTH_LONG).show();
        }
    }
    private void amThanhHaiChieu()
    {
        if (!isTalking) {
            if (mEZPlayer.startVoiceTalk()) {
                mEZPlayer.setVoiceTalkStatus(true);
                isTalking = true;
                btnMic.setImageResource(R.drawable.ic_mic_on);
                imgAudio.setImageResource(R.drawable.ic_mic_on);
            } else {
                makeText(this, "Không thể bắt đầu voice talk", LENGTH_SHORT).show();
            }
        } else {
            mEZPlayer.stopVoiceTalk();
            isTalking = false;
            btnMic.setImageResource(R.drawable.ic_mic_off);
            imgAudio.setImageResource(R.drawable.ic_mic_off);
        }
    }
    private void latAnh()
    {
        try {
            EZGlobalSDK.getInstance().controlVideoFlip(deviceSerial, cameraNo, EZConstants.EZPTZDisplayCommand.EZPTZDisplayCommandFlip);
        } catch (Exception e) {
            e.printStackTrace();
            makeText(this, "Lỗi khi lật hình ảnh: " + e.getMessage(), LENGTH_SHORT).show();
        }
    }
    @Override
    public void onResume() {
        super.onResume();
//        loadCamera();
    }
    @Override
    public void onPause() {
        super.onPause();
        if (mEZPlayer != null) {
            mEZPlayer.stopRealPlay();
            mEZPlayer.release();
            mEZPlayer = null;
        }
    }

    private void requestPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(new String[]{Manifest.permission.READ_MEDIA_IMAGES}, 100);
        } else {
            requestPermissions(new String[]{
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE
            }, 100);
        }
        //
        if (this.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1001);
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.RECORD_AUDIO},
                    1);
        }

    }
}