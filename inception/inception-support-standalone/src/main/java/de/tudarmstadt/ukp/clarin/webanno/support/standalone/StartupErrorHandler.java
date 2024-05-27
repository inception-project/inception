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

import static de.tudarmstadt.ukp.inception.support.db.LiquibaseLockManager.PROP_FORCE_RELEASE_LOCK;
import static javax.swing.JOptionPane.ERROR_MESSAGE;
import static javax.swing.WindowConstants.DISPOSE_ON_CLOSE;
import static org.apache.commons.lang3.exception.ExceptionUtils.getRootCause;
import static org.apache.commons.lang3.exception.ExceptionUtils.getRootCauseMessage;
import static org.apache.commons.text.WordUtils.wrap;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.net.BindException;

import javax.swing.JDialog;
import javax.swing.JOptionPane;

import org.springframework.boot.context.event.ApplicationFailedEvent;

import de.tudarmstadt.ukp.inception.support.db.LockRemovedException;
import de.tudarmstadt.ukp.inception.support.db.NotLockedException;
import liquibase.exception.LockException;

public class StartupErrorHandler
{
    private final String applicationName;

    public StartupErrorHandler(String aApplicationName)
    {
        super();
        applicationName = aApplicationName;
    }

    public void handleError(ApplicationFailedEvent aEvent)
    {
        JOptionPane pane = new JOptionPane(getErrorMessage(aEvent), ERROR_MESSAGE);
        pane.addPropertyChangeListener(event -> {
            if (JOptionPane.VALUE_PROPERTY.equals(event.getPropertyName())) {
                System.exit(0);
            }
        });

        JDialog dialog = pane.createDialog(null, getDialogTitle(aEvent));
        dialog.setModal(false);
        dialog.setVisible(true);
        dialog.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        dialog.setAlwaysOnTop(true); // bring to front...
        dialog.setAlwaysOnTop(false); // ... but do not annoy user
        dialog.requestFocus();
        dialog.addWindowListener(new WindowAdapter()
        {
            @Override
            public void windowClosed(WindowEvent aE)
            {
                System.exit(0);
            }
        });
    }

    public String getDialogTitle(ApplicationFailedEvent aEvent)
    {
        Throwable rootCause = getRootCause(aEvent.getException());

        if (rootCause instanceof LockRemovedException) {
            return applicationName + " - Recovery complete";
        }

        return applicationName + " - Error";
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
            msg.append("\n");
        }

        if (rootCause instanceof LockException) {
            msg.append("It appears that another instance of " + applicationName
                    + " is already using the database.\n"
                    + "Please check if any other instances of " + applicationName
                    + " are running.\n"
                    + "When you are sure no other instances are running, you can forcibly "
                    + "release the lock by\nrunning " + applicationName
                    + " ONCE with the parameter '-D" + PROP_FORCE_RELEASE_LOCK + "=true'.");
            msg.append("\n");
        }

        if (rootCause instanceof NotLockedException) {
            msg.append("The database is not locked.\nPlease remove the parameter '-D"
                    + PROP_FORCE_RELEASE_LOCK + "=true' and restart.");
            msg.append("\n");
        }

        if (rootCause instanceof LockRemovedException) {
            msg.append("The database lock has been removed.\nPlease remove the parameter '-D"
                    + PROP_FORCE_RELEASE_LOCK + "=true' and restart.");
            msg.append("\n");
            return msg.toString();
        }

        msg.append("\n");
        msg.append("Error type: " + getRootCause(aEvent.getException()).getClass() + "\n");
        msg.append("Error message: " + wrap(rootCauseMsg, 80));

        return msg.toString();
    }
}
