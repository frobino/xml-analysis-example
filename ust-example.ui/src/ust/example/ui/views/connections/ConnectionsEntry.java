/*******************************************************************************
 * Copyright (c) 2012 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Patrick Tasse - Initial API and implementation
 *******************************************************************************/

package ust.example.ui.views.connections;

import org.eclipse.linuxtools.tmf.ui.views.timegraph.AbstractTimeGraphEntry;

import ust.example.core.trace.MyUstTrace;


/**
 * An entry, or row, in the oscilloscope view
 *
 * @author Patrick Tasse
 */
public class ConnectionsEntry extends AbstractTimeGraphEntry {

    /**
     * Standard constructor
     *
     * @param quark
     *            The quark of the state system attribute whose state is shown
     *            on this row
     * @param trace
     *            The trace that this view is talking about
     * @param device
     *            The device which this entry represents
     */
    public ConnectionsEntry(int quark, MyUstTrace trace, String device) {
        super(quark, trace, device);
    }
}
