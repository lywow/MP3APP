package com.lywow.mp3app;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.viewpager.widget.ViewPager;

import com.lywow.mp3app.adapter.MusicPagerAdapter;
import com.lywow.mp3app.fragment.LogicFragment;
import com.lywow.mp3app.utils.MusicData;
import com.master.permissionhelper.PermissionHelper;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

//实现OnClickListener的接口
public class MainActivity extends AppCompatActivity implements View.OnClickListener, LogicFragment.MyListener {
    private static final String TAG = "MainService";

    private long mExitTime;       //实现“再按一次退出”的记录时间变量
    int i=0;   //播放顺序模式转换的计数器
    private Handler mHandler = new Handler();

    private ViewPager viewPager;
    private List<Fragment> fragmentList = new ArrayList<>();//将Fragment放入List集合中，存放fragment对象
    private TextView currrentTv;
    private TextView totalTv;
    private ImageView prevImgv;
    private ImageView nextImgv;
    private ImageView pauseImgv;
    private SeekBar seekBar;
    private LinearLayout music_linlayout;
    private TextView playing_music_title;
    Intent MediaServiceIntent;
    private LogicFragment logicFragment;

    private MusicService.MusicBinder binder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        init();

        logicFragment = new LogicFragment();
        fragmentList.add(logicFragment);
        MusicPagerAdapter musicPagerAdapter = new MusicPagerAdapter(getSupportFragmentManager(), fragmentList);
        viewPager.setAdapter(musicPagerAdapter);

        MediaServiceIntent = new Intent(this, MusicService.class);

        //判断权限够不够，不够就给
        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
            }, 1);
        } else {
            //够了就设置路径等，准备播放
            bindService(MediaServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
        }


    }

    private void init() {
        viewPager = findViewById(R.id.main_vp);
        currrentTv = findViewById(R.id.music_current_tv);
        totalTv = findViewById(R.id.music_total_tv);
        prevImgv = findViewById(R.id.music_prev_imgv);
        nextImgv = findViewById(R.id.music_next_imgv);
        pauseImgv = findViewById(R.id.music_pause_imgv);
        seekBar = findViewById(R.id.music_seekbar);
        music_linlayout = findViewById(R.id.music_linlayout);
        playing_music_title=findViewById(R.id.playing_music_title);
        music_linlayout.setVisibility(View.GONE);
        pauseImgv.setOnClickListener(this);
        prevImgv.setOnClickListener(this);
        nextImgv.setOnClickListener(this);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[]permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case 1:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    bindService(MediaServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
                } else {
                    Toast.makeText(this, "权限不够获取不到音乐，程序将退出", Toast.LENGTH_SHORT).show();
                    finish();
                }
                break;
            default:
                break;
        }
    }

    private ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            binder = (MusicService.MusicBinder) service;
//            mMediaService = ((MediaService.MyBinder) service).getInstance();
            binder.getService().setCL(new MusicService.ChangeListener() {
                @Override
                public void changeView(int position) {
                    setPlayerView(position);
                }

            });
            seekBar.setMax(binder.getProgress());

            seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    //这里很重要，如果不判断是否来自用户操作进度条，会不断执行下面语句块里面的逻辑，然后就会卡顿卡顿
                    if(fromUser){
                        binder.seekToPositon(seekBar.getProgress());
//                    mMediaService.mMediaPlayer.seekTo(seekBar.getProgress());
                    }
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {

                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {

                }
            });

            mHandler.post(mRunnable);

            Log.d(TAG, "Service与Activity已连接");
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

        }
    };

    @Override //再按一次退出程序
    public void onBackPressed() {
        if (System.currentTimeMillis() - mExitTime < 2000) {
            super.onBackPressed();
            finish();
        } else {
            mExitTime = System.currentTimeMillis();
            Toast.makeText(MainActivity.this, "再按一次返回键退出应用", Toast.LENGTH_SHORT).show();
        }
    }


    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.music_prev_imgv:
                binder.setButtonWitch(1);
                binder.ponMusic();
                break;
            case R.id.music_next_imgv:
                binder.setButtonWitch(2);
                binder.ponMusic();
                break;
            case R.id.music_pause_imgv:
                if (binder.getMediaPlayer().isPlaying()) {
                    binder.getMediaPlayer().pause();
                    pauseImgv.setImageResource(R.mipmap.ic_play_btn_play);
                } else {
                    binder.getMediaPlayer().start();
                    pauseImgv.setImageResource(R.mipmap.ic_play_btn_pause);
                }
                break;
            default:
                break;
        }
    }

    //格式化数字
    private String formatTime(int length) {
        Date date = new Date(length);
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("mm:ss");    //规定固定的格式
        String totaltime = simpleDateFormat.format(date);
        return totaltime;
    }

    @Override
    public void sendContent(int position) {
        Toast.makeText(MainActivity.this, "点击了"+position, Toast.LENGTH_SHORT).show();
        binder.setPosition(position);
        setPlayerView(position);
        binder.startPlayer();
    }

    private Runnable mRunnable = new Runnable() {
        @Override
        public void run() {
            seekBar.setProgress(binder.getPlayPosition());
            currrentTv.setText(formatTime(binder.getPlayPosition()));
            mHandler.postDelayed(mRunnable, 1000);
        }
    };

    public void setPlayerView(int position){
        music_linlayout.setVisibility(View.VISIBLE);
        seekBar.setMax(MusicData.musicList.get(position).length);
        totalTv.setText(formatTime(MusicData.musicList.get(position).length));
        playing_music_title.setText("正在播放："+MusicData.musicList.get(position).title);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //我们的handler发送是定时1000s发送的，如果不关闭，MediaPlayer release掉了还在获取getCurrentPosition就会爆IllegalStateException错误
        mHandler.removeCallbacks(mRunnable);
        binder.closeMedia();
        unbindService(mServiceConnection);
    }
}

