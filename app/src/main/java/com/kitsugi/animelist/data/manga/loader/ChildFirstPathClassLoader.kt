package com.kitsugi.animelist.data.manga.loader

import dalvik.system.PathClassLoader
import java.io.IOException
import java.io.InputStream
import java.net.URL
import java.util.Enumeration

/**
 * A parent-last class loader that will try in order:
 * - the system class loader
 * - the child class loader (local to the loaded apk/dex)
 * - the parent class loader.
 */
class ChildFirstPathClassLoader(
    dexPath: String,
    librarySearchPath: String?,
    parent: ClassLoader,
) : PathClassLoader(dexPath, librarySearchPath, parent) {

    private val systemClassLoader: ClassLoader? = getSystemClassLoader()

    private val parentFirstPrefixes = listOf(
        "kotlin.",
        "kotlinx.",
        "okhttp3.",
        "okio.",
        "uy.kohesive.injekt.",
        "android.",
        "androidx.",
        "rx.",
        "org.jsoup.",
        "com.fasterxml.jackson."
    )

    private fun isParentFirst(name: String?): Boolean {
        if (name == null) return false
        if (parentFirstPrefixes.any { name.startsWith(it) }) return true
        if (name.startsWith("eu.kanade.tachiyomi.") && !name.startsWith("eu.kanade.tachiyomi.extension.")) return true
        return false
    }

    override fun loadClass(name: String?, resolve: Boolean): Class<*> {
        if (isParentFirst(name)) {
            return super.loadClass(name, resolve)
        }

        var c = findLoadedClass(name)

        if (c == null && systemClassLoader != null) {
            try {
                c = systemClassLoader.loadClass(name)
            } catch (_: ClassNotFoundException) {}
        }

        if (c == null) {
            c = try {
                findClass(name)
            } catch (_: ClassNotFoundException) {
                super.loadClass(name, resolve)
            }
        }

        if (resolve) {
            resolveClass(c)
        }

        return c
    }

    override fun getResource(name: String?): URL? {
        return systemClassLoader?.getResource(name)
            ?: findResource(name)
            ?: super.getResource(name)
    }

    override fun getResources(name: String?): Enumeration<URL> {
        val systemUrls = systemClassLoader?.getResources(name)
        val localUrls = findResources(name)
        val parentUrls = parent?.getResources(name)
        val urls = buildList {
            while (systemUrls?.hasMoreElements() == true) {
                add(systemUrls.nextElement())
            }

            while (localUrls?.hasMoreElements() == true) {
                add(localUrls.nextElement())
            }

            while (parentUrls?.hasMoreElements() == true) {
                add(parentUrls.nextElement())
            }
        }

        return object : Enumeration<URL> {
            val iterator = urls.iterator()

            override fun hasMoreElements() = iterator.hasNext()
            override fun nextElement() = iterator.next()
        }
    }

    override fun getResourceAsStream(name: String?): InputStream? {
        return try {
            getResource(name)?.openStream()
        } catch (_: IOException) {
            null
        }
    }
}
