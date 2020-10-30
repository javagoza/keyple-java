/********************************************************************************
 * Copyright (c) 2020 Calypso Networks Association https://www.calypsonet-asso.org/
 *
 * See the NOTICE file(s) distributed with this work for additional information regarding copyright
 * ownership.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/
package org.eclipse.keyple.plugin.android.omapi

import android.content.Context
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test

internal abstract class AbstractAndroidOmapiPluginTest<T, V> {

    companion object {
        private val READERS_TO_MOCK = mapOf(Pair("SIM1", true), Pair("SIM2", false))
    }

    lateinit var context: Context
    internal abstract var androidOmapiPlugin: AbstractAndroidOmapiPlugin<T, V>

    internal abstract fun buildAndroidOmapiPlugin(context: Context): AbstractAndroidOmapiPlugin<T, V>
    abstract fun mockReader(name: String, isPresent: Boolean): T
    internal abstract fun mockGetNativeReaders(androidOmapiPlugin: AbstractAndroidOmapiPlugin<T, V>, readersToMock: Map<String, Boolean>)
    abstract fun triggerOnConnected()

    @Before
    fun setUp() {
        context = mockContext()
        androidOmapiPlugin = buildAndroidOmapiPlugin(context)
        mockGetNativeReaders(androidOmapiPlugin, READERS_TO_MOCK)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun connectToSe() {
        androidOmapiPlugin.connectToSe(context)
    }

    @Test
    fun getNativeReaders() {
        Assert.assertNotNull(androidOmapiPlugin.readers)
    }

    @Test
    fun mapToReader() {
        Companion.READERS_TO_MOCK.forEach {
            val omapiReader = mockReader(it.key, it.value)
            val reader = androidOmapiPlugin.mapToReader(omapiReader)
            Assert.assertNotNull(reader)
            Assert.assertEquals(it.key, reader.name)
            Assert.assertEquals(it.value, reader.isCardPresent)
        }
    }

    @Test
    fun onConnected() {
        Assert.assertNotNull(androidOmapiPlugin.readers)
        Assert.assertEquals(0, androidOmapiPlugin.readers.size)
        androidOmapiPlugin.connectToSe(context)
        triggerOnConnected() // Thanks to the object mockk we can simulate readers retrieval
        Assert.assertEquals(Companion.READERS_TO_MOCK.size, androidOmapiPlugin.readers.size)
    }

    private fun mockContext(): Context {
        val context = mockk<Context>()
        every { context.applicationContext } returns context
        every { context.bindService(any(), any(), any()) } returns true
        return context
    }
}
