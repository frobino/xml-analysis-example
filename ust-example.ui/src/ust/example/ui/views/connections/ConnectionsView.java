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
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.linuxtools.tmf.core.ctfadaptor.CtfTmfTimestamp;
import org.eclipse.linuxtools.tmf.core.event.ITmfTimestamp;
import org.eclipse.linuxtools.tmf.core.exceptions.AttributeNotFoundException;
import org.eclipse.linuxtools.tmf.core.exceptions.StateValueTypeException;
import org.eclipse.linuxtools.tmf.core.exceptions.TimeRangeException;
import org.eclipse.linuxtools.tmf.core.interval.ITmfStateInterval;
import org.eclipse.linuxtools.tmf.core.statesystem.IStateSystemQuerier;
import org.eclipse.linuxtools.tmf.core.trace.ITmfTrace;
import org.eclipse.linuxtools.tmf.core.trace.TmfExperiment;
import org.eclipse.linuxtools.tmf.ui.views.timegraph.AbstractTimeGraphView;
import org.eclipse.linuxtools.tmf.ui.widgets.timegraph.ITimeGraphPresentationProvider;
import org.eclipse.linuxtools.tmf.ui.widgets.timegraph.model.ITimeEvent;

import ust.example.core.trace.MyUstTrace;


/**
 * Example implementation of a Connections View
 *
 * @author Alexandre Montplaisir
 */
public class ConnectionsView extends AbstractTimeGraphView<ConnectionsEntry> {

    // ------------------------------------------------------------------------
    // Constants
    // ------------------------------------------------------------------------

    /** View ID. */
    public static final String ID = "ust.example.ui.views.connections"; //$NON-NLS-1$


    // ------------------------------------------------------------------------
    // Constructors
    // ------------------------------------------------------------------------

    /**
     * Default constructor
     */
    public ConnectionsView() {
        super(ID);

    }

    // ------------------------------------------------------------------------
    // Internal
    // ------------------------------------------------------------------------

    @Override
    protected void selectExperiment(TmfExperiment experiment) {
        fStartTime = Long.MAX_VALUE;
        fEndTime = Long.MIN_VALUE;
        fSelectedExperiment = experiment;
        ArrayList<TraceEntry> entryList = new ArrayList<TraceEntry>();
        for (ITmfTrace trace : experiment.getTraces()) {
            if (trace instanceof MyUstTrace) {
                MyUstTrace ctfFlexTrace = (MyUstTrace) trace;
                IStateSystemQuerier ssq = ctfFlexTrace.getStateSystem();
                long startTime = ssq.getStartTime();
                long endTime = ssq.getCurrentEndTime() + 1;
                TraceEntry traceEntry = new TraceEntry(ctfFlexTrace, trace.getName(), startTime, endTime);
                entryList.add(traceEntry);
                fStartTime = Math.min(fStartTime, startTime);
                fEndTime = Math.max(fEndTime, endTime);
                List<Integer> connectionQuarks = ssq.getQuarks("Connections", "*"); //$NON-NLS-1$ //$NON-NLS-2$
                for (int i = 0; i < connectionQuarks.size(); i++) {
                    int deviceQuark = connectionQuarks.get(i);
                    String device = ssq.getAttributeName(deviceQuark);

                    /* Add the entry */
                    ConnectionsEntry dspEntry = new ConnectionsEntry(deviceQuark, ctfFlexTrace, device);
                    traceEntry.addChild(dspEntry);
                }
            }
        }
        synchronized (fEntryListSyncObj) {
            fEntryList = (ArrayList<TraceEntry>) entryList.clone();
        }
        refresh();
        for (TraceEntry traceEntry : entryList) {
            ITmfTrace ctfFlexTrace = traceEntry.getTrace();
            IStateSystemQuerier ssq = ((MyUstTrace) ctfFlexTrace).getStateSystem();
            long startTime = ssq.getStartTime();
            long endTime = ssq.getCurrentEndTime() + 1;
            long resolution = (endTime - startTime) / fDisplayWidth;
            for (ConnectionsEntry entry : traceEntry.getChildren()) {
                List<ITimeEvent> eventList = getEventList(entry, startTime, endTime, resolution, false, new NullProgressMonitor());
                entry.setEventList(eventList);
                refresh();
            }
        }
    }

    @Override
    protected List<ITimeEvent> getEventList(ConnectionsEntry entry,
            long startTime, long endTime, long resolution,
            boolean includeNull, IProgressMonitor monitor) {

        IStateSystemQuerier ssq = ((MyUstTrace) entry.getTrace()).getStateSystem();
        startTime = Math.max(startTime, ssq.getStartTime());
        endTime = Math.min(endTime, ssq.getCurrentEndTime() + 1);
        if (endTime <= startTime) {
            return null;
        }
        List<ITimeEvent> eventList = null;
        int quark = entry.getQuark();
        try {

            List<ITmfStateInterval> statusIntervals = ssq.queryHistoryRange(quark, startTime, endTime - 1, resolution, monitor);
            eventList = new ArrayList<ITimeEvent>(statusIntervals.size());

            for (ITmfStateInterval statusInterval : statusIntervals) {
                if (monitor.isCanceled()) {
                    return null;
                }
                int status = statusInterval.getStateValue().unboxInt();
                long time = statusInterval.getStartTime();
                long duration = statusInterval.getEndTime() - time + 1;
                if (!statusInterval.getStateValue().isNull()) {
                    eventList.add(new ConnectionsEvent(entry, time, duration, status));
                }
            }

        } catch (AttributeNotFoundException e) {
            e.printStackTrace();
        } catch (TimeRangeException e) {
            e.printStackTrace();
        } catch (StateValueTypeException e) {
            e.printStackTrace();
        }
        return eventList;
    }

    // ------------------------------------------------------------------------
    // Abstract methods
    // ------------------------------------------------------------------------

    @Override
    protected String viewName() {
        return "Connections"; //$NON-NLS-1$
    }

    @Override
    protected Class<? extends ITmfTrace> getTraceType() {
        return MyUstTrace.class;
    }

    @Override
    protected ITmfTimestamp getNewTimestamp(long ts) {
        return new CtfTmfTimestamp(ts);
    }

    @Override
    protected ITimeGraphPresentationProvider getNewPresentationProvider() {
        return new ConnectionsPresentationProvider();
    }

}
