package fr.inria.tyrex.senslogs.ui.utils.transitions;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.transition.Transition;
import android.transition.TransitionValues;
import android.util.Property;
import android.util.TypedValue;
import android.view.ViewGroup;
import android.widget.TextView;


/**
 * Custom Text Size transition
 * https://github.com/alexjlockwood/custom-lollipop-transitions/blob/master/app/src/main/java/com/alexjlockwood/transitions/custom/TextSizeTransition.java
 */
public class TextSizeTransition extends Transition {
    private static final String PROPNAME_TEXT_SIZE = "alexjlockwood:transition:textsize";
    private static final String[] TRANSITION_PROPERTIES = { PROPNAME_TEXT_SIZE };

    private static final Property<TextView, Float> TEXT_SIZE_PROPERTY =
            new Property<TextView, Float>(Float.class, "textSize") {
                @Override
                public Float get(TextView textView) {
                    return textView.getTextSize();
                }

                @Override
                public void set(TextView textView, Float textSizePixels) {
                    textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSizePixels);
                }
            };
    

    @Override
    public String[] getTransitionProperties() {
        return TRANSITION_PROPERTIES;
    }

    @Override
    public void captureStartValues(TransitionValues transitionValues) {
        captureValues(transitionValues);
    }

    @Override
    public void captureEndValues(TransitionValues transitionValues) {
        captureValues(transitionValues);
    }

    private void captureValues(TransitionValues transitionValues) {
        if (transitionValues.view instanceof TextView) {
            TextView textView = (TextView) transitionValues.view;
            transitionValues.values.put(PROPNAME_TEXT_SIZE, textView.getTextSize());
        }
    }

    @Override
    public Animator createAnimator(ViewGroup sceneRoot, TransitionValues startValues, TransitionValues endValues) {
        if (startValues == null || endValues == null) {
            return null;
        }

        Float startSize = (Float) startValues.values.get(PROPNAME_TEXT_SIZE);
        Float endSize = (Float) endValues.values.get(PROPNAME_TEXT_SIZE);
        if (startSize == null || endSize == null || startSize.floatValue() == endSize.floatValue()) {
            return null;
        }

        TextView view = (TextView) endValues.view;
        view.setTextSize(TypedValue.COMPLEX_UNIT_PX, startSize);
        return ObjectAnimator.ofFloat(view, TEXT_SIZE_PROPERTY, startSize, endSize);
    }
}