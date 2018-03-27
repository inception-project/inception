/*
 * Copyright 2017
 * Ubiquitous Knowledge Processing (UKP) Lab
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
package de.tudarmstadt.ukp.inception.recommendation.imls.util;

import static org.apache.batik.util.SVGConstants.SVG_NAMESPACE_URI;
import static org.apache.batik.util.SVGConstants.SVG_STYLE_ATTRIBUTE;
import static org.apache.batik.util.SVGConstants.SVG_VIEW_BOX_ATTRIBUTE;
import static org.apache.batik.util.XMLConstants.XLINK_NAMESPACE_URI;
import static org.apache.batik.util.XMLConstants.XLINK_PREFIX;
import static org.apache.batik.util.XMLConstants.XMLNS_NAMESPACE_URI;
import static org.apache.batik.util.XMLConstants.XMLNS_PREFIX;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringWriter;
import java.util.List;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.batik.dom.GenericDOMImplementation;
import org.apache.batik.svggen.SVGCSSStyler;
import org.apache.batik.svggen.SVGGraphics2D;
import org.dkpro.lab.reporting.ChartUtil;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.NumberTickUnit;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.xy.XYSplineRenderer;
import org.jfree.data.xy.DefaultXYDataset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import de.tudarmstadt.ukp.inception.recommendation.imls.conf.EvaluationConfiguration;
import de.tudarmstadt.ukp.inception.recommendation.imls.core.dataobjects.EvaluationResult;
import de.tudarmstadt.ukp.inception.recommendation.imls.core.dataobjects.ExtendedResult;

/**
 * Helper class. It creates the html reports and draws the graphical plot.
 * 
 *
 *
 */
public class Reporter
{
    private static Logger LOG = LoggerFactory.getLogger(Reporter.class);
    
    private static final String MS_FORMAT = "s SSS";
    private static final String NEXT_TABLE_CELL = "</td><td>";
    private static final int SVG_WIDTH = 800;
    private static final int SVG_HEIGHT = 300;

    private Reporter()
    {
    }

    public static void dumpResults(File fOut, EvaluationConfiguration testConf,
            EvaluationResult results)
        throws IOException
    {
        final StringBuilder sb = new StringBuilder("");
        final String df = "%.3f";

        sb.append("<html><head><style>svg {width:" + SVG_WIDTH + "px; height:" + SVG_HEIGHT
                + "px;}</style></head><body>");
        sb.append("<h1>Test Results</h1>");

        sb.append("<h2>Test Configuration</h2>");
        sb.append(testConf);

        sb.append("<h2>Chart: Known Data</h2>");
        List<ExtendedResult> knownDataResults = results.getKnownDataResults();
        sb.append(plotToString(knownDataResults));
        sb.append("<p />");

        sb.append("<h2>Chart: Unknown Data</h2>");
        List<ExtendedResult> unknownDataResults = results.getUnknownDataResults();
        sb.append(plotToString(unknownDataResults));
        sb.append("<p />");

        sb.append("<h2>Known Data</h2>");
        sb.append("<table border=1>");
        sb.append(
                "<thead><tr><th>Iteration</th><th>Trainings Set Size</th><th>F-Score</th><th>Precision</th><th>Recall</th><th>Training Duration [ms]</th><th>Classifying Duration [ms]</th></tr></thead>");
        sb.append("<tbody>");

        for (int i = 0; i < knownDataResults.size(); i++) {
            ExtendedResult r = knownDataResults.get(i);
            sb.append("<tr><td>").append(r.getIterationNumber()).append(NEXT_TABLE_CELL)
                    .append(r.getTrainingSetSize()).append(NEXT_TABLE_CELL)
                    .append(String.format(df, r.getFscore())).append(NEXT_TABLE_CELL)
                    .append(String.format(df, r.getPrecision())).append(NEXT_TABLE_CELL)
                    .append(String.format(df, r.getRecall())).append(NEXT_TABLE_CELL)
                    .append(r.getTrainingDuration(MS_FORMAT)).append(NEXT_TABLE_CELL)
                    .append(r.getClassifyingDuration(MS_FORMAT)).append("</td></tr>");
        }

        sb.append("</tbody></table><p/>");

        sb.append("<h2>Next Data</h2>");
        sb.append("<table border=1>");
        sb.append(
                "<thead><tr><th>Iteration</th><th>Next Delta Set Size</th><th>F-Score</th><th>Precision</th><th>Recall</th><th>Training Duration [ms]</th><th>Classifying Duration [ms]</th></tr></thead>");
        sb.append("<tbody>");

        for (int i = 0; i < unknownDataResults.size(); i++) {
            ExtendedResult r = unknownDataResults.get(i);
            sb.append("<tr><td>").append(r.getIterationNumber()).append(NEXT_TABLE_CELL)
                    .append(r.getTrainingSetSize()).append(NEXT_TABLE_CELL)
                    .append(String.format(df, r.getFscore())).append(NEXT_TABLE_CELL)
                    .append(String.format(df, r.getPrecision())).append(NEXT_TABLE_CELL)
                    .append(String.format(df, r.getRecall())).append(NEXT_TABLE_CELL)
                    .append(r.getTrainingDuration(MS_FORMAT)).append(NEXT_TABLE_CELL)
                    .append(r.getClassifyingDuration(MS_FORMAT)).append("</td></tr>");
        }

        sb.append("</tbody></table>");

        sb.append("</body></html>");

        try (BufferedWriter bf = new BufferedWriter(new FileWriter(fOut))) {
            bf.write(sb.toString());
            bf.flush();
        }
    }

    private static JFreeChart plotChart(List<ExtendedResult> results)
    {
        double[][] dataFScore = new double[2][results.size()];
        double[][] dataPrecision = new double[2][results.size()];
        double[][] dataRecall = new double[2][results.size()];

        for (int i = 0; i < results.size(); i++) {
            ExtendedResult r = results.get(i);

            dataFScore[0][i] = r.getIterationNumber();
            dataFScore[1][i] = r.getFscore();

            dataPrecision[0][i] = r.getIterationNumber();
            dataPrecision[1][i] = r.getPrecision();

            dataRecall[0][i] = r.getIterationNumber();
            dataRecall[1][i] = r.getRecall();
        }

        DefaultXYDataset dataset = new DefaultXYDataset();
        dataset.addSeries("F-Score", dataFScore);
        dataset.addSeries("Precision", dataPrecision);
        dataset.addSeries("Recall", dataRecall);

        JFreeChart chart = ChartFactory.createXYLineChart(null, "Increment #", "Value", dataset,
                PlotOrientation.VERTICAL, true, false, false);
        chart.getXYPlot().setRenderer(new XYSplineRenderer());
        chart.getXYPlot().getRangeAxis().setRange(0, 1.0);
        NumberAxis xAxis = new NumberAxis();
        xAxis.setTickUnit(new NumberTickUnit(1));
        xAxis.setAutoRange(true);
        xAxis.setAutoRangeIncludesZero(false);
        xAxis.setTickUnit(new NumberTickUnit(1));
        chart.getXYPlot().setDomainAxis(xAxis);

        return chart;
    }

    public static BufferedImage plotToImage(List<ExtendedResult> results, int width, int height)
    {
        return plotChart(results).createBufferedImage(width, height);
    }

    public static void plotToStream(List<ExtendedResult> results, OutputStream os)
        throws IOException
    {
        ChartUtil.writeChartAsSVG(os, plotChart(results), SVG_WIDTH, SVG_HEIGHT);
    }

    public static String plotToString(List<ExtendedResult> results)
    {
        JFreeChart chart = plotChart(results);

        // Get a DOMImplementation and create an XML document
        DOMImplementation domImpl = GenericDOMImplementation.getDOMImplementation();
        Document document = domImpl.createDocument(null, "svg", null);

        // Create an instance of the SVG Generator
        SVGGraphics2D svgGenerator = new SVGGraphics2D(document);

        // draw the chart in the SVG generator
        chart.draw(svgGenerator, new Rectangle(SVG_WIDTH, SVG_HEIGHT));

        Element svgRoot = svgGenerator.getRoot();
        svgRoot.setAttributeNS(XMLNS_NAMESPACE_URI, XMLNS_PREFIX, SVG_NAMESPACE_URI);
        svgRoot.setAttributeNS(XMLNS_NAMESPACE_URI, XMLNS_PREFIX + ":" + XLINK_PREFIX,
                XLINK_NAMESPACE_URI);
//        DocumentFragment svgDocument = svgRoot.getOwnerDocument().createDocumentFragment();
//        svgDocument.appendChild(svgRoot);
//        SVGCSSStyler.style(svgDocument);
        SVGCSSStyler.style(svgRoot);
//        String style = svgRoot.getAttributeNS(null, SVG_STYLE_ATTRIBUTE);
//        style = "height: " + SVG_HEIGHT + "px; width: " + SVG_WIDTH + "px; " + style;
//        svgRoot.setAttributeNS(null, SVG_STYLE_ATTRIBUTE, style);
        String style = svgRoot.getAttributeNS(null, SVG_STYLE_ATTRIBUTE);
        style = "height: auto; width: 100%; " + style;
        svgRoot.setAttributeNS(null, SVG_STYLE_ATTRIBUTE, style);
        svgRoot.setAttributeNS(null, SVG_VIEW_BOX_ATTRIBUTE, "0 0 " + SVG_WIDTH + " " + SVG_HEIGHT);

        try {
            StringWriter writer = new StringWriter();
            Transformer trans = TransformerFactory.newInstance().newTransformer();
            trans.setOutputProperty(OutputKeys.INDENT, "yes");
            trans.setOutputProperty(OutputKeys.VERSION, "1.0");
            trans.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            trans.transform(new DOMSource(svgRoot), new StreamResult(writer));
            writer.close();
            return writer.toString();
        }
        catch (Exception e) {
            LOG.error("Unable to render SVG", e);
            return "";
        }
    }
}
