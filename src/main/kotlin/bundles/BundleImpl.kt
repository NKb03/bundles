/**
 *@author Nikolaus Knop
 */

package bundles

import reaktive.event.EventStream
import reaktive.event.event
import reaktive.impl.WeakReactive
import reaktive.value.*

internal class BundleImpl : Bundle {
    private val change = event<BundleChange<*>>()
    override val changed: EventStream<BundleChange<*>>
        get() = change.stream
    private val properties = mutableMapOf<Property<*, *, *>, Any?>()
    private val managed = mutableMapOf<ReactiveProperty<*, *, *>, WeakReactive<ReactiveVariable<Any?>>>()

    override val entries: Sequence<Pair<Property<*, *, *>, Any?>>
        get() = properties.entries.asSequence().map { it.toPair() }

    override fun <Read : Any> hasProperty(permission: Read, property: Property<*, Read, *>): Boolean =
        property in properties

    override fun hasProperty(property: Property<*, Any, *>): Boolean = hasProperty(Any(), property)

    @Suppress("UNCHECKED_CAST")
    override fun <Read : Any, T> get(permission: Read, property: Property<out T, Read, *>): T {
        val value = properties[property] as T?
        return value ?: property.default
    }

    override fun <Read : Any, T> get(
        permission: Read,
        property: ReactiveProperty<out T, Read, *>
    ): ReactiveValue<T> {
        val value = get(permission, property as Property<out T, Read, *>)
        return getReactive(property, value)
    }

    override fun <T> get(property: Property<out T, Any, *>): T = get(Any(), property)

    override fun <T> get(property: ReactiveProperty<out T, Any, *>): ReactiveValue<T> {
        val value = get(property as Property<out T, Any, *>)
        return getReactive(property, value)
    }

    @Suppress("UNCHECKED_CAST")
    override fun <Write : Any, T> set(write: Write, property: Property<in T, *, Write>, value: T) {
        val old = properties[property]
        properties[property] = value
        if (property is ReactiveProperty) {
            change.fire(BundleChange(this, property, old as T?, value))
            managed[property]?.reactive?.set(value)
        }
    }

    override fun <T> set(property: Property<in T, *, Any>, value: T) {
        set(Any(), property, value)
    }

    @Suppress("UNCHECKED_CAST")
    private fun <Read : Any, T> getReactive(
        property: ReactiveProperty<out T, Read, *>,
        value: T
    ): ReactiveValue<T> {
        val reactive = managed[property]?.reactive
        return if (reactive == null) {
            val new = reactiveVariable(value)
            managed[property] = new.weak as WeakReactive<ReactiveVariable<Any?>>
            new
        } else reactive as ReactiveValue<T>
    }

    override fun <Read : Any, Write : Read> delete(permission: Write, property: Property<*, Read, Write>) {
        properties.remove(property) ?: throw NoSuchElementException("Cannot delete $property")
        if (property is ReactiveProperty) {
            val v = managed[property]?.reactive
            if (v != null) {
                val default = property.default
                v.set(default)
            }
        }
    }

    override fun delete(property: Property<*, Any, Any>) {
        delete(Any(), property)
    }
}