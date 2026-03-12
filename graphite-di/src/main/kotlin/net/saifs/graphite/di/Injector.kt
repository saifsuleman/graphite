package net.saifs.graphite.di

import net.saifs.graphite.di.dependency.Dependency
import net.saifs.graphite.di.dependency.DependencyGraph
import net.saifs.graphite.di.dependency.DependencyNode
import net.saifs.graphite.di.module.Module
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.typeOf

@Suppress("UNCHECKED_CAST")
class Injector private constructor(
    private val parent: Injector?,
    private val modules: List<Module>,
) {
    private val namedInstances: MutableMap<String, Pair<Any, Boolean>> = LinkedHashMap()
    private val multiInstances: MutableMap<KClass<*>, MutableList<Pair<Any, Boolean>>> = mutableMapOf()

    init {
        register(this, primary = true)
        modules.forEach { register(it, false) }

        val context = InjectorContext().also { context ->
            modules.forEach { it.run { context.configure() } }
        }

        val graph = DependencyGraph.build(context.declarations, parent)

        graph.nodes.values
            .filterIsInstance<DependencyNode.Prebuilt>()
            .forEach { node ->
                register(node.instance, node.primary)
            }

        for (node in graph.resolve()) {
            runCatching {
                construct(node)
            }
                .onSuccess {
                    if (node.name != null) {
                        register(node.name, it, node.primary)
                    } else {
                        register(it, node.primary)
                    }
                }
                .onFailure {
                    throw IllegalStateException("Failed to construct '${node.type.qualifiedName}' required by '${node.type.qualifiedName}'", it)
                }
        }
    }

    private fun construct(node: DependencyNode.Constructable): Any {
        val args = arrayOfNulls<Any>(node.dependencies.size)

        for ((i, dep) in node.dependencies.withIndex()) {
            args[i] = when (dep) {
                is Dependency.Typed -> lookup(dep.type)
                    ?: if (dep.optional) {
                        null
                    } else {
                        throw IllegalStateException("No binding for '${dep.type.qualifiedName}' required by '${node.type.qualifiedName}'")
                    }
                is Dependency.Named -> lookup(dep.name)
                    ?: if (dep.optional) {
                        null
                    } else {
                        throw IllegalStateException("No binding for '${dep.name}' required by '${node.type.qualifiedName}'")
                    }
            }
        }

        return node.constructor.call(*args) as Any
    }

    private fun register(name: String, instance: Any, primary: Boolean) {
        namedInstances[name] = instance to primary
        register(instance, primary)
    }

    private fun register(instance: Any, primary: Boolean) {
        if (instance::class in multiInstances) {
            throw IllegalStateException("Duplicate binding for '${instance::class.qualifiedName}' in the same injector")
        }

        multiInstances.computeIfAbsent(instance::class) { mutableListOf() }.add(instance to primary)
        instance::class.allSuperTypes().forEach { superType ->
            multiInstances.computeIfAbsent(superType) { mutableListOf() }.add(instance to primary)
        }
    }

    fun <T : Any> lookup(name: String): T? =
        (namedInstances[name]?.first ?: parent?.lookup(name)) as T?

    fun <T : Any> lookup(type: KClass<*>): T? {
        val candidates = getCandidates(type)
        return when (candidates.size) {
            0 -> null
            1 -> candidates.single().first as T
            else -> candidates.singleOrNull { it.second }?.first as T?
                ?: throw IllegalStateException("Multiple bindings found for '${type.qualifiedName}' and no primary specified")
        }
    }

    fun <T : Any> inject(type: KType): T {
        if (type.classifier == List::class) {
            val parameter = type.arguments.single().type
                ?: throw IllegalStateException("Attempted to resolve star-projected collection")
            return getCandidates(parameter.classifier as KClass<*>).toList() as T
        }

        val classifier = type.classifier as? KClass<*>
            ?: throw IllegalStateException("Unsupported type $type for injection")
        return lookup(classifier) ?: throw IllegalStateException("No binding found for '${type}'")
    }

    inline fun <reified T : Any> inject(): T = inject(typeOf<T>())

    private fun getCandidates(type: KClass<*>): List<Pair<Any, Boolean>> =
        (multiInstances[type] ?: emptyList()) + (parent?.getCandidates(type) ?: emptyList())

    fun child(vararg modules: Module): Injector =
        Injector(this, modules.toList())

    companion object {
        fun create(vararg modules: Module): Injector =
            Injector(null, modules.toList())
    }
}

internal fun KClass<*>.allSuperTypes(): Set<KClass<*>> {
    val result = mutableSetOf<KClass<*>>()

    fun collect(kClass: KClass<*>) {
        for (supertype in kClass.supertypes) {
            val classifier = supertype.classifier
            if (classifier is KClass<*> && result.add(classifier)) {
                collect(classifier)
            }
        }
    }

    collect(this)

    return result
}