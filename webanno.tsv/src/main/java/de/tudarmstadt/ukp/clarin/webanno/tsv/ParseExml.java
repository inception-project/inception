package de.tudarmstadt.ukp.clarin.webanno.tsv;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.apache.uima.UIMAException;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.POS;
import de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Lemma;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;

public class ParseExml
{

    public static void main(String[] args)
        throws SAXException, IOException, ParserConfigurationException, TransformerException,
        UIMAException
    {
        DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
        Document document = docBuilder.parse(new File(
                "src/test/resources/tsv/webannotest_2100105761.xml"));
        getWords(document.getElementsByTagName("*"));
    }

    public static void getWords(NodeList nodeList)
        throws UIMAException
    {
        StringBuffer text = new StringBuffer();
        Map<String, Token> tokenId = new LinkedHashMap<String, Token>();

        JCas jCas = JCasFactory.createJCas();
        int sentenceStart = -1;
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node node = nodeList.item(i);
            NamedNodeMap attributes = node.getAttributes();
            if (node.getNodeType() == Node.ELEMENT_NODE && (node.getNodeName().equals("p"))) {
                if(sentenceStart == -1){
                    sentenceStart = 0;
                }
                else{
                    Sentence sentence = new Sentence(jCas, sentenceStart, text.length());
                    sentence.addToIndexes();
                    sentenceStart = text.length();
                }
            }
            else if (node.getNodeType() == Node.ELEMENT_NODE && (node.getNodeName().equals("c"))) {
                if (attributes.getNamedItem("type").getFirstChild().getTextContent().equals("p")) {
                    int begin = text.length();
                    text.append(node.getFirstChild().getTextContent());
                    Token token = new Token(jCas, begin, text.length());
                    token.addToIndexes();
                    tokenId.put(attributes.getNamedItem("xml:id").getFirstChild().getTextContent(),
                            token);
                }
                else {
                    text.append(" ");
                }
            }
            else if (node.getNodeType() == Node.ELEMENT_NODE && node.getNodeName().equals("w")) {
                int begin = text.length();
                text.append(node.getFirstChild().getTextContent());
                Token token = new Token(jCas, begin, text.length());
                token.addToIndexes();
                tokenId.put(attributes.getNamedItem("xml:id").getFirstChild().getTextContent(),
                        token);
            }
            else if (node.getNodeType() == Node.ELEMENT_NODE && node.getNodeName().equals("span")) {
                if(sentenceStart != -1){
                    Sentence sentence = new Sentence(jCas, sentenceStart, text.length());
                    sentence.addToIndexes();
                    sentenceStart = -1;
                }
                Node parentNode = node.getParentNode();
                NamedNodeMap parentAttribute = parentNode.getAttributes();
                if (parentAttribute.getNamedItem("ana").getTextContent().equals("#ePOSlemmatizer")) {

                    Token token = tokenId.get(attributes.getNamedItem("from").getFirstChild()
                            .getTextContent().substring(1));
                    Lemma lemma = new Lemma(jCas, token.getBegin(), token.getEnd());
                    lemma.setValue(node.getFirstChild().getTextContent());
                    lemma.addToIndexes();
                    token.setLemma(lemma);
                    token.addToIndexes();
                }

            }

            else if (node.getNodeType() == Node.ELEMENT_NODE && node.getNodeName().equals("span")) {
                Node parentNode = node.getParentNode();
                NamedNodeMap parentAttribute = parentNode.getAttributes();
                if (parentAttribute.getNamedItem("ana").getTextContent().equals("#ePOStagger")) {

                    Token token = tokenId.get(attributes.getNamedItem("from").getFirstChild()
                            .getTextContent().substring(1));
                    System.out.println(token.getCoveredText());
                    POS pos = new POS(jCas, token.getBegin(), token.getEnd());
                    String posString = node.getFirstChild().getTextContent();
                    pos.setPosValue(posString.substring(posString.indexOf(":", 1)));
                    pos.addToIndexes();
                    token.setPos(pos);
                    token.addToIndexes();
                }

            }

            else if (node.getNodeType() == Node.ELEMENT_NODE && node.getNodeName().equals("span")) {
                Node parentNode = node.getParentNode();
                NamedNodeMap parentAttribute = parentNode.getAttributes();
                if (parentAttribute.getNamedItem("ana").getTextContent()
                        .equals("#automatic-supersense-from-dannet")) {

                    Token token = tokenId.get(attributes.getNamedItem("from").getFirstChild()
                            .getTextContent().substring(1));
                    System.out.println(token.getCoveredText());
                    NamedEntity ne = new NamedEntity(jCas, token.getBegin(), token.getEnd());
                    String neValue = node.getFirstChild().getTextContent();
                    ne.setValue(neValue);
                    ne.addToIndexes();
                }

            }

        }
        jCas.setDocumentText(text.toString());
       for(Sentence sentence: JCasUtil.select(jCas, Sentence.class)){
           System.out.println(sentence.getBegin());
           System.out.println(sentence.getEnd());
           System.out.println(sentence.getCoveredText());
       }
        System.out.println(tokenId);
        System.out.println(text.toString());
    }
}
