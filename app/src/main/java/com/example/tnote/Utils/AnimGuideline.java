package com.example.tnote.Utils;

import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.constraintlayout.widget.Guideline;

/**
 * 自定义可动画化和可拖动的Guideline组件
 * 功能特性：
 * 1. 支持通过触摸区域拖动调整位置
 * 2. 支持平滑动画过渡
 * 3. 提供拖动状态回调接口
 */
public class AnimGuideline extends Guideline {
    // 最小和最大位置百分比限制
    private static final float MIN_PERCENT = 0.1f;
    private static final float MAX_PERCENT = 1.0f;

    // 触摸关联区域视图
    private View touchArea;
    // 拖动起始坐标和百分比
    private float startX;
    private float startPercent;
    // 最后移动坐标和时间（用于计算速度）
    private float lastMoveX;
    private long lastMoveTime;
    // 当前活动的动画实例
    private ValueAnimator currentAnimator;
    // 状态监听器
    private GuidelineListener listener;

    // 构造方法
    public AnimGuideline(Context context) {
        super(context);
    }

    public AnimGuideline(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public AnimGuideline(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    /**
     * 设置关联的触摸区域视图
     * @param touchArea 接收触摸事件的视图
     */
    public void setTouchArea(View touchArea) {
        this.touchArea = touchArea;
    }

    /**
     * 初始化拖动手势处理
     */
    @SuppressLint("ClickableViewAccessibility")
    public void setupDragBehavior() {
        touchArea.setOnTouchListener((v, event) -> {
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    handleDragStart(event);
                    return true;
                case MotionEvent.ACTION_MOVE:
                    handleDragMove(event);
                    return true;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    handleDragEnd();
                    return true;
            }
            return false;
        });
    }

    /**
     * 处理拖动开始事件
     * @param event 触摸事件
     */
    private void handleDragStart(@NonNull MotionEvent event) {
        cancelCurrentAnimation();
        startX = event.getRawX();  // 记录初始X坐标
        startPercent = getCurrentPercent();  // 获取当前百分比
        lastMoveX = startX;
        lastMoveTime = System.currentTimeMillis();

        // 触发拖动开始回调
        if (listener != null) {
            listener.onDragStart();
        }
    }

    /**
     * 处理拖动移动事件
     * @param event 触摸事件
     */
    private void handleDragMove(@NonNull MotionEvent event) {
        // 计算移动速度和灵敏度
        long currentTime = System.currentTimeMillis();
        float timeDelta = currentTime - lastMoveTime;
        float posDelta = event.getRawX() - lastMoveX;
        float velocity = timeDelta > 0 ? posDelta / timeDelta : 0;

        // 动态调整灵敏度（基于移动速度）
        float sensitivity = Math.min(1.5f, Math.max(0.5f, 0.8f + Math.abs(velocity) * 0.05f));
        float deltaX = (event.getRawX() - startX) * sensitivity;

        // 获取父容器并计算新百分比
        ConstraintLayout parent = (ConstraintLayout) getParent();
        if (parent == null) return;

        float parentWidth = parent.getWidth();
        float newPercent = startPercent + deltaX / parentWidth;
        updatePercent(newPercent);

        // 更新最后移动参数
        lastMoveX = event.getRawX();
        lastMoveTime = currentTime;
    }

    /**
     * 处理拖动结束事件
     */
    private void handleDragEnd() {
        float currentPercent = getCurrentPercent();
        // 自动吸附到边界
        if (currentPercent < 0.05f) {
            animateToPercent(MIN_PERCENT);
        } else if (currentPercent > 0.85f) {
            animateToPercent(MAX_PERCENT);
        }

        // 触发拖动结束回调
        if (listener != null) {
            listener.onDragEnd(getCurrentPercent());
        }
    }

    /**
     * 执行百分比动画
     * @param targetPercent 目标百分比
     */
    public void animateToPercent(float targetPercent) {
        currentAnimator = ValueAnimator.ofFloat(getCurrentPercent(), targetPercent);
        currentAnimator.addUpdateListener(animation ->
                updatePercent((Float) animation.getAnimatedValue()));
        currentAnimator.setDuration(150).start();
    }

    /**
     * 切换Guideline可见性（带动画）
     * @param visible 是否可见
     * @param visiblePercent 可见时的目标百分比
     */
    public void toggleVisibility(boolean visible, float visiblePercent) {
        float target = visible ? visiblePercent : MAX_PERCENT;
        currentAnimator = ValueAnimator.ofFloat(getCurrentPercent(), target);
        currentAnimator.addUpdateListener(animation -> {
            float percent = (Float) animation.getAnimatedValue();
            updatePercent(percent);
            // 触发可见性变化回调
            if (listener != null) {
                listener.onVisibilityChanged(percent < 0.95f);
            }
        });
        currentAnimator.setDuration(300).start();
    }

    /**
     * 更新Guideline百分比并应用布局
     * @param percent 新百分比（自动限制在MIN-MAX范围内）
     */
    private void updatePercent(float percent) {
        percent = Math.max(MIN_PERCENT, Math.min(MAX_PERCENT, percent));
        ConstraintLayout parent = (ConstraintLayout) getParent();
        if (parent == null) return;

        // 使用ConstraintSet更新布局
        ConstraintSet set = new ConstraintSet();
        set.clone(parent);
        set.setGuidelinePercent(getId(), percent);
        set.applyTo(parent);
    }

    /**
     * 获取当前Guideline百分比
     */
    public float getCurrentPercent() {
        ConstraintLayout.LayoutParams params =
                (ConstraintLayout.LayoutParams) getLayoutParams();
        return params.guidePercent;
    }

    /**
     * 取消当前动画
     */
    private void cancelCurrentAnimation() {
        if (currentAnimator != null && currentAnimator.isRunning()) {
            currentAnimator.cancel();
        }
    }

    /**
     * 设置Guideline状态监听器
     */
    public void setGuidelineListener(GuidelineListener listener) {
        this.listener = listener;
    }

    /**
     * Guideline状态回调接口
     */
    public interface GuidelineListener {
        void onDragStart();
        void onDragEnd(float finalPercent);
        void onVisibilityChanged(boolean visible);
    }
}