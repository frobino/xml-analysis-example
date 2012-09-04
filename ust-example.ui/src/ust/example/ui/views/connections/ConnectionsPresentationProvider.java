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

import org.eclipse.linuxtools.tmf.ui.widgets.timegraph.StateItem;
import org.eclipse.linuxtools.tmf.ui.widgets.timegraph.TimeGraphPresentationProvider;
import org.eclipse.linuxtools.tmf.ui.widgets.timegraph.model.ITimeEvent;
import org.eclipse.swt.graphics.RGB;

import ust.example.core.stateprovider.StateValues;


/**
 * Presentation provider for the Resource view, based on the generic TMF
 * presentation provider.
 *
 * @author Patrick Tasse
 * @author Alexandre Montplaisir
 */
public class ConnectionsPresentationProvider extends TimeGraphPresentationProvider {

    private enum State {
        UNKNOWN  (new RGB(100, 100, 100)),
        ACTIVE   (new RGB(0, 150, 0)),
        WAIT     (new RGB(255, 220, 0));

        public final RGB rgb;

        private State (RGB rgb) {
            this.rgb = rgb;
        }
    }

    @Override
    public String getStateTypeName() {
        return "Device"; //$NON-NLS-1$
    }

    @Override
    public StateItem[] getStateTable() {
        StateItem[] stateTable = new StateItem[State.values().length];
        for (int i = 0; i < stateTable.length; i++) {
            State state = State.values()[i];
            stateTable[i] = new StateItem(state.rgb, state.toString());
        }
        return stateTable;
    }

    @Override
    public int getStateTableIndex(ITimeEvent event) {
        if ((event instanceof ConnectionsEvent) && (event.getEntry() instanceof ConnectionsEntry)) {
            ConnectionsEvent oscilloscopeEvent = (ConnectionsEvent) event;
            int status = oscilloscopeEvent.getValue();

            if (status == StateValues.CONNECTION_STATUS_ACTIVE) {
                return State.ACTIVE.ordinal();
            } else if (status == StateValues.CONNECTION_STATUS_WAIT) {
                return State.WAIT.ordinal();
            }

            return -1; // NULL
        }
        return State.UNKNOWN.ordinal();
    }

    @Override
    public String getEventName(ITimeEvent event) {
        if ((event instanceof ConnectionsEvent) && (event.getEntry() instanceof ConnectionsEntry)) {
            ConnectionsEvent oscilloscopeEvent = (ConnectionsEvent) event;
            int status = oscilloscopeEvent.getValue();

            if (status == StateValues.CONNECTION_STATUS_ACTIVE) {
                return State.ACTIVE.toString();
            } else if (status == StateValues.CONNECTION_STATUS_WAIT) {
                return State.WAIT.toString();
            }

            return null;
        }
        return State.UNKNOWN.toString();
    }
}
