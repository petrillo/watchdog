package nl.tudelft.watchdog.eclipse.logic.ui.listeners;

import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchListener;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

import nl.tudelft.watchdog.core.logic.event.DebugEventManager;
import nl.tudelft.watchdog.core.logic.ui.events.WatchDogEvent;
import nl.tudelft.watchdog.core.logic.ui.events.WatchDogEvent.EventType;
import nl.tudelft.watchdog.eclipse.logic.InitializationManager;
import nl.tudelft.watchdog.eclipse.logic.event.listeners.BreakpointListener;
import nl.tudelft.watchdog.eclipse.logic.event.listeners.DebugEventListener;
import nl.tudelft.watchdog.eclipse.logic.network.TransferManager;
import nl.tudelft.watchdog.eclipse.logic.ui.WatchDogEventManager;

/**
 * Sets up the listeners for eclipse UI events and registers the shutdown
 * listeners.
 */
public class WorkbenchListener {
	/** The serialization manager. */
	private TransferManager transferManager;

	/** The editorObservable. */
	private WatchDogEventManager watchDogEventManager;

	/** The debug event manager used to process debug events. */
	private DebugEventManager debugEventManager;

	/**
	 * The window listener. An Eclipse window is the whole Eclipse application
	 * window.
	 */
	private WindowListener windowListener;

	private IWorkbench workbench;

	/**
	 * Constructor.
	 * 
	 * @param debugEventManager
	 */
	public WorkbenchListener(WatchDogEventManager userActionManager,
			DebugEventManager debugEventManager,
			TransferManager transferManager) {
		this.watchDogEventManager = userActionManager;
		this.debugEventManager = debugEventManager;
		this.transferManager = transferManager;
		this.workbench = PlatformUI.getWorkbench();
	}

	/**
	 * Adds listeners to Workbench including already opened windows and
	 * registers shutdown and debugger listeners.
	 */
	public void attachListeners() {
		watchDogEventManager
				.update(new WatchDogEvent(workbench, EventType.START_IDE));
		windowListener = new WindowListener(watchDogEventManager);
		workbench.addWindowListener(windowListener);
		addListenersToAlreadyOpenWindows();
		new JUnitListener(watchDogEventManager);
		new GeneralActivityListener(watchDogEventManager,
				workbench.getDisplay());
		addDebuggerListeners();
		addShutdownListeners();
	}

	/** Initializes the listeners for debug intervals and events. */
	private void addDebuggerListeners() {
		DebugPlugin debugPlugin = DebugPlugin.getDefault();
		debugPlugin.addDebugEventListener(
				new DebuggerListener(watchDogEventManager));
		debugPlugin.getBreakpointManager().addBreakpointListener(
				new BreakpointListener(debugEventManager));
		debugPlugin.addDebugEventListener(
				new DebugEventListener(debugEventManager));
	}

	/** The shutdown listeners, executed when Eclipse is shutdown. */
	private void addShutdownListeners() {
		workbench.addWorkbenchListener(new IWorkbenchListener() {

			private InitializationManager initializationManager;

			@Override
			public boolean preShutdown(final IWorkbench workbench,
					final boolean forced) {
				initializationManager = InitializationManager.getInstance();
				watchDogEventManager.update(
						new WatchDogEvent(workbench, EventType.END_IDE));
				initializationManager.getIntervalManager().closeAllIntervals();
				transferManager.sendItemsImmediately();
				return true;
			}

			@Override
			public void postShutdown(final IWorkbench workbench) {
				initializationManager.shutdown();
			}
		});
	}

	/**
	 * If windows are already open when the listener registration from WatchDog
	 * starts (e.g. due to saved Eclipse workspace state), add these listeners
	 * to already opened windows.
	 * 
	 * This is usually the single Eclipse application window.
	 */
	private void addListenersToAlreadyOpenWindows() {
		for (IWorkbenchWindow window : workbench.getWorkbenchWindows()) {
			windowListener.windowOpened(window);
		}
		IWorkbenchWindow activeWindow = workbench.getActiveWorkbenchWindow();
		windowListener.windowActivated(activeWindow);
	}

}