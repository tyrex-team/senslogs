package fr.inria.tyrex.senslogs.ui.utils.transitions;


import android.annotation.TargetApi;
import android.app.Activity;
import android.app.SharedElementCallback;
import android.content.res.Resources;
import android.os.Build;
import android.support.v4.util.Pair;
import android.transition.Transition;
import android.transition.TransitionSet;
import android.util.TypedValue;
import android.view.View;
import android.widget.TextView;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Handle enter shared transitions for text size
 * http://stackoverflow.com/questions/26599824/how-can-i-scale-textviews-using-shared-element-transitions/33914006#33914006
 */
public class EnterSharedElementTextSizeHandler extends SharedElementCallback {

    private final TransitionSet mTransitionSet;
    private final Activity mActivity;

    public Map<TextView, Pair<Integer, Integer>> textViewList = new HashMap<>();


    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public EnterSharedElementTextSizeHandler(Activity activity) {

        mActivity = activity;

        Transition transitionWindow = activity.getWindow().getSharedElementEnterTransition();

        if (!(transitionWindow instanceof TransitionSet)) {
            mTransitionSet = new TransitionSet();
            mTransitionSet.addTransition(transitionWindow);
        } else {
            mTransitionSet = (TransitionSet) transitionWindow;
        }

        activity.setEnterSharedElementCallback(this);

    }


    public void addTextViewSizeResource(TextView tv, int sizeBegin, int sizeEnd) {

        Resources res = mActivity.getResources();
        addTextView(tv,
                res.getDimensionPixelSize(sizeBegin),
                res.getDimensionPixelSize(sizeEnd));
    }

    public void addTextView(TextView tv, int sizeBegin, int sizeEnd) {

        Transition textSize = new TextSizeTransition();
        textSize.addTarget(tv.getId());
        textSize.addTarget(tv.getText().toString());
        mTransitionSet.addTransition(textSize);

        textViewList.put(tv, new Pair<>(sizeBegin, sizeEnd));
    }

    @Override
    public void onSharedElementStart(List<String> sharedElementNames, List<View> sharedElements, List<View> sharedElementSnapshots) {

        for (View v : sharedElements) {

            if (!textViewList.containsKey(v)) {
                continue;
            }

            ((TextView) v).setTextSize(TypedValue.COMPLEX_UNIT_PX, textViewList.get(v).first);
        }
    }

    @Override
    public void onSharedElementEnd(List<String> sharedElementNames, List<View> sharedElements, List<View> sharedElementSnapshots) {
        for (View v : sharedElements) {

            if (!textViewList.containsKey(v)) {
                continue;
            }

            TextView textView = (TextView) v;

            // Record the TextView's old width/height.
            int oldWidth = textView.getMeasuredWidth();
            int oldHeight = textView.getMeasuredHeight();

            // Setup the TextView's end values.
            textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, textViewList.get(v).second);

            // Re-measure the TextView (since the text size has changed).
            int widthSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
            int heightSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
            textView.measure(widthSpec, heightSpec);

            // Record the TextView's new width/height.
            int newWidth = textView.getMeasuredWidth();
            int newHeight = textView.getMeasuredHeight();

            // Layout the TextView in the center of its container, accounting for its new width/height.
            int widthDiff = newWidth - oldWidth;
            int heightDiff = newHeight - oldHeight;
            textView.layout(textView.getLeft() - widthDiff / 2, textView.getTop() - heightDiff / 2,
                    textView.getRight() + widthDiff / 2, textView.getBottom() + heightDiff / 2);
        }
    }
}
