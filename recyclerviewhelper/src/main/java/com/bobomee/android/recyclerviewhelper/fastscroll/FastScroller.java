/*
 * Copyright (C) 2016.  BoBoMEe(wbwjx115@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.bobomee.android.recyclerviewhelper.fastscroll;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.support.annotation.NonNull;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import com.bobomee.android.recyclerviewhelper.R;
import com.bobomee.android.recyclerviewhelper.fastscroll.interfaces.BubbleTextCreator;
import com.bobomee.android.recyclerviewhelper.fastscroll.interfaces.OnScrollStateChange;
import com.bobomee.android.recyclerviewhelper.fastscroll.util.AnimatorUtil;
import com.bobomee.android.recyclerviewhelper.fastscroll.util.GradientDrawableHelper;
import com.bobomee.android.recyclerviewhelper.fastscroll.util.StateListDrawableHelper;

import static com.bobomee.android.recyclerviewhelper.fastscroll.Utils.getValueInRange;
import static com.bobomee.android.recyclerviewhelper.fastscroll.Utils.setTextBackground;

/**
 * Class taken from GitHub, customized and optimized for FlexibleAdapter project.
 *
 * @see <a href="https://github.com/AndroidDeveloperLB/LollipopContactsRecyclerViewFastScroller">
 * github.com/AndroidDeveloperLB/LollipopContactsRecyclerViewFastScroller</a>
 * @since Up to the date 23/01/2016
 * <br/>23/01/2016 Added onFastScrollerStateChange in the listener
 */
public class FastScroller extends FrameLayout {

  TextView bubble;
  ImageView handle;
  int height;
  private boolean isInitialized = false;
  private ObjectAnimator currentAnimator;
  private BubbleTextCreator bubbleTextCreator;
  private OnScrollStateChange mOnScrollStateChange;

  public FastScroller(Context context) {
    this(context, null);
  }

  public FastScroller(Context context, AttributeSet attrs) {
    this(context, attrs, 0);
  }

  public FastScroller(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    init();
  }

  private void init() {
    if (isInitialized) return;
    isInitialized = true;
    setClipChildren(false);
    mOnScrollStateChange = new OnScrollStateChange();
  }

  public void setBubbleTextCreator(BubbleTextCreator _bubbleTextCreator) {
    bubbleTextCreator = _bubbleTextCreator;
  }

  public void addOnScrollStateChangeListener(OnScrollStateChange.OnScrollStateChangeListener _onScrollStateChangeListener){
    mOnScrollStateChange.addListener(_onScrollStateChangeListener);
  }

  @Override protected void onFinishInflate() {
    super.onFinishInflate();
    if (!isInEditMode()){
      if (bubble != null) return;//Already inflated
      LayoutInflater inflater = LayoutInflater.from(getContext());
      inflater.inflate(R.layout.library_fast_scroller_layout, this, true);
      bubble = (TextView) findViewById(R.id.fast_scroller_bubble);
      if (bubble != null) bubble.setVisibility(INVISIBLE);
      handle = (ImageView) findViewById(R.id.fast_scroller_handle);
    }
  }


  /**
   * Layout customization<br/>
   * Color for Selected State is also customized by the user.
   *
   * @param accentColor Color for Selected state during touch and scrolling (usually accent color)
   */
  public void setAccentColor(int accentColor) {
    setBubbleAndHandleColor(accentColor);
  }

  private void setBubbleAndHandleColor(int accentColor) {
    //BubbleDrawable accentColor
    GradientDrawable bubbleDrawable =
        GradientDrawableHelper.getGradientDrawable(getContext(), R.drawable.fast_scroller_bubble);
    assert bubbleDrawable != null;
    bubbleDrawable.setColor(accentColor);
    setTextBackground(bubble, bubbleDrawable);

    //HandleDrawable accentColor
    StateListDrawable stateListDrawable =
        StateListDrawableHelper.getStateListDrawable(getContext(), R.drawable.fast_scroller_handle);
    GradientDrawable handleDrawable =
        (GradientDrawable) StateListDrawableHelper.getStateDrawable(stateListDrawable, 0);
    assert handleDrawable != null;
    handleDrawable.setColor(accentColor);
    handle.setImageDrawable(stateListDrawable);
  }

  @Override protected void onSizeChanged(int w, int h, int oldw, int oldh) {
    super.onSizeChanged(w, h, oldw, oldh);
    height = h;
  }

  @Override public boolean onTouchEvent(@NonNull MotionEvent event) {
    int action = event.getAction();
    switch (action) {
      case MotionEvent.ACTION_DOWN:
        if (event.getX() < handle.getX() - ViewCompat.getPaddingStart(handle)) return false;
        if (currentAnimator != null) currentAnimator.cancel();
        handle.setSelected(true);
        mOnScrollStateChange.onFastScrollerStateChange(true);
        showBubble();
      case MotionEvent.ACTION_MOVE:
        float y = event.getY();
        setBubbleAndHandlePosition(y);
        onScrollPosition(y);
        return true;
      case MotionEvent.ACTION_UP:
      case MotionEvent.ACTION_CANCEL:
        handle.setSelected(false);
        mOnScrollStateChange.onFastScrollerStateChange(false);
        hideBubble();
        return true;
    }
    return super.onTouchEvent(event);
  }

  void onScrollPosition(float _y) {

  }

  void setBubbleText(int _targetPos) {
    if (bubble != null) {
      String bubbleText = bubbleTextCreator.onCreateBubbleText(_targetPos);
      if (bubbleText != null) {
        bubble.setVisibility(View.VISIBLE);
        bubble.setText(bubbleText);
      } else {
        bubble.setVisibility(View.GONE);
      }
    }
  }

  void setBubbleAndHandlePosition(float y) {
    int handleHeight = handle.getHeight();
    handle.setY(getValueInRange(0, height - handleHeight, (int) (y - handleHeight / 2)));
    if (bubble != null) {
      int bubbleHeight = bubble.getHeight();
      bubble.setY(
          getValueInRange(0, height - bubbleHeight - handleHeight / 2, (int) (y - bubbleHeight)));
    }
  }

  private void showBubble() {
    if (currentAnimator != null) currentAnimator.cancel();
    currentAnimator = AnimatorUtil.to_alpha(bubble);
  }

  private void hideBubble() {
    if (null != currentAnimator) currentAnimator.cancel();
    currentAnimator = AnimatorUtil.alpha_to(bubble);
  }

}