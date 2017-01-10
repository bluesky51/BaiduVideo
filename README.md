例子的具体说明步骤:  
1.集成百度云播放器:  
  1》添加libs依赖库：cyberplayer-sdk.jar  
  2》添加so文件库(主要支持arm64-v8a和armabi-v7a):使用jniLibs进行导入  
  3》申请access Key:申请地址如下:  
 https://console.bce.baidu.com/iam/?_=1481952806347#/iam/accesslist  
  4》添加权限  
     <uses-permission android:name="android.permission.INTERNET" />  
     <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE"/>  
     <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"/>  
     <uses-permission android:name="android.permission.WRITE_SETTINGS" />  
     <uses-permission android:name="android.permission.READ_PHONE_STATE" />  
   5》使用BVideoView进行视频播放(BMediaController在本人测试机华为荣耀8，android7.0的系统崩溃无法使用，所以控制都是自己定义完成)  
2.xml文件UI书写<br />  
3.java代码<br />  
  1.查找控件，主要是对BVideoView进行配置，设置以下监<br />  
  BVideoView.OnPreparedListener:设置播放器的状态为准备状态，具体如下:<br />  
    @Override 
      public void onPrepared() {
          isPrepared = true;
          changeStatus(PlayerStatus.PLAYER_PREPARED);
      }
  BVideoView.OnCompletionListener,设置播放器状态为完成状态，具体如下:<br />  
    @Override
    public void onCompletion() {
        synchronized (syncPlaying) {
            isReadyForQuit = true;
            syncPlaying.notifyAll();
        }
        changeStatus(PlayerStatus.PLAYER_COMPLETED);
    }

  BVideoView.OnErrorListener,设置播放器状态为空闲状态，具体如下:<br />  
    @Override
      public boolean onError(int i, int i1) {
          synchronized (syncPlaying) {
              isReadyForQuit = true;
              syncPlaying.notifyAll();
          }
          changeStatus(PlayerStatus.PLAYER_IDLE);
          return true;
      }
  BVideoView.OnPositionUpdateListener,设置进度条的变化，具体如下:<br />   
   //每200ms调用一次(百度云直播已经写好，在后台执行)<br />  
      @Override
      public boolean onPositionUpdate(long newPositionIiSeconds) {

          long newPositionInSeconds = newPositionInSecondsonIiSeconds / 1000;
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

  BVideoView.OnTotalCacheUpdateListener，设置视频缓存的进度在SeekBar上的体现，具体如下:<br />  
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
 备注：更改状态的方法较为复杂:<br />  
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
   2.播放按钮的主要如下:<br />  
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

   3.视频全屏的操作如下:<br />  
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

   4.视频切换地址播放请看tryToPlayOther(String url)方法即可；
