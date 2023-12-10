package com.foobnix.utils;

import android.view.View;
import androidx.test.espresso.matcher.BoundedMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;

public class CustomMatchers {
    public static Matcher<View> withFocusable(final boolean focusable) {
        return new BoundedMatcher<View, View>(View.class) {
            @Override
            public void describeTo(Description description) {
                description.appendText("has-focusable=" + focusable);
            }

            @Override
            protected boolean matchesSafely(View view) {
                return view.isFocusable() == focusable;
            }
        };
    }
}
