/*******************************************************************************
 * Copyright (c) 2012 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Alexandre Montplaisir - Initial API and implementation
 ******************************************************************************/

package ust.example.core.stateprovider;

import org.eclipse.linuxtools.tmf.core.ctfadaptor.CtfTmfEvent;
import org.eclipse.linuxtools.tmf.core.event.ITmfEvent;
import org.eclipse.linuxtools.tmf.core.exceptions.AttributeNotFoundException;
import org.eclipse.linuxtools.tmf.core.exceptions.StateValueTypeException;
import org.eclipse.linuxtools.tmf.core.exceptions.TimeRangeException;
import org.eclipse.linuxtools.tmf.core.statesystem.IStateChangeInput;
import org.eclipse.linuxtools.tmf.core.statesystem.IStateSystemBuilder;
import org.eclipse.linuxtools.tmf.core.statevalue.ITmfStateValue;
import org.eclipse.linuxtools.tmf.core.statevalue.TmfStateValue;
import org.eclipse.linuxtools.tmf.core.trace.ITmfTrace;

import ust.example.core.trace.MyUstTrace;


/**
 * State provider for CTF Flex traces.
 *
 * The resulting attribute tree will look like this:
 *
 * <root>
 *  |-- Connections
 *  |     |-- <1>
 *  |     |-- <2>
 *  |     |-- <3>
 *  |    ...
 *  \--
 *
 * @version 1.0
 * @author Alexandre Montplaisir
 */
@SuppressWarnings("nls")
public class MyUstTraceInput implements IStateChangeInput {

    private final MyUstTrace fTrace;
    private boolean fStateSystemAssigned;

    private IStateSystemBuilder ss;

    private CtfTmfEvent fCurrentEvent;

    /* Common locations in the attribute tree */
    private int connAttribute = -1;

    /**
     * Constructor
     *
     * @param trace
     *            The flex trace
     */

    public MyUstTraceInput(MyUstTrace trace) {
        fTrace = trace;
        fStateSystemAssigned = false;
    }

    @Override
    public ITmfTrace getTrace() {
        return fTrace;
    }

    @Override
    public long getStartTime() {
        return fTrace.getStartTime().getValue();
    }

    @Override
    public ITmfEvent getExpectedEventType() {
        return CtfTmfEvent.getNullEvent();
    }

    @Override
    public void assignTargetStateSystem(IStateSystemBuilder ssb) {
        this.ss = ssb;
        fStateSystemAssigned = true;

        /* Setup common locations */
        connAttribute = ss.getQuarkAbsoluteAndAdd("Connections");
    }


    @Override
    public void dispose() {
        closeStateSystem();
        fStateSystemAssigned = false;
        this.ss = null;
    }

    private void closeStateSystem() {
        /* Close the History system, if there is one */
        if (fCurrentEvent == null) {
            return;
        }
        try {
            ss.closeHistory(fCurrentEvent.getTimestamp().getValue());
        } catch (TimeRangeException e) {
            /*
             * Since we're using currentEvent.getTimestamp, this shouldn't
             * cause any problem
             */
            e.printStackTrace();
        }
    }

    @Override
    public synchronized void processEvent(ITmfEvent ev) {

        /* Make sure the target state system has been assigned */
        if (!fStateSystemAssigned) {
            return;
        }

        /* Make sure the event is of the right type */
        if (!(ev instanceof CtfTmfEvent)) {
            return;
        }
        CtfTmfEvent event = (CtfTmfEvent) ev;
        fCurrentEvent = event;

        String eventName = event.getEventName();
        long t = event.getTimestampValue();


        int quark;
        ITmfStateValue value;

        try {
            /* Get the connection ID from the event's payload */
            Long connectionId = (Long) event.getContent().getField("id").getValue();

            if (eventName.equals("ust_myprog:connection_start")) {
                /* Assign the "Connection Active" state */
                quark = ss.getQuarkRelativeAndAdd(connAttribute, connectionId.toString());
                value = TmfStateValue.newValueInt(StateValues.CONNECTION_STATUS_ACTIVE);
                ss.modifyAttribute(t, value, quark);

            } else if (eventName.equals("ust_myprog:connection_end")) {
                /* Revert to null state */
                quark = ss.getQuarkRelativeAndAdd(connAttribute, connectionId.toString());
                value = TmfStateValue.nullValue();
                ss.modifyAttribute(t, value, quark);
            }

        } catch (TimeRangeException e) {
            e.printStackTrace();
        } catch (AttributeNotFoundException e) {
            e.printStackTrace();
        } catch (StateValueTypeException e) {
            e.printStackTrace();
        }

    }

}
