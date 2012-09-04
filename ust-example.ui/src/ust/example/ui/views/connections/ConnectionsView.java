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
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.linuxtools.tmf.core.ctfadaptor.CtfTmfTimestamp;
import org.eclipse.linuxtools.tmf.core.event.TmfTimeRange;
import org.eclipse.linuxtools.tmf.core.event.TmfTimestamp;
import org.eclipse.linuxtools.tmf.core.exceptions.AttributeNotFoundException;
import org.eclipse.linuxtools.tmf.core.exceptions.StateValueTypeException;
import org.eclipse.linuxtools.tmf.core.exceptions.TimeRangeException;
import org.eclipse.linuxtools.tmf.core.interval.ITmfStateInterval;
import org.eclipse.linuxtools.tmf.core.signal.TmfExperimentSelectedSignal;
import org.eclipse.linuxtools.tmf.core.signal.TmfRangeSynchSignal;
import org.eclipse.linuxtools.tmf.core.signal.TmfSignalHandler;
import org.eclipse.linuxtools.tmf.core.signal.TmfStateSystemBuildCompleted;
import org.eclipse.linuxtools.tmf.core.signal.TmfTimeSynchSignal;
import org.eclipse.linuxtools.tmf.core.statesystem.IStateSystemQuerier;
import org.eclipse.linuxtools.tmf.core.trace.ITmfTrace;
import org.eclipse.linuxtools.tmf.core.trace.TmfExperiment;
import org.eclipse.linuxtools.tmf.ui.views.TmfView;
import org.eclipse.linuxtools.tmf.ui.widgets.timegraph.ITimeGraphRangeListener;
import org.eclipse.linuxtools.tmf.ui.widgets.timegraph.ITimeGraphTimeListener;
import org.eclipse.linuxtools.tmf.ui.widgets.timegraph.TimeGraphRangeUpdateEvent;
import org.eclipse.linuxtools.tmf.ui.widgets.timegraph.TimeGraphTimeEvent;
import org.eclipse.linuxtools.tmf.ui.widgets.timegraph.TimeGraphViewer;
import org.eclipse.linuxtools.tmf.ui.widgets.timegraph.model.ITimeEvent;
import org.eclipse.linuxtools.tmf.ui.widgets.timegraph.model.ITimeGraphEntry;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IActionBars;

import ust.example.core.trace.MyUstTrace;


/**
 * Main implementation for the Flex Oscilloscope view
 *
 * @author Patrick Tasse
 * @author Alexandre Montplaisir
 */
public class ConnectionsView extends TmfView {

    // ------------------------------------------------------------------------
    // Constants
    // ------------------------------------------------------------------------

    /** View ID. */
    public static final String ID = "ust.example.ui.views.connections"; //$NON-NLS-1$

    /**
     * Redraw state enum
     */
    private enum State { IDLE, BUSY, PENDING }

    // ------------------------------------------------------------------------
    // Fields
    // ------------------------------------------------------------------------

    // The time graph viewer
    TimeGraphViewer fTimeGraphViewer;

    // The selected experiment
    private TmfExperiment fSelectedExperiment;

    // The time graph entry list
    private ArrayList<TraceEntry> fEntryList;

    // The time graph entry list synchronization object
    final private Object fEntryListSyncObj = new Object();

    // The start time
    private long fStartTime;

    // The end time
    private long fEndTime;

    // The display width
    private final int fDisplayWidth;

    // The next resource action
    private Action fNextConnectionAction;

    // The previous resource action
    private Action fPreviousConnectionAction;

    // The zoom thread
    private ZoomThread fZoomThread;

    // The redraw state used to prevent unnecessary queuing of display runnables
    private State fRedrawState = State.IDLE;

    // The redraw synchronization object
    final private Object fSyncObj = new Object();

    // ------------------------------------------------------------------------
    // Classes
    // ------------------------------------------------------------------------

    private class TraceEntry implements ITimeGraphEntry {
        // The Trace
        private final MyUstTrace fTrace;
        // The start time
        private final long fTraceStartTime;
        // The end time
        private final long fTraceEndTime;
        // The children of the entry
        private final ArrayList<ConnectionsEntry> fChildren;
        // The name of entry
        private final String fName;

        public TraceEntry(MyUstTrace trace, String name, long startTime, long endTime) {
            fTrace = trace;
            fChildren = new ArrayList<ConnectionsEntry>();
            fName = name;
            fTraceStartTime = startTime;
            fTraceEndTime = endTime;
        }

        @Override
        public ITimeGraphEntry getParent() {
            return null;
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
            return fTraceStartTime;
        }

        @Override
        public long getEndTime() {
            return fTraceEndTime;
        }

        @Override
        public boolean hasTimeEvents() {
            return false;
        }

        @Override
        public Iterator<ITimeEvent> getTimeEventsIterator() {
            return null;
        }

        @Override
        public <T extends ITimeEvent> Iterator<T> getTimeEventsIterator(long startTime, long stopTime, long visibleDuration) {
            return null;
        }

        public MyUstTrace getTrace() {
            return fTrace;
        }

        public void addChild(ConnectionsEntry entry1) {
            int index;
            for (index = 0; index < fChildren.size(); index++ ) {
                ConnectionsEntry other = fChildren.get(index);
                if (entry1.getName().compareTo(other.getName()) < 0) {
                    break;
                }
            }
            entry1.setParent(this);
            fChildren.add(index, entry1);
        }
    }

    private static class TraceEntryComparator implements Comparator<ITimeGraphEntry> {
        @Override
        public int compare(ITimeGraphEntry o1, ITimeGraphEntry o2) {
            int result = o1.getStartTime() < o2.getStartTime() ? -1 : o1.getStartTime() > o2.getStartTime() ? 1 : 0;
            if (result == 0) {
                result = o1.getName().compareTo(o2.getName());
            }
            return result;
        }
    }

    private class ZoomThread extends Thread {
        private final long fZoomStartTime;
        private final long fZoomEndTime;
        private final IProgressMonitor fMonitor;

        public ZoomThread(long startTime, long endTime) {
            super("OscilloscopeView zoom"); //$NON-NLS-1$
            fZoomStartTime = startTime;
            fZoomEndTime = endTime;
            fMonitor = new NullProgressMonitor();
        }

        @Override
        public void run() {
            ArrayList<TraceEntry> entryList = null;
            synchronized (fEntryListSyncObj) {
                entryList = fEntryList;
            }
            if (entryList == null) {
                return;
            }
            long resolution = Math.max(1, (fZoomEndTime - fZoomStartTime) / fDisplayWidth);
            for (TraceEntry traceEntry : entryList) {
                for (ITimeGraphEntry child : traceEntry.getChildren()) {
                    if (fMonitor.isCanceled()) {
                        break;
                    }
                    ConnectionsEntry entry = (ConnectionsEntry) child;
                    if (fZoomStartTime <= fStartTime && fZoomEndTime >= fEndTime) {
                        entry.setZoomedEventList(null);
                    } else {
                        List<ITimeEvent> zoomedEventList = getEventList(entry,
                                fZoomStartTime, fZoomEndTime, resolution,
                                fMonitor);
                        if (zoomedEventList != null) {
                            entry.setZoomedEventList(zoomedEventList);
                        }
                    }
                    redraw();
                }
            }
        }

        public void cancel() {
            fMonitor.setCanceled(true);
        }
    }

    // ------------------------------------------------------------------------
    // Constructors
    // ------------------------------------------------------------------------

    /**
     * Default constructor
     */
    public ConnectionsView() {
        super(ID);
        fDisplayWidth = Display.getDefault().getBounds().width;
    }

    // ------------------------------------------------------------------------
    // ViewPart
    // ------------------------------------------------------------------------

    /* (non-Javadoc)
     * @see org.eclipse.linuxtools.tmf.ui.views.TmfView#createPartControl(org.eclipse.swt.widgets.Composite)
     */
    @Override
    public void createPartControl(Composite parent) {
        fTimeGraphViewer = new TimeGraphViewer(parent, SWT.NONE);

        fTimeGraphViewer.setTimeGraphProvider(new ConnectionsPresentationProvider());

        fTimeGraphViewer.addRangeListener(new ITimeGraphRangeListener() {
            @Override
            public void timeRangeUpdated(TimeGraphRangeUpdateEvent event) {
                long startTime = event.getStartTime();
                long endTime = event.getEndTime();
                TmfTimeRange range = new TmfTimeRange(new CtfTmfTimestamp(startTime), new CtfTmfTimestamp(endTime));
                TmfTimestamp time = new CtfTmfTimestamp(fTimeGraphViewer.getSelectedTime());
                broadcast(new TmfRangeSynchSignal(ConnectionsView.this, range, time));
                startZoomThread(startTime, endTime);
            }
        });

        fTimeGraphViewer.addTimeListener(new ITimeGraphTimeListener() {
            @Override
            public void timeSelected(TimeGraphTimeEvent event) {
                long time = event.getTime();
                broadcast(new TmfTimeSynchSignal(ConnectionsView.this, new CtfTmfTimestamp(time)));
            }
        });

        final Thread thread = new Thread("OscilloscopeView build") { //$NON-NLS-1$
            @Override
            public void run() {
                if (TmfExperiment.getCurrentExperiment() != null) {
                    selectExperiment(TmfExperiment.getCurrentExperiment());
                }
            }
        };
        thread.start();

        // View Action Handling
        makeActions();
        contributeToActionBars();
    }

    /* (non-Javadoc)
     * @see org.eclipse.ui.part.WorkbenchPart#setFocus()
     */
    @Override
    public void setFocus() {
        fTimeGraphViewer.setFocus();
    }

    // ------------------------------------------------------------------------
    // Signal handlers
    // ------------------------------------------------------------------------

    /**
     * Handler for the ExperimentSelected signal
     *
     * @param signal
     *            The incoming signal
     */
    @TmfSignalHandler
    public void experimentSelected(final TmfExperimentSelectedSignal signal) {
        if (signal.getExperiment().equals(fSelectedExperiment)) {
            return;
        }

        final Thread thread = new Thread("OscilloscopeView build") { //$NON-NLS-1$
            @Override
            public void run() {
                selectExperiment(signal.getExperiment());
            }
        };
        thread.start();
    }

    /**
     * Handler for the TimeSynch signal
     *
     * @param signal
     *            The incoming signal
     */
    @TmfSignalHandler
    public void synchToTime(final TmfTimeSynchSignal signal) {
        if (signal.getSource() == this || fSelectedExperiment == null) {
            return;
        }
        final long time = signal.getCurrentTime().normalize(0, -9).getValue();
        Display.getDefault().asyncExec(new Runnable() {
            @Override
            public void run() {
                if (fTimeGraphViewer.getControl().isDisposed()) {
                    return;
                }
                fTimeGraphViewer.setSelectedTime(time, true);
                startZoomThread(fTimeGraphViewer.getTime0(), fTimeGraphViewer.getTime1());
            }
        });
    }

    /**
     * Handler for the RangeSynch signal
     *
     * @param signal
     *            The incoming signal
     */
    @TmfSignalHandler
    public void synchToRange(final TmfRangeSynchSignal signal) {
        if (signal.getSource() == this || fSelectedExperiment == null) {
            return;
        }
        final long startTime = signal.getCurrentRange().getStartTime().normalize(0, -9).getValue();
        final long endTime = signal.getCurrentRange().getEndTime().normalize(0, -9).getValue();
        final long time = signal.getCurrentTime().normalize(0, -9).getValue();
        Display.getDefault().asyncExec(new Runnable() {
            @Override
            public void run() {
                if (fTimeGraphViewer.getControl().isDisposed()) {
                    return;
                }
                fTimeGraphViewer.setStartFinishTime(startTime, endTime);
                fTimeGraphViewer.setSelectedTime(time, false);
                startZoomThread(startTime, endTime);
            }
        });
    }

    /**
     * Handler for the StatesystemBuildCompleted signal
     *
     * @param signal
     *            The incoming signal
     */
    @TmfSignalHandler
    public void stateSystemBuildCompleted (final TmfStateSystemBuildCompleted signal) {
        final TmfExperiment selectedExperiment = fSelectedExperiment;
        if (selectedExperiment == null || selectedExperiment.getTraces() == null) {
            return;
        }
        for (ITmfTrace trace : selectedExperiment.getTraces()) {
            if (trace == signal.getTrace() && trace instanceof MyUstTrace) {
                final Thread thread = new Thread("OscilloscopeView build") { //$NON-NLS-1$
                    @Override
                    public void run() {
                        // rebuild the model
                        selectExperiment(selectedExperiment);
                    }
                };
                thread.start();
            }
        }
    }

    // ------------------------------------------------------------------------
    // Internal
    // ------------------------------------------------------------------------

    private void selectExperiment(TmfExperiment experiment) {
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
                    int connectionQuark = connectionQuarks.get(i);
                    String name = ssq.getAttributeName(connectionQuark);

                    /* Add the entry */
                    ConnectionsEntry entry = new ConnectionsEntry(connectionQuark, ctfFlexTrace, name);
                    traceEntry.addChild(entry);
                }
            }
        }
        synchronized (fEntryListSyncObj) {
            fEntryList = (ArrayList<TraceEntry>) entryList.clone();
        }
        refresh();
        for (TraceEntry traceEntry : entryList) {
            MyUstTrace ctfFlexTrace = traceEntry.getTrace();
            IStateSystemQuerier ssq = ctfFlexTrace.getStateSystem();
            long startTime = ssq.getStartTime();
            long endTime = ssq.getCurrentEndTime() + 1;
            long resolution = (endTime - startTime) / fDisplayWidth;
            for (ConnectionsEntry entry : traceEntry.getChildren()) {
                List<ITimeEvent> eventList = getEventList(entry, startTime, endTime, resolution, new NullProgressMonitor());
                entry.setEventList(eventList);
                refresh();
            }
        }
    }

    private static List<ITimeEvent> getEventList(ConnectionsEntry entry,
            long startTime, long endTime, long resolution,
            IProgressMonitor monitor) {

        IStateSystemQuerier ssq = entry.getTrace().getStateSystem();
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

    private void refresh() {
        Display.getDefault().asyncExec(new Runnable() {
            @Override
            public void run() {
                if (fTimeGraphViewer.getControl().isDisposed()) {
                    return;
                }
                ITimeGraphEntry[] entries = null;
                synchronized (fEntryListSyncObj) {
                    entries = fEntryList.toArray(new ITimeGraphEntry[0]);
                }
                Arrays.sort(entries, new TraceEntryComparator());
                fTimeGraphViewer.setInput(entries);
                for (ITimeGraphEntry entry : entries) {
                    for (ITimeGraphEntry child : entry.getChildren()) {
                        fTimeGraphViewer.setExpandedState(child, false);
                    }
                }
                fTimeGraphViewer.setTimeBounds(fStartTime, fEndTime);
                fTimeGraphViewer.setStartFinishTime(fStartTime, fEndTime);

                startZoomThread(fStartTime, fEndTime);
            }
        });
    }


    private void redraw() {
        synchronized (fSyncObj) {
            if (fRedrawState == State.IDLE) {
                fRedrawState = State.BUSY;
            } else {
                fRedrawState = State.PENDING;
                return;
            }
        }
        Display.getDefault().asyncExec(new Runnable() {
            @Override
            public void run() {
                if (fTimeGraphViewer.getControl().isDisposed()) {
                    return;
                }
                fTimeGraphViewer.getControl().redraw();
                fTimeGraphViewer.getControl().update();
                synchronized (fSyncObj) {
                    if (fRedrawState == State.PENDING) {
                        fRedrawState = State.IDLE;
                        redraw();
                    } else {
                        fRedrawState = State.IDLE;
                    }
                }
            }
        });
    }

    private void startZoomThread(long startTime, long endTime) {
        if (fZoomThread != null) {
            fZoomThread.cancel();
        }
        fZoomThread = new ZoomThread(startTime, endTime);
        fZoomThread.start();
    }

    private void makeActions() {
        fPreviousConnectionAction = fTimeGraphViewer.getPreviousItemAction();
        fPreviousConnectionAction.setText("Previous Connection"); //$NON-NLS-1$
        fPreviousConnectionAction.setToolTipText("Select Previous Connection"); //$NON-NLS-1$
        fNextConnectionAction = fTimeGraphViewer.getNextItemAction();
        fNextConnectionAction.setText("Next Connection"); //$NON-NLS-1$
        fNextConnectionAction.setToolTipText("Select Next Connection"); //$NON-NLS-1$
    }

    private void contributeToActionBars() {
        IActionBars bars = getViewSite().getActionBars();
        fillLocalToolBar(bars.getToolBarManager());
    }

    private void fillLocalToolBar(IToolBarManager manager) {
        manager.add(fTimeGraphViewer.getShowLegendAction());
        manager.add(new Separator());
        manager.add(fTimeGraphViewer.getResetScaleAction());
        manager.add(fTimeGraphViewer.getPreviousEventAction());
        manager.add(fTimeGraphViewer.getNextEventAction());
        manager.add(fPreviousConnectionAction);
        manager.add(fNextConnectionAction);
        manager.add(fTimeGraphViewer.getZoomInAction());
        manager.add(fTimeGraphViewer.getZoomOutAction());
        manager.add(new Separator());
    }
}
