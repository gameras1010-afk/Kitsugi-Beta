package uy.kohesive.injekt

import uy.kohesive.injekt.api.InjektScope
import uy.kohesive.injekt.api.TypeReference
import uy.kohesive.injekt.api.erasedType
import java.lang.reflect.Type

object Injekt : InjektScope {
    val registry = mutableMapOf<Class<*>, Any>()
    val factories = mutableMapOf<Class<*>, () -> Any>()

    inline fun <reified T : Any> import(instance: T) {
        registry[T::class.java] = instance
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> get(clazz: Class<T>): T {
        val instance = registry[clazz]
        if (instance != null) return instance as T

        val factory = factories[clazz]
        if (factory != null) {
            val newInstance = factory()
            registry[clazz] = newInstance
            return newInstance as T
        }

        throw IllegalStateException("No instance or factory registered for ${clazz.name}")
    }

    @Suppress("UNCHECKED_CAST")
    override fun <R : Any> getInstance(forType: Type): R {
        val clazz = forType.erasedType() as Class<R>
        return get(clazz)
    }

    override fun <T : Any> addSingleton(clazz: Class<T>, instance: T) {
        registry[clazz] = instance
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> addSingleton(forType: TypeReference<T>, singleInstance: T) {
        val clazz = forType.type.erasedType() as Class<T>
        registry[clazz] = singleInstance
    }

    override fun <T : Any> addSingletonFactory(clazz: Class<T>, factory: () -> T) {
        factories[clazz] = factory
    }

    @Suppress("UNCHECKED_CAST")
    override fun <R : Any> addSingletonFactory(forType: TypeReference<R>, factoryCalledOnce: () -> R) {
        val clazz = forType.type.erasedType() as Class<R>
        factories[clazz] = factoryCalledOnce
    }
}

/**
 * Keiyoushi eklentilerinin bytecode'da çağırdığı top-level fonksiyon.
 * Java tarafında: InjektKt.getInjekt() → bu fonksiyon derlendikten sonra JVM'de
 * "getInjekt()" statik metodu olarak görünür.
 */
fun getInjekt(): InjektScope = Injekt

inline fun <reified T : Any> injectLazy(): Lazy<T> {
    return lazy { Injekt.get(T::class.java) }
}

