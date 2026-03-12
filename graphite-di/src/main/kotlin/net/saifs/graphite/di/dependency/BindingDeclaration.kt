package net.saifs.graphite.di.dependency

import kotlin.reflect.KClass
import kotlin.reflect.KFunction

sealed interface BindingDeclaration {
    val primary: Boolean

    data class FactoryTypeBinding(val type: KClass<*>, val factory: KFunction<*>, override val primary: Boolean) : BindingDeclaration
    data class FactoryNamedBinding(val name: String, val type: KClass<*>, val factory: KFunction<*>, override val primary: Boolean) : BindingDeclaration
    data class TypeBinding(val type: KClass<*>, override val primary: Boolean) : BindingDeclaration
    data class InstanceBinding(val type: KClass<*>, val instance: Any, override val primary: Boolean) : BindingDeclaration
    data class NamedBinding(val name: String, val instance: Any, override val primary: Boolean) : BindingDeclaration
}