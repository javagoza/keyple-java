/* **************************************************************************************
 * Copyright (c) 2019 Calypso Networks Association https://www.calypsonet-asso.org/
 *
 * See the NOTICE file(s) distributed with this work for additional information
 * regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License 2.0 which is available at http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 ************************************************************************************** */
package org.eclipse.keyple.core.card.selection;

import java.util.*;
import org.eclipse.keyple.core.CoreBaseTest;
import org.eclipse.keyple.core.card.command.AbstractApduCommandBuilder;
import org.eclipse.keyple.core.card.command.CardCommand;
import org.eclipse.keyple.core.card.message.ApduRequest;
import org.eclipse.keyple.core.card.message.ApduResponse;
import org.eclipse.keyple.core.card.message.CardResponse;
import org.eclipse.keyple.core.card.message.CardSelectionResponse;
import org.eclipse.keyple.core.card.message.ChannelControl;
import org.eclipse.keyple.core.card.message.DefaultSelectionsRequest;
import org.eclipse.keyple.core.card.message.DefaultSelectionsResponse;
import org.eclipse.keyple.core.card.message.SelectionStatus;
import org.eclipse.keyple.core.service.event.AbstractDefaultSelectionsResponse;
import org.eclipse.keyple.core.service.exception.KeypleException;
import org.eclipse.keyple.core.service.util.ContactlessCardCommonProtocols;
import org.eclipse.keyple.core.util.ByteArrayUtil;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CardSelectionTest extends CoreBaseTest {

  private static final Logger logger = LoggerFactory.getLogger(CardSelectionTest.class);

  @Before
  public void setUp() {
    logger.info("------------------------------");
    logger.info("Test {}", name.getMethodName() + "");
    logger.info("------------------------------");
  }

  @Test
  public void prepareSelection() {
    CardSelectionsService cardSelectionsService = createCardSelection();

    // let's check if the result is as expected
    // (see createSelectionSelectionSelection to have a look at the expected values)

    // get the selection operation
    DefaultSelectionsRequest selectionOperation =
        (DefaultSelectionsRequest) cardSelectionsService.getDefaultSelectionsRequest();

    // check common flags
    Assert.assertEquals(
        MultiSelectionProcessing.FIRST_MATCH, selectionOperation.getMultiSelectionProcessing());
    Assert.assertEquals(ChannelControl.KEEP_OPEN, selectionOperation.getChannelControl());

    // get the cardRequest set
    List<org.eclipse.keyple.core.card.message.CardSelectionRequest> cardSelectionRequests =
        selectionOperation.getCardSelectionRequests();
    Assert.assertEquals(2, cardSelectionRequests.size());

    // get the two card requests
    Iterator<org.eclipse.keyple.core.card.message.CardSelectionRequest> iterator =
        cardSelectionRequests.iterator();
    org.eclipse.keyple.core.card.message.CardSelectionRequest cardRequest1 = iterator.next();
    org.eclipse.keyple.core.card.message.CardSelectionRequest cardRequest2 = iterator.next();

    // check selectors
    Assert.assertEquals(
        "AABBCCDDEE",
        ByteArrayUtil.toHex(cardRequest1.getCardSelector().getAidSelector().getAidToSelect()));
    Assert.assertEquals(
        "1122334455",
        ByteArrayUtil.toHex(cardRequest2.getCardSelector().getAidSelector().getAidToSelect()));

    Assert.assertEquals(
        CardSelector.AidSelector.FileOccurrence.FIRST,
        cardRequest1.getCardSelector().getAidSelector().getFileOccurrence());
    Assert.assertEquals(
        CardSelector.AidSelector.FileOccurrence.NEXT,
        cardRequest2.getCardSelector().getAidSelector().getFileOccurrence());

    Assert.assertEquals(
        CardSelector.AidSelector.FileControlInformation.FCI,
        cardRequest1.getCardSelector().getAidSelector().getFileControlInformation());
    Assert.assertEquals(
        CardSelector.AidSelector.FileControlInformation.FCP,
        cardRequest2.getCardSelector().getAidSelector().getFileControlInformation());

    Assert.assertNull(
        cardRequest1.getCardSelector().getAidSelector().getSuccessfulSelectionStatusCodes());

    Assert.assertEquals(
        1,
        cardRequest2.getCardSelector().getAidSelector().getSuccessfulSelectionStatusCodes().size());
    Assert.assertEquals(
        0x6283,
        cardRequest2
            .getCardSelector()
            .getAidSelector()
            .getSuccessfulSelectionStatusCodes()
            .toArray()[0]);

    Assert.assertNull(cardRequest1.getCardSelector().getAtrFilter());
    Assert.assertEquals(".*", cardRequest2.getCardSelector().getAtrFilter().getAtrRegex());

    Assert.assertEquals(2, cardRequest1.getCardRequest().getApduRequests().size());
    Assert.assertEquals(0, cardRequest2.getCardRequest().getApduRequests().size());

    List<ApduRequest> apduRequests = cardRequest1.getCardRequest().getApduRequests();

    Assert.assertArrayEquals(apduRequests.get(0).getBytes(), ByteArrayUtil.fromHex("001122334455"));
    Assert.assertArrayEquals(apduRequests.get(1).getBytes(), ByteArrayUtil.fromHex("66778899AABB"));

    Assert.assertFalse(apduRequests.get(0).isCase4());
    Assert.assertTrue(apduRequests.get(1).isCase4());

    // that's all!
  }

  @Test
  public void processDefaultSelectionNull() {
    CardSelectionsService cardSelectionsService = Mockito.mock(CardSelectionsService.class);

    try {
      Assert.assertNull(cardSelectionsService.processDefaultSelectionsResponse(null));
    } catch (KeypleException e) {
      Assert.fail("Exception raised: " + e.getMessage());
    }
  }

  @Test
  public void processDefaultSelectionEmpty() {
    CardSelectionsService cardSelectionsService = createCardSelection();

    AbstractDefaultSelectionsResponse defaultSelectionsResponse;
    List<CardSelectionResponse> cardSelectionResponses = new ArrayList<CardSelectionResponse>();

    defaultSelectionsResponse = new DefaultSelectionsResponse(cardSelectionResponses);

    CardSelectionsResult cardSelectionsResult = null;
    try {
      cardSelectionsResult =
          cardSelectionsService.processDefaultSelectionsResponse(defaultSelectionsResponse);
    } catch (KeypleException e) {
      Assert.fail("Exception raised: " + e.getMessage());
    }

    Assert.assertFalse(cardSelectionsResult.hasActiveSelection());
    Assert.assertEquals(0, cardSelectionsResult.getSmartCards().size());
  }

  @Test
  public void processDefaultSelectionNotMatching() {
    // create a CardSelectionsService
    CardSelectionsService cardSelectionsService = createCardSelection();

    // create a selection response
    AbstractDefaultSelectionsResponse defaultSelectionsResponse;
    List<CardSelectionResponse> cardSelectionResponses = new ArrayList<CardSelectionResponse>();

    ApduResponse apduResponse =
        new ApduResponse(
            ByteArrayUtil.fromHex(
                "CC 11223344 9999 00112233445566778899AABBCCDDEEFF 00112233445566778899AABBCC 9000"),
            null);

    List<ApduResponse> apduResponses = new ArrayList<ApduResponse>();

    apduResponses.add(apduResponse);

    SelectionStatus selectionStatus =
        new SelectionStatus(
            null, new ApduResponse(ByteArrayUtil.fromHex("001122334455669000"), null), false);

    CardSelectionResponse cardSelectionResponse =
        new CardSelectionResponse(selectionStatus, new CardResponse(true, apduResponses));

    cardSelectionResponses.add(cardSelectionResponse);

    defaultSelectionsResponse = new DefaultSelectionsResponse(cardSelectionResponses);

    // process the selection response with the CardSelectionsService
    CardSelectionsResult cardSelectionsResult = null;
    try {
      cardSelectionsResult =
          cardSelectionsService.processDefaultSelectionsResponse(defaultSelectionsResponse);
    } catch (KeypleException e) {
      Assert.fail("Exception raised: " + e.getMessage());
    }

    Assert.assertFalse(cardSelectionsResult.hasActiveSelection());
    try {
      cardSelectionsResult.getActiveSmartCard();
    } catch (Exception e) {
      Assert.assertTrue(e.getMessage().contains("No active Matching card is available"));
    }
  }

  @Test
  public void processDefaultSelectionMatching() {
    // create a CardSelectionsService
    CardSelectionsService cardSelectionsService = createCardSelection();

    // create a selection response
    AbstractDefaultSelectionsResponse defaultSelectionsResponse;
    List<CardSelectionResponse> cardSelectionResponses = new ArrayList<CardSelectionResponse>();

    ApduResponse apduResponse =
        new ApduResponse(
            ByteArrayUtil.fromHex(
                "CC 11223344 9999 00112233445566778899AABBCCDDEEFF 00112233445566778899AABBCC 9000"),
            null);

    List<ApduResponse> apduResponses = new ArrayList<ApduResponse>();

    apduResponses.add(apduResponse);

    SelectionStatus selectionStatus =
        new SelectionStatus(
            null, new ApduResponse(ByteArrayUtil.fromHex("001122334455669000"), null), true);

    CardSelectionResponse cardSelectionResponse =
        new CardSelectionResponse(selectionStatus, new CardResponse(true, apduResponses));

    cardSelectionResponses.add(cardSelectionResponse);

    defaultSelectionsResponse = new DefaultSelectionsResponse(cardSelectionResponses);

    // process the selection response with the CardSelectionsService
    CardSelectionsResult cardSelectionsResult = null;
    try {
      cardSelectionsResult =
          cardSelectionsService.processDefaultSelectionsResponse(defaultSelectionsResponse);
    } catch (KeypleException e) {
      Assert.fail("Exception raised: " + e.getMessage());
    }

    Assert.assertTrue(cardSelectionsResult.hasActiveSelection());
    Assert.assertNotNull(cardSelectionsResult.getActiveSmartCard());
  }

  /*
   * @Test public void processExplicitSelections() { // create a CardSelectionsService CardSelectionsService
   * cardSelectionsService = createCardSelection();
   *
   * AbstractLocalReader r = Mockito.spy(new Reader("CardSelectionP", "CardSelectionR"));
   *
   * // success apdu doReturn(ByteArrayUtil.fromHex(
   * "001122334455669000")).when(r).transmitApdu(ByteArrayUtil.fromHex( "001122334455669000"));
   *
   * // aid selection doReturn(ByteArrayUtil.fromHex(
   * "6F25840BA000000291A00000019102A516BF0C13C70800000000C0E11FA653070A3C230C1410019000"))
   * .when(r).transmitApdu(ByteArrayUtil
   * .fromHex("00 A4 04 00 0A A0 00 00 02 91 A0 00 00 01 91 00"));
   *
   * // physical channel is open doReturn(true).when(r).isPhysicalChannelOpen(); }
   */

  /** Create a CardSelectionsService object */
  private CardSelectionsService createCardSelection() {
    CardSelectionsService cardSelectionsService = new CardSelectionsService();

    // create and add two selection cases
    CardSelector.AidSelector aidSelector =
        CardSelector.AidSelector.builder()
            .aidToSelect("AABBCCDDEE")
            .fileOccurrence(CardSelector.AidSelector.FileOccurrence.FIRST)
            .fileControlInformation(CardSelector.AidSelector.FileControlInformation.FCI)
            .build();
    CardSelector cardSelector1 =
        CardSelector.builder()
            .cardProtocol(ContactlessCardCommonProtocols.ISO_14443_4.name())
            .aidSelector(aidSelector)
            .build();

    // APDU requests
    List<AbstractApduCommandBuilder> commandBuilders = new ArrayList<AbstractApduCommandBuilder>();
    commandBuilders.add(
        new CommandBuilder(
            CardCommandTest.COMMAND_1,
            new ApduRequest(ByteArrayUtil.fromHex("001122334455"), false)
                .setName("Apdu 001122334455")));
    commandBuilders.add(
        new CommandBuilder(
            CardCommandTest.COMMAND_1,
            new ApduRequest(ByteArrayUtil.fromHex("66778899AABB"), true)
                .setName("Apdu 66778899AABB")));

    cardSelectionsService.prepareSelection(
        new CardSelectionRequest(cardSelector1, commandBuilders));

    aidSelector =
        CardSelector.AidSelector.builder()
            .aidToSelect("1122334455")
            .fileOccurrence(CardSelector.AidSelector.FileOccurrence.NEXT)
            .fileControlInformation(CardSelector.AidSelector.FileControlInformation.FCP)
            .build();
    aidSelector.addSuccessfulStatusCode(0x6283);

    CardSelector cardSelector2 =
        CardSelector.builder()
            .cardProtocol(ContactlessCardCommonProtocols.INNOVATRON_B_PRIME_CARD.name())
            .atrFilter(new CardSelector.AtrFilter(".*"))
            .aidSelector(aidSelector)
            .build();

    cardSelectionsService.prepareSelection(new CardSelectionRequest(cardSelector2, null));

    return cardSelectionsService;
  }

  /** Selection Request instantiation */
  private final class CardSelectionRequest extends AbstractCardSelection {

    public CardSelectionRequest(
        CardSelector cardSelector, List<AbstractApduCommandBuilder> commandBuilders) {
      super(cardSelector);

      if (commandBuilders != null) {
        for (AbstractApduCommandBuilder commandBuilder : commandBuilders) {
          super.addCommandBuilder(commandBuilder);
        }
      }
    }

    @Override
    protected AbstractSmartCard parse(CardSelectionResponse cardSelectionResponse) {
      return new SmartCard(cardSelectionResponse);
    }
  }

  /** Matching card instantiation */
  private final class SmartCard extends AbstractSmartCard {
    SmartCard(CardSelectionResponse cardSelectionResponse) {
      super(cardSelectionResponse);
    }
  }

  private final class CommandBuilder extends AbstractApduCommandBuilder {

    public CommandBuilder(CardCommand commandRef, ApduRequest request) {
      super(commandRef, request);
    }
  }

  public enum CardCommandTest implements CardCommand {
    COMMAND_1("COMMAND_1", (byte) 0xC1),
    COMMAND_2("COMMAND_2", (byte) 0xC2);

    private final String name;
    private final byte instructionByte;

    CardCommandTest(String name, byte instructionByte) {
      this.name = name;
      this.instructionByte = instructionByte;
    }

    @Override
    public String getName() {
      return name;
    }

    @Override
    public byte getInstructionByte() {
      return instructionByte;
    }
  }
}
