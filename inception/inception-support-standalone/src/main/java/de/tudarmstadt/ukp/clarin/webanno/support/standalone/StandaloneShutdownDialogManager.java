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
import static java.awt.Font.BOLD;
import static java.awt.Font.SANS_SERIF;
import static java.text.MessageFormat.format;
import static javax.swing.BoxLayout.PAGE_AXIS;
import static javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS;
import static javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS;
import static javax.swing.WindowConstants.DISPOSE_ON_CLOSE;

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
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.time.Year;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import javax.swing.border.Border;
import javax.swing.text.DefaultCaret;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.web.context.WebServerInitializedEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import de.tudarmstadt.ukp.clarin.webanno.support.SettingsUtil;
import de.tudarmstadt.ukp.clarin.webanno.support.about.ApplicationInformation;
import de.tudarmstadt.ukp.clarin.webanno.support.logging.LogMessage;
import de.tudarmstadt.ukp.clarin.webanno.support.logging.RingBufferAppender;

@Component("standaloneShutdownDialogManager")
@Order(Ordered.HIGHEST_PRECEDENCE)
public class StandaloneShutdownDialogManager
{
    private final Logger log = LoggerFactory.getLogger(getClass());

    private static final String ACTION_OPEN_BROWSER = "Open browser";
    private static final String ACTION_SHUTDOWN = "Shut down";

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private ApplicationContext context;

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

            EventQueue.invokeLater(this::showShutdownDialog);
        }
        else {
            log.info(
                    "Running in server environment or from command line: disabling interactive shutdown dialog.");
            log.info("You can close INCEpTION from the command line by pressing Ctrl+C");
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
        System.exit(SpringApplication.exit(context));
    }

    private void actionShowLog()
    {
        JFrame frame = new JFrame(applicationName + " - Log");

        JPanel contentPanel = new JPanel();
        Border padding = BorderFactory.createEmptyBorder(10, 10, 10, 10);
        contentPanel.setBorder(padding);
        contentPanel.setLayout(new BoxLayout(contentPanel, PAGE_AXIS));

        JTextArea logArea = new JTextArea(20, 80);
        logArea.setEditable(false);
        // Set text before setting the caret policy so we start in follow mode
        logArea.setText(getLog());
        DefaultCaret caret = (DefaultCaret) logArea.getCaret();
        caret.setUpdatePolicy(DefaultCaret.NEVER_UPDATE);
        JScrollPane scroll = new JScrollPane(logArea);
        scroll.setVerticalScrollBarPolicy(VERTICAL_SCROLLBAR_ALWAYS);
        scroll.setHorizontalScrollBarPolicy(HORIZONTAL_SCROLLBAR_ALWAYS);
        contentPanel.add(scroll);
        frame.add(contentPanel);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        frame.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        bringToFront(frame);

        // Watch the log and if the log has scrolled down all the way to the bottom, follow
        Timer timer = new Timer(250, e -> {
            int savedHorizontal = scroll.getHorizontalScrollBar().getValue();
            int vpos = scroll.getVerticalScrollBar().getValue() + scroll.getHeight()
                    - scroll.getHorizontalScrollBar().getHeight() /*- (lineHeight / 2)*/;
            boolean atBottom = vpos >= scroll.getVerticalScrollBar().getMaximum();
            logArea.setText(getLog());
            if (savedHorizontal != 0) {
                scroll.getHorizontalScrollBar().setValue(savedHorizontal);
            }
            if (atBottom) {
                EventQueue.invokeLater(() -> {
                    scroll.getVerticalScrollBar()
                            .setValue(scroll.getVerticalScrollBar().getMaximum());
                });
            }
        });
        timer.start();

        frame.addWindowListener(new WindowAdapter()
        {
            @Override
            public void windowClosed(WindowEvent aE)
            {
                timer.stop();
            }
        });
    }

    private String getLog()
    {
        return RingBufferAppender.events().stream() //
                .map(LogMessage::getMessage) //
                .collect(Collectors.joining("\n"));
    }

    private void bringToFront(JFrame aFrame)
    {
        EventQueue.invokeLater(() -> {
            aFrame.setAlwaysOnTop(true);
            aFrame.setAlwaysOnTop(false);
        });
    }

    private void actionShowAbout()
    {
        ResourceBundle bundle = ResourceBundle.getBundle(getClass().getName());

        JFrame frame = new JFrame(applicationName + " - About");

        JPanel contentPanel = new JPanel();
        Border padding = BorderFactory.createEmptyBorder(10, 10, 10, 10);
        contentPanel.setBorder(padding);
        contentPanel.setLayout(new BoxLayout(contentPanel, PAGE_AXIS));

        JLabel header = new JLabel(applicationName);
        header.setFont(new Font(SANS_SERIF, BOLD, 16));
        contentPanel.add(header);

        contentPanel.add(new JLabel(bundle.getString("shortDescription")));
        contentPanel.add(new JLabel("Java version: " + System.getProperty("java.version")));
        contentPanel.add(new JLabel("INCEpTION version: " + SettingsUtil.getVersionString()));
        contentPanel.add(new JLabel(bundle.getString("license")));
        contentPanel.add(new JLabel(
                format(bundle.getString("copyright"), Integer.toString(Year.now().getValue()))));

        JTextArea dependencies = new JTextArea(20, 80);
        dependencies.setEditable(false);
        dependencies.setText(ApplicationInformation.loadDependencies());
        dependencies.setCaretPosition(0);
        JScrollPane scroll = new JScrollPane(dependencies);
        scroll.setVerticalScrollBarPolicy(VERTICAL_SCROLLBAR_ALWAYS);
        contentPanel.add(scroll);

        frame.add(contentPanel);

        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        bringToFront(frame);
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

        if (Desktop.isDesktopSupported()
                && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
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

            MenuItem logItem = new MenuItem("Log...");
            logItem.addActionListener(e -> actionShowLog());
            popupMenu.add(logItem);

            MenuItem aboutItem = new MenuItem("About...");
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
        frame.requestFocus();
    }
}
