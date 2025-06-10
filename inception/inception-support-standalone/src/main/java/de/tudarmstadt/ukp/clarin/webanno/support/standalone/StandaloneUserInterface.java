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

import static de.tudarmstadt.ukp.inception.support.SettingsUtil.getGlobalLogFile;
import static de.tudarmstadt.ukp.inception.support.SettingsUtil.getGlobalLogFolder;
import static de.tudarmstadt.ukp.inception.support.SettingsUtil.getSettingsFileLocation;
import static java.awt.Desktop.getDesktop;
import static java.awt.Font.BOLD;
import static java.awt.Font.SANS_SERIF;
import static java.text.MessageFormat.format;
import static javax.swing.BoxLayout.LINE_AXIS;
import static javax.swing.BoxLayout.PAGE_AXIS;
import static javax.swing.JOptionPane.showMessageDialog;
import static javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS;
import static javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS;
import static javax.swing.WindowConstants.DISPOSE_ON_CLOSE;
import static org.apache.commons.lang3.exception.ExceptionUtils.getRootCauseMessage;
import static org.slf4j.LoggerFactory.getLogger;

import java.awt.AWTException;
import java.awt.Desktop;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.Image;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.Toolkit;
import java.awt.TrayIcon;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.lang.invoke.MethodHandles;
import java.time.Year;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.Timer;
import javax.swing.text.DefaultCaret;

import org.slf4j.Logger;

import de.tudarmstadt.ukp.inception.support.SettingsUtil;
import de.tudarmstadt.ukp.inception.support.about.ApplicationInformation;
import de.tudarmstadt.ukp.inception.support.logging.RingBufferAppender;

public class StandaloneUserInterface
{
    private static final Logger LOG = getLogger(MethodHandles.lookup().lookupClass());

    public static final String ACTION_OPEN_BROWSER = "Open browser";
    public static final String ACTION_SHUTDOWN = "Shut down";

    private static void actionShutdown()
    {
        System.exit(0);
    }

    public static void actionLocateSettingsProperties()
    {
        if (!Desktop.isDesktopSupported()) {
            return;
        }

        try {
            var file = getSettingsFileLocation();
            file.getParentFile().mkdirs();
            if (!file.exists()) {
                file.createNewFile();
            }

            if (getDesktop().isSupported(Desktop.Action.BROWSE_FILE_DIR)) {
                getDesktop().browseFileDirectory(file);
            }
            else {
                var label = new JLabel("Path to the settings properties file:");

                var textField = new JTextField(file.getAbsolutePath());
                textField.setEditable(false);

                var copyButton = new JButton("Copy");
                copyButton.addActionListener(le -> {
                    StringSelection stringSelection = new StringSelection(textField.getText());
                    Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                    clipboard.setContents(stringSelection, null);
                });

                var panel = new JPanel();
                panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
                panel.add(label);
                panel.add(Box.createVerticalGlue());

                var row = new JPanel();
                row.add(textField);
                row.add(copyButton);
                panel.add(row);

                var title = "Settings Properties";
                JOptionPane.showMessageDialog(null, panel, title, JOptionPane.INFORMATION_MESSAGE);
            }
        }
        catch (Exception e) {
            LOG.error("Unable to open settings file", e);
            showMessageDialog(null, "Unable to open settings file: " + getRootCauseMessage(e));
        }
    }

    public static void actionShowLog(String applicationName)
    {
        var frame = new JFrame(applicationName + " - Log");

        var contentPanel = new JPanel();
        var padding = BorderFactory.createEmptyBorder(10, 10, 10, 10);
        contentPanel.setBorder(padding);
        contentPanel.setLayout(new BoxLayout(contentPanel, PAGE_AXIS));

        var info = new JLabel(
                "Only recent log messages are displayed here. For more information, check the log file.");
        contentPanel.add(info);

        var logArea = new JTextArea(20, 80);
        logArea.setTabSize(2);
        logArea.setEditable(false);
        // Set text before setting the caret policy so we start in follow mode
        logArea.setText(getLog());

        var caret = (DefaultCaret) logArea.getCaret();
        caret.setUpdatePolicy(DefaultCaret.NEVER_UPDATE);

        var scroll = new JScrollPane(logArea);
        scroll.setVerticalScrollBarPolicy(VERTICAL_SCROLLBAR_ALWAYS);
        scroll.setHorizontalScrollBarPolicy(HORIZONTAL_SCROLLBAR_ALWAYS);
        contentPanel.add(scroll);

        var buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, LINE_AXIS));
        contentPanel.add(buttonPanel);

        var copyToClipboardButton = new JButton("Copy to clipboard");
        copyToClipboardButton.addActionListener(StandaloneUserInterface::actionCopyLogToClipboard);
        buttonPanel.add(copyToClipboardButton);

        if (getGlobalLogFolder().isPresent() && Desktop.isDesktopSupported()) {
            if (getDesktop().isSupported(Desktop.Action.BROWSE_FILE_DIR)) {
                var openLogFolderButton = new JButton("Open log folder");
                openLogFolderButton.addActionListener(StandaloneUserInterface::actionOpenLogFolder);
                buttonPanel.add(openLogFolderButton);
            }

            if (getDesktop().isSupported(Desktop.Action.OPEN)) {
                var openLogFileButton = new JButton("Open log file");
                openLogFileButton.addActionListener(StandaloneUserInterface::actionOpenLogFile);
                buttonPanel.add(openLogFileButton);
            }
        }

        frame.add(contentPanel);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        frame.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        bringToFront(frame);

        // Watch the log and if the log has scrolled down all the way to the bottom, follow
        var timer = new Timer(250, e -> {
            int savedHorizontal = scroll.getHorizontalScrollBar().getValue();
            int vpos = scroll.getVerticalScrollBar().getValue() + scroll.getHeight()
                    - scroll.getHorizontalScrollBar().getHeight() /*- (lineHeight / 2)*/;
            var atBottom = vpos >= scroll.getVerticalScrollBar().getMaximum();
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

    private static String getLog()
    {
        return RingBufferAppender.messages().stream() //
                .map(msg -> "[" + msg.level + "]: "
                        + msg.getMessage().replace("↩", "").replace("\n", "↩\n\t")) //
                .collect(Collectors.joining("\n"));
    }

    public static void bringToFront(JFrame aFrame)
    {
        EventQueue.invokeLater(() -> {
            aFrame.setAlwaysOnTop(true);
            aFrame.setAlwaysOnTop(false);
        });
    }

    private static void actionCopyLogToClipboard(ActionEvent aEvent)
    {
        try {
            var stringSelection = new StringSelection(getLog());
            var clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            clipboard.setContents(stringSelection, null);
        }
        catch (Exception e) {
            LOG.error("Unable to copy log to clipboard", e);
            showMessageDialog(null, "Unable to copy log to clipboard: " + getRootCauseMessage(e));
        }
    }

    private static void actionOpenLogFolder(ActionEvent aEvent)
    {
        try {
            getDesktop().browseFileDirectory(getGlobalLogFile().get().toFile());
        }
        catch (Exception e) {
            LOG.error("Unable to open log folder", e);
            showMessageDialog(null, "Unable to open log folder: " + getRootCauseMessage(e));
        }
    }

    private static void actionOpenLogFile(ActionEvent aEvent)
    {
        try {
            getDesktop().open(getGlobalLogFile().get().toFile());
        }
        catch (Exception e) {
            LOG.error("Unable to open log file", e);
            showMessageDialog(null, "Unable to open log file: " + getRootCauseMessage(e));
        }
    }

    public static void actionShowAbout(String applicationName)
    {
        var bundle = ResourceBundle.getBundle(StandaloneUserInterface.class.getName());

        var frame = new JFrame(applicationName + " - About");

        var contentPanel = new JPanel();
        var padding = BorderFactory.createEmptyBorder(10, 10, 10, 10);
        contentPanel.setBorder(padding);
        contentPanel.setLayout(new BoxLayout(contentPanel, PAGE_AXIS));

        var header = new JLabel(applicationName);
        header.setFont(new Font(SANS_SERIF, BOLD, 16));
        contentPanel.add(header);

        contentPanel.add(new JLabel(bundle.getString("shortDescription")));
        contentPanel.add(new JLabel("Java version: " + System.getProperty("java.version")));
        contentPanel.add(new JLabel("INCEpTION version: " + SettingsUtil.getVersionString()));
        contentPanel.add(new JLabel(bundle.getString("license")));
        contentPanel.add(new JLabel(
                format(bundle.getString("copyright"), Integer.toString(Year.now().getValue()))));

        var dependencies = new JTextArea(20, 80);
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

    public static TrayIcon makeStartupSystemTrayMenu(String applicationName) throws AWTException
    {
        var tray = SystemTray.getSystemTray();
        var trayIcon = new TrayIcon(getIconImage());
        trayIcon.setImageAutoSize(true);

        var popupMenu = new PopupMenu();

        var logItem = new MenuItem("Log...");
        logItem.addActionListener(e -> actionShowLog(applicationName));
        popupMenu.add(logItem);

        if (getSettingsFileLocation() != null) {
            var settingsPropertiesItem = new MenuItem("Locate settings file");
            settingsPropertiesItem.addActionListener(e -> actionLocateSettingsProperties());
            popupMenu.add(settingsPropertiesItem);
        }

        var aboutItem = new MenuItem("About...");
        aboutItem.addActionListener(e -> actionShowAbout(applicationName));
        popupMenu.add(aboutItem);

        var shutdownItem = new MenuItem(ACTION_SHUTDOWN + " (may take a moment)");
        shutdownItem.addActionListener(e -> {
            tray.remove(trayIcon);
            actionShutdown();
        });
        popupMenu.add(shutdownItem);

        trayIcon.setToolTip(applicationName + " " + SettingsUtil.getVersionString());
        trayIcon.setPopupMenu(popupMenu);

        tray.add(trayIcon);

        return trayIcon;
    }

    public static Image getIconImage()
    {
        var iconUrl = LoadingSplashScreen.class.getResource("/icon.png");
        var icon = new ImageIcon(iconUrl);
        return icon.getImage();
    }
}
