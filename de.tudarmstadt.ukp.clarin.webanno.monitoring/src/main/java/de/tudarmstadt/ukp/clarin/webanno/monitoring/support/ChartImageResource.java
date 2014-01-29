/*******************************************************************************
 * Copyright 2012
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
package de.tudarmstadt.ukp.clarin.webanno.monitoring.support;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.apache.wicket.request.resource.DynamicImageResource;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;

/**
 * A base class for the dynamically generated chart Images for percenatage and number of document
 * finished charts in {@link MonitoringPage}
 *
 * @author Seid Muhie Yimam
 *
 */
public class ChartImageResource
    extends DynamicImageResource
{
    private static final long serialVersionUID = 1L;
    private JFreeChart chart;
    private int width;
    private int height;

    public ChartImageResource(JFreeChart aChart, int aWidth, int aHeight)
    {
        chart = aChart;
        width = aWidth;
        height = aHeight;
    }

    @Override
    protected byte[] getImageData(Attributes aAttributes)
    {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ChartUtilities.writeChartAsPNG(bos, chart, width, height);
            return bos.toByteArray();
        }
        catch (IOException e) {
            return new byte[0];
        }
    }

}
