package net.saifs.graphite.di.dependency

import kotlin.reflect.KClass

sealed interface Dependency {
    val optional: Boolean

    data class Typed(
        val type: KClass<*>,
        override val optional: Boolean
    ) : Dependency

    data class Named(
        val name: String,
        override val optional: Boolean
    ) : Dependency

    data class Collection(
        val type: KClass<*>,
        override val optional: Boolean
    ) : Dependency

    data class LazyTyped(
        val type: KClass<*>,
        override val optional: Boolean
    ) : Dependency

    data class LazyNamed(
        val name: String,
        override val optional: Boolean
    ) : Dependency
}