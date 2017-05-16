package org.fruct.oss.socialnavigator.audiblealert;

import android.content.Context;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.content.LocalBroadcastManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class AudioPlayer implements MediaPlayer.OnPreparedListener, MediaPlayer.OnCompletionListener, MediaPlayer.OnErrorListener {
    public static String BC_ACTION_START_PLAY = "org.fruct.oss.audioguide.AudioPlayer.START_PLAY";
    public static String BC_ACTION_STOP_PLAY = "org.fruct.oss.audioguide.AudioPlayer.STOP_PLAY";
    public static String BC_ACTION_POSITION = "org.fruct.oss.audioguide.AudioPlayer.POSITION";

    private final static Logger log = LoggerFactory.getLogger(AudioPlayer.class);
    private final Context context;

    private MediaPlayer player;
    private String currentUri;
    private AssetManager assets;


    public AudioPlayer(Context context) {
        this.context = context;
        assets = context.getAssets();
    }


    public boolean startAudioTrack(String uri) {
        if (player != null || uri == null) {
            return false;
        }

        player = new MediaPlayer();
        player.setAudioStreamType(AudioManager.STREAM_MUSIC);
        try {
            AssetFileDescriptor afd = assets.openFd(uri);
            //player.setDataSource(context, uri);
            player.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
        } catch (IOException e) {
            log.warn("Cannot set data source for player with url = '{}'", uri);
            return false;
        }

        currentUri = uri;
        player.setOnCompletionListener(this);
        player.setOnPreparedListener(this);
        player.setOnErrorListener(this);
        player.prepareAsync();
        return true;
    }



    public void stopAudioTrack() {
        if (player != null) {
            player.stop();
            player = null;
            currentUri = null;
            LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent(BC_ACTION_STOP_PLAY));
        }
    }

    public boolean isPlaying(Uri uri) {
        return player != null && (uri == null || uri.equals(currentUri));
    }

    @Override
    public void onPrepared(MediaPlayer mediaPlayer) {
        log.trace("Playing uri {}", currentUri);
        mediaPlayer.start();

        Intent intent = new Intent(BC_ACTION_START_PLAY);
        intent.putExtra("duration", mediaPlayer.getDuration());
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);

        handlerPlayer = mediaPlayer;
        handler = new Handler(Looper.getMainLooper());
        handler.postDelayed(positionUpdater, 1000);
    }

    private MediaPlayer handlerPlayer;
    private Handler handler;
    private Runnable positionUpdater = new Runnable() {
        @Override
        public void run() {
            if (player != handlerPlayer) {
                return;
            }

            Intent intent = new Intent(BC_ACTION_POSITION);
            intent.putExtra("position", player.getCurrentPosition());

            LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
            handler.postDelayed(positionUpdater, 1000);
        }
    };

    @Override
    public void onCompletion(MediaPlayer mediaPlayer) {
        if (player != null) {
            player.release();
            player = null;
            currentUri = null;
            currentUri = null;
           // LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent(BC_ACTION_STOP_PLAY));
            context.sendBroadcast(new Intent(BC_ACTION_STOP_PLAY));
        }
    }

    @Override
    public boolean onError(MediaPlayer mediaPlayer, int what, int extra) {
        log.warn("Player error with uri " + currentUri + " " + what + " " + extra);

        player = null;
        currentUri = null;
        currentUri = null;

        return false;
    }

    public void pause() {
        if (player != null && player.isPlaying()) {
            player.pause();
        }
    }

    public void unpause() {
        if (player != null) {
            player.start();
        }
    }

    public void seek(int position) {
        if (player != null) {
            player.seekTo(position);
        }
    }
}
