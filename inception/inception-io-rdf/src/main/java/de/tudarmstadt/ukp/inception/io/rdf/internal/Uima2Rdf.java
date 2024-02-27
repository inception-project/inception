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
package de.tudarmstadt.ukp.inception.io.rdf.internal;

import static java.lang.String.format;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.ontology.Individual;
import org.apache.jena.ontology.OntModel;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.Type;
import org.apache.uima.jcas.JCas;

import de.tudarmstadt.ukp.clarin.webanno.diag.CasDoctorUtils;
import de.tudarmstadt.ukp.dkpro.core.api.metadata.type.DocumentMetaData;

public class Uima2Rdf
{
    private static final String DOCUMENT_SCHEME = "doc:";

    private static final Pattern DKPRO_CORE_SCHEME = Pattern.compile(
            "(?<LONG>de\\.tudarmstadt\\.ukp\\.dkpro\\.core\\.api\\.(?<MODULE>[^.]+)\\.type(\\.(?<INMODULE>.*))?\\.)[^.]+");

    private final Set<String> iriFeatures = new HashSet<>();

    public Uima2Rdf(Set<String> aIriFeatures)
    {
        if (aIriFeatures != null) {
            iriFeatures.addAll(aIriFeatures);
        }
    }

    public void convert(JCas aJCas, OntModel aTarget) throws CASException
    {
        // Set up prefix mappings
        var ts = aJCas.getTypeSystem();
        aTarget.setNsPrefix("cas", RdfCas.NS_UIMA + "uima.cas.");
        aTarget.setNsPrefix("tcas", RdfCas.NS_UIMA + "uima.tcas.");
        aTarget.setNsPrefix(RdfCas.PREFIX_RDFCAS, RdfCas.NS_RDFCAS);

        // Additional prefix mappings for DKPro Core typesystems
        for (var t : ts.getProperlySubsumedTypes(ts.getTopType())) {
            var nameMatcher = DKPRO_CORE_SCHEME.matcher("");
            var typeName = t.getName();
            if (typeName.endsWith("[]")) {
                typeName = typeName.substring(0, typeName.length() - 2);
            }
            nameMatcher.reset(typeName);
            if (nameMatcher.matches()) {
                var prefix = nameMatcher.group("MODULE");
                if (nameMatcher.group("INMODULE") != null) {
                    prefix = prefix + "-" + nameMatcher.group("INMODULE");
                }
                aTarget.setNsPrefix(prefix, RdfCas.NS_UIMA + nameMatcher.group("LONG"));
            }
        }

        var viewIterator = aJCas.getViewIterator();
        while (viewIterator.hasNext()) {
            convertView(viewIterator.next(), aTarget);
        }
    }

    private void convertView(JCas aJCas, OntModel aTarget)
    {
        // Shorten down variable name for model
        var m = aTarget;

        // Set up names
        var tView = m.createResource(RdfCas.TYPE_VIEW);
        var tFeatureStructure = m.createResource(RdfCas.TYPE_FEATURE_STRUCTURE);
        var pIndexedIn = m.createProperty(RdfCas.PROP_INDEXED_IN);

        // Get a URI for the document
        var dmd = DocumentMetaData.get(aJCas);
        var docuri = dmd.getDocumentUri() != null ? dmd.getDocumentUri()
                : DOCUMENT_SCHEME + dmd.getDocumentId();

        // These only collect a single view...
        var reachable = CasDoctorUtils.collectReachable(aJCas.getCas());
        var indexed = CasDoctorUtils.collectIndexed(aJCas.getCas());
        // ... they do not collect the SOFA, so we add that explicitly
        reachable.add(aJCas.getSofa());

        // Set up the view itself
        var viewUri = format("%s#%d", docuri, aJCas.getLowLevelCas().ll_getFSRef(aJCas.getSofa()));
        var rdfView = m.createIndividual(viewUri, tView);

        for (var uimaFS : reachable) {
            var uri = format("%s#%d", docuri, aJCas.getLowLevelCas().ll_getFSRef(uimaFS));
            var rdfFS = m.createIndividual(uri, m.createResource(rdfType(uimaFS.getType())));

            // The SoFa is not a regular FS - do not mark it as such
            if (uimaFS != aJCas.getSofa()) {
                rdfFS.addOntClass(tFeatureStructure);
            }

            // Internal UIMA information
            if (indexed.contains(uimaFS)) {
                rdfFS.addProperty(pIndexedIn, rdfView);
            }

            // Convert features
            convertFeatures(docuri, uimaFS, rdfFS);
        }
    }

    private void convertFeatures(String docuri, FeatureStructure uimaFS, Individual rdfFS)
    {
        var m = rdfFS.getOntModel();

        for (var uimaFeat : uimaFS.getType().getFeatures()) {
            var rdfFeat = m.createProperty(rdfFeature(uimaFeat));
            if (uimaFeat.getRange().isPrimitive()) {
                switch (uimaFeat.getRange().getName()) {
                case CAS.TYPE_NAME_BOOLEAN:
                    rdfFS.addLiteral(rdfFeat, m.createTypedLiteral(uimaFS.getBooleanValue(uimaFeat),
                            XSDDatatype.XSDboolean));
                    break;
                case CAS.TYPE_NAME_BYTE:
                    rdfFS.addLiteral(rdfFeat, m.createTypedLiteral(uimaFS.getByteValue(uimaFeat),
                            XSDDatatype.XSDbyte));
                    break;
                case CAS.TYPE_NAME_DOUBLE:
                    rdfFS.addLiteral(rdfFeat, m.createTypedLiteral(uimaFS.getDoubleValue(uimaFeat),
                            XSDDatatype.XSDdouble));
                    break;
                case CAS.TYPE_NAME_FLOAT:
                    rdfFS.addLiteral(rdfFeat, m.createTypedLiteral(uimaFS.getFloatValue(uimaFeat),
                            XSDDatatype.XSDfloat));
                    break;
                case CAS.TYPE_NAME_INTEGER:
                    rdfFS.addLiteral(rdfFeat,
                            m.createTypedLiteral(uimaFS.getIntValue(uimaFeat), XSDDatatype.XSDint));
                    break;
                case CAS.TYPE_NAME_LONG:
                    rdfFS.addLiteral(rdfFeat, m.createTypedLiteral(uimaFS.getLongValue(uimaFeat),
                            XSDDatatype.XSDlong));
                    break;
                case CAS.TYPE_NAME_SHORT:
                    rdfFS.addLiteral(rdfFeat, m.createTypedLiteral(uimaFS.getShortValue(uimaFeat),
                            XSDDatatype.XSDshort));
                    break;
                case CAS.TYPE_NAME_STRING: {
                    var s = uimaFS.getStringValue(uimaFeat);
                    if (s != null) {
                        if (iriFeatures.contains(uimaFeat.getName())) {
                            rdfFS.addProperty(rdfFeat, m.createResource(s));
                        }
                        else {
                            rdfFS.addLiteral(rdfFeat,
                                    m.createTypedLiteral(s, XSDDatatype.XSDstring));
                        }
                    }
                    break;
                }
                default:
                    throw new IllegalArgumentException(
                            "Feature [" + uimaFeat.getName() + "] has unsupported primitive type ["
                                    + uimaFeat.getRange().getName() + "]");
                }
            }
            else {
                var targetUimaFS = uimaFS.getFeatureValue(uimaFeat);
                if (targetUimaFS != null) {
                    rdfFS.addProperty(rdfFeat, m.createResource(rdfUri(docuri, targetUimaFS)));
                }
            }
        }
    }

    private static String rdfUri(String docuri, FeatureStructure uimaFS)
    {
        return format("%s#%d", docuri, uimaFS.getCAS().getLowLevelCAS().ll_getFSRef(uimaFS));
    }

    private static String rdfFeature(Feature aUimaFeature)
    {
        return rdfType(aUimaFeature.getDomain()) + "-" + aUimaFeature.getShortName();
    }

    private static String rdfType(Type aUimaType)
    {
        return RdfCas.NS_UIMA + aUimaType.getName();
    }
}
