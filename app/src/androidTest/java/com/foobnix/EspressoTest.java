package com.foobnix;

import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static org.hamcrest.Matchers.allOf;
import static org.junit.Assert.assertEquals;

import android.content.Context;

import androidx.test.core.app.ActivityScenario;
import androidx.test.espresso.Espresso;
import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;

import com.foobnix.utils.CustomMatchers;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class EspressoTest {
    @Rule
    public ActivityTestRule<com.foobnix.ui2.MainTabs2> activityRule =
            new ActivityTestRule<>(com.foobnix.ui2.MainTabs2.class);

    @Before
    public void setup() {
        ActivityScenario.launch(com.foobnix.ui2.MainTabs2.class);
    }

    @Test
    public void verifyCorrectAppPackage() {
        // Context of the app under test.
        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        assertEquals("com.foobnix.pro.pdf.reader", appContext.getPackageName());
    }

    @Test
    public void verifyLibraryTextIsDisplayedAndFocusable() {
        Espresso.onView(allOf(
                        ViewMatchers.withText("LIBRARY"),
                        CustomMatchers.withFocusable(true)
                ))
                .check(matches(ViewMatchers.isDisplayed()));
    }

    @After
    public void teardown() {
    }
}