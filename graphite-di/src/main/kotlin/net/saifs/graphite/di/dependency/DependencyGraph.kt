package net.saifs.graphite.di.dependency

import net.saifs.graphite.di.Injector
import net.saifs.graphite.di.allSuperTypes
import net.saifs.graphite.di.annotation.Inject
import net.saifs.graphite.di.annotation.Named
import kotlin.collections.iterator
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.primaryConstructor

class DependencyGraph private constructor(
    val nodes: Map<KClass<*>, DependencyNode>,
    val named: Map<String, DependencyNode>,
) {
    companion object {
        fun build(
            declarations: List<BindingDeclaration>,
            parent: Injector?,
        ): DependencyGraph {
            val nodes = LinkedHashMap<KClass<*>, DependencyNode>()
            val named = LinkedHashMap<String, DependencyNode>()

            for (declaration in declarations) {
                when (declaration) {
                    is BindingDeclaration.InstanceBinding -> {
                        conflict(declaration.type, nodes[declaration.type], parent?.lookup(declaration.type))
                        nodes[declaration.type] = DependencyNode.Prebuilt(declaration.type, declaration.instance, declaration.primary)
                    }
                    is BindingDeclaration.NamedBinding -> {
                        conflict(declaration.name, named[declaration.name], parent?.lookup(declaration.name))
                        val node = DependencyNode.Prebuilt(declaration.instance::class, declaration.instance, declaration.primary)
                        nodes[declaration.instance::class] = node
                        named[declaration.name] = node
                    }
                    is BindingDeclaration.TypeBinding -> {
                        conflict(declaration.type, nodes[declaration.type], parent?.lookup(declaration.type))

                        val constructor = pickConstructor(declaration.type)
                        val dependencies = constructor.parameters.map { resolveDependency(it, declaration.type) }

                        nodes[declaration.type] = DependencyNode.Constructable(
                            type = declaration.type,
                            constructor = constructor,
                            dependencies = dependencies,
                            primary = declaration.primary
                        )
                    }
                    is BindingDeclaration.FactoryTypeBinding -> {
                        conflict(declaration.type, nodes[declaration.type], parent?.lookup(declaration.type))

                        val factoryFunction = declaration.factory
                        val dependencies = factoryFunction.parameters.map { resolveDependency(it, declaration.type) }

                        nodes[declaration.type] = DependencyNode.Constructable(
                            type = declaration.type,
                            constructor = factoryFunction,
                            dependencies = dependencies,
                            primary = declaration.primary
                        )
                    }
                    is BindingDeclaration.FactoryNamedBinding -> {
                        conflict(declaration.name, named[declaration.name], parent?.lookup(declaration.name))

                        val factoryFunction = declaration.factory
                        val dependencies = factoryFunction.parameters.map { resolveDependency(it, declaration.type) }

                        named[declaration.name] = DependencyNode.Constructable(
                            type = declaration.type,
                            constructor = factoryFunction,
                            dependencies = dependencies,
                            primary = declaration.primary,
                            name = declaration.name,
                        )
                    }
                }
            }

            return DependencyGraph(nodes, named)
        }

        private fun resolveDependency(parameter: KParameter, ownerType: KClass<*>): Dependency {
            parameter.findAnnotation<Named>()?.let { return Dependency.Named(it.value, parameter.isOptional) }
            val type = parameter.type.classifier as? KClass<*>
                ?: throw IllegalStateException("Unsupported parameter type ${parameter.type} for ${ownerType.qualifiedName}")
            return Dependency.Typed(type, parameter.isOptional)
        }

        private fun conflict(key: Any, local: Any?, parentHas: Any?) {
            if (local != null) {
                throw IllegalStateException("Duplicate binding for '$key' in the same injector")
            }

            if (parentHas != null) {
                throw IllegalStateException("Binding for '$key' already exists in a parent injector")
            }
        }

        private fun <T : Any> pickConstructor(type: KClass<T>): KFunction<T> {
            val tagged = type.constructors.filter { it.findAnnotation<Inject>() != null }
            return when (tagged.size) {
                1 -> tagged.single()
                0 -> type.primaryConstructor
                    ?: type.constructors.singleOrNull()
                    ?: throw IllegalStateException("No suitable constructor found for ${type.qualifiedName}")
                else -> throw IllegalStateException("Multiple constructors annotated with @Inject found for ${type.qualifiedName}")
            }
        }
    }


    fun resolve(): List<DependencyNode.Constructable> {
        val constructableNodes = nodes.values.filterIsInstance<DependencyNode.Constructable>()
        val constructableNamed = named.values.filterIsInstance<DependencyNode.Constructable>()

        val nodesByType = buildMap {
            val byType = mutableMapOf<KClass<*>, MutableList<DependencyNode.Constructable>>()

            constructableNodes.forEach { node ->
                byType.computeIfAbsent(node.type) { mutableListOf() }.add(node)
                node.type.allSuperTypes().forEach { superType ->
                    byType.computeIfAbsent(superType) { mutableListOf() }.add(node)
                }
            }

            byType.forEach { (type, candidates) ->
                this[type] = when (candidates.size) {
                    1 -> candidates.single()
                    else -> candidates.singleOrNull { it.primary }
                        ?: candidates.firstOrNull()
                        ?: throw IllegalStateException("Multiple bindings found for '${type.qualifiedName}' but no primary candidate could be determined")
                }
            }
        }

        val nodesByName = constructableNamed.associateBy { it.name!! }

        val sorted = mutableListOf<DependencyNode.Constructable>()
        val visited = mutableSetOf<SortingKey>()
        val stack = mutableSetOf<SortingKey>()

        fun visit(node: DependencyNode.Constructable, key: SortingKey, path: List<SortingKey>) {
            if (key in visited) return

            if (key in stack) {
                val cycle = (path + key).joinToString(" → ") { it.toString() }
                throw IllegalStateException("Cyclic dependency detected: $cycle")
            }

            stack += key

            for (dependency in node.dependencies) {
                when (dependency) {
                    is Dependency.Typed -> nodesByType[dependency.type]?.let { visit(it, SortingKey.Typed(it.type), path + key) }
                    is Dependency.Named -> nodesByName[dependency.name]?.let { visit(it, SortingKey.Named(dependency.name), path + key) }
                }
            }

            stack -= key
            visited += key
            sorted += node
        }

        constructableNodes.forEach {
            visit(
                it,
                SortingKey.Typed(it.type),
                emptyList()
            )
        }

        constructableNamed.forEach {
            visit(
                node = it,
                key = SortingKey.Named(it.name!!),
                path = emptyList()
            )
        }

        return sorted
    }

    sealed interface SortingKey {
        data class Named(val name: String) : SortingKey
        data class Typed(val type: KClass<*>) : SortingKey
    }

    override fun toString(): String {
        return buildString {
            appendLine("DependencyGraph {")
            for (node in nodes.values) {
                appendLine("  $node")
            }
            for ((name, instance) in named) {
                appendLine("  '$name' -> $instance")
            }
            appendLine("}")
        }
    }
}