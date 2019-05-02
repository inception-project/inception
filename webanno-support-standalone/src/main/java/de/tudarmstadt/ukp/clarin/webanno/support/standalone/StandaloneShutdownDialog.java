/*
 * Copyright 2014
 * Ubiquitous Knowledge Processing (UKP) Lab and FG Language Technology
 * Technische Universität Darmstadt
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *  
 *  http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tudarmstadt.ukp.clarin.webanno.support.standalone;

import java.awt.Desktop;
import java.awt.Desktop.Action;
import java.awt.EventQueue;
import java.awt.GraphicsEnvironment;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.io.IOException;
import java.net.URI;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;

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

@Component("standaloneShutdownDialog")
@Order(Ordered.HIGHEST_PRECEDENCE)
public class StandaloneShutdownDialog
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
                    new ShutdownDialogAvailableEvent(StandaloneShutdownDialog.this));

            final int style;
            final String[] options;
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Action.BROWSE)) {
                style = JOptionPane.OK_CANCEL_OPTION;
                options = new String[] { ACTION_SHUTDOWN, ACTION_OPEN_BROWSER };
            }
            else {
                style = JOptionPane.OK_OPTION;
                options = new String[] { ACTION_SHUTDOWN };
            }
            
            EventQueue.invokeLater(() -> {
                final JOptionPane optionPane = new JOptionPane(
                        new JLabel("<HTML>" + applicationName + " is running now and can be "
                                + "accessed via <b>http://localhost:8080</b>.<br>"
                                + applicationName
                                + " works best with the browsers Google Chrome or Safari.<br>"
                                + "Use this dialog to shut " + applicationName + " down.</HTML>"),
                        JOptionPane.INFORMATION_MESSAGE, style, null, options);

                final JDialog dialog = new JDialog((JFrame) null, applicationName, false);
                dialog.setContentPane(optionPane);
                dialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
                dialog.addWindowListener(new WindowAdapter()
                {
                    @Override
                    public void windowClosing(WindowEvent we)
                    {
                        // Avoid closing window by other means than button
                    }
                });
                optionPane.addPropertyChangeListener(this::handleCloseEvent);
                dialog.pack();
                dialog.setVisible(true);
            });
        }
        else {
            log.info("Running in server environment or from command line: disabling interactive shutdown dialog.");
        }
    }
    
    @EventListener
    public void onApplicationEvent(WebServerInitializedEvent aEvt) {
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
}
