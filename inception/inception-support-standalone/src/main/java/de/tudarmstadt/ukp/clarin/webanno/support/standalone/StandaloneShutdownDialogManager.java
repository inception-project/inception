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

import java.awt.*;
import java.beans.PropertyChangeEvent;
import java.io.IOException;
import java.net.URI;
import java.net.URL;

import javax.swing.*;
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

            EventQueue.invokeLater(this::showShutdownDialog);
        }
        else {
            log.info(
                    "Running in server environment or from command line: disabling interactive shutdown dialog.");
        }
    }

    @EventListener
    public void onApplicationEvent(WebServerInitializedEvent aEvt)
    {
        port = aEvt.getWebServer().getPort();
    }

    private void handleCloseEvent(PropertyChangeEvent aEvt)
    {
        if (aEvt.getPropertyName().equals(JOptionPane.VALUE_PROPERTY)) {
            switch ((String) aEvt.getNewValue()) {
            case ACTION_OPEN_BROWSER:
                try {
                    Desktop.getDesktop().browse(URI.create("http://localhost:8080"));
                }
                catch (IOException e) {
                    log.error("Unable to open browser", e);
                }
                break;
            case ACTION_SHUTDOWN:
                System.exit(0);
                break;
            }
        }
    }

    private void showShutdownDialog()
    {
        JFrame frame = new JFrame("INCEpTION");

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
                + "accessed via <b>http://localhost:8080</b>.<br>" + applicationName
                + " works best with the browsers Google Chrome or Safari.<br>"
                + "Use this dialog to shut " + applicationName + " down.</html>");

        contentPanel.add(label, BorderLayout.PAGE_START);

        // Button Layout
        JPanel buttonPanel = new JPanel();

        JButton shutdownButton = new JButton(ACTION_SHUTDOWN);
        shutdownButton.addActionListener(e -> System.exit(0));
        buttonPanel.add(shutdownButton);

        if (Desktop.isDesktopSupported()
                && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            JButton browseButton = new JButton(ACTION_OPEN_BROWSER);
            browseButton.addActionListener(event -> {
                try {
                    Desktop.getDesktop().browse(URI.create("http://localhost:8080"));
                }
                catch (IOException e) {
                    log.error("Unable to open browser", e);
                }
            });
            buttonPanel.add(browseButton);
        }
        buttonPanel.setAlignmentX(CENTER_ALIGNMENT);
        contentPanel.add(buttonPanel, BorderLayout.PAGE_END);

        // Event handler
        frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);

        //
        // final JDialog dialog = new JDialog(jFrame, applicationName,
        // java.awt.Dialog.ModalityType.TOOLKIT_MODAL);
        // dialog.setContentPane(optionPane);
        // dialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        // dialog.addWindowListener(new WindowAdapter()
        // {
        // @Override
        // public void windowClosing(WindowEvent we)
        // {
        // // Avoid closing window by other means than button
        // }
        // });
        // optionPane.addPropertyChangeListener(this::handleCloseEvent);
        frame.add(contentPanel);
        frame.pack();
        frame.setVisible(true);
    }
}
