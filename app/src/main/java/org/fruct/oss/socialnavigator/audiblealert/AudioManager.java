package org.fruct.oss.socialnavigator.audiblealert;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;

import org.fruct.oss.socialnavigator.points.Category;
import org.fruct.oss.socialnavigator.points.Point;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Locale;

//import org.fruct.oss.socialnavigator.poi.gets.CategoriesList;
//import org.fruct.oss.socialnavigator.service.Direction;
//import org.fruct.oss.socialnavigator.service.DirectionService;

/**
 * Provides audio uri's to audio player
 *
 *
 */
public class AudioManager {

    private final static Logger log = LoggerFactory.getLogger(AudioPlayer.class);

    // направление
    public static final String LEFT = "left";
    public static final String RIGHT = "right";
    public static final String FORWARD = "forward";
    public static final String BACK= "back";

    // движение
    public static final String TURN = "turn";
    public static final String TO_LEFT = "turn_left";
    public static final String TO_RIGHT = "turn_right";

    // сервисные сообщения
    public static final String WELCOME = "welcome";
    public static final String UNKNOWN_DIRECTION = "unknown_dir";

    // TODO: in case of need to check whether you already left the zone of queued point, add list of points
    private ArrayList<String> uris;
    private String playingNow;
    private AudioPlayer audioPlayer;

    private Context context;

    private boolean TTS_enabled;
    public boolean keepPlaying = true;

    // TODO: восстановить
    // TextToSpeachPlayer ttsp;

    private BroadcastReceiver audioFinishedReceiver;
    private BroadcastReceiver stopTrackingReceiver;

    public AudioManager(Context ctx){
        log.error("Created AudioManager ^^^^^^^^^^^^^^^^^^");
        uris = new ArrayList<>();

        //CategoriesManager.init(); //???
        context = ctx;
        audioFinishedReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                playNext();
                //log.debug("AUDIOPLAYER: PLAYING NEXT ^^^^^^^^");
            }
        };
       context.registerReceiver(audioFinishedReceiver, new IntentFilter(AudioPlayer.BC_ACTION_STOP_PLAY));

        stopTrackingReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                    log.trace("Playing exiting message");
                    stopPlaying();
            }
        };

        //TODO: узнать как сейчас
//        LocalBroadcastManager.getInstance(context).registerReceiver(stopTrackingReceiver,
//                new IntentFilter(MainActivity.STOP_TRACKING_SERVICE));

        audioPlayer = new AudioPlayer(context);
        //ttsp = new TextToSpeachPlayer(context, TextToSpeachPlayer.TTS_PLAYER_EN); //???
        //TTS_enabled = ttsp.checkInit(); //???


    }

    public void startPlaying(){
        keepPlaying = true;
        uris.add(getUriByCategory(WELCOME));
        playNext();
    }


    public void queueToPlay(Point point, String dir){
        String category, direction;
        category = getUriByCategory(point.getCategory().getIdentifiedName());
        direction = getUriByDirection(dir);
        if(category!= "" && direction != "") {
            uris.add(category);
            uris.add(direction);
        }else{
            log.error("Couldn't add point to play queue");
        }
    }

    /**
     * добавление в очередь для проигрывания поворотов
     * @param dir направление поворота
     */
    public void queueTurnToPlay(String dir) {
        uris.add(getUriByCategory(TURN));
        uris.add(getUriByTurnDirection(dir));
    }

    public void playNext(){
        if(uris.size() == 0){
            return;
        }
        playingNow = uris.get(0);
        if(audioPlayer.startAudioTrack(playingNow))
            uris.remove(playingNow); // playin started successfully

    }

    private String getUriByCategory(String cat) {
        //String folder = "android.resource://" + App.getInstance().getPackageName() + "/";
        String folder = "sounds/"; //file:///android_asset/sounds/";
        if (Locale.getDefault().getLanguage().equals("ru")) {
            folder += "ru/";
        } else {
            folder += "en/";
        }

        return folder + cat + ".mp3";
    }

    private String getUriByDirection(String dir){
        String folder = "sounds/"; //"android.resource://" + App.getInstance().getPackageName() + "/";
        if (Locale.getDefault().getLanguage().equals("ru")) {
            folder += "ru/";
        }

        String ret = folder + dir + ".mp3";
        switch (dir) {
            case LEFT:
            case RIGHT:
            case FORWARD:
            case BACK:
                return ret;
            default:
                return folder + UNKNOWN_DIRECTION + ".mp3";
        }
    }

    private String getUriByTurnDirection(String dir){
        String folder = "sounds/"; //"android.resource://" + App.getInstance().getPackageName() + "/";
        if (Locale.getDefault().getLanguage().equals("ru")) {
            folder += "ru/";
        }

        String ret = folder + dir + ".mp3";
        switch (dir) {
            case TO_LEFT:
            case TO_RIGHT:
                return ret;
            default:
                return folder + UNKNOWN_DIRECTION + ".mp3";
        }
    }


    public void stopPlaying(){
        keepPlaying = false;
        uris.clear();
        audioPlayer.stopAudioTrack();
        //audioPlayer.startAudioTrack(getUriForCategory(GOODBYE));
    }

    public void onDestroy(){
        //ttsp.destroy();
        context.unregisterReceiver(audioFinishedReceiver);
        LocalBroadcastManager.getInstance(context).unregisterReceiver(stopTrackingReceiver);
    }

}



