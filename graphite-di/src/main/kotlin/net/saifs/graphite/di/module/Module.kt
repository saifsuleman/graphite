package net.saifs.graphite.di.module

import net.saifs.graphite.di.InjectorContext

fun interface Module {
    fun InjectorContext.configure()
}