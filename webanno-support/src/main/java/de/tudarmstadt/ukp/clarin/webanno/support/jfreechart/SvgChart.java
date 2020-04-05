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
package de.tudarmstadt.ukp.clarin.webanno.support.jfreechart;

import static org.apache.batik.constants.XMLConstants.XLINK_NAMESPACE_URI;
import static org.apache.batik.constants.XMLConstants.XLINK_PREFIX;
import static org.apache.batik.constants.XMLConstants.XMLNS_NAMESPACE_URI;
import static org.apache.batik.constants.XMLConstants.XMLNS_PREFIX;
import static org.apache.batik.util.SVGConstants.SVG_NAMESPACE_URI;
import static org.apache.batik.util.SVGConstants.SVG_STYLE_ATTRIBUTE;
import static org.apache.batik.util.SVGConstants.SVG_VIEW_BOX_ATTRIBUTE;

import java.awt.Rectangle;
import java.io.StringWriter;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.batik.dom.GenericDOMImplementation;
import org.apache.batik.svggen.SVGCSSStyler;
import org.apache.batik.svggen.SVGGraphics2D;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.jfree.chart.JFreeChart;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaModel;

public class SvgChart
    extends Panel
{
    private static final Logger LOG = LoggerFactory.getLogger(SvgChart.class);

    private static final long serialVersionUID = -8781623348962721601L;

    private SvgChartOptions options;

    public SvgChart(String aId, IModel<JFreeChart> aModel)
    {
        this(aId, aModel, null);
    }

    public SvgChart(String aId, IModel<JFreeChart> aModel, SvgChartOptions aOptions)
    {
        super(aId, aModel);

        options = aOptions != null ? aOptions : new SvgChartOptions();

        add(new Label("svgWrapper", LambdaModel.of(this::renderSvg)).setEscapeModelStrings(false));
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public IModel<JFreeChart> getModel()
    {
        return (IModel) getDefaultModel();
    }

    public JFreeChart getModelObject()
    {
        return (JFreeChart) getDefaultModelObject();
    }

    public void setModel(IModel<JFreeChart> aModel)
    {
        setDefaultModel(aModel);
    }

    public void setModelObject(JFreeChart aObject)
    {
        setDefaultModelObject(aObject);
    }

    public SvgChartOptions getOptions()
    {
        return options;
    }

    public void setOptions(SvgChartOptions aOptions)
    {
        options = aOptions;
    }

    private String renderSvg()
    {
        // Get a DOMImplementation and create an XML document
        DOMImplementation domImpl = GenericDOMImplementation.getDOMImplementation();
        Document document = domImpl.createDocument(null, "svg", null);

        // Create an instance of the SVG Generator
        SVGGraphics2D svgGenerator = new SVGGraphics2D(document);

        // draw the chart in the SVG generator
        getModelObject().draw(svgGenerator,
                new Rectangle(options.getViewBoxWidth(), options.getViewBoxHeight()));

        Element svgRoot = svgGenerator.getRoot();
        svgRoot.setAttributeNS(XMLNS_NAMESPACE_URI, XMLNS_PREFIX, SVG_NAMESPACE_URI);
        svgRoot.setAttributeNS(XMLNS_NAMESPACE_URI, XMLNS_PREFIX + ":" + XLINK_PREFIX,
                XLINK_NAMESPACE_URI);
        SVGCSSStyler.style(svgRoot);
        // String style = svgRoot.getAttributeNS(null, SVG_STYLE_ATTRIBUTE);
        // style = "height: " + SVG_HEIGHT + "px; width: " + SVG_WIDTH + "px; " + style;
        // svgRoot.setAttributeNS(null, SVG_STYLE_ATTRIBUTE, style);
        String style = svgRoot.getAttributeNS(null, SVG_STYLE_ATTRIBUTE);
        style = "height: auto; width: 100%; " + style;
        svgRoot.setAttributeNS(null, SVG_STYLE_ATTRIBUTE, style);
        svgRoot.setAttributeNS(null, SVG_VIEW_BOX_ATTRIBUTE,
                "0 0 " + options.getViewBoxWidth() + " " + options.getViewBoxHeight());

        try {
            @SuppressWarnings("resource")
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
