package de.tudarmstadt.ukp.clarin.webanno.brat.support;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.apache.wicket.request.resource.DynamicImageResource;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;

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
