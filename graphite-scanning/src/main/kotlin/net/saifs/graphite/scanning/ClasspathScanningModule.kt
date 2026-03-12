package net.saifs.graphite.scanning

import io.github.classgraph.ClassGraph
import net.saifs.graphite.di.InjectorContext
import net.saifs.graphite.di.annotation.Named
import net.saifs.graphite.di.dependency.BindingDeclaration
import net.saifs.graphite.di.module.Module
import net.saifs.graphite.scanning.annotation.Component
import net.saifs.graphite.scanning.annotation.Factory
import net.saifs.graphite.scanning.annotation.Primary
import kotlin.reflect.KClass
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.memberFunctions

class ClasspathScanningModule(private vararg val basePackages: String) : Module {
    override fun InjectorContext.configure() {
        ClassGraph()
            .acceptPackages(*basePackages)
            .enableClassInfo()
            .enableAnnotationInfo()
            .scan()
            .use { result ->
                val components = result.getClassesWithAnnotation(Component::class.java)
                components.forEach { component ->
                    val klass = component.loadClass().kotlin
                    val primary = klass.findAnnotation<Primary>() != null
                    declarations += BindingDeclaration.TypeBinding(klass, primary)

                    klass.memberFunctions
                        .filter { it.findAnnotation<Factory>() != null }
                        .forEach { method ->
                            val returnType = method.returnType.classifier as? KClass<*>
                                ?: throw IllegalStateException("Factory method ${method.name} in ${klass.qualifiedName} does not have a valid return type")
                            val factoryPrimary = method.findAnnotation<Primary>() != null
                            val factoryNamed = method.findAnnotation<Named>()?.value

                            declarations += if (factoryNamed != null) {
                                BindingDeclaration.FactoryNamedBinding(factoryNamed, returnType, method, factoryPrimary)
                            } else {
                                BindingDeclaration.FactoryTypeBinding(returnType, method, factoryPrimary)
                            }
                        }
                }
            }
    }
}