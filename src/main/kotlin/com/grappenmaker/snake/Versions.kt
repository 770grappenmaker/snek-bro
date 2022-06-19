package com.grappenmaker.snake

import java.util.*
import kotlin.properties.ReadOnlyProperty

object Versions {
    private val props by lazy {
        val resource = Versions::class.java.classLoader.getResourceAsStream("versions.txt")
        resource.use { stream ->
            Properties().also {
                it.load(
                    stream?.bufferedReader()
                        ?: error("versions were not found on runtime")
                )
            }
        }
    }

    private val getProp = ReadOnlyProperty<Versions, String> { _, prop ->
        props.getProperty(prop.name) ?: error("Property ${prop.name} was not found")
    }

    val version by getProp
    val timestamp by getProp
}