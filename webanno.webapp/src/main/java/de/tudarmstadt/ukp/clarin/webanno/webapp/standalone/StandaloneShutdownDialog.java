/*******************************************************************************
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
 ******************************************************************************/
package de.tudarmstadt.ukp.clarin.webanno.webapp.standalone;

import java.awt.EventQueue;
import java.awt.GraphicsEnvironment;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.SmartLifecycle;

public class StandaloneShutdownDialog
    implements SmartLifecycle
{
    private final Log log = LogFactory.getLog(getClass());

    private boolean running = false;

    @Override
    public void start()
    {
        running = true;
        displayShutdownDialog();
    }

    @Override
    public void stop()
    {
        running = false;
    }

    @Override
    public boolean isRunning()
    {
        return running;
    }

    @Override
    public int getPhase()
    {
        return Integer.MIN_VALUE;
    }

    @Override
    public boolean isAutoStartup()
    {
        return true;
    }

    @Override
    public void stop(Runnable aCallback)
    {
        stop();
        aCallback.run();
    }

    private void displayShutdownDialog()
    {
        String serverId =  ServerDetector.getServerId();
        log.info("Running in: " + (serverId != null ? serverId : "unknown server"));
        log.info("Console: " + ((System.console() != null) ? "available" : "not available"));
        log.info("Headless: " + (GraphicsEnvironment.isHeadless() ? "yes" : "no"));
        
        // Show this only when run from the standalone JAR via a double-click
        if (System.console() == null && !GraphicsEnvironment.isHeadless() && ServerDetector.isWinstone()) {
            log.info("If you are running WebAnno in a server environment, please use '-Djava.awt.headless=true'");

            EventQueue.invokeLater(new Runnable()
            {
                @Override
                public void run()
                {
                    final JOptionPane optionPane = new JOptionPane(
                            new JLabel(
                                    "<HTML>WebAnno is running now and can be accessed via <a href=\"http://localhost:8080\">http://localhost:8080</a>.<br>"
                                            + "WebAnno works best with the browsers Google Chrome or Safari.<br>"
                                            + "Use this dialog to shut WebAnno down.</HTML>"),
                            JOptionPane.INFORMATION_MESSAGE, JOptionPane.OK_OPTION, null,
                            new String[] { "Shutdown" });

                    final JDialog dialog = new JDialog((JFrame) null, "WebAnno", true);
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
                    optionPane.addPropertyChangeListener(new PropertyChangeListener()
                    {
                        @Override
                        public void propertyChange(PropertyChangeEvent aEvt)
                        {
                            if (dialog.isVisible() && (aEvt.getSource() == optionPane)
                                    && (aEvt.getPropertyName().equals(JOptionPane.VALUE_PROPERTY))) {
                                System.exit(0);
                            }
                        }
                    });
                    dialog.pack();
                    dialog.setVisible(true);
                }
            });
        }
        else {
            log.info("Running in server environment or from command line: disabling interactive shutdown dialog.");
        }
    }
}
