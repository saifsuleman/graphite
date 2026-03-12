package net.saifs.graphite.di.dependency

import kotlin.reflect.KClass
import kotlin.reflect.KFunction

sealed interface DependencyNode {
    val type: KClass<*>
    val primary: Boolean

    data class Constructable(
        override val type: KClass<*>,
        val constructor: KFunction<*>,
        val dependencies: List<Dependency>,
        override val primary: Boolean,
        val name: String? = null,
    ) : DependencyNode

    data class Prebuilt(
        override val type: KClass<*>,
        val instance: Any,
        override val primary: Boolean,
    ) : DependencyNode
}

