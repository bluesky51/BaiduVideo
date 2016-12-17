package com.sky.video.baiduvideo;

import com.baidu.cyberplayer.core.BVideoView;

/**
 * Created by BlueSky on 16/12/17.
 */

public class VideoController {


    public void play(BVideoView bVideoView,String path) {
        bVideoView.setVideoPath(path);
        bVideoView.start();

    }

    public void pause(BVideoView bVideoView) {
        if (bVideoView!=null&&bVideoView.isPlaying()){
            bVideoView.pause();

        }

    }

    public void resume(BVideoView bVideoView) {
        if (bVideoView!=null&&bVideoView.isPlaying()){

        }

    }
}
