package io.github.readwell.domain

/**
 * Fixed-capacity log implemented as a circular buffer. Old history is dropped
 * once the capacity is reached without growing memory indefinitely.
 */
class ActivityRing(
    initial: Collection<Activity> = emptyList(),
    private val capacity: Int = DEFAULT_CAPACITY,
) {
    private val buffer = arrayOfNulls<Activity>(capacity)
    private var start = 0
    private var itemCount = 0

    init {
        require(capacity > 0) { "Activity capacity must be positive." }
        initial.toList().takeLast(capacity).forEach(::add)
    }

    fun add(activity: Activity) {
        val insertionIndex = (start + itemCount) % capacity
        buffer[insertionIndex] = activity
        if (itemCount < capacity) {
            itemCount += 1
        } else {
            start = (start + 1) % capacity
        }
    }

    fun newestFirst(): List<Activity> =
        List(itemCount) { offset ->
            val index = (start + itemCount - 1 - offset) % capacity
            requireNotNull(buffer[index])
        }

    fun oldestFirst(): List<Activity> = newestFirst().asReversed()

    companion object {
        const val DEFAULT_CAPACITY = 200
    }
}
