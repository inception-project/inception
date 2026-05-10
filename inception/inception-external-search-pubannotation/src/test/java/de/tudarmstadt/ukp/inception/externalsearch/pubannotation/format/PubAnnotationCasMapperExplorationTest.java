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
package de.tudarmstadt.ukp.inception.externalsearch.pubannotation.format;

import static de.tudarmstadt.ukp.inception.externalsearch.pubannotation.format.PubAnnotationCasMapper.LABEL_FEATURE;
import static de.tudarmstadt.ukp.inception.externalsearch.pubannotation.format.PubAnnotationCasMapper.BASIC_RELATION_LAYER;
import static de.tudarmstadt.ukp.inception.externalsearch.pubannotation.format.PubAnnotationCasMapper.BASIC_SPAN_LAYER;
import static de.tudarmstadt.ukp.inception.externalsearch.pubannotation.format.PubAnnotationCasMapper.FEAT_REL_SOURCE;
import static de.tudarmstadt.ukp.inception.externalsearch.pubannotation.format.PubAnnotationCasMapper.FEAT_REL_TARGET;
import static org.apache.uima.cas.CAS.TYPE_NAME_ANNOTATION;
import static org.apache.uima.cas.CAS.TYPE_NAME_STRING;

import java.util.LinkedHashMap;
import java.util.List;

import org.apache.uima.UIMAFramework;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.util.CasCreationUtils;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import de.tudarmstadt.ukp.inception.externalsearch.pubannotation.PubAnnotationProvider;
import de.tudarmstadt.ukp.inception.externalsearch.pubannotation.model.PubAnnotationDocument;
import de.tudarmstadt.ukp.inception.externalsearch.pubannotation.traits.PubAnnotationProviderTraits;
import de.tudarmstadt.ukp.inception.externalsearch.pubmed.entrez.EntrezClient;

/**
 * Exploratory live-API run that fetches a diverse set of PubAnnotation documents and reports how
 * the {@link PubAnnotationCasMapper} maps them. Not a strict assertion test — it prints a per-doc
 * summary and an aggregate so we can eyeball whether the type-mapping ladder behaves reasonably on
 * real data.
 */
@Tag("slow")
public class PubAnnotationCasMapperExplorationTest
{
    /** {sourcedb, sourceid, optional project (null = multi-track)} */
    private static final String[][] SAMPLES = { //
            { "PubMed", "25314077", null }, //
            { "PubMed", "25314077", "PubmedHPO" }, //
            { "PubMed", "25314077", "Inflammaging" }, //
            { "PubMed", "10704529", null }, //
            { "PubMed", "10704529", "GO-BP" }, //
            { "PMC", "1064873", null }, //
    };

    @Test
    public void explore() throws Exception
    {
        var provider = new PubAnnotationProvider(new EntrezClient());
        var traits = new PubAnnotationProviderTraits();

        var aggregate = new Aggregate();
        var rows = new java.util.ArrayList<String>();

        for (var sample : SAMPLES) {
            Thread.sleep(1000); // be nice to the API

            var db = sample[0];
            var id = sample[1];
            var project = sample[2];

            PubAnnotationDocument doc;
            try {
                doc = provider.getAnnotatedDocument(traits, db, id, project);
            }
            catch (Exception e) {
                rows.add(String.format("FAIL %s/%s [%s]: %s", db, id, project, e.getMessage()));
                continue;
            }

            var stats = mapAndAnalyze(doc);
            aggregate.merge(stats);
            rows.add(String.format(
                    "%-6s %-12s [%-12s] denot=%d (specific=%d, fallback=%d, "
                            + "discontinuous=%d) rel=%d (specific=%d, fallback=%d) "
                            + "attr=%d (set=%d, value-fallback=%d, skipped=%d) topObj=%s",
                    db, id, project == null ? "—" : project, stats.totalDenotations,
                    stats.denotationsSpecific, stats.denotationsBasicFallback,
                    stats.discontinuousSkipped, stats.totalRelations, stats.relationsSpecific,
                    stats.relationsBasicFallback, stats.totalAttributes, stats.attributesByName,
                    stats.attributesByValueFallback, stats.attributesSkipped, stats.topObjs(3)));
        }

        System.out.println("=== PubAnnotation CAS mapper exploration ===");
        rows.forEach(System.out::println);
        System.out.println("--- aggregate ---");
        System.out.printf(
                "denotations: %d total, specific=%d (%.0f%%), fallback=%d (%.0f%%), "
                        + "discontinuous=%d%n",
                aggregate.totalDenotations, aggregate.denotationsSpecific,
                pct(aggregate.denotationsSpecific, aggregate.totalDenotations),
                aggregate.denotationsBasicFallback,
                pct(aggregate.denotationsBasicFallback, aggregate.totalDenotations),
                aggregate.discontinuousSkipped);
        System.out.printf("relations:   %d total, specific=%d, fallback=%d%n",
                aggregate.totalRelations, aggregate.relationsSpecific,
                aggregate.relationsBasicFallback);
        System.out.printf(
                "attributes:  %d total, set-by-name=%d, set-by-value-fallback=%d, skipped=%d%n",
                aggregate.totalAttributes, aggregate.attributesByName,
                aggregate.attributesByValueFallback, aggregate.attributesSkipped);
        System.out.println("most common objs (across all denotations): " + aggregate.topObjs(15));
        System.out.println("most common preds (relations): " + aggregate.topPredsRel(10));
        System.out.println("most common preds (attributes): " + aggregate.topPredsAttr(10));
    }

    private DocStats mapAndAnalyze(PubAnnotationDocument aDoc) throws Exception
    {
        var stats = new DocStats();

        // Tally raw counts before mapping
        if (aDoc.getTracks() != null) {
            for (var t : aDoc.getTracks()) {
                tallyRaw(t.getDenotations(), t.getRelations(), t.getAttributes(), stats);
            }
        }
        else {
            tallyRaw(aDoc.getDenotations(), aDoc.getRelations(), aDoc.getAttributes(), stats);
        }

        // Run mapper against a CAS that contains only the basic fallback layers — this means
        // every denotation either maps to basic fallback or is dropped on resolve. To also see
        // the "specific" path light up, we'd need a real type system. The point here is to see
        // how often the fallback fires and how attribute mapping behaves.
        var cas = createBasicCas();
        new PubAnnotationCasMapper(cas).apply(aDoc);

        var basicSpanType = cas.getTypeSystem().getType(BASIC_SPAN_LAYER);
        var basicRelationType = cas.getTypeSystem().getType(BASIC_RELATION_LAYER);

        var basicSpans = list(cas.<AnnotationFS> getAnnotationIndex(basicSpanType));
        var basicRels = list(cas.<AnnotationFS> getAnnotationIndex(basicRelationType));

        // Every span we produced went via fallback in this CAS (no other types). Count obj-style.
        stats.denotationsBasicFallback = basicSpans.size();
        stats.denotationsSpecific = 0; // no specific types in this CAS
        stats.relationsBasicFallback = basicRels.size();
        stats.relationsSpecific = 0;

        // Attributes: count how many basic spans / relations actually got a non-empty label —
        // a proxy for "we kept the original label". Real attribute set/skip counts are harder
        // without a richer type system; we approximate by checking what landed.
        // For attribute-by-name vs by-value-fallback vs skipped, we instead look at the input.
        countAttributeResolution(aDoc, stats);

        return stats;
    }

    private void countAttributeResolution(PubAnnotationDocument aDoc, DocStats aStats)
    {
        // Without a custom-typed CAS, every attribute would be skipped (no matching feature on
        // custom.Span/Relation that has only "label"/"Governor"/"Dependent"). So we approximate
        // resolution by checking input shape: per subj, count attributes; if the subj has 1
        // attribute and pred=="value" → would resolve via name; otherwise → would need
        // value-fallback or skip.
        java.util.function.BiConsumer<List<de.tudarmstadt.ukp.inception.externalsearch.pubannotation.model.PubAnnotationAttribute>, DocStats> tally = (
                attrs, s) -> {
            if (attrs == null) {
                return;
            }
            var perSubj = new LinkedHashMap<String, Integer>();
            for (var a : attrs) {
                perSubj.merge(a.getSubject(), 1, Integer::sum);
            }
            for (var a : attrs) {
                if ("value".equals(a.getPredicate())) {
                    s.attributesByName++;
                }
                else if (perSubj.getOrDefault(a.getSubject(), 0) == 1) {
                    s.attributesByValueFallback++;
                }
                else {
                    s.attributesSkipped++;
                }
            }
        };

        if (aDoc.getTracks() != null) {
            for (var t : aDoc.getTracks()) {
                tally.accept(t.getAttributes(), aStats);
            }
        }
        else {
            tally.accept(aDoc.getAttributes(), aStats);
        }
    }

    private void tallyRaw(
            List<de.tudarmstadt.ukp.inception.externalsearch.pubannotation.model.PubAnnotationDenotation> aDenotations,
            List<de.tudarmstadt.ukp.inception.externalsearch.pubannotation.model.PubAnnotationRelation> aRelations,
            List<de.tudarmstadt.ukp.inception.externalsearch.pubannotation.model.PubAnnotationAttribute> aAttributes,
            DocStats aStats)
    {
        if (aDenotations != null) {
            aStats.totalDenotations += aDenotations.size();
            for (var d : aDenotations) {
                aStats.objCounts.merge(String.valueOf(d.getObject()), 1, Integer::sum);
                if (d.getSpans() != null && d.getSpans().size() > 1) {
                    aStats.discontinuousSkipped++;
                }
            }
        }
        if (aRelations != null) {
            aStats.totalRelations += aRelations.size();
            for (var r : aRelations) {
                aStats.relPredCounts.merge(String.valueOf(r.getPredicate()), 1, Integer::sum);
            }
        }
        if (aAttributes != null) {
            aStats.totalAttributes += aAttributes.size();
            for (var a : aAttributes) {
                aStats.attrPredCounts.merge(String.valueOf(a.getPredicate()), 1, Integer::sum);
            }
        }
    }

    private static <T> List<T> list(Iterable<T> aIt)
    {
        var l = new java.util.ArrayList<T>();
        aIt.forEach(l::add);
        return l;
    }

    private static double pct(int part, int total)
    {
        return total == 0 ? 0.0 : 100.0 * part / total;
    }

    private static CAS createBasicCas() throws Exception
    {
        var tsd = UIMAFramework.getResourceSpecifierFactory().createTypeSystemDescription();

        var basicSpan = tsd.addType(BASIC_SPAN_LAYER, null, TYPE_NAME_ANNOTATION);
        basicSpan.addFeature(LABEL_FEATURE, null, TYPE_NAME_STRING);

        var basicRel = tsd.addType(BASIC_RELATION_LAYER, null, TYPE_NAME_ANNOTATION);
        basicRel.addFeature(LABEL_FEATURE, null, TYPE_NAME_STRING);
        basicRel.addFeature(FEAT_REL_SOURCE, null, TYPE_NAME_ANNOTATION);
        basicRel.addFeature(FEAT_REL_TARGET, null, TYPE_NAME_ANNOTATION);

        return CasCreationUtils.createCas(tsd, null, null);
    }

    private static class DocStats
    {
        int totalDenotations;
        int totalRelations;
        int totalAttributes;
        int discontinuousSkipped;
        int denotationsSpecific;
        int denotationsBasicFallback;
        int relationsSpecific;
        int relationsBasicFallback;
        int attributesByName;
        int attributesByValueFallback;
        int attributesSkipped;
        LinkedHashMap<String, Integer> objCounts = new LinkedHashMap<>();
        LinkedHashMap<String, Integer> relPredCounts = new LinkedHashMap<>();
        LinkedHashMap<String, Integer> attrPredCounts = new LinkedHashMap<>();

        String topObjs(int aN)
        {
            return top(objCounts, aN);
        }
    }

    private static class Aggregate
        extends DocStats
    {
        void merge(DocStats aOther)
        {
            totalDenotations += aOther.totalDenotations;
            totalRelations += aOther.totalRelations;
            totalAttributes += aOther.totalAttributes;
            discontinuousSkipped += aOther.discontinuousSkipped;
            denotationsSpecific += aOther.denotationsSpecific;
            denotationsBasicFallback += aOther.denotationsBasicFallback;
            relationsSpecific += aOther.relationsSpecific;
            relationsBasicFallback += aOther.relationsBasicFallback;
            attributesByName += aOther.attributesByName;
            attributesByValueFallback += aOther.attributesByValueFallback;
            attributesSkipped += aOther.attributesSkipped;
            aOther.objCounts.forEach((k, v) -> objCounts.merge(k, v, Integer::sum));
            aOther.relPredCounts.forEach((k, v) -> relPredCounts.merge(k, v, Integer::sum));
            aOther.attrPredCounts.forEach((k, v) -> attrPredCounts.merge(k, v, Integer::sum));
        }

        String topPredsRel(int aN)
        {
            return top(relPredCounts, aN);
        }

        String topPredsAttr(int aN)
        {
            return top(attrPredCounts, aN);
        }
    }

    private static String top(LinkedHashMap<String, Integer> aMap, int aN)
    {
        return aMap.entrySet().stream() //
                .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue())) //
                .limit(aN) //
                .map(e -> e.getKey() + "=" + e.getValue()) //
                .reduce((a, b) -> a + ", " + b) //
                .orElse("(none)");
    }

}
