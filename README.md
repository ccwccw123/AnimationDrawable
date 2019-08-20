&nbsp;&nbsp;&nbsp;&nbsp;公司有一个项目需求，需要在一个特定的android机顶盒上做一组帧动画的效果，由于设备配置比较低导致出现卡顿现象，而在我自己的手机及pad上是不会卡的,参考了网上的代码，发现还是又卡顿现象，于是自己再稍微做了下修改解决卡顿问题<br/>
直接上代码，需要使用的兄弟直接复制即可<br/>
<h4>

	public class AnimationDrawable {
   	 private static final int MSG_START = 0xf1;
   	 private static final int MSG_STOP = 0xf2;
   	 private static final int STATE_STOP = 0xf3;
    private static final int STATE_RUNNING = 0xf4;

    //运行状态
    private int mState = STATE_RUNNING;
    //显示图片的View
    private ImageView mImageView = null;
    //图片资源的ID列表
    private List<Integer> mResourceIdList = null;
    //定时任务器
    private Timer mTimer = null;
    //定时任务
    private AnimTimerTask mTimeTask = null;
    //记录播放位置
    private int mFrameIndex = 0;
    //播放形式
    private boolean isLooping = false;

    public AnimationDrawable() {
        mTimer = new Timer();
    }

    /**
     * 设置动画播放资源
     */
    public void setAnimation(ImageView imageview, List<Integer> resourceIdList) {
        mImageView = imageview;
        if(mResourceIdList==null){
            mResourceIdList = new ArrayList<Integer>();
        }else{
            mResourceIdList.clear();
        }
        mResourceIdList.addAll(resourceIdList);
    }

    /**
     * 设置动画播放资源
     */
    public void setAnimation(Context context, int resourceId, ImageView imageview) {
        this.mImageView = imageview;
        if(mResourceIdList==null){
            mResourceIdList = new ArrayList<Integer>();
        }else{
            mResourceIdList.clear();
        }

        loadFromXml(context, resourceId, new OnParseListener() {
            @Override
            public void onParse(List<Integer> res) {
                mResourceIdList.addAll(res);
            }
        });
    }

    /**
     * 解析xml
     *
     * @param context
     * @param resourceId 资源id
     */
    private void loadFromXml(final Context context, final int resourceId,
                             final OnParseListener onParseListener) {
        if (context == null) {
            return;
        }

        final List<Integer> res = new ArrayList<Integer>();
        XmlResourceParser parser = context.getResources().getXml(resourceId);

        try {
            int eventType = parser.getEventType();
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_DOCUMENT) {
                } else if (eventType == XmlPullParser.START_TAG) {
                    if (parser.getName().equals("item")) {
                        for (int i = 0; i < parser.getAttributeCount(); i++) {
                            if (parser.getAttributeName(i).equals("drawable")) {
                                int resId = Integer.parseInt(parser.getAttributeValue(i).substring(1));
                                res.add(resId);
                            }
                        }
                    }
                } else if (eventType == XmlPullParser.END_TAG) {
                } else if (eventType == XmlPullParser.TEXT) {
                }

                eventType = parser.next();
            }
        } catch (IOException e) {
            // TODO: handle exception
            e.printStackTrace();
        } catch (XmlPullParserException e2) {
            // TODO: handle exception
            e2.printStackTrace();
        } finally {
            parser.close();
        }

        if (onParseListener != null) {
            onParseListener.onParse(res);
        }
    }

    private AnimationLisenter lisenter;


    /**
     * 开始播放动画
     *
     * @param loop     是否循环播放
     * @param duration 动画播放时间间隔
     */
    public void start(boolean loop, int duration, AnimationLisenter lisenter) {
        this.lisenter = lisenter;
        stop();
        if (mResourceIdList == null || mResourceIdList.size() == 0) {
            return;
        }
        if (mTimer == null) {
            mTimer = new Timer();
        }
        isLooping = loop;
        mFrameIndex = 0;
        mState = STATE_RUNNING;
        mTimeTask = new AnimTimerTask();
        mTimer.schedule(mTimeTask, 0, duration);
        lisenter.startAnimation();
    }

    /**
     * 停止动画播放
     */
    public void stop() {
        if (mTimer != null) {
            mTimer.purge();
            mTimer.cancel();
            mTimer = null;
        }
        if (mTimeTask != null) {
            mFrameIndex = 0;
            mState = STATE_STOP;
            mTimeTask.cancel();
            mTimeTask = null;
        }
        //移除Handler消息
        if (AnimHandler != null) {
            AnimHandler.removeMessages(MSG_START);
            AnimHandler.removeMessages(MSG_STOP);
            AnimHandler.removeCallbacksAndMessages(null);
        }
    }

    /**
     * 定时器任务
     */
    class AnimTimerTask extends TimerTask {

        @Override
        public void run() {
            if (mFrameIndex < 0 || mState == STATE_STOP) {
                return;
            }

            if (mFrameIndex < mResourceIdList.size()) {
                Message msg = AnimHandler.obtainMessage(MSG_START, 0, 0, null);
                msg.sendToTarget();
            } else {
                mFrameIndex = 0;
                if (!isLooping) {
                    Message msg = AnimHandler.obtainMessage(MSG_STOP, 0, 0, null);
                    msg.sendToTarget();
                }
            }
        }
    }

    /**
     * Handler
     */
    private Handler AnimHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_START: {
                    if (mFrameIndex >= 0 && mFrameIndex < mResourceIdList.size() && mState == STATE_RUNNING) {
                        //这里不能使用image.setImageResource 因为源码中也是创建了bitmap 所以这里我们自己创建
                        Bitmap bitmap=readBitMap(mImageView.getContext(),mResourceIdList.get(mFrameIndex));
                        mImageView.setImageBitmap(bitmap);
                        mFrameIndex++;
                    }
                }
                break;
                case MSG_STOP: {
                    if (mTimeTask != null) {
                        mFrameIndex = 0;
                        mTimer.purge();
                        mTimeTask.cancel();
                        mState = STATE_STOP;
                        mTimeTask = null;
                        lisenter.endAnimation();
                        if (isLooping) {
                            mImageView.setImageResource(0);
                        }
                    }
                }
                break;
                default:
                    break;
            }
        }
    };

    public static Bitmap readBitMap(Context context, int resId) {
        BitmapFactory.Options opt = new BitmapFactory.Options();
        opt.inPreferredConfig = Bitmap.Config.RGB_565;
        opt.inPurgeable = true;
        opt.inInputShareable = true;
        InputStream is = context.getResources().openRawResource(resId);
        return BitmapFactory.decodeStream(is, null, opt);
    }

    public interface OnParseListener {
        void onParse(List<Integer> res);
    }

    public interface AnimationLisenter {
        void startAnimation();

        void endAnimation();
    }
}
</h4>
<br/>然后调用代码

		 animationDrawable.setAnimation(view, 	resourceIdSuccessList)
            animationDrawable.start(false, 40, object : AnimationDrawable.AnimationLisenter {
                override fun startAnimation() {

                }

                override fun endAnimation() {
      
                }
            })
            
   <br/>这样就好了，这里主要是借用定时器timer+handler去实现帧动画效果，然后bitmap=readBitMap(mImageView.getContext(),mResourceIdList.get(mFrameIndex));再这里去创建bitmap 并设置config，尽量使用最省内存方式去处理帧动画的播放
   <br/><h4/>总结
   </br><font color='red'>
   		&nbsp;&nbsp;&nbsp;&nbsp;*  那是为什么，会导致oom呢：
 *  原来当使用像 imageView.setBackgroundResource，imageView.setImageResource, 或者 BitmapFactory.decodeResource  这样的方法来设置一张大图片的时候，
 *  这些函数在完成decode后，最终都是通过java层的createBitmap来完成的，需要消耗更多内存。
 *  因此，改用先通过BitmapFactory.decodeStream方法，创建出一个bitmap，再将其设为ImageView的 source，decodeStream最大的秘密在于其直接调用JNI>>nativeDecodeAsset()来完成decode，无需再使用java层的createBitmap，从而节省了java层的空间。如果在读取时加上图片的Config参数，可以跟有效减少加载的内存，从而跟有效阻止抛out of Memory异常。
 *  另外，需要特别注意：
 *  decodeStream是直接读取图片资料的字节码了， 不会根据机器的各种分辨率来自动适应，使用了decodeStream之后，需要在hdpi和mdpi，ldpi中配置相应的图片资源，否则在不同分辨率机器上都是同样大小（像素点数量），显示出来的大小就不对了。
   
   </font>




				
