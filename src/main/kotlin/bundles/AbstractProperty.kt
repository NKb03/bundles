/**
 *@author Nikolaus Knop
 */

package bundles

internal abstract class AbstractProperty<T : Any, in P : Permission> : Property<T, P> {
    override fun equals(other: Any?): Boolean = when {
        this === other                   -> true
        other !is Property<*, *> -> false
        this.name != other.name          -> false
        else                             -> true
    }

    override fun hashCode(): Int = name.hashCode()
}