package com.lywow.mp3app;

import android.annotation.TargetApi;
import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import com.lywow.mp3app.utils.MusicData;

import java.io.IOException;

public class MusicService extends Service {
    private static final String TAG = "MusicService";

    private ChangeListener cl=null;
    private int buttonWitch = 2;
    private int position=0;
    private final MusicBinder binder = new MusicBinder();
    public MediaPlayer mediaPlayer = new MediaPlayer();
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    public void onCreate(){

    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        //isStop = false;
    }

    public class MusicBinder extends Binder {
        public void setButtonWitch(int bw){
            buttonWitch=bw;
        }

        public void setPosition(int p){
            MusicData.musicList.get(position).isPlaying=false;
            position=p;
        }

        public MediaPlayer getMediaPlayer(){
            return mediaPlayer;
        }

        public int getProgress() {
            return mediaPlayer.getDuration();
        }

        public int getPosition(){
            return position;
        }

        /**
         * 获取播放位置
         */
        public int getPlayPosition() {
            return mediaPlayer.getCurrentPosition();
        }
        /**
         * 播放指定位置
         */
        public void seekToPositon(int msec) {
            mediaPlayer.seekTo(msec);
        }

        public void startPlayer() {
            try {
                mediaPlayer.reset();
                MusicData.musicList.get(position).isPlaying=true;
                mediaPlayer.setDataSource(MusicData.musicList.get(position).path);
                mediaPlayer.prepare();
                cl.changeView(position);
                mediaPlayer.start();                        // 启动
                mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
                    @Override
                    public void onCompletion(MediaPlayer mp) {
                        if(!mediaPlayer.isPlaying()){
                            binder.ponMusic();
                        }
                    }
                });
            } catch (IOException e) {
                e.printStackTrace();
            }
        }


        @TargetApi(Build.VERSION_CODES.KITKAT)
        public void ponMusic(){
            if (position == MusicData.musicList.size() - 1)//默认循环播放
            {
                if (buttonWitch == 1) {
                    position--;
                } else if (buttonWitch == 2) {
                    position = 0;// 第一首
                }
            } else if (position == 0) {
                if (buttonWitch == 1) {
                    position = MusicData.musicList.size() - 1;
                } else if (buttonWitch == 2) {
                    position++;
                }
            }else {
                if (buttonWitch == 1) {
                    position--;
                } else if (buttonWitch == 2) {
                    position++;
                }
            }
            buttonWitch = 2 ;
            startPlayer();
        }

        public MusicService getService() {
            return MusicService.this;
        }

        public void closeMedia() {
            if (mediaPlayer != null) {
                mediaPlayer.stop();
                mediaPlayer.release();
            }
        }
    }

    public void setCL(ChangeListener cl) {
        this.cl = cl;
    }

    public interface ChangeListener{
        public void changeView(int position);
    }
}















