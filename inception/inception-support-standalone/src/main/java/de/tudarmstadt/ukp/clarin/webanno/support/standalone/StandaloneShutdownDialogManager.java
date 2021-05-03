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

import static java.awt.Component.CENTER_ALIGNMENT;

import java.awt.AWTException;
import java.awt.BorderLayout;
import java.awt.Desktop;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.time.Year;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import javax.swing.border.Border;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.web.context.WebServerInitializedEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import de.tudarmstadt.ukp.clarin.webanno.support.SettingsUtil;

@Component("standaloneShutdownDialogManager")
@Order(Ordered.HIGHEST_PRECEDENCE)
public class StandaloneShutdownDialogManager
{
    private final Logger log = LoggerFactory.getLogger(getClass());

    private static final String ACTION_OPEN_BROWSER = "Open browser";
    private static final String ACTION_SHUTDOWN = "Shut down";

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Value("${running.from.commandline}")
    private boolean runningFromCommandline;

    @Value("${spring.application.name}")
    private String applicationName;

    private int port = -1;

    @EventListener
    public void onApplicationEvent(ApplicationReadyEvent aEvt)
    {
        log.info("Console: " + ((System.console() != null) ? "available" : "not available"));
        log.info("Headless: " + (GraphicsEnvironment.isHeadless() ? "yes" : "no"));

        // Show this only when run from the standalone JAR via a double-click
        if (port != -1 && System.console() == null && !GraphicsEnvironment.isHeadless()
                && runningFromCommandline) {
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

            EventQueue.invokeLater(this::showShutdownDialog);
        }
        else {
            log.info(
                    "Running in server environment or from command line: disabling interactive shutdown dialog.");
            log.info("You can close INCEpTION from the comand line by pressing via Ctrl+C");
        }
    }

    @EventListener
    public void onApplicationEvent(WebServerInitializedEvent aEvt)
    {
        port = aEvt.getWebServer().getPort();
    }

    private void actionBrowse()
    {
        try {
            Desktop.getDesktop().browse(URI.create("http://localhost:8080"));
        }
        catch (IOException e) {
            log.error("Unable to open browser", e);
        }
    }

    private void actionShutdown()
    {
        System.exit(0);
    }

    private void actionShowAbout()
    {
        JFrame frame = new JFrame(applicationName + " - About");

        JPanel contentPanel = new JPanel();
        Border padding = BorderFactory.createEmptyBorder(10, 10, 10, 10);
        contentPanel.setBorder(padding);
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.PAGE_AXIS));

        JLabel header = new JLabel(applicationName);
        header.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 16));
        contentPanel.add(header);

        contentPanel.add(new JLabel(
                "A semantic annotation platform offering intelligent assistance and knowledge management."));

        contentPanel.add(new JLabel(
                "<html>For more information, see <b>https://inception-project.github.io</b>"));

        contentPanel.add(new JLabel("Java version: " + System.getProperty("java.version")));
        contentPanel.add(new JLabel("INCEpTION version: " + SettingsUtil.getVersionString()));

        contentPanel.add(
                new JLabel("INCEpTION is provided as open source under the Apache License v2.0."));
        contentPanel.add(new JLabel("<html>© " + Year.now().getValue()
                + " The Ubiquitous Knowledge Processing (UKP) Lab <br/>"
                + "at the Department of Computer Science, Technical University Darmstadt.</html>"));

        frame.add(contentPanel);

        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private void showShutdownDialog()
    {
        JFrame frame = new JFrame(applicationName);

        // Image icon
        URL iconUrl = LoadingSplashScreen.class.getResource("/icon.png");
        ImageIcon icon = new ImageIcon(iconUrl);
        frame.setIconImage(icon.getImage());
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

        JButton shutdownButton = new JButton(ACTION_SHUTDOWN);
        shutdownButton.addActionListener(e -> actionShutdown());
        buttonPanel.add(shutdownButton);

        if (Desktop.isDesktopSupported()
                && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            JButton browseButton = new JButton(ACTION_OPEN_BROWSER);
            browseButton.addActionListener(e -> actionBrowse());
            buttonPanel.add(browseButton);
        }
        buttonPanel.setAlignmentX(CENTER_ALIGNMENT);
        contentPanel.add(buttonPanel, BorderLayout.PAGE_END);

        // Tray
        if (SystemTray.isSupported()) {
            // We only hide on close so that we can show the window again if needed
            frame.setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);

            SystemTray tray = SystemTray.getSystemTray();
            TrayIcon trayIcon = new TrayIcon(icon.getImage());
            trayIcon.setImageAutoSize(true);

            PopupMenu popupMenu = new PopupMenu();

            MenuItem untrayItem = new MenuItem("Show/Hide");
            popupMenu.add(untrayItem);
            untrayItem.addActionListener(e -> frame.setVisible(!frame.isVisible()));

            MenuItem browseItem = new MenuItem(ACTION_OPEN_BROWSER);
            browseItem.addActionListener(e -> actionBrowse());
            popupMenu.add(browseItem);

            MenuItem aboutItem = new MenuItem("About");
            aboutItem.addActionListener(e -> actionShowAbout());
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

            try {
                tray.add(trayIcon);

                contentPanel.add(new JLabel("Closing this window minimizes it to tray."),
                        BorderLayout.CENTER);
            }
            catch (AWTException e) {
                log.error("Could not add to tray", e);
                frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
            }
        }
        else {
            frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        }

        frame.add(contentPanel);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}
