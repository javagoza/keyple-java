/* **************************************************************************************
 * Copyright (c) 2018 Calypso Networks Association https://www.calypsonet-asso.org/
 *
 * See the NOTICE file(s) distributed with this work for additional information
 * regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License 2.0 which is available at http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 ************************************************************************************** */
package org.eclipse.keyple.example.generic.standalone.common;

import org.eclipse.keyple.core.util.ByteArrayUtil;
import org.eclipse.keyple.plugin.stub.StubSmartCard;

/** Simple contact card Stub (no command) */
public final class StubMifareDesfire extends StubSmartCard {

  static final String cardProtocol = "MIFARE_DESFIRE";
  final String ATR_HEX = "3B8180018080";

  public StubMifareDesfire() {
    /* Get data */
    addHexCommand("FFCA 000000", "223344556677889000");
  }

  @Override
  public byte[] getATR() {
    return ByteArrayUtil.fromHex(ATR_HEX);
  }

  @Override
  public String getCardProtocol() {
    return cardProtocol;
  }
}
