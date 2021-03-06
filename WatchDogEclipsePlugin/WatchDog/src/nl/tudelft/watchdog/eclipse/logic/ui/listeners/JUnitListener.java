package nl.tudelft.watchdog.eclipse.logic.ui.listeners;

import nl.tudelft.watchdog.core.logic.ui.events.JUnitEvent;
import nl.tudelft.watchdog.eclipse.logic.interval.intervaltypes.JUnitInterval;
import nl.tudelft.watchdog.eclipse.logic.ui.WatchDogEventManager;

import org.eclipse.jdt.junit.JUnitCore;
import org.eclipse.jdt.junit.TestRunListener;
import org.eclipse.jdt.junit.model.ITestRunSession;

/** A listener to the execution of Junit test via the IDE. */
public class JUnitListener {

	/** Constructor. */
	public JUnitListener(final WatchDogEventManager eventManager) {

		JUnitCore.addTestRunListener(new TestRunListener() {
			@Override
			public void sessionFinished(ITestRunSession session) {
				super.sessionFinished(session);
				JUnitInterval interval = new JUnitInterval(session);

				eventManager.update(new JUnitEvent(interval));
			}
		});
	}
}
