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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.linuxtools.tmf.ui.widgets.timegraph.model.ITimeEvent;
import org.eclipse.linuxtools.tmf.ui.widgets.timegraph.model.ITimeGraphEntry;

import ust.example.core.trace.MyUstTrace;
import ust.example.ui.views.common.EventIterator;


/**
 * An entry, or row, in the connections view
 *
 * @author Patrick Tasse
 * @author Alexandre Montplaisir
 */
public class ConnectionsEntry implements ITimeGraphEntry {

    private final int fQuark;
    private final MyUstTrace fTrace;
    private ITimeGraphEntry fParent = null;
    private final ArrayList<ConnectionsEntry> fChildren = new ArrayList<ConnectionsEntry>();
    private final String fName;
    private long fStartTime;
    private long fEndTime;
    private List<ITimeEvent> fEventList = new ArrayList<ITimeEvent>();
    private List<ITimeEvent> fZoomedEventList = null;

    /**
     * Standard constructor
     *
     * @param quark
     *            The quark of the state system attribute whose state is shown
     *            on this row
     * @param trace
     *            The trace that this view is talking about
     * @param name
     *            The name of this connection
     */
    public ConnectionsEntry(int quark, MyUstTrace trace, String name) {
        fQuark = quark;
        fTrace = trace;
        fName = name;
    }

    @Override
    public ITimeGraphEntry getParent() {
        return fParent;
    }

    @Override
    public boolean hasChildren() {
        return fChildren != null && fChildren.size() > 0;
    }

    @Override
    public ConnectionsEntry[] getChildren() {
        return fChildren.toArray(new ConnectionsEntry[0]);
    }

    @Override
    public String getName() {
        return fName;
    }

    @Override
    public long getStartTime() {
        return fStartTime;
    }

    @Override
    public long getEndTime() {
        return fEndTime;
    }

    @Override
    public boolean hasTimeEvents() {
        return true;
    }

    @Override
    public Iterator<ITimeEvent> getTimeEventsIterator() {
        return new EventIterator(fEventList, fZoomedEventList);
    }

    @Override
    public Iterator<ITimeEvent> getTimeEventsIterator(long startTime, long stopTime, long visibleDuration) {
        return new EventIterator(fEventList, fZoomedEventList, startTime, stopTime);
    }

    /**
     * Assign a parent entry to this one, to organize them in a tree in the
     * view.
     *
     * @param parent
     *            The parent entry
     */
    public void setParent(ITimeGraphEntry parent) {
        fParent = parent;
    }

    /**
     * Retrieve the attribute quark that's represented by this entry.
     *
     * @return The integer quark
     */
    public int getQuark() {
        return fQuark;
    }

    /**
     * Retrieve the trace that is associated to this Resource view.
     *
     * @return The flex trace
     */
    public MyUstTrace getTrace() {
        return fTrace;
    }

    /**
     * Assign the target event list to this view.
     *
     * @param eventList
     *            The list of time events
     */
    public void setEventList(List<ITimeEvent> eventList) {
        fEventList = eventList;
        if (eventList != null && eventList.size() > 0) {
            fStartTime = eventList.get(0).getTime();
            ITimeEvent lastEvent = eventList.get(eventList.size() - 1);
            fEndTime = lastEvent.getTime() + lastEvent.getDuration();
        }
    }

    /**
     * Assign the zoomed event list to this view.
     *
     * @param eventList
     *            The list of "zoomed" time events
     */
    public void setZoomedEventList(List<ITimeEvent> eventList) {
        fZoomedEventList = eventList;
    }

    /**
     * Add a child entry to this one (to show relationships between processes as
     * a tree)
     *
     * @param child
     *            The child entry
     */
    public void addChild(ConnectionsEntry child) {
        child.fParent = this;
        fChildren.add(child);
    }

    /**
     * Add an event to the event list
     *
     * @param timeEvent
     *          The event
     */
    public void addEvent(ITimeEvent timeEvent) {
        fEventList.add(timeEvent);
    }
}
