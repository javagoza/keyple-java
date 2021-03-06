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

import io.mockk.MockKAnnotations
import io.mockk.unmockkAll
import java.io.IOException
import org.eclipse.keyple.core.card.message.ApduRequest
import org.eclipse.keyple.core.card.message.CardRequest
import org.eclipse.keyple.core.card.message.CardSelectionRequest
import org.eclipse.keyple.core.card.message.ChannelControl
import org.eclipse.keyple.core.card.selection.CardSelector
import org.eclipse.keyple.core.card.selection.MultiSelectionProcessing
import org.eclipse.keyple.core.service.exception.KeypleReaderException
import org.eclipse.keyple.core.service.exception.KeypleReaderIOException
import org.eclipse.keyple.core.service.util.ContactCardCommonProtocols
import org.eclipse.keyple.core.util.ByteArrayUtil
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Ignore
import org.junit.Test

internal abstract class AbstractAndroidOmapiReaderTest<T, V : AbstractAndroidOmapiReader> {

    companion object {
        internal const val PLUGIN_NAME = "AndroidOmapiPluginImpl"
        internal const val PO_AID = "A000000291A000000191"
        internal const val PO_AID_RESPONSE = "6F25840BA000000291A00000019102A516BF0C13C70800000000C0E11FA653070A3C230C1410019000"
    }

    abstract var nativeReader: T
    abstract var reader: V

    abstract fun mockReader(): T
    abstract fun mockReaderWithExceptionOnOpenLogicalChannel(throwable: Throwable): T
    abstract fun mockReaderWithNullOnOpenLogicalChannel(): T
    abstract fun mockReaderWithExceptionOnOpenBasicChannel(throwable: Throwable): T
    abstract fun mockReaderWithNullOnOpenBasicChannel(): T
    abstract fun mockReaderWithNoAid(): T
    abstract fun mockReaderWithExceptionOnOpenSession(throwable: Throwable): T
    abstract fun buildOmapiReaderImpl(nativeReader: T): V
    abstract fun getNativeReaderName(): String
    abstract fun mockReaderWithExceptionOnCloseChannel(throwable: Throwable): T
    abstract fun mockReaderWithExceptionWhileTransmittingApdu(throwable: Throwable): T

    @Before
    fun setUp() {
        MockKAnnotations.init(this, relaxUnitFun = true)
        // default reader connected with secure element with poAid
        nativeReader = mockReader()

        // instantiate reader with nativeReader
        reader = buildOmapiReaderImpl(nativeReader)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }
    /*
     * TEST READERS
     */

    @Test
    fun getInstance() {
        Assert.assertNotNull(reader)
    }

    @Test
    fun getName() {
        Assert.assertEquals(getNativeReaderName(), reader.name)
    }

    @Test
    fun getTransmissionMode() {
        Assert.assertEquals(false, reader.isContactless)
    }

    @Test
    fun isSEPresent() {
        Assert.assertEquals(true, reader.isCardPresent)
    }

    @Test(expected = KeypleReaderIOException::class)
    fun transmitIOException() {
        nativeReader = mockReaderWithExceptionOnOpenLogicalChannel(IOException())
        reader = buildOmapiReaderImpl(nativeReader)
        // test
        reader.transmitCardSelectionRequests(getSampleSelectionRequest(), MultiSelectionProcessing.FIRST_MATCH, ChannelControl.KEEP_OPEN)
    }

    @Test(expected = KeypleReaderIOException::class)
    fun transmitSecurityException() {
        nativeReader = mockReaderWithExceptionOnOpenLogicalChannel(SecurityException())
        reader = buildOmapiReaderImpl(nativeReader)
        // test
        reader.transmitCardSelectionRequests(getSampleSelectionRequest(), MultiSelectionProcessing.FIRST_MATCH, ChannelControl.KEEP_OPEN)
    }

    @Test(expected = IllegalArgumentException::class)
    fun transmitNoSuchElementException() {
        nativeReader = mockReaderWithExceptionOnOpenLogicalChannel(NoSuchElementException())
        reader = buildOmapiReaderImpl(nativeReader)
        // test
        val cardResponseList = reader.transmitCardSelectionRequests(getSampleSelectionRequest(), MultiSelectionProcessing.FIRST_MATCH, ChannelControl.KEEP_OPEN)
        Assert.assertFalse(cardResponseList[0].selectionStatus.hasMatched()) // If container is not found a null responsed is returned
    }

    @Test(expected = KeypleReaderIOException::class)
    fun transmitOpenChannelFailed() {
        nativeReader = mockReaderWithNullOnOpenLogicalChannel()
        reader = buildOmapiReaderImpl(nativeReader)
        // test
        reader.transmitCardSelectionRequests(getSampleSelectionRequest(), MultiSelectionProcessing.FIRST_MATCH, ChannelControl.KEEP_OPEN)
    }

    @Test(expected = KeypleReaderIOException::class)
    fun transmitNoAidOpenChannelFailed() {
        nativeReader = mockReaderWithNullOnOpenBasicChannel()
        reader = buildOmapiReaderImpl(nativeReader)
        // test
        reader.transmitCardSelectionRequests(getNoAidSampleCardRequest(), MultiSelectionProcessing.FIRST_MATCH, ChannelControl.KEEP_OPEN)
    }

    @Test(expected = KeypleReaderIOException::class)
    fun transmitNoAidIOException() {
        nativeReader = mockReaderWithExceptionOnOpenBasicChannel(IOException())
        reader = buildOmapiReaderImpl(nativeReader)
        // test
        reader.transmitCardSelectionRequests(getNoAidSampleCardRequest(), MultiSelectionProcessing.FIRST_MATCH, ChannelControl.KEEP_OPEN)
    }

    @Test(expected = KeypleReaderIOException::class)
    fun transmitNoAidSecurityException() {
        nativeReader = mockReaderWithExceptionOnOpenBasicChannel(SecurityException())
        reader = buildOmapiReaderImpl(nativeReader)
        // test
        reader.transmitCardSelectionRequests(getNoAidSampleCardRequest(), MultiSelectionProcessing.FIRST_MATCH, ChannelControl.KEEP_OPEN)
    }

    @Test(expected = IllegalArgumentException::class)
    fun transmitNoAid() {

        // init
        nativeReader = mockReaderWithNoAid()
        reader = buildOmapiReaderImpl(nativeReader)

        // test
        val cardResponseList = reader.transmitCardSelectionRequests(getSampleSelectionRequest(), MultiSelectionProcessing.FIRST_MATCH, ChannelControl.KEEP_OPEN)

        // assert
        Assert.assertFalse(cardResponseList[0].selectionStatus.hasMatched())
    }

    @Test
    fun transmitWrongProtocol() {
        // init
        val poAid = "A000000291A000000191"

        // wrong protocol
        val cardRequest = CardRequest(ArrayList())

        val cardSelectionRequest = CardSelectionRequest(CardSelector.builder()
                .cardProtocol("MIFARE_ULTRA_LIGHT")
                .aidSelector(CardSelector.AidSelector.builder()
                        .aidToSelect(poAid).build()).build(), cardRequest)

        // test
        val cardSelectionRequests = ArrayList<CardSelectionRequest>()
        cardSelectionRequests.add(cardSelectionRequest)
        val cardResponseList = reader.transmitCardSelectionRequests(cardSelectionRequests, MultiSelectionProcessing.FIRST_MATCH, ChannelControl.KEEP_OPEN)

        // assert
        Assert.assertFalse(cardResponseList[0].selectionStatus.hasMatched())
    }

    @Test(expected = KeypleReaderException::class)
    fun transmitNotConnected() {

        // init
        val nativeReader = mockReaderWithExceptionOnOpenSession(IOException())
        reader = buildOmapiReaderImpl(nativeReader)

        // test
        reader.transmitCardSelectionRequests(getSampleSelectionRequest(), MultiSelectionProcessing.FIRST_MATCH, ChannelControl.KEEP_OPEN)
    }

    @Test
    fun closeChannelAfterTransmit() {

        // init
        val nativeReader = mockReader()
        reader = buildOmapiReaderImpl(nativeReader)

        // test
        val cardResponseList = reader.transmitCardSelectionRequests(getSampleSelectionRequest(), MultiSelectionProcessing.PROCESS_ALL, ChannelControl.CLOSE_AFTER)
        Assert.assertNotNull(cardResponseList)
    }

    @Test(expected = KeypleReaderIOException::class)
    @Ignore("Implementation of closePhysicalChannel() may encounter a IOException, it should be catched as a KeypleReaderIOException, it is not")
    fun exceptionOnChannelAfterTransmit() {
        // init
        val nativeReader = mockReaderWithExceptionOnCloseChannel(IOException())
        reader = buildOmapiReaderImpl(nativeReader)

        // test
        reader.transmitCardSelectionRequests(getSampleSelectionRequest(), MultiSelectionProcessing.PROCESS_ALL, ChannelControl.CLOSE_AFTER)
    }

    @Test(expected = KeypleReaderIOException::class)
    fun exceptionWhileTransmittingApdu() {
        // init
        val nativeReader = mockReaderWithExceptionWhileTransmittingApdu(IOException())
        reader = buildOmapiReaderImpl(nativeReader)

        // test
        reader.transmitCardSelectionRequests(getSampleSelectionRequest(), MultiSelectionProcessing.FIRST_MATCH, ChannelControl.KEEP_OPEN)
    }

    private fun getSampleSelectionRequest(): List<CardSelectionRequest> {

        val poApduRequestList = listOf(ApduRequest(ByteArrayUtil.fromHex("0000"), true))

        val cardRequest = CardRequest(poApduRequestList)
        val cardSelectionRequest = CardSelectionRequest(CardSelector.builder()
                .cardProtocol(ContactCardCommonProtocols.ISO_7816_3.name)
                .aidSelector(CardSelector.AidSelector.builder()
                        .aidToSelect(PO_AID).build()).build(), cardRequest)
        val selectionRequestList = ArrayList<CardSelectionRequest>()
        selectionRequestList.add(cardSelectionRequest)
        return selectionRequestList
    }

    private fun getNoAidSampleCardRequest(): List<CardSelectionRequest> {

        val poApduRequestList = listOf(ApduRequest(ByteArrayUtil.fromHex("0000"), true))

        val cardRequest = CardRequest(poApduRequestList)

        val cardSelectionRequest = CardSelectionRequest(CardSelector.builder()
                .cardProtocol(ContactCardCommonProtocols.ISO_7816_3.name).build(), cardRequest)

        val selectionRequestList = ArrayList<CardSelectionRequest>()
        selectionRequestList.add(cardSelectionRequest)
        return selectionRequestList
    }
}
