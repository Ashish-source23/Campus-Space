package com.example.campusspace.ui

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.widget.FrameLayout

/**
 * A custom FrameLayout that intercepts touch events.
 * This is used to wrap the MapView and prevent a ViewPager
 * or NestedScrollView from "stealing" the swipe gesture.
 */
class TouchableWrapper @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        when (ev.action) {
            MotionEvent.ACTION_DOWN ->
                // Tell the parent (ViewPager/NestedScrollView) to NOT intercept this gesture.
                parent.requestDisallowInterceptTouchEvent(true)

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL ->
                // Release the "lock" so the parent can swipe/scroll again.
                parent.requestDisallowInterceptTouchEvent(false)
        }
        // Let the child (the MapView) handle the event.
        return super.dispatchTouchEvent(ev)
    }
}