package com.ccenglish.civainteractionanswer;

import android.content.Context;
import android.content.res.XmlResourceParser;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Message;
import android.widget.ImageView;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * desc:播放帧动画管理类
 * author：ccw
 * date:2019-08-12
 * time:19:25
 */
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
                        //这里不能使用image.setResourceDrawable 因为源码中也是创建了bitmap 所以这里我们自己创建
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
