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

import org.eclipse.linuxtools.tmf.ui.widgets.timegraph.model.TimeEvent;

/**
 * Time Event implementation specific to the Connections View
 *
 * @author Patrick Tasse
 * @author Alexandre Montplaisir
 */
public class ConnectionsEvent extends TimeEvent {

    private final int fValue;

    /**
     * Standard constructor
     *
     * @param entry
     *            The entry that this event affects
     * @param time
     *            The start time of the event
     * @param duration
     *            The duration of the event
     * @param value
     *            The value type associated to this event
     */
    public ConnectionsEvent(ConnectionsEntry entry, long time, long duration, int value) {
        super(entry, time, duration);
        fValue = value;
    }

    /**
     * Base constructor, with no value assigned
     *
     * @param entry
     *            The entry that this event affects
     * @param time
     *            The start time of the event
     * @param duration
     *            The duration of the event
     */
    public ConnectionsEvent(ConnectionsEntry entry, long time, long duration) {
        super(entry, time, duration);
        fValue = -1;
    }

    /**
     * Retrieve the value associated with this event
     *
     * @return The integer value
     */
    public int getValue() {
        return fValue;
    }
}
