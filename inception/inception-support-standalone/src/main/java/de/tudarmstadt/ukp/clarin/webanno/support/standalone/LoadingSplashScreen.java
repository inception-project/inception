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

import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.net.URL;
import java.util.Optional;

import javax.swing.*;

public class LoadingSplashScreen
{
    public static Optional<JFrame> setupScreen(URL aSplashScreenImageUrl, URL aIconUrl)
    {
        if (GraphicsEnvironment.isHeadless()) {
            return Optional.empty();
        }

        SplashWindow window = new SplashWindow(aSplashScreenImageUrl, aIconUrl);
        window.setVisible(true);

        return Optional.of(window);
    }

    private static class SplashWindow
        extends JFrame
    {
        private static final long serialVersionUID = 2429606748142131008L;

        public SplashWindow(URL aSplashScreenImageUrl, URL aIconUrl)
        {
            JLabel l = new JLabel(new ImageIcon(aSplashScreenImageUrl));
            JLabel info = new JLabel("Application is loading...");
            getContentPane().add(l, BorderLayout.CENTER);
            getContentPane().add(info, BorderLayout.SOUTH);

            ImageIcon img = new ImageIcon(aIconUrl);
            setIconImage(img.getImage());

            setUndecorated(true);
            pack();

            Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
            Dimension labelSize = l.getPreferredSize();
            setLocation(screenSize.width / 2 - (labelSize.width / 2),
                    screenSize.height / 2 - (labelSize.height / 2));

            addMouseMotionListener(new MouseMotionListener()
            {
                private int mx, my;

                @Override
                public void mouseMoved(MouseEvent aEvent)
                {
                    mx = aEvent.getXOnScreen();
                    my = aEvent.getYOnScreen();
                }

                @Override
                public void mouseDragged(MouseEvent aEvent)
                {
                    Point p = SplashWindow.this.getLocation();
                    p.x += aEvent.getXOnScreen() - mx;
                    p.y += aEvent.getYOnScreen() - my;
                    mx = aEvent.getXOnScreen();
                    my = aEvent.getYOnScreen();
                    SplashWindow.this.setLocation(p);
                }
            });

            setVisible(true);
        }
    }
}
