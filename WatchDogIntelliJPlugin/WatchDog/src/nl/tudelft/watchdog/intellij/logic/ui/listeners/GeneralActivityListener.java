package nl.tudelft.watchdog.intellij.logic.ui.listeners;

import nl.tudelft.watchdog.intellij.logic.ui.WatchDogEventManager;
import nl.tudelft.watchdog.core.logic.ui.events.WatchDogEvent;
import nl.tudelft.watchdog.core.logic.ui.events.WatchDogEvent.EventType;
import nl.tudelft.watchdog.intellij.util.WatchDogUtils;

import java.awt.*;
import java.awt.event.*;

/**
 * A listener that determines whether there was general activity in the IntelliJ
 * window.
 */
public class GeneralActivityListener {

    private AWTEventListener mouseActivityListener;
    private AWTEventListener keyboardActivityListener;


    /** Constructor. */
    public GeneralActivityListener(final WatchDogEventManager eventManager, final String projectName) {
        mouseActivityListener = new AWTEventListener() {
            @Override
            public void eventDispatched(AWTEvent event) {
                if (!WatchDogUtils.getProjectName().equals(projectName)) {
                    return;
                }
                eventManager.update(new WatchDogEvent(event, EventType.USER_ACTIVITY));
            }
        };

        Toolkit.getDefaultToolkit().addAWTEventListener(mouseActivityListener, AWTEvent.MOUSE_EVENT_MASK | AWTEvent.MOUSE_MOTION_EVENT_MASK | AWTEvent.MOUSE_WHEEL_EVENT_MASK);

        keyboardActivityListener = new AWTEventListener() {
            @Override
            public void eventDispatched(AWTEvent event) {
                if (!WatchDogUtils.getProjectName().equals(projectName)) {
                    return;
                }
                switch (((KeyEvent) event).getKeyCode()) {
                    case KeyEvent.VK_RIGHT:
                    case KeyEvent.VK_UP:
                    case KeyEvent.VK_DOWN:
                    case KeyEvent.VK_LEFT:
                    case KeyEvent.VK_PAGE_DOWN:
                    case KeyEvent.VK_PAGE_UP:
                        eventManager.update(new WatchDogEvent(event, EventType.USER_ACTIVITY));
                }
            }
        };
        Toolkit.getDefaultToolkit().addAWTEventListener(keyboardActivityListener, AWTEvent.KEY_EVENT_MASK);
    }

    public void removeListeners() {
        Toolkit.getDefaultToolkit().removeAWTEventListener(mouseActivityListener);
        Toolkit.getDefaultToolkit().removeAWTEventListener(keyboardActivityListener);
    }
}
