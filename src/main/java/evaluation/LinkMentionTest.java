package evaluation;

import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.apache.uima.UIMAException;
import org.eclipse.rdf4j.repository.sparql.SPARQLRepository;
import org.junit.Before;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;

class LinkMentionTest<Token>
{

    private static Logger logger = LoggerFactory.getLogger(LinkMentionTest.class);

    @Test
    void testGetMentionSentenceStringString()
    {
        List<de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token> test = null;
        try {
            test = LinkMention.getMentionSentence("Hello. It's Peter.", "Peter");
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
            LinkMention.findMentionSentenceInDoc(docText, mention);
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
        List<de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token> mentionSentence;
        try {
            mentionSentence = LinkMention.getMentionSentence(text, mention);

            List<String> splitMention = Arrays.asList(mention.split(" "));
            List<de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token> mentionContext = LinkMention
                    .getMentionContext(mentionSentence, splitMention, mentionContextSize);
        }
        catch (UIMAException | IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    @Test
    public void testGetSemanticSignature()
    {
         LinkMention.initializeConnection();
         SemanticSignature s = LinkMention.getSemanticSignature("Q1");
         System.out.println(s);
    }

}
