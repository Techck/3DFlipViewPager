package com.example.yuehuang.myapplication;

import android.content.Context;
import android.graphics.Camera;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Scroller;

/**
 * @author huangyue
 * @version V1.0
 * @Package com.example.yuehuang.myapplication
 * @Description:
 * @date 2016/11/01 13:55
 */

public class MyView extends ViewGroup{

    public enum State {
        ToPre, ToNext
    }

    private State state;//标记滚动的状态

    private int mWidth, mHeight;//ViewGroup的宽高
    private float mDownX, mMoveX = 0;
    private VelocityTracker mVelocityTracker;//测速
    private float mAngle = 90;//两个item间的夹角

    private int standerSpeed = 200;//规定的手指移动速率

    private int a = -2500;//阻力

    private Scroller mScroller;
    private Camera mCamera;
    private Matrix mMatrix;

    private boolean flag = false;//标记是否在增加view

    public MyView(Context context){
        this(context, null);
    }

    public MyView(Context context, AttributeSet attributeSet){
        super(context, attributeSet);
        mScroller = new Scroller(context);
        mCamera = new Camera();
        mMatrix = new Matrix();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        measureChildren(widthMeasureSpec, heightMeasureSpec);
        mWidth = getMeasuredWidth();
        mHeight = getMeasuredHeight();
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int childLeft = 0;
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            if (child.getVisibility() == VISIBLE) {
                child.layout(childLeft,
                        0,
                        childLeft + child.getMeasuredWidth(),
                        child.getMeasuredHeight());
                childLeft = childLeft + child.getMeasuredWidth();
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        }
        mVelocityTracker.addMovement(event);


        if (event.getAction() == MotionEvent.ACTION_MOVE){
            float x = event.getX();
            int tempX = (int)(mDownX - x);
            mDownX = x;
            scrollBy(tempX, 0);
            //根据距离判断首尾view的添加,保证循环滚动
            changeView();
        }else if(event.getAction() == MotionEvent.ACTION_DOWN){
            mDownX = event.getX();
        }else if(event.getAction() == MotionEvent.ACTION_UP){
            //通过一个速率的单位计算速率的值
            mVelocityTracker.computeCurrentVelocity(1000);
            //往左滑小于0   往右滑大于0
            float xVelocity = mVelocityTracker.getXVelocity();
            //滑动的速度大于规定的速度时,开始自动减速滑动直至静止
            if (xVelocity > standerSpeed) {
                state = State.ToPre;
                //根据重力加速度计算需要滑动的路程
                float s = xVelocity * (-xVelocity / a) / 2;
                //计算需要滑动的时间
                int t = (int) (-xVelocity / a * 2000);
                //开始计算滑动过程
                mScroller.startScroll(0, 0, (int) s, 0, t);
            }else if(xVelocity < -standerSpeed){
                state = State.ToNext;
                //根据重力加速度计算需要滑动的路程
                xVelocity = -xVelocity;
                float s = xVelocity * (-xVelocity / a) / 2;
                //计算需要滑动的时间
                int t = (int) (-xVelocity / a * 2000);
                //开始计算滑动过程
                mScroller.startScroll(0, 0, (int) s, 0, t);
            }
        }
        return true;
    }

    @Override
    public void computeScroll() {
        if (mScroller.computeScrollOffset() && flag == false) {
            if (state == State.ToPre) {
                int tempX = (int) (mScroller.getCurrX() - mMoveX);
                mMoveX = mScroller.getCurrX();
                scrollBy(-tempX, 0);
            }else if(state == State.ToNext){
                int tempX = (int) (mScroller.getCurrX() - mMoveX);
                mMoveX = mScroller.getCurrX();
                scrollBy(tempX, 0);
            }
            invalidate();
        }
        if(mScroller.isFinished())
            mMoveX = 0;
        //判断一下是否需要在首尾添加界面
        changeView();
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        //遍历子View,并画出来
        for (int i = 0; i < getChildCount(); i++) {
            drawScreen(canvas, i, getDrawingTime());
        }
        //如果不想要3D效果只想实现平移,注释掉上面的代码,开启下面的代码即可
//        super.dispatchDraw(canvas);
    }

    /**
     * 画3D效果
     */
    private void drawScreen(Canvas canvas, int i, long drawingTime){
        int curScreenX = mWidth * i;
        //屏幕中不显示的部分不进行绘制
        if (getScrollX() + mWidth < curScreenX) {
            return;
        }
        if (curScreenX < getScrollX() - mWidth) {
            return;
        }

        //计算中心坐标
        float centerX = (getScrollX() > curScreenX) ? curScreenX + mWidth : curScreenX;
        float centerY = mHeight / 2;
        //计算角度
        float degree = mAngle * (getScrollX() - curScreenX) / mWidth;
        if (degree > 90 || degree < -90) {
            return;
        }
        canvas.save();

        //利用Camera进行一次旋转拍照
        mCamera.save();
        mCamera.rotateY(-degree);
        //并将结果保存在Matrix矩阵中
        mCamera.getMatrix(mMatrix);
        mCamera.restore();

        //将矩阵移动到视图中心位置
        mMatrix.preTranslate(-centerX, -centerY);
        mMatrix.postTranslate(centerX, centerY);
        //将矩阵设置到画布上
        canvas.concat(mMatrix);
        //画子View
        drawChild(canvas, getChildAt(i), drawingTime);
        canvas.restore();
    }

    /**
     * 当超过规定位置时,增加首尾的页数,达到拼接的效果
     */
    private void changeView(){
        if (getScrollX() < 20){
            addPre();
            LogUtil.d("TAG", "前面加了一页");
        }else if(getScrollX() > (getChildCount() - 1) * mWidth - 20){
            addNext();
            LogUtil.d("TAG", "后面加了一页");
        }
    }

    /**
     * 把第一个item移动到最后一个item位置
     */
    private void addNext() {
        int childCount = getChildCount();
        View view = getChildAt(0);
        removeViewAt(0);
        addView(view, childCount - 1);
        scrollBy(-mWidth, 0);
    }

    /**
     * 把最后一个item移动到第一个item位置
     */
    private void addPre() {
        int childCount = getChildCount();
        View view = getChildAt(childCount - 1);
        flag = true;
        removeViewAt(childCount - 1);
        addView(view, 0);
        scrollBy(mWidth, 0);
        flag = false;
    }
}
