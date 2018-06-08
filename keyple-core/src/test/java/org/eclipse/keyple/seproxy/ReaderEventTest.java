/*
 * Copyright (c) 2018 Calypso Networks Association https://www.calypsonet-asso.org/
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License version 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/org/documents/epl-2.0/EPL-2.0.html
 */

package org.eclipse.keyple.seproxy;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import org.eclipse.keyple.seproxy.event.ReaderEvent;
import org.junit.Test;

/**
 * @deprecated TODO useful???
 */
public class ReaderEventTest {

    @Test
    public void testReaderEvent() {
        ReaderEvent event = ReaderEvent.IO_ERROR;
        assertNotNull(event);
    }

    @Test
    public void testGetEvent() {
        ReaderEvent event = ReaderEvent.IO_ERROR;
        assertEquals(ReaderEvent.IO_ERROR, event);
    }

}
