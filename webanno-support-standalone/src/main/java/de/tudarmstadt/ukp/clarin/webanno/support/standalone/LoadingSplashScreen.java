/*
 * Copyright 2017
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

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GraphicsEnvironment;
import java.awt.Toolkit;
import java.net.URL;
import java.util.Optional;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JWindow;

public class LoadingSplashScreen
{
    public static Optional<JWindow> setupScreen(URL aImage)
    {
        if (GraphicsEnvironment.isHeadless()) {
            return Optional.empty();
        }

        SplashWindow window = new SplashWindow(aImage);
        window.setVisible(true);

        return Optional.of(window);
    }

    private static class SplashWindow
        extends JWindow
    {
        private static final long serialVersionUID = 2429606748142131008L;

        public SplashWindow(URL aUrl)
        {
            JLabel l = new JLabel(new ImageIcon(aUrl));
            JLabel info = new JLabel("Application is loading...");
            getContentPane().add(l, BorderLayout.CENTER);
            getContentPane().add(info, BorderLayout.SOUTH);
            pack();
            Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
            Dimension labelSize = l.getPreferredSize();
            setLocation(screenSize.width / 2 - (labelSize.width / 2),
                    screenSize.height / 2 - (labelSize.height / 2));
            setVisible(true);
        }
    }
}
