package ca.creativepixels.schoolstuff

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SchoolStuffNavigatorTest {
    @Test
    fun nestedScreensNavigateBackOneLevelAtATime() {
        val navigator = SchoolStuffNavigator()

        navigator.openChild("child-1")
        navigator.openAddThing()

        assertEquals(SchoolStuffDestination.AddThing, navigator.currentDestination)
        assertTrue(navigator.navigateBack())
        assertEquals(SchoolStuffDestination.Child("child-1"), navigator.currentDestination)
        assertTrue(navigator.navigateBack())
        assertEquals(SchoolStuffDestination.Main, navigator.currentDestination)
        assertFalse(navigator.navigateBack())
    }

    @Test
    fun backAvailabilityChangesBeforeAndAfterInternalNavigation() {
        val navigator = SchoolStuffNavigator()
        val availability = mutableListOf<Boolean>()
        navigator.setBackAvailabilityListener(availability::add)

        navigator.openChild("child-2")
        navigator.navigateBack()

        assertEquals(listOf(false, true, false), availability)
    }

    @Test
    fun galleryIsASeparateChildLevelDestination() {
        val navigator = SchoolStuffNavigator()

        navigator.openChild("child-3")
        navigator.openGallery("child-3")

        assertEquals(SchoolStuffDestination.Gallery("child-3"), navigator.currentDestination)
        assertTrue(navigator.navigateBack())
        assertEquals(SchoolStuffDestination.Child("child-3"), navigator.currentDestination)
        assertTrue(navigator.navigateBack())
        assertEquals(SchoolStuffDestination.Main, navigator.currentDestination)
    }
}
