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

import static de.tudarmstadt.ukp.clarin.webanno.support.SettingsUtil.getSettingsFileLocation;
import static de.tudarmstadt.ukp.clarin.webanno.support.standalone.StandaloneUserInterface.ACTION_OPEN_BROWSER;
import static de.tudarmstadt.ukp.clarin.webanno.support.standalone.StandaloneUserInterface.ACTION_SHUTDOWN;
import static de.tudarmstadt.ukp.clarin.webanno.support.standalone.StandaloneUserInterface.actionLocateSettingsProperties;
import static de.tudarmstadt.ukp.clarin.webanno.support.standalone.StandaloneUserInterface.actionShowAbout;
import static de.tudarmstadt.ukp.clarin.webanno.support.standalone.StandaloneUserInterface.actionShowLog;
import static de.tudarmstadt.ukp.clarin.webanno.support.standalone.StandaloneUserInterface.bringToFront;
import static java.awt.BorderLayout.CENTER;
import static java.awt.Component.CENTER_ALIGNMENT;
import static java.awt.Desktop.getDesktop;
import static java.awt.Desktop.isDesktopSupported;
import static java.awt.EventQueue.invokeLater;
import static java.lang.ProcessBuilder.Redirect.INHERIT;
import static javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE;
import static javax.swing.WindowConstants.HIDE_ON_CLOSE;

import java.awt.AWTException;
import java.awt.BorderLayout;
import java.awt.Desktop;
import java.awt.GraphicsEnvironment;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.net.URI;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.UIManager;
import javax.swing.border.Border;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.web.context.WebServerInitializedEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import de.tudarmstadt.ukp.clarin.webanno.support.SettingsUtil;

@ConditionalOnWebApplication
@Component("standaloneShutdownDialogManager")
@Order(Ordered.HIGHEST_PRECEDENCE)
public class StandaloneShutdownDialogManager
{
    private final Logger log = LoggerFactory.getLogger(getClass());

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private ApplicationContext context;

    @Value("${running.from.commandline}")
    private boolean runningFromCommandline;

    @Value("${spring.application.name}")
    private String applicationName;

    @Value("${commands.open-browser:}")
    private String openBrowserCommand;

    private int port = -1;

    @EventListener
    public void onApplicationEvent(ApplicationReadyEvent aEvt)
    {
        log.info("Console: " + ((System.console() != null) ? "available" : "not available"));
        log.info("Headless: " + (GraphicsEnvironment.isHeadless() ? "yes" : "no"));

        boolean forceUi = System.getProperty("forceUi") != null;

        // Show this only when run from the standalone JAR via a double-click
        if (forceUi || (port != -1 && System.console() == null && !GraphicsEnvironment.isHeadless()
                && runningFromCommandline)) {
            log.info("If you are running " + applicationName
                    + " in a server environment, please use '-Djava.awt.headless=true'");
            eventPublisher.publishEvent(
                    new ShutdownDialogAvailableEvent(StandaloneShutdownDialogManager.this));

            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            }
            catch (Exception e) {
                log.info("Unable to set system look and feel", e);
            }

            invokeLater(() -> showShutdownDialog());
        }
        else {
            log.info(
                    "Running in server environment or from command line: disabling interactive shutdown dialog.");
            if (System.console() != null) {
                log.info("You can close INCEpTION from the command line by pressing Ctrl+C");
            }
        }

        log.info("You can now access INCEpTION at http://localhost:{}", port);
    }

    @EventListener
    public void onApplicationEvent(WebServerInitializedEvent aEvt)
    {
        port = aEvt.getWebServer().getPort();
    }

    private void showShutdownDialog()
    {
        JFrame frame = new JFrame(applicationName);

        // Image icon
        frame.setIconImage(StandaloneUserInterface.getIconImage());
        frame.setResizable(false);

        // Content panel
        JPanel contentPanel = new JPanel();
        Border padding = BorderFactory.createEmptyBorder(10, 10, 10, 10);
        contentPanel.setBorder(padding);
        contentPanel.setLayout(new BorderLayout());

        // Label
        JLabel label = new JLabel("<html>" + applicationName + " is running now and can be "
                + "accessed via <b>http://localhost:" + port + "</b>." + "<br>" //
                + applicationName + " works best with the browsers Google Chrome or Safari.<br>" //
                + "Use this dialog to shut " + applicationName + " down.</html>");

        contentPanel.add(label, BorderLayout.PAGE_START);

        // Button Layout
        JPanel buttonPanel = new JPanel();

        if (isDesktopSupported() && getDesktop().isSupported(Desktop.Action.BROWSE)) {
            JButton browseButton = new JButton(ACTION_OPEN_BROWSER);
            browseButton.addActionListener(e -> actionBrowse());
            buttonPanel.add(browseButton);
            frame.getRootPane().setDefaultButton(browseButton);
        }

        JButton shutdownButton = new JButton(ACTION_SHUTDOWN);
        shutdownButton.addActionListener(e -> actionShutdown());
        buttonPanel.add(shutdownButton);

        buttonPanel.setAlignmentX(CENTER_ALIGNMENT);
        contentPanel.add(buttonPanel, BorderLayout.PAGE_END);

        // Tray
        if (SystemTray.isSupported()) {
            try {
                // We only hide on close so that we can show the window again if needed
                frame.setDefaultCloseOperation(HIDE_ON_CLOSE);

                makeSystemTrayMenu(frame);

                contentPanel.add(new JLabel("Closing this window minimizes it to tray."), CENTER);
            }
            catch (AWTException e) {
                log.error("Could not application menu add to tray", e);
                frame.setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
            }

        }
        else {
            frame.setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        }

        frame.add(contentPanel);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        bringToFront(frame);
        frame.requestFocus();

        if (isDesktopSupported() && getDesktop().isSupported(Desktop.Action.BROWSE)) {
            actionBrowse();
        }
    }

    private void makeSystemTrayMenu(JFrame frame) throws AWTException
    {
        SystemTray tray = SystemTray.getSystemTray();
        TrayIcon trayIcon = new TrayIcon(frame.getIconImage());
        trayIcon.setImageAutoSize(true);

        PopupMenu popupMenu = new PopupMenu();

        MenuItem untrayItem = new MenuItem("Show/Hide");
        popupMenu.add(untrayItem);
        untrayItem.addActionListener(e -> frame.setVisible(!frame.isVisible()));

        MenuItem browseItem = new MenuItem(ACTION_OPEN_BROWSER);
        browseItem.addActionListener(e -> actionBrowse());
        popupMenu.add(browseItem);

        MenuItem logItem = new MenuItem("Log...");
        logItem.addActionListener(e -> actionShowLog(applicationName));
        popupMenu.add(logItem);

        if (getSettingsFileLocation() != null) {
            MenuItem settingsPropertiesItem = new MenuItem("Locate settings file");
            settingsPropertiesItem.addActionListener(e -> actionLocateSettingsProperties());
            popupMenu.add(settingsPropertiesItem);
        }

        MenuItem aboutItem = new MenuItem("About...");
        aboutItem.addActionListener(e -> actionShowAbout(applicationName));
        popupMenu.add(aboutItem);

        MenuItem shutdownItem = new MenuItem(ACTION_SHUTDOWN);
        shutdownItem.addActionListener(e -> {
            tray.remove(trayIcon);
            actionShutdown();
        });
        popupMenu.add(shutdownItem);

        trayIcon.setToolTip(applicationName + " " + SettingsUtil.getVersionString());
        trayIcon.setPopupMenu(popupMenu);

        // Click should show/hide
        trayIcon.addActionListener(e -> frame.setVisible(!frame.isVisible()));

        tray.add(trayIcon);
    }

    private void actionBrowse()
    {
        try {
            if (!StringUtils.isBlank(openBrowserCommand)) {
                String cmd = openBrowserCommand.replace("%u", "http://localhost:" + port);

                String[] cmdTokens = cmd.split("\\s(?=([^\"]*\"[^\"]*\")*[^\"]*$)");

                for (int i = 0; i < cmdTokens.length; i++) {
                    if (cmdTokens[i].startsWith("\"") || cmdTokens[i].endsWith("\"")) {
                        cmdTokens[i] = StringUtils.substring(cmdTokens[i], 1, -1);
                    }
                }

                new ProcessBuilder(cmdTokens) //
                        .redirectOutput(INHERIT) //
                        .redirectError(INHERIT) //
                        .start();

                return;
            }

            Desktop.getDesktop().browse(URI.create("http://localhost:" + port));
        }
        catch (Exception e) {
            log.error("Unable to open browser", e);
        }
    }

    private void actionShutdown()
    {
        System.exit(SpringApplication.exit(context));
    }
}
