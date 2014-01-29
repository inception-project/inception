package de.tudarmstadt.ukp.clarin.webanno.tsv;

import static org.apache.commons.io.IOUtils.closeQuietly;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.jcas.JCas;

import com.ibm.icu.util.StringTokenizer;

import de.tudarmstadt.ukp.dkpro.core.api.io.JCasResourceCollectionReader_ImplBase;
import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.POS;
import de.tudarmstadt.ukp.dkpro.core.api.parameter.ComponentParameters;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Lemma;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;

public class WebAnnoHornMorhoResultToTsv  extends JCasResourceCollectionReader_ImplBase
{
    public void convertToCas(JCas aJCas, InputStream aIs, String aEncoding)
            throws IOException

        {

        String hornMorPhoResult = IOUtils.toString(aIs, aEncoding);
        String[] words = hornMorPhoResult.split("word:|\\?word:");
        StringBuffer text = new StringBuffer();
        int sentenceBegin = 0;
        int tokenBeginPosition = 0;
        for (String word : words) {
            if (word.isEmpty()) {
                continue;
            }
            String[] lines = word.split("\n");
            text.append(lines[0].trim()+ " ");
            Token outToken = new Token(aJCas, tokenBeginPosition, tokenBeginPosition
                    + lines[0].trim().length() );
            outToken.addToIndexes();
            if (lines.length == 1) {
                // sentence marker
                if (lines[0].trim().equals("፡፡")) {
                    Sentence outSentence = new Sentence(aJCas);
                    outSentence.setBegin(sentenceBegin);
                    outSentence.setEnd(text.length());
                    outSentence.addToIndexes();

                    tokenBeginPosition = outToken.getEnd() +1;
                    sentenceBegin = tokenBeginPosition;
                    continue;
                }
                tokenBeginPosition = outToken.getEnd()+1;
                continue;
            }
            String[] details = lines[1].split(",");
            String pos ="";
            String lemma = "";
            for (String detail : details) {
                StringTokenizer detailTk = new StringTokenizer(detail, ":");
                while (detailTk.hasMoreElements()) {
                    String tag = detailTk.nextToken().trim();
                    if (tag.equals("POS") || tag.equals("?POS")){
                        pos = detailTk.nextToken().trim();
                    }
                    if( tag.equals("stem")
                    || tag.equals("citation")){
                        lemma = detailTk.nextToken().trim();
                    }
                }
            }
            if(!lemma.equals("")){
                Lemma outLemma = new Lemma(aJCas, outToken.getBegin(), outToken.getEnd());
                outLemma.setValue(lemma);
                outLemma.addToIndexes();
                outToken.setLemma(outLemma);
            }
            if(!pos.equals("")){
                POS outPos = new POS(aJCas, outToken.getBegin(), outToken.getEnd());
                outPos.setPosValue(pos);
                outPos.addToIndexes();
                outToken.setPos(outPos);
            }
            tokenBeginPosition = outToken.getEnd()+1;
        }
        aJCas.setDocumentText(text.toString());
    }

    public static final String PARAM_ENCODING = ComponentParameters.PARAM_SOURCE_ENCODING;
    @ConfigurationParameter(name = PARAM_ENCODING, mandatory = true, defaultValue = "UTF-8")
    private String encoding;

    @Override
    public void getNext(JCas aJCas)
        throws IOException, CollectionException
    {
        Resource res = nextFile();
        initCas(aJCas, res);
        InputStream is = null;
        try {
            is = res.getInputStream();
            convertToCas(aJCas, is, encoding);
        }
        finally {
            closeQuietly(is);
        }

    }
}
