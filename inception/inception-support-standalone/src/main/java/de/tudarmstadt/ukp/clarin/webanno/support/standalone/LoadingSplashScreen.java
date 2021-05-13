/*
 * Licensed to the Technische Universität Darmstadt under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The Technische Universität Darmstadt 
 * licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.
 *  
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tudarmstadt.ukp.clarin.webanno.support.standalone;

import static javax.swing.JOptionPane.ERROR_MESSAGE;
import static javax.swing.JOptionPane.showMessageDialog;
import static org.apache.commons.lang3.exception.ExceptionUtils.getRootCause;
import static org.apache.commons.lang3.exception.ExceptionUtils.getRootCauseMessage;
import static org.slf4j.LoggerFactory.getLogger;

import java.awt.AWTException;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.SystemTray;
import java.awt.Toolkit;
import java.awt.TrayIcon;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.lang.invoke.MethodHandles;
import java.net.BindException;
import java.net.URL;
import java.util.Optional;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;

import org.slf4j.Logger;
import org.springframework.boot.context.event.ApplicationFailedEvent;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationEvent;

public class LoadingSplashScreen
{
    private static final Logger LOG = getLogger(MethodHandles.lookup().lookupClass());

    public static Optional<SplashWindow> setupScreen(URL aSplashScreenImageUrl, URL aIconUrl,
            String aApplicationName)
    {
        if (GraphicsEnvironment.isHeadless()) {
            return Optional.empty();
        }

        SplashWindow window = new SplashWindow(aSplashScreenImageUrl, aIconUrl, aApplicationName);
        window.setVisible(true);

        return Optional.of(window);
    }

    public static Optional<SplashWindow> setupScreen(String aApplicationName)
    {
        URL splasHScreenImageUrl = LoadingSplashScreen.class.getResource("/splash.png");
        URL iconUrl = LoadingSplashScreen.class.getResource("/icon.png");
        return setupScreen(splasHScreenImageUrl, iconUrl, aApplicationName);
    }

    public static class SplashWindow
        extends JFrame
    {
        private static final long serialVersionUID = 2429606748142131008L;

        private JLabel info;
        private boolean disposed = false;
        private String applicationName;
        private TrayIcon trayIcon;

        public SplashWindow(URL aSplashScreenImageUrl, URL aIconUrl, String aApplicationName)
        {
            applicationName = aApplicationName;

            JLabel l = new JLabel(new ImageIcon(aSplashScreenImageUrl));
            getContentPane().add(l, BorderLayout.CENTER);

            info = new JLabel(applicationName + " is loading...");
            getContentPane().add(info, BorderLayout.SOUTH);

            ImageIcon img = new ImageIcon(aIconUrl);
            setIconImage(img.getImage());

            setUndecorated(true);
            pack();

            Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
            Dimension labelSize = l.getPreferredSize();
            setLocation(screenSize.width / 2 - (labelSize.width / 2),
                    screenSize.height / 2 - (labelSize.height / 2));

            addMouseMotionListener(new MouseMotionListener()
            {
                private int mx, my;

                @Override
                public void mouseMoved(MouseEvent aEvent)
                {
                    mx = aEvent.getXOnScreen();
                    my = aEvent.getYOnScreen();
                }

                @Override
                public void mouseDragged(MouseEvent aEvent)
                {
                    Point p = SplashWindow.this.getLocation();
                    p.x += aEvent.getXOnScreen() - mx;
                    p.y += aEvent.getYOnScreen() - my;
                    mx = aEvent.getXOnScreen();
                    my = aEvent.getYOnScreen();
                    SplashWindow.this.setLocation(p);
                }
            });

            setVisible(true);

            // Tray
            if (SystemTray.isSupported()) {
                try {
                    trayIcon = StandaloneUserInterface.makeStartupSystemTrayMenu(aApplicationName);
                }
                catch (AWTException e) {
                    LOG.error("Could not add startup menu to tray", e);
                }
            }
        }

        @Override
        public void dispose()
        {
            if (trayIcon != null) {
                SystemTray.getSystemTray().remove(trayIcon);
            }

            disposed = true;
            super.dispose();
        }

        public boolean isDisposed()
        {
            return disposed;
        }

        public void setInfo(String aInfo)
        {
            if (info == null) {
                return;
            }

            info.setText(aInfo);
        }

        public void handleEvent(ApplicationEvent aEvent)
        {
            if (aEvent instanceof ApplicationReadyEvent
                    || aEvent instanceof ShutdownDialogAvailableEvent) {
                dispose();
                return;
            }

            if (!isDisposed()) {
                setInfo(applicationName + " is loading... - " + aEvent.getClass().getSimpleName());
            }

            if (aEvent instanceof ApplicationFailedEvent) {
                ApplicationFailedEvent failEvent = (ApplicationFailedEvent) aEvent;
                showMessageDialog(null, getErrorMessage(failEvent), applicationName + " - Error",
                        ERROR_MESSAGE);
                System.exit(0);
            }
        }

        public String getErrorMessage(ApplicationFailedEvent aEvent)
        {
            if (aEvent.getException() == null) {
                return "Unknown error";
            }

            StringBuilder msg = new StringBuilder();

            String rootCauseMsg = getRootCauseMessage(aEvent.getException());
            Throwable rootCause = getRootCause(aEvent.getException());
            if (rootCause instanceof BindException || rootCauseMsg.contains("already in use")) {
                msg.append("It appears the network port " + applicationName
                        + " is trying to use is already being used by another application.\nMaybe "
                        + "you have already started " + applicationName + " before?\n");
            }

            msg.append("\n");
            msg.append("Error type: " + getRootCause(aEvent.getException()).getClass() + "\n");
            msg.append("Error message: " + rootCauseMsg);

            return msg.toString();
        }
    }
}
