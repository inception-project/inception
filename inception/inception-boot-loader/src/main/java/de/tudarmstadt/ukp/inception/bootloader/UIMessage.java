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
package de.tudarmstadt.ukp.inception.bootloader;

import static javax.swing.JOptionPane.INFORMATION_MESSAGE;
import static javax.swing.JOptionPane.OK_OPTION;

import java.beans.PropertyChangeEvent;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;

public class UIMessage
{
    private static final String ACTION_SHUTDOWN = "OK";

    public static final void displayMessage(String aMessage)
    {
        String formattedMessage = "<html>" + aMessage.replace("\n", "<br>") + "</html>";
        final JOptionPane optionPane = new JOptionPane(new JLabel(formattedMessage),
                INFORMATION_MESSAGE, OK_OPTION, null, new String[] { ACTION_SHUTDOWN });
        optionPane.addPropertyChangeListener(UIMessage::handleCloseEvent);

        JFrame frame = new JFrame("INCEpTION System Requirements Check");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setContentPane(optionPane);
        frame.pack();
        frame.setVisible(true);
    }

    private static void handleCloseEvent(PropertyChangeEvent aEvt)
    {
        if (aEvt.getPropertyName().equals(JOptionPane.VALUE_PROPERTY)) {
            switch ((String) aEvt.getNewValue()) {
            case ACTION_SHUTDOWN:
                System.exit(0);
                break;
            }
        }
    }
}
