/* **************************************************************************************
 * Copyright (c) 2020 Calypso Networks Association https://www.calypsonet-asso.org/
 *
 * See the NOTICE file(s) distributed with this work for additional information
 * regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License 2.0 which is available at http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 ************************************************************************************** */
package org.eclipse.keyple.example.generic.standalone.UseCase2_DefaultSelectionNotification;

import static org.eclipse.keyple.example.generic.standalone.UseCase2_DefaultSelectionNotification.CardSelectionConfig.getDefaultSelection;

import org.eclipse.keyple.core.card.selection.AbstractSmartCard;
import org.eclipse.keyple.core.service.event.ObservableReader;
import org.eclipse.keyple.core.service.event.ReaderEvent;
import org.eclipse.keyple.core.service.exception.KeypleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** A reader Observer handles card event such as CARD_INSERTED, CARD_MATCHED, CARD_REMOVED */
class CardReaderObserver implements ObservableReader.ReaderObserver {

  private static final Logger logger = LoggerFactory.getLogger(CardReaderObserver.class);

  /**
   * Method invoked in the case of a reader event
   *
   * @param event the reader event
   */
  @Override
  public void update(ReaderEvent event) {
    switch (event.getEventType()) {
      case CARD_MATCHED:
        // the selection has one target, get the result at index 0
        AbstractSmartCard selectedCard = null;
        try {
          selectedCard =
              getDefaultSelection()
                  .processDefaultSelectionsResponse(event.getDefaultSelectionsResponse())
                  .getActiveSmartCard();
        } catch (KeypleException e) {
          logger.error("Exception: {}", e.getMessage());
          ((ObservableReader) (event.getReader())).finalizeCardProcessing();
        }

        if (selectedCard != null) {
          logger.info("Observer notification: the selection of the card has succeeded.");

          logger.info("= #### End of the card processing.");
        } else {
          logger.error(
              "The selection of the card has failed. Should not have occurred due to the MATCHED_ONLY selection mode.");
        }
        break;
      case CARD_INSERTED:
        logger.error(
            "CARD_INSERTED event: should not have occurred due to the MATCHED_ONLY selection mode.");
        break;
      case CARD_REMOVED:
        logger.trace("There is no PO inserted anymore. Return to the waiting state...");
        break;
      default:
        break;
    }
    if (event.getEventType() == ReaderEvent.EventType.CARD_INSERTED
        || event.getEventType() == ReaderEvent.EventType.CARD_MATCHED) {
      // Informs the underlying layer of the end of the card processing, in order to manage the
      // removal sequence.

      ((ObservableReader) (event.getReader())).finalizeCardProcessing();
    }
  }
}
