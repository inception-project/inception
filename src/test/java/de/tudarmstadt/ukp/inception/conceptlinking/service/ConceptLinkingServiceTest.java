package de.tudarmstadt.ukp.inception.conceptlinking.service;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.apache.uima.UIMAException;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.inception.conceptlinking.model.SemanticSignature;

class ConceptLinkingServiceTest
{

    private static Logger logger = LoggerFactory.getLogger(ConceptLinkingServiceTest.class);

    @Test
    void testGetMentionSentenceStringString()
    {
        List<de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token> test = null;
        try {
            test = ConceptLinkingService.getMentionSentence("Hello. It's Peter.", "Peter");
        }
        catch (UIMAException | IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        System.out.println(test.toString());
    }

    @Test
    public void detectSentences()
    {
        String docText = "Der Gotthard-Basistunnel - Panorama - DerWesten\n"
                + "Der Gotthard-Basistunnel\n" + "\n"
                + "In der Schweiz entsteht der längste Tunnel der Welt - nun steht der Durchstich an.\n"
                + "Blick in die Röhre: Im Gotthard-Basistunnel in der Schweiz... Blick in die Röhre: Im Gotthard-Basistunnel in der Schweiz... ...steht jetzt das Fest der Mineure an: Der Durchstich. Insgesamt... ...wird der Tunnel 57 Kilometer lang sein. Damit... ...ist es der längste Tunnel der Welt. Um eine Stunde... .";
        String mention = "Schweiz";
        try {
            ConceptLinkingService.findMentionSentenceInDoc(docText, mention);
        }
        catch (IOException e) {
            logger.error("Something went wrong.");
        }
    }

    @Test
    public void testGetMentionContext()
    {
        String mention = "mecklenburgische schweiz";
        String text = "Trotzdem heißt die Region Mecklenburgische Schweiz.";
        int mentionContextSize = 2;
        List<Token> mentionSentence;
        try {
            mentionSentence = ConceptLinkingService.getMentionSentence(text, mention);

            List<String> splitMention = Arrays.asList(mention.split(" "));
            List<Token> mentionContext = ConceptLinkingService.getMentionContext(mentionSentence,
                    splitMention, mentionContextSize);
        }
        catch (UIMAException | IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    @Test
    public void testGetSemanticSignature()
    {
        ConceptLinkingService.initializeConnection();
         SemanticSignature s = ConceptLinkingService.getSemanticSignature("Q1");
         System.out.println(s);
    }

}
