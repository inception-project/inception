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

import static de.tudarmstadt.ukp.inception.io.rdf.internal.RdfCas.PREFIX_RDFCAS;
import static de.tudarmstadt.ukp.inception.io.rdf.internal.RdfCas.SCHEME_UIMA;
import static java.lang.String.format;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.Type;
import org.apache.uima.jcas.JCas;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;

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

    public void convert(JCas aJCas, Model aTarget) throws CASException
    {
        // Set up prefix mappings
        var ts = aJCas.getTypeSystem();
        aTarget.setNamespace("cas", SCHEME_UIMA + "uima.cas.");
        aTarget.setNamespace("tcas", SCHEME_UIMA + "uima.tcas.");
        aTarget.setNamespace(PREFIX_RDFCAS, RdfCas.NS_RDFCAS);

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
                aTarget.setNamespace(prefix, SCHEME_UIMA + nameMatcher.group("LONG"));
            }
        }

        var viewIterator = aJCas.getViewIterator();
        while (viewIterator.hasNext()) {
            convertView(viewIterator.next(), aTarget);
        }
    }

    private void convertView(JCas aJCas, Model aTarget)
    {
        var vf = SimpleValueFactory.getInstance();

        // Get a URI for the document
        var dmd = DocumentMetaData.get(aJCas);
        var docuri = dmd.getDocumentUri() != null ? dmd.getDocumentUri()
                : DOCUMENT_SCHEME + dmd.getDocumentId();

        if (docuri.indexOf(':') < 0) {
            docuri = DOCUMENT_SCHEME + docuri;
        }

        // These only collect a single view...
        var reachable = CasDoctorUtils.collectReachable(aJCas.getCas());
        var indexed = CasDoctorUtils.collectIndexed(aJCas.getCas());
        // ... they do not collect the SOFA, so we add that explicitly
        reachable.add(aJCas.getSofa());

        // Set up the view itself
        var rdfView = vf.createIRI(
                format("%s#%d", docuri, aJCas.getLowLevelCas().ll_getFSRef(aJCas.getSofa())));
        aTarget.add(rdfView, RDF.TYPE, RdfCas.TYPE_VIEW);

        for (var uimaFS : reachable) {
            var uri = format("%s#%d", docuri, aJCas.getLowLevelCas().ll_getFSRef(uimaFS));
            var rdfFS = vf.createIRI(uri);
            aTarget.add(rdfFS, RDF.TYPE, rdfType(aTarget, uimaFS.getType()));

            // The SoFa is not a regular FS - do not mark it as such
            if (uimaFS != aJCas.getSofa()) {
                aTarget.add(rdfFS, RDF.TYPE, RdfCas.TYPE_FEATURE_STRUCTURE);
            }

            // Internal UIMA information
            if (indexed.contains(uimaFS)) {
                aTarget.add(rdfFS, RdfCas.PROP_INDEXED_IN, rdfView);
            }

            // Convert features
            convertFeatures(aTarget, docuri, uimaFS, rdfFS);
        }
    }

    private void convertFeatures(Model aTarget, String docuri, FeatureStructure uimaFS, IRI rdfFS)
    {
        var vf = SimpleValueFactory.getInstance();

        for (var uimaFeat : uimaFS.getType().getFeatures()) {
            var rdfFeat = rdfFeature(aTarget, uimaFeat);
            if (uimaFeat.getRange().isPrimitive()) {
                switch (uimaFeat.getRange().getName()) {
                case CAS.TYPE_NAME_BOOLEAN:
                    aTarget.add(rdfFS, rdfFeat, vf.createLiteral(uimaFS.getBooleanValue(uimaFeat)));
                    break;
                case CAS.TYPE_NAME_BYTE:
                    aTarget.add(rdfFS, rdfFeat, vf.createLiteral(uimaFS.getByteValue(uimaFeat)));
                    break;
                case CAS.TYPE_NAME_DOUBLE:
                    aTarget.add(rdfFS, rdfFeat, vf.createLiteral(uimaFS.getDoubleValue(uimaFeat)));
                    break;
                case CAS.TYPE_NAME_FLOAT:
                    aTarget.add(rdfFS, rdfFeat, vf.createLiteral(uimaFS.getFloatValue(uimaFeat)));
                    break;
                case CAS.TYPE_NAME_INTEGER:
                    aTarget.add(rdfFS, rdfFeat, vf.createLiteral(uimaFS.getIntValue(uimaFeat)));
                    break;
                case CAS.TYPE_NAME_LONG:
                    aTarget.add(rdfFS, rdfFeat, vf.createLiteral(uimaFS.getLongValue(uimaFeat)));
                    break;
                case CAS.TYPE_NAME_SHORT:
                    aTarget.add(rdfFS, rdfFeat, vf.createLiteral(uimaFS.getShortValue(uimaFeat)));
                    break;
                case CAS.TYPE_NAME_STRING: {
                    var s = uimaFS.getStringValue(uimaFeat);
                    if (s != null) {
                        if (iriFeatures.contains(uimaFeat.getName())) {
                            aTarget.add(rdfFS, rdfFeat, vf.createIRI(s));
                        }
                        else {
                            aTarget.add(rdfFS, rdfFeat, vf.createLiteral(s));
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
                    aTarget.add(rdfFS, rdfFeat, vf.createIRI(rdfUri(docuri, targetUimaFS)));
                }
            }
        }
    }

    private static String rdfUri(String docuri, FeatureStructure uimaFS)
    {
        return format("%s#%d", docuri, uimaFS.getCAS().getLowLevelCAS().ll_getFSRef(uimaFS));
    }

    private static IRI rdfFeature(Model aModel, Feature aUimaFeature)
    {
        var typeIri = rdfType(aModel, aUimaFeature.getDomain());
        return new BasicIRI(typeIri.getNamespace(),
                typeIri.getLocalName() + "-" + aUimaFeature.getShortName());
    }

    private static IRI rdfType(Model aModel, Type aUimaType)
    {
        Namespace bestNs = null;
        for (var ns : aModel.getNamespaces()) {
            var nsName = ns.getName().substring(SCHEME_UIMA.length());
            if (aUimaType.getName().startsWith(nsName)
                    && (bestNs == null || nsName.length() > bestNs.getName().length())) {
                bestNs = ns;
            }
        }

        var vf = SimpleValueFactory.getInstance();
        if (bestNs != null) {
            var namespace = bestNs.getName();
            var localName = aUimaType.getName()
                    .substring(bestNs.getName().length() - SCHEME_UIMA.length());
            return new BasicIRI(namespace, localName);
        }

        return vf.createIRI(SCHEME_UIMA + aUimaType.getName());
    }
}
