/********************************************************************************
 * Copyright (c) 2019 Calypso Networks Association https://www.calypsonet-asso.org/
 *
 * See the NOTICE file(s) distributed with this work for additional information regarding copyright
 * ownership.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/
package org.eclipse.keyple.core.seproxy.plugin;

import org.eclipse.keyple.core.seproxy.event.ObservableReader;

/**
 * Interface to be implemented by the readers requiring a thread handled by Keyple core to monitor
 * the card insertion/removal
 */
public interface ThreadedMonitoringReader extends ObservableReader {

}
