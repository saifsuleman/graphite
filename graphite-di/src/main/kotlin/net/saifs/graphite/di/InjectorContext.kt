package net.saifs.graphite.di

import net.saifs.graphite.di.annotation.Named
import net.saifs.graphite.di.dependency.BindingDeclaration
import kotlin.reflect.KFunction
import kotlin.reflect.full.findAnnotation

class InjectorContext {
    val declarations = mutableListOf<BindingDeclaration>()

    inline fun <reified T : Any> bind(factory: KFunction<T>, primary: Boolean = false) {
        val named = factory.findAnnotation<Named>()
        declarations += if (named != null) {
            BindingDeclaration.FactoryNamedBinding(named.value, T::class, factory, primary)
        } else {
            BindingDeclaration.FactoryTypeBinding(T::class, factory, primary)
        }
    }

    inline fun <reified T : Any> bind(primary: Boolean = false) {
        declarations += BindingDeclaration.TypeBinding(T::class, primary)
    }

    inline fun <reified T : Any> bind(instance: T, primary: Boolean = false) {
        declarations += BindingDeclaration.InstanceBinding(T::class, instance, primary)
    }

    fun <T : Any> bind(name: String, instance: T, primary: Boolean = false) {
        declarations += BindingDeclaration.NamedBinding(name, instance, primary)
    }
}