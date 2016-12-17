package com.sky.video.baiduvideo;

import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.os.Process;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.baidu.cyberplayer.core.BVideoView;
import com.sky.video.baiduvideo.utils.DensityUtils;
import com.sky.video.baiduvideo.utils.TimeUtils;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

import static com.sky.video.baiduvideo.utils.TimeUtils.formatSecond;

public class MainActivity extends AppCompatActivity implements
        BVideoView.OnPreparedListener,
        BVideoView.OnCompletionListener,
        BVideoView.OnErrorListener,
        BVideoView.OnPositionUpdateListener,
        BVideoView.OnTotalCacheUpdateListener,
        SeekBar.OnSeekBarChangeListener {
    @BindView(R.id.videoview)
    BVideoView bVideoView;
    @BindView(R.id.btn_play)
    ImageView btn_play; //控制播放暂停继续
    @BindView(R.id.btn_resolution)
    Button btn_resolution;  //调整分辨率
    @BindView(R.id.seekBar)
    SeekBar seekBar;//进度条
    @BindView(R.id.btn_fullscreen)
    ImageView btn_fullScreen;//设置全屏
    @BindView(R.id.tvCurrentDuration)
    TextView tvCurrentDuration;
    @BindView(R.id.tvMaxDuration)
    TextView tvMaxDuration;
    @BindView(R.id.button)
    Button btn_playOther;

    /**
     * 记录播放位置
     */
    private int mLastPos = 0;

    /**
     * 播放状态
     */
    public enum PlayerStatus {
        PLAYER_IDLE, PLAYER_PREPARING, PLAYER_PREPARED, PLAYER_COMPLETED
    }

    private PlayerStatus mPlayerStatus = PlayerStatus.PLAYER_IDLE;

    boolean isPrepared = false;
    private volatile boolean isFullScreen = false;
    private volatile boolean isReadyForQuit = true;
    private final Object syncPlaying = new Object();
    boolean IsDragging = false;//是否是用户手动拖拽进度条
    //直播最大值不确定，所以使用布尔值做判断
    private boolean isMaxSetted = false;
    private HandlerThread mHandlerThread;
    ProgressHandler progressHandler;
    //进度条的当前位置
    private long currentPositionInSeconds = 0L;


    private String path = "http://121.46.19.90/?new=/36/112/sc1OQIuZQmyZDF8uSz263F.mp4&key=CWdSmRODAhAieshPNQWXmMVcZ61rtxmM&prod=ugc&pg=0&cateCode=128113&vid=84310632&ch=my&plat=6&mkey=s2dparmoDY1yTFCXOb-2_Gst9rjORbGq";
    class ProgressHandler extends Handler {

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:

                    /**
                     * 如果已经播放了，等待上一次播放结束
                     */
                    if (mPlayerStatus == PlayerStatus.PLAYER_PREPARING
                            || mPlayerStatus == PlayerStatus.PLAYER_PREPARED) {
                        synchronized (syncPlaying) {
                            try {

                                syncPlaying.wait(2 * 1000);
                            } catch (InterruptedException e) {
                                // TODO Auto-generated catch block
                                e.printStackTrace();
                            }
                        }
                    }

                    /**
                     * 设置播放url
                     */
                    bVideoView.setVideoPath((String)msg.obj);


                    /**
                     * 续播，如果需要如此
                     */
                    if (mLastPos > 0) {

                        bVideoView.seekTo(mLastPos);
                        mLastPos = 0;
                    }

                    /**
                     * 显示或者隐藏缓冲提示
                     */
                    bVideoView.showCacheInfo(true);
                    /**
                     * 开始播放
                     */
                    // mVV.start();

                    /**
                     * 已经开启播放，必须有结束消息后才能结束
                     */
                    isReadyForQuit = false;

                    changeStatus(PlayerStatus.PLAYER_PREPARING);
                    break;
                default:
                    break;
            }
        }
    }
    public boolean getIsDragging() {
        return IsDragging;
    }

    private void updateDuration(int second) {
        if (tvMaxDuration != null) {
            tvMaxDuration.setText(formatSecond(second));
        }
    }

    public void setMax(int maxProgress) {
        if (isMaxSetted) {
            return;
        }
        if (seekBar != null) {
            seekBar.setMax(maxProgress);
        }
        updateDuration(maxProgress);
        if (maxProgress > 0) {
            isMaxSetted = true;
        }
    }

    //每200ms调用一次(百度云直播已经写好)
    @Override
    public boolean onPositionUpdate(long newPositionIiSeconds) {

        long newPositionInSeconds = newPositionIiSeconds / 1000;
        long previousPosition = currentPositionInSeconds;
        if (newPositionInSeconds > 0 && !getIsDragging()) {
            currentPositionInSeconds = newPositionInSeconds;
        }
        if (!getIsDragging()) {
            int duration = bVideoView.getDuration();
            if (duration > 0) {
                this.setMax(duration);
                // 直播视频的duration为0，此时不设置进度
                if (newPositionInSeconds > 0 && previousPosition != newPositionInSeconds) {
                    if (tvCurrentDuration != null) {
                        Log.e("====", "==tvCurrentDuration====");
                        seekBar.setProgress((int) newPositionInSeconds);

                    }

                }
            }
        }
        return false;
    }

    @Override
    public void onTotalCacheUpdate(final long l) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                int cache = (int) l + 10;
                if (seekBar != null && cache != seekBar.getSecondaryProgress()) {
                    seekBar.setSecondaryProgress(cache);
                }
            }
        });
    }

    @Override
    public void onCompletion() {
        synchronized (syncPlaying) {
            isReadyForQuit = true;
            syncPlaying.notifyAll();
        }
        changeStatus(PlayerStatus.PLAYER_COMPLETED);
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        seekBar.setMax(0);
        seekBar.setOnSeekBarChangeListener(this);
        setVideoView();

        /**
         * 开启后台事件处理线程
         */
        mHandlerThread = new HandlerThread("event handler thread", Process.THREAD_PRIORITY_BACKGROUND);
        mHandlerThread.start();
        progressHandler =new ProgressHandler();


    }


    @OnClick(R.id.button)
    public void playOther(View view){
        path = "http://gkkskijidms30qudc3v.exp.bcevod.com/mda-gkks7fejzyj89qkf/mda-gkks7fejzyj89qkf.m3u8";
        tryToPlayOther(path);

    }

    //配置视频播放器
    private void setVideoView() {
        bVideoView.setAK(Constants.ACESS_KEY);
        //设置视频的缩放模式
        bVideoView.setVideoScalingMode(BVideoView.VIDEO_SCALING_MODE_SCALE_TO_FIT);
        /**
         * 注册listener
         */
        bVideoView.setOnPreparedListener(this);
        bVideoView.setOnCompletionListener(this);
        bVideoView.setOnErrorListener(this);
        bVideoView.setOnPositionUpdateListener(this);
        bVideoView.setOnTotalCacheUpdateListener(this);

        /**
         * 设置解码模式 为保证兼容性，当前仅支持软解
         */
        bVideoView.setDecodeMode(BVideoView.DECODE_SW);
        bVideoView.selectResolutionType(BVideoView.RESOLUTION_TYPE_AUTO);

        bVideoView.setVideoPath(path);
        bVideoView.start();
        btn_play.setImageResource(R.mipmap.item_pause);
    }

    @OnClick(R.id.btn_play)
    public void videoPlay(View view) {
        if (bVideoView == null) {
            return;
        } else {
            if (!isPrepared) {
                btn_play.setImageResource(R.mipmap.item_continue);
                bVideoView.start();
            } else {
                if (bVideoView.isPlaying()) {
                    btn_play.setImageResource(R.mipmap.item_continue);
                    bVideoView.pause();
                } else {
                    btn_play.setImageResource(R.mipmap.item_pause);
                    bVideoView.resume();
                }
            }
        }
    }

    @OnClick(R.id.btn_fullscreen)
    public void fullScreen(View view) {
        if (isFullScreen) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            FullScreenUtils.toggleHideyBar(MainActivity.this);
            isFullScreen = false;
            bVideoView.setLayoutParams(new RelativeLayout.LayoutParams(
                    RelativeLayout.LayoutParams.MATCH_PARENT,
                    DensityUtils.dip2px(this, 200)
            ));
            btn_fullScreen.setImageResource(R.mipmap.btn_to_fullscreen);
        } else {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            FullScreenUtils.toggleHideyBar(MainActivity.this);
            bVideoView.setLayoutParams(new RelativeLayout.LayoutParams(
                    RelativeLayout.LayoutParams.MATCH_PARENT,
                    RelativeLayout.LayoutParams.MATCH_PARENT
            ));
            isFullScreen = true;
            btn_fullScreen.setImageResource(R.mipmap.btn_to_mini);

        }
    }

    @Override
    public boolean onError(int i, int i1) {
        synchronized (syncPlaying) {
            isReadyForQuit = true;
            syncPlaying.notifyAll();
        }
        changeStatus(PlayerStatus.PLAYER_IDLE);
        return true;
    }

    @Override
    public void onPrepared() {
        isPrepared = true;
        changeStatus(PlayerStatus.PLAYER_PREPARED);
    }

    /**
     * @param status
     */
    public void changeStatus(final PlayerStatus status) {
        Log.e("=======", "mediaController: changeStatus=" + status.name());
        mPlayerStatus = status;
        isMaxSetted = false;
        runOnUiThread(new Runnable() {

            @Override
            public void run() {
                if (status == PlayerStatus.PLAYER_IDLE) {
                    btn_play.setEnabled(true);
                    btn_play.setImageResource(R.mipmap.item_continue);
                    seekBar.setEnabled(false);
                    updatePostion(bVideoView == null ? 0 : bVideoView.getCurrentPosition());
                    updateDuration(bVideoView == null ? 0 : bVideoView.getDuration());
                    isPrepared = false;
                } else if (status == PlayerStatus.PLAYER_PREPARING) {
                    btn_play.setEnabled(false);
                    btn_play.setImageResource(R.mipmap.item_pause);
                    seekBar.setEnabled(false);
                    isPrepared = false;
                } else if (status == PlayerStatus.PLAYER_PREPARED) {
                    isPrepared = true;
                    btn_play.setEnabled(true);
                    btn_play.setImageResource(R.mipmap.item_pause);
                    seekBar.setEnabled(true);
                } else if (status == PlayerStatus.PLAYER_COMPLETED) {
                    btn_play.setEnabled(true);
                    btn_play.setImageResource(R.mipmap.item_continue);
                    isPrepared = false;
                }
            }

        });

    }

    private void updatePostion(int second) {
        if (tvCurrentDuration != null) {
            tvCurrentDuration.setText(formatSecond(second));
        }
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if ((bVideoView != null && bVideoView.isPlaying())) {
            bVideoView.stopPlayback();
        }
        /**
         * 结束后台事件处理线程
         */
        mHandlerThread.quit();
        synchronized (syncPlaying) {
            try {
                if (!isReadyForQuit) {
                    syncPlaying.wait(2 * 1000);
                }
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        tvCurrentDuration.setText(TimeUtils.formatSecond(progress));
    }


    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        IsDragging = true;
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        currentPositionInSeconds = seekBar.getProgress();
        if (bVideoView != null) {
            bVideoView.seekTo(seekBar.getProgress());
        }

        IsDragging = false;


    }


    public void clearViewContent() {
        currentPositionInSeconds= 0L;
        tvCurrentDuration.setText("00:00");
        tvMaxDuration.setText("00:00");
        seekBar.setMax(0);
        seekBar.setProgress(0);

    }
    private void tryToPlayOther(String url) {
        /**
         * 如果已经开发播放，先停止播放
         */
        if (mPlayerStatus != PlayerStatus.PLAYER_IDLE) {
            bVideoView.stopPlayback();
        }
        clearViewContent();

        /**
         * 发起一次新的播放任务
         */
        if (progressHandler.hasMessages(1)) {
            progressHandler.removeMessages(1);
        }
        progressHandler.obtainMessage(1,url).sendToTarget();
    }



}
