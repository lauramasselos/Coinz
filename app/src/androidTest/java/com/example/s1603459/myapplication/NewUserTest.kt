@file:Suppress("DEPRECATION")

package com.example.s1603459.myapplication


import android.view.View
import android.view.ViewGroup
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.filters.LargeTest
import androidx.test.rule.ActivityTestRule
import androidx.test.runner.AndroidJUnit4
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.Matchers.allOf
import org.hamcrest.TypeSafeMatcher
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class NewUserTest {

    @Rule
    @JvmField
    var mActivityTestRule = ActivityTestRule(LaunchActivity::class.java)

    @Test
    fun newUserActivityTest() {
        // Added a sleep statement to match the app's execution delay.
        // The recommended way to handle such scenarios is to use Espresso idling resources:
        // https://google.github.io/android-testing-support-library/docs/espresso/idling-resource/index.html
        Thread.sleep(7000)

        val appCompatButton = onView(
                allOf(withId(R.id.startButton), withText("Start"),
                        childAtPosition(
                                childAtPosition(
                                        withId(android.R.id.content),
                                        0),
                                0),
                        isDisplayed()))
        appCompatButton.perform(click())

        // Added a sleep statement to match the app's execution delay.
        // The recommended way to handle such scenarios is to use Espresso idling resources:
        // https://google.github.io/android-testing-support-library/docs/espresso/idling-resource/index.html
        Thread.sleep(7000)

        val appCompatButton2 = onView(
                allOf(withId(R.id.btn_register_account), withText("Create Account"),
                        childAtPosition(
                                allOf(withId(R.id.main_layout),
                                        childAtPosition(
                                                withId(android.R.id.content),
                                                0)),
                                4),
                        isDisplayed()))
        appCompatButton2.perform(click())

        // Added a sleep statement to match the app's execution delay.
        // The recommended way to handle such scenarios is to use Espresso idling resources:
        // https://google.github.io/android-testing-support-library/docs/espresso/idling-resource/index.html
        Thread.sleep(7000)

        val appCompatEditText = onView(
                allOf(withId(R.id.et_first_name),
                        childAtPosition(
                                allOf(withId(R.id.coordinatorLayout_register),
                                        childAtPosition(
                                                withId(android.R.id.content),
                                                0)),
                                0),
                        isDisplayed()))
        appCompatEditText.perform(click())

        val appCompatEditText2 = onView(
                allOf(withId(R.id.et_first_name),
                        childAtPosition(
                                allOf(withId(R.id.coordinatorLayout_register),
                                        childAtPosition(
                                                withId(android.R.id.content),
                                                0)),
                                0),
                        isDisplayed()))
        appCompatEditText2.perform(replaceText("Test"), closeSoftKeyboard())

        val appCompatEditText3 = onView(
                allOf(withId(R.id.et_first_name), withText("Test"),
                        childAtPosition(
                                allOf(withId(R.id.coordinatorLayout_register),
                                        childAtPosition(
                                                withId(android.R.id.content),
                                                0)),
                                0),
                        isDisplayed()))
        appCompatEditText3.perform(pressImeActionButton())

        val appCompatEditText4 = onView(
                allOf(withId(R.id.et_last_name),
                        childAtPosition(
                                allOf(withId(R.id.coordinatorLayout_register),
                                        childAtPosition(
                                                withId(android.R.id.content),
                                                0)),
                                1),
                        isDisplayed()))
        appCompatEditText4.perform(replaceText("User"), closeSoftKeyboard())

        val appCompatEditText5 = onView(
                allOf(withId(R.id.et_last_name), withText("User"),
                        childAtPosition(
                                allOf(withId(R.id.coordinatorLayout_register),
                                        childAtPosition(
                                                withId(android.R.id.content),
                                                0)),
                                1),
                        isDisplayed()))
        appCompatEditText5.perform(pressImeActionButton())

        val appCompatEditText6 = onView(
                allOf(withId(R.id.et_email),
                        childAtPosition(
                                allOf(withId(R.id.coordinatorLayout_register),
                                        childAtPosition(
                                                withId(android.R.id.content),
                                                0)),
                                2),
                        isDisplayed()))
        appCompatEditText6.perform(replaceText("test@user.com"), closeSoftKeyboard())

        val appCompatEditText7 = onView(
                allOf(withId(R.id.et_email), withText("test@user.com"),
                        childAtPosition(
                                allOf(withId(R.id.coordinatorLayout_register),
                                        childAtPosition(
                                                withId(android.R.id.content),
                                                0)),
                                2),
                        isDisplayed()))
        appCompatEditText7.perform(pressImeActionButton())

        val appCompatEditText8 = onView(
                allOf(withId(R.id.et_password),
                        childAtPosition(
                                allOf(withId(R.id.coordinatorLayout_register),
                                        childAtPosition(
                                                withId(android.R.id.content),
                                                0)),
                                3),
                        isDisplayed()))
        appCompatEditText8.perform(replaceText("abc123"), closeSoftKeyboard())

        val appCompatButton3 = onView(
                allOf(withId(R.id.btn_register), withText("Create Account"),
                        childAtPosition(
                                allOf(withId(R.id.coordinatorLayout_register),
                                        childAtPosition(
                                                withId(android.R.id.content),
                                                0)),
                                4),
                        isDisplayed()))
        appCompatButton3.perform(click())
    }

    private fun childAtPosition(
            parentMatcher: Matcher<View>, position: Int): Matcher<View> {

        return object : TypeSafeMatcher<View>() {
            override fun describeTo(description: Description) {
                description.appendText("Child at position $position in parent ")
                parentMatcher.describeTo(description)
            }

            public override fun matchesSafely(view: View): Boolean {
                val parent = view.parent
                return parent is ViewGroup && parentMatcher.matches(parent)
                        && view == parent.getChildAt(position)
            }
        }
    }
}
