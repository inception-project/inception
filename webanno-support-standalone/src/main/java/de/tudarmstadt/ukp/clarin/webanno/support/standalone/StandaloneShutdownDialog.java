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

import java.awt.EventQueue;
import java.awt.GraphicsEnvironment;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.annotation.Resource;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.SmartLifecycle;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Component("standaloneShutdownDialog")
@Lazy(false)
public class StandaloneShutdownDialog
    implements SmartLifecycle
{
    private final Logger log = LoggerFactory.getLogger(getClass());

    private boolean running = false;

    @Resource
    private ApplicationEventPublisher eventPublisher;
    
    @Value(value = "${running.from.commandline}")
    private boolean runningFromCommandline;    
    
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
        log.info("Console: " + ((System.console() != null) ? "available" : "not available"));
        log.info("Headless: " + (GraphicsEnvironment.isHeadless() ? "yes" : "no"));
        
        // Show this only when run from the standalone JAR via a double-click
        if (System.console() == null && !GraphicsEnvironment.isHeadless()
                && runningFromCommandline) {
            log.info("If you are running WebAnno in a server environment, please use '-Djava.awt.headless=true'");
            eventPublisher.publishEvent(
                    new ShutdownDialogAvailableEvent(StandaloneShutdownDialog.this));

            EventQueue.invokeLater(() -> {
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
                optionPane.addPropertyChangeListener(aEvt -> {
                    if (dialog.isVisible() && (aEvt.getSource() == optionPane)
                            && (aEvt.getPropertyName().equals(JOptionPane.VALUE_PROPERTY))) {
                        System.exit(0);
                    }
                });
                dialog.pack();
                dialog.setVisible(true);
            });
        }
        else {
            log.info("Running in server environment or from command line: disabling interactive shutdown dialog.");
        }
    }
}
