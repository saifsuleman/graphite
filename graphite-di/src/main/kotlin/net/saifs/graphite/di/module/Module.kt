package net.saifs.graphite.di.module

import net.saifs.graphite.di.InjectorContext

interface Module {
    fun InjectorContext.configure()
}