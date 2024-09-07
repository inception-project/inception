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
package de.tudarmstadt.ukp.inception.search.index.mtas;

import static de.tudarmstadt.ukp.inception.search.index.mtas.MtasUtils.bytesToChars;
import static java.util.stream.Collectors.toList;
import static org.apache.lucene.search.ScoreMode.COMPLETE_NO_SCORES;
import static org.apache.uima.fit.factory.JCasFactory.createJCas;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Objects;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.custom.CustomAnalyzer;
import org.apache.lucene.analysis.miscellaneous.PerFieldAnalyzerWrapper;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.codecs.Codec;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.SegmentReader;
import org.apache.lucene.index.Terms;
import org.apache.lucene.queries.spans.SpanWeight;
import org.apache.lucene.queries.spans.Spans;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.uima.cas.CASException;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.fit.testing.factory.TokenBuilder;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.xml.sax.SAXException;

import de.tudarmstadt.ukp.dkpro.core.api.metadata.type.DocumentMetaData;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.inception.support.uima.WebAnnoCasUtil;
import mtas.analysis.token.MtasTokenString;
import mtas.analysis.util.MtasTokenizerFactory;
import mtas.codec.MtasCodec;
import mtas.codec.util.CodecInfo;
import mtas.codec.util.CodecSearchTree.MtasTreeHit;
import mtas.codec.util.CodecUtil;
import mtas.parser.cql.MtasCQLParser;
import mtas.parser.cql.ParseException;
import mtas.search.spans.util.MtasSpanQuery;

/**
 * The Class MtasSearchTestConsistency.
 */
@Disabled("Currently does not run because requires INCEpTION running as well")
public class MtasUimaParserLuceneTest
{
    /** The Constant FIELD_ID. */
    private static final String FIELD_ID = "id";

    /** The Constant FIELD_TITLE. */
    private static final String FIELD_TITLE = "title";

    /** The Constant FIELD_CONTENT. */
    private static final String FIELD_CONTENT = "content";

    private static final String MTAS_PARSER = MtasUimaParser.class.getName();

    @Test
    public void testUimaParser(@TempDir Path aTemp) throws Exception
    {
        Directory directory = FSDirectory.open(aTemp.resolve("index"));

        PerFieldAnalyzerWrapper analyzer = createAnalyzer();

        try (IndexWriter w = createIndexWriter(directory, analyzer)) {
            indexDocument(w, 1, "Test", "11", "This is a test . This is sentence two .");
            indexDocument(w, 1, "Test", "12", "This is second document .");
        }

        // String cql = "([][Token=\"test\" | Token=\"Test\"]) within <Sentence/>";
        var cql = "([Token=\"this\" | Token=\"This\"])";
        // cql = "([])";
        IndexReader indexReader = DirectoryReader.open(directory);

        MtasSpanQuery q = createQuery(FIELD_CONTENT, cql);

        // Build the query prefixes list from the annotation types
        List<String> prefixes = createJCas().select(Annotation.class) //
                .map(a -> a.getType().getShortName()) //
                .distinct() //
                .collect(toList());

        doQuery(indexReader, FIELD_CONTENT, q, prefixes);
    }

    static byte[] createBinaryCasDocument(int aDocId, String aTitle, String aText)
        throws ResourceInitializationException, CASException, IOException
    {
        JCas jcas = JCasFactory.createJCas();
        TokenBuilder<Token, Sentence> tb = new TokenBuilder<>(Token.class, Sentence.class);
        tb.buildTokens(jcas, aText);

        DocumentMetaData dmd = DocumentMetaData.create(jcas);
        dmd.setDocumentTitle(aTitle);
        dmd.setDocumentId(Integer.toString(aDocId));

        return WebAnnoCasUtil.casToByteArray(jcas.getCas());
    }

    static void indexDocument(IndexWriter aWriter, int aDocId, String aTitle, String aField,
            String aText)
        throws ResourceInitializationException, CASException, SAXException, IOException,
        UnsupportedEncodingException
    {
        var binaryCas = createBinaryCasDocument(aDocId, aTitle, aText);
        String encodedCAS = new String(bytesToChars(binaryCas));

        Document doc = new Document();
        doc.add(new StringField(FIELD_ID, aField, Field.Store.YES));
        doc.add(new StringField(FIELD_TITLE, aTitle, Field.Store.YES));
        doc.add(new TextField(FIELD_CONTENT, encodedCAS, Field.Store.YES));
        aWriter.addDocument(doc);
        aWriter.commit();
    }

    static IndexWriter createIndexWriter(Directory directory, PerFieldAnalyzerWrapper analyzer)
        throws IOException
    {
        IndexWriterConfig config = new IndexWriterConfig(analyzer);
        config.setUseCompoundFile(false);
        config.setCodec(Codec.forName(MtasCodec.MTAS_CODEC_NAME));
        IndexWriter w = new IndexWriter(directory, config);
        w.deleteAll();
        return w;
    }

    static PerFieldAnalyzerWrapper createAnalyzer() throws IOException
    {
        Map<String, String> paramsTokenizer = new HashMap<String, String>();
        paramsTokenizer.put(MtasTokenizerFactory.ARGUMENT_PARSER, MTAS_PARSER);
        paramsTokenizer.put(MtasTokenizerFactory.ARGUMENT_PARSER_ARGS, "{\"projectId\": 1}");

        Analyzer mtasAnalyzer = CustomAnalyzer.builder() //
                .withTokenizer("mtas", paramsTokenizer) //
                .build();
        Map<String, Analyzer> analyzerPerField = new HashMap<String, Analyzer>();
        analyzerPerField.put(FIELD_CONTENT, mtasAnalyzer);
        return new PerFieldAnalyzerWrapper(new StandardAnalyzer(), analyzerPerField);
    }

    static void doQuery(IndexReader indexReader, String field, MtasSpanQuery q,
            List<String> prefixes)
        throws IOException
    {
        ListIterator<LeafReaderContext> iterator = indexReader.leaves().listIterator();
        IndexSearcher searcher = new IndexSearcher(indexReader);
        final float boost = 0;
        SpanWeight spanweight = q.rewrite(indexReader).createWeight(searcher, COMPLETE_NO_SCORES,
                boost);

        while (iterator.hasNext()) {
            System.out.println("#### new iteration ####");
            LeafReaderContext lrc = iterator.next();
            Spans spans = spanweight.getSpans(lrc, SpanWeight.Postings.POSITIONS);
            SegmentReader segmentReader = (SegmentReader) lrc.reader();
            Terms terms = segmentReader.terms(field);
            CodecInfo mtasCodecInfo = CodecInfo.getCodecInfoFromTerms(terms);
            if (spans != null) {
                while (spans.nextDoc() != Spans.NO_MORE_DOCS) {
                    if (segmentReader.numDocs() == segmentReader.maxDoc()
                            || segmentReader.getLiveDocs().get(spans.docID())) {
                        String idValue = segmentReader.document(spans.docID()).getField(FIELD_ID)
                                .stringValue();
                        System.out.println("********  New doc " + spans.docID() + "-" + idValue);
                        while (spans.nextStartPosition() != Spans.NO_MORE_POSITIONS) {
                            System.out.println("------");
                            List<MtasTokenString> tokens = mtasCodecInfo
                                    .getPrefixFilteredObjectsByPositions(field, spans.docID(),
                                            prefixes, spans.startPosition(),
                                            (spans.endPosition() - 1));
                            for (MtasTokenString token : tokens) {
                                System.out.print("docId: " + (lrc.docBase + spans.docID()) + ", ");
                                System.out.print(" position: " + token.getPositionStart()
                                        + (!Objects.equals(token.getPositionEnd(),
                                                token.getPositionStart())
                                                        ? "-" + token.getPositionEnd()
                                                        : ""));
                                System.out.print(" offset: " + token.getOffsetStart() + "-"
                                        + token.getOffsetEnd());
                                System.out.print(" mtasId: " + token.getId());
                                System.out.println(" " + token.getPrefix()
                                        + (token.getPostfix() != null ? ":" + token.getPostfix()
                                                : "")
                                        + ", ");
                            }
                            System.out.println("------");
                            List<MtasTreeHit<String>> hits = mtasCodecInfo
                                    .getPositionedTermsByPrefixesAndPositionRange(field,
                                            spans.docID(), prefixes, spans.startPosition(),
                                            (spans.endPosition() - 1));
                            for (MtasTreeHit<String> hit : hits) {
                                System.out.print("docId: " + (lrc.docBase + spans.docID()) + ", ");
                                System.out.print("position: " + hit.startPosition
                                        + (hit.endPosition != hit.startPosition
                                                ? "-" + hit.endPosition
                                                : ""));
                                System.out.println(" " + CodecUtil.termPrefix(hit.data)
                                        + (CodecUtil.termValue(hit.data) != null
                                                ? ":" + CodecUtil.termValue(hit.data)
                                                : "")
                                        + ", ");
                            }
                        }
                        // if (prefixes != null && !prefixes.isEmpty()) {
                        // }
                    }
                }
            }
        }
    }

    private static MtasSpanQuery createQuery(String field, String cql) throws ParseException
    {
        Reader reader = new BufferedReader(new StringReader(cql));
        MtasCQLParser p = new MtasCQLParser(reader);
        MtasSpanQuery q = p.parse(field, null, null, null, null);
        return q;
    }
}
