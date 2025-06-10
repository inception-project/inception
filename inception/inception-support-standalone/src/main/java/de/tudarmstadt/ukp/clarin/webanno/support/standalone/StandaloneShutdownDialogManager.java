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

import static de.tudarmstadt.ukp.clarin.webanno.support.standalone.StandaloneUserInterface.ACTION_OPEN_BROWSER;
import static de.tudarmstadt.ukp.clarin.webanno.support.standalone.StandaloneUserInterface.ACTION_SHUTDOWN;
import static de.tudarmstadt.ukp.clarin.webanno.support.standalone.StandaloneUserInterface.actionLocateSettingsProperties;
import static de.tudarmstadt.ukp.clarin.webanno.support.standalone.StandaloneUserInterface.actionShowAbout;
import static de.tudarmstadt.ukp.clarin.webanno.support.standalone.StandaloneUserInterface.actionShowLog;
import static de.tudarmstadt.ukp.clarin.webanno.support.standalone.StandaloneUserInterface.bringToFront;
import static de.tudarmstadt.ukp.inception.support.SettingsUtil.getSettingsFileLocation;
import static de.tudarmstadt.ukp.inception.support.logging.BaseLoggers.BOOT_LOG;
import static java.awt.BorderLayout.CENTER;
import static java.awt.Component.CENTER_ALIGNMENT;
import static java.awt.Desktop.getDesktop;
import static java.awt.Desktop.isDesktopSupported;
import static java.awt.EventQueue.invokeLater;
import static java.lang.ProcessBuilder.Redirect.INHERIT;
import static javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE;
import static javax.swing.WindowConstants.HIDE_ON_CLOSE;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.prependIfMissing;

import java.awt.AWTException;
import java.awt.BorderLayout;
import java.awt.Desktop;
import java.awt.GraphicsEnvironment;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.lang.invoke.MethodHandles;
import java.net.URI;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.UIManager;

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

import de.tudarmstadt.ukp.inception.support.SettingsUtil;

@ConditionalOnWebApplication
@Component("standaloneShutdownDialogManager")
@Order(Ordered.HIGHEST_PRECEDENCE)
public class StandaloneShutdownDialogManager
{
    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private ApplicationContext context;

    @Value("${server.servlet.context-path:}")
    private String servletContextPath;

    @Value("${running.from.commandline}")
    private boolean isRunningFromCommandline;

    @Value("${spring.application.name}")
    private String applicationName;

    @Value("${commands.open-browser:}")
    private String openBrowserCommand;

    private int port = -1;

    @EventListener
    public void onApplicationEvent(ApplicationReadyEvent aEvt)
    {
        var hasConsole = System.console() != null;
        var isHeadless = GraphicsEnvironment.isHeadless();
        BOOT_LOG.info("Console: " + (hasConsole ? "available" : "not available"));
        BOOT_LOG.info("Headless: " + (isHeadless ? "yes" : "no"));

        var forceUi = System.getProperty("forceUi") != null;

        // Show this only when run from the standalone JAR via a double-click
        if (forceUi || (port != -1 && !hasConsole && !isHeadless && isRunningFromCommandline)) {
            LOG.info(
                    "If you are running {} in a server environment, consider using '-Djava.awt.headless=true'",
                    applicationName);
            eventPublisher.publishEvent(
                    new ShutdownDialogAvailableEvent(StandaloneShutdownDialogManager.this));

            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            }
            catch (Exception e) {
                LOG.error("Unable to set system look and feel", e);
            }

            invokeLater(() -> showShutdownDialog());
        }
        else {
            BOOT_LOG.info(
                    "Running in server environment or from command line: disabling interactive shutdown dialog.");
            if (hasConsole) {
                BOOT_LOG.info("You can close INCEpTION from the command line by pressing Ctrl+C");
            }

            invokeLater(() -> {
                makeSystemTrayMenu();
            });
        }

        BOOT_LOG.info("You can now access INCEpTION at http://localhost:{}{}", port,
                prependIfMissing(servletContextPath, "/"));
    }

    @EventListener
    public void onApplicationEvent(WebServerInitializedEvent aEvt)
    {
        port = aEvt.getWebServer().getPort();
    }

    private void showShutdownDialog()
    {
        var frame = new JFrame(applicationName);

        // Image icon
        frame.setIconImage(StandaloneUserInterface.getIconImage());
        frame.setResizable(false);

        // Content panel
        var contentPanel = new JPanel();
        var padding = BorderFactory.createEmptyBorder(10, 10, 10, 10);
        contentPanel.setBorder(padding);
        contentPanel.setLayout(new BorderLayout());

        // Label
        var label = new JLabel("<html>" + applicationName + " is running now and can be "
                + "accessed via <b>http://localhost:" + port + "</b>." + "<br>" //
                + applicationName + " works best with the browsers Google Chrome or Safari.<br>" //
                + "Use this dialog to shut " + applicationName + " down.</html>");

        contentPanel.add(label, BorderLayout.PAGE_START);

        // Button Layout
        var buttonPanel = new JPanel();

        if (isDesktopSupported() && getDesktop().isSupported(Desktop.Action.BROWSE)) {
            var browseButton = new JButton(ACTION_OPEN_BROWSER);
            browseButton.addActionListener(e -> actionBrowse());
            buttonPanel.add(browseButton);
            frame.getRootPane().setDefaultButton(browseButton);
        }

        var shutdownButton = new JButton(ACTION_SHUTDOWN);
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
                LOG.error("Could not application menu add to tray", e);
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

    private void makeSystemTrayMenu()
    {
        try {
            makeSystemTrayMenu(null);
        }
        catch (AWTException e) {
            LOG.error("Could not application menu add to tray", e);
        }
    }

    private void makeSystemTrayMenu(JFrame aFrame) throws AWTException
    {
        if (GraphicsEnvironment.isHeadless()) {
            return;
        }

        var tray = SystemTray.getSystemTray();
        var trayIcon = new TrayIcon(StandaloneUserInterface.getIconImage());
        trayIcon.setImageAutoSize(true);
        trayIcon.setToolTip(applicationName + " " + SettingsUtil.getVersionString());

        var popupMenu = new PopupMenu();

        if (aFrame != null) {
            var showHideItem = new MenuItem("Show/Hide");
            showHideItem.addActionListener(e -> aFrame.setVisible(!aFrame.isVisible()));
            popupMenu.add(showHideItem);

            // Click should show/hide
            trayIcon.addActionListener(e -> aFrame.setVisible(!aFrame.isVisible()));
        }

        var browseItem = new MenuItem(ACTION_OPEN_BROWSER);
        browseItem.addActionListener(e -> actionBrowse());
        popupMenu.add(browseItem);

        var logItem = new MenuItem("Log...");
        logItem.addActionListener(e -> actionShowLog(applicationName));
        popupMenu.add(logItem);

        if (getSettingsFileLocation() != null) {
            MenuItem settingsPropertiesItem = new MenuItem("Locate settings file");
            settingsPropertiesItem.addActionListener(e -> actionLocateSettingsProperties());
            popupMenu.add(settingsPropertiesItem);
        }

        var aboutItem = new MenuItem("About...");
        aboutItem.addActionListener(e -> actionShowAbout(applicationName));
        popupMenu.add(aboutItem);

        var shutdownItem = new MenuItem(ACTION_SHUTDOWN);
        shutdownItem.addActionListener(e -> {
            tray.remove(trayIcon);
            actionShutdown();
        });
        popupMenu.add(shutdownItem);

        trayIcon.setPopupMenu(popupMenu);

        tray.add(trayIcon);
    }

    private void actionBrowse()
    {
        try {

            if (!isBlank(openBrowserCommand)) {
                var cmd = openBrowserCommand.replace("%u",
                        "http://localhost:" + port + prependIfMissing(servletContextPath, "/"));

                var cmdTokens = cmd.split("\\s(?=([^\"]*\"[^\"]*\")*[^\"]*$)");

                for (var i = 0; i < cmdTokens.length; i++) {
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
            LOG.error("Unable to open browser", e);
        }
    }

    private void actionShutdown()
    {
        System.exit(SpringApplication.exit(context));
    }
}
