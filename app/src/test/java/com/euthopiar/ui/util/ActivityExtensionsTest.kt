package com.euthopiar.ui.util

import android.app.Activity
import android.content.ContextWrapper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ActivityExtensionsTest {

    @Test
    fun testFindActivityDirect() {
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        assertEquals(activity, activity.findActivity())
    }

    @Test
    fun testFindActivitySingleWrapper() {
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        val wrapped = ContextWrapper(activity)
        assertEquals(activity, wrapped.findActivity())
    }

    @Test
    fun testFindActivityNestedWrapper() {
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        val wrapped1 = ContextWrapper(activity)
        val wrapped2 = ContextWrapper(wrapped1)
        val wrapped3 = ContextWrapper(wrapped2)
        assertEquals(activity, wrapped3.findActivity())
    }

    @Test
    fun testFindActivityNonActivityContextReturnsNull() {
        val appContext = RuntimeEnvironment.getApplication()
        assertNull(appContext.findActivity())
    }
}
