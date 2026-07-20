package uy.kohesive.injekt.api

import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type

@Suppress("UNCHECKED_CAST")
fun Type.erasedType(): Class<Any> {
    return when (this) {
        is Class<*> -> this as Class<Any>
        is ParameterizedType -> this.rawType.erasedType()
        is java.lang.reflect.GenericArrayType -> {
            val elementType = this.genericComponentType.erasedType()
            val testArray = java.lang.reflect.Array.newInstance(elementType, 0)
            testArray.javaClass
        }
        is java.lang.reflect.TypeVariable<*> -> {
            throw IllegalStateException("Not sure what to do here yet")
        }
        is java.lang.reflect.WildcardType -> {
            this.upperBounds[0].erasedType()
        }
        else -> throw IllegalStateException("Should not get here.")
    }
}

inline fun <reified T : Any> typeRef(): FullTypeReference<T> = object : FullTypeReference<T>() {}
inline fun <reified T : Any> fullType(): FullTypeReference<T> = object : FullTypeReference<T>() {}

interface TypeReference<T> {
    val type: Type
}

abstract class FullTypeReference<T> protected constructor() : TypeReference<T> {
    override val type: Type = javaClass.genericSuperclass.let { superClass ->
        if (superClass is Class<*>) {
            throw IllegalArgumentException("Internal error: TypeReference constructed without actual type information")
        }
        (superClass as ParameterizedType).getActualTypeArguments()[0]
    }
}

interface InjektScope : InjektRegistry, InjektFactory {
    fun <T : Any> get(clazz: Class<T>): T
    fun <T : Any> addSingleton(clazz: Class<T>, instance: T)
    fun <T : Any> addSingletonFactory(clazz: Class<T>, factory: () -> T)
}

interface InjektRegistry {
    fun <T : Any> addSingleton(forType: TypeReference<T>, singleInstance: T)
    fun <R : Any> addSingletonFactory(forType: TypeReference<R>, factoryCalledOnce: () -> R)
}

interface InjektFactory {
    fun <R : Any> getInstance(forType: Type): R
}

interface InjektRegistrar : InjektRegistry

interface InjektModule {
    fun InjektRegistrar.registerInjectables()
}

inline fun <reified T : Any> InjektScope.get(): T {
    return get(T::class.java)
}

inline fun <reified T : Any> InjektScope.addSingleton(instance: T) {
    addSingleton(T::class.java, instance)
}

inline fun <reified T : Any> InjektScope.addSingletonFactory(noinline factory: () -> T) {
    addSingletonFactory(T::class.java, factory)
}

inline fun <reified T : Any> InjektRegistrar.addSingleton(instance: T) {
    addSingleton(fullType<T>(), instance)
}

inline fun <reified T : Any> InjektRegistrar.addSingletonFactory(noinline factory: () -> T) {
    addSingletonFactory(fullType<T>(), factory)
}

inline fun <reified T : Any> InjektRegistrar.get(): T {
    return uy.kohesive.injekt.Injekt.get(T::class.java)
}

inline fun <reified T : Any> InjektScope.get(typeRef: TypeReference<T>): T {
    return getInstance(typeRef.type)
}

