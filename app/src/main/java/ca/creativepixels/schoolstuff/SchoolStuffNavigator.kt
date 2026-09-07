package ca.creativepixels.schoolstuff

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

internal sealed interface SchoolStuffDestination {
    data object Main : SchoolStuffDestination
    data class Child(val childId: String) : SchoolStuffDestination
    data class Gallery(val childId: String) : SchoolStuffDestination
    data object AddThing : SchoolStuffDestination
}

/**
 * Owns School Stuff's in-app history independently of the composable tree.
 *
 * MainActivity registers its Android back callback against [canNavigateBack], so a system back
 * gesture is claimed before it can finish the Activity whenever an internal screen is visible.
 */
internal class SchoolStuffNavigator {
    private val backStack = ArrayDeque<SchoolStuffDestination>()
    private var backAvailabilityListener: ((Boolean) -> Unit)? = null

    var currentDestination by mutableStateOf<SchoolStuffDestination>(SchoolStuffDestination.Main)
        private set

    val canNavigateBack: Boolean
        get() = backStack.isNotEmpty()

    fun setBackAvailabilityListener(listener: (Boolean) -> Unit) {
        backAvailabilityListener = listener
        listener(canNavigateBack)
    }

    fun openChild(childId: String) {
        navigateTo(SchoolStuffDestination.Child(childId))
    }

    fun openAddThing() {
        navigateTo(SchoolStuffDestination.AddThing)
    }

    fun openGallery(childId: String) {
        navigateTo(SchoolStuffDestination.Gallery(childId))
    }

    fun navigateBack(): Boolean {
        val previous = backStack.removeLastOrNull() ?: return false
        currentDestination = previous
        notifyBackAvailability()
        return true
    }

    private fun navigateTo(destination: SchoolStuffDestination) {
        if (destination == currentDestination) return
        backStack.addLast(currentDestination)
        currentDestination = destination
        notifyBackAvailability()
    }

    private fun notifyBackAvailability() {
        backAvailabilityListener?.invoke(canNavigateBack)
    }
}
