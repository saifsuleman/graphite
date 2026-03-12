package net.saifs.graphite.di.annotation

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
annotation class Named(val value: String)