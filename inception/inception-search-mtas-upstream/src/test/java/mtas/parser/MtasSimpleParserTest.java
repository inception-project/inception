package mtas.parser;

import static org.junit.Assert.*;

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import mtas.parser.simple.ParseException;
import mtas.parser.simple.util.MtasSimpleParserWordQuery;
import mtas.parser.simple.MtasSimpleParser;
import mtas.search.spans.MtasSpanAndQuery;
import mtas.search.spans.MtasSpanOrQuery;
import mtas.search.spans.MtasSpanSequenceItem;
import mtas.search.spans.MtasSpanSequenceQuery;
import mtas.search.spans.util.MtasSpanQuery;
import mtas.search.spans.util.MtasSpanUniquePositionQuery;

/**
 * The Class MtasCQLParserTestWord.
 */
public class MtasSimpleParserTest {

  /** The log. */
  private static final Logger log = LoggerFactory.getLogger(MtasSimpleParserTest.class);

  
  private void testSimpleParse(String field, String defaultPrefix, String simple,
     MtasSpanQuery q) {
    List<MtasSpanQuery> qList = new ArrayList<>();
    qList.add(q);
    testSimpleParse(field,defaultPrefix,simple,qList);
  }
  
  private void testSimpleParse(String field, String defaultPrefix, String simple,
      List<MtasSpanQuery> qList) {
    MtasSimpleParser p = new MtasSimpleParser(
        new BufferedReader(new StringReader(simple)));
    try {
      assertEquals(p.parse(field, defaultPrefix, null, null), qList);
    } catch (ParseException e) {
      log.error("Error", e);
      e.printStackTrace();
    }
  }

  /**
   * Test CQL equivalent.
   *
   * @param field the field
   * @param defaultPrefix the default prefix
   * @param simple1 the first simple expression
   * @param simple2 the second simple expression
   */
  private void testSimpleEquivalent(String field, String defaultPrefix,
      String simple1, String simple2) {
    MtasSimpleParser p1 = new MtasSimpleParser(
        new BufferedReader(new StringReader(simple1)));
    MtasSimpleParser p2 = new MtasSimpleParser(
        new BufferedReader(new StringReader(simple2)));
    try {
      assertEquals(p1.parse(field, defaultPrefix, null, null),
          p2.parse(field, defaultPrefix, null, null));
    } catch (ParseException e) {
      log.error("Error", e);
    }
  }


  @org.junit.Test
  public void singleWord() throws ParseException {
    String field = "testveld";
    String prefix = "lemma";
    String simple = "koe";
    MtasSpanQuery q = new MtasSimpleParserWordQuery(field, prefix, simple);
    testSimpleParse(field, prefix, simple, new MtasSpanUniquePositionQuery(q));
  }
  
  @org.junit.Test
  public void singleWordBracketQuotes() throws ParseException {
    String field = "testveld";
    String prefix = "lemma";
    String simple = "(\"koe\")";
    MtasSpanQuery q = new MtasSimpleParserWordQuery(field, prefix, "koe");
    testSimpleParse(field, prefix, simple, new MtasSpanUniquePositionQuery(q));
  }
   
  @org.junit.Test
  public void singleWordBracketQuotesEscaped() throws ParseException {
    String field = "testveld";
    String prefix = "lemma";
    String simple = "\\(\\\"koe\\\"\\)";
    MtasSpanQuery q = new MtasSimpleParserWordQuery(field, prefix, "(\"koe\")");
    testSimpleParse(field, prefix, simple, new MtasSpanUniquePositionQuery(q));
  }
  
  @org.junit.Test
  public void singleWordPrefix() throws ParseException {
    String field = "testveld";
    String prefix = "lemma";
    String simple = "pos:N";
    MtasSpanQuery q = new MtasSimpleParserWordQuery(field, "pos", "N");
    testSimpleParse(field, prefix, simple, new MtasSpanUniquePositionQuery(q));
  }
  
  @org.junit.Test
  public void singleWordPrefixMultipleAnd() throws ParseException {
    String field = "testveld";
    String prefix = "lemma";
    String simple = "pos:LID&t:den&de";
    MtasSpanQuery q1 = new MtasSimpleParserWordQuery(field, "pos", "LID");
    MtasSpanQuery q2 = new MtasSimpleParserWordQuery(field, "t", "den");
    MtasSpanQuery q3 = new MtasSimpleParserWordQuery(field, "lemma", "de");
    MtasSpanQuery q = new MtasSpanAndQuery(q1,q2,q3);
    testSimpleParse(field, prefix, simple, new MtasSpanUniquePositionQuery(q));
  }
  
  @org.junit.Test
  public void singleWordPrefixMultipleOr() throws ParseException {
    String field = "testveld";
    String prefix = "lemma";
    String simple = "pos:LID|t:den|de";
    MtasSpanQuery q1 = new MtasSimpleParserWordQuery(field, "pos", "LID");
    MtasSpanQuery q2 = new MtasSimpleParserWordQuery(field, "t", "den");
    MtasSpanQuery q3 = new MtasSimpleParserWordQuery(field, "lemma", "de");
    MtasSpanQuery q = new MtasSpanOrQuery(q1,q2,q3);
    testSimpleParse(field, prefix, simple, new MtasSpanUniquePositionQuery(q));
  }
  
  @org.junit.Test
  public void singleWordPrefixMultipleAndOr() throws ParseException {
    String field = "testveld";
    String prefix1 = "lemma";
    String simple1 = "(pos:LID&t:den)|de";
    String prefix2 = "lemma";
    String simple2 = "(pos:N|t_lc:koe)&paard";
    String simple = "\""+simple1+" "+simple2+"\"";
    MtasSpanQuery q1 = new MtasSimpleParserWordQuery(field, "pos", "LID");
    MtasSpanQuery q2 = new MtasSimpleParserWordQuery(field, "t", "den");
    MtasSpanQuery q3 = new MtasSimpleParserWordQuery(field, "lemma", "de");
    MtasSpanQuery q4 = new MtasSimpleParserWordQuery(field, "pos", "N");
    MtasSpanQuery q5 = new MtasSimpleParserWordQuery(field, "t_lc", "koe");
    MtasSpanQuery q6 = new MtasSimpleParserWordQuery(field, "lemma", "paard");
    MtasSpanQuery q12 = new MtasSpanAndQuery(q1,q2);
    MtasSpanQuery q123 = new MtasSpanOrQuery(q12,q3);
    testSimpleParse(field, prefix1, simple1, new MtasSpanUniquePositionQuery(q123));
    MtasSpanQuery q45 = new MtasSpanOrQuery(q4,q5);
    MtasSpanQuery q456 = new MtasSpanAndQuery(q45,q6);
    testSimpleParse(field, prefix2, simple2, new MtasSpanUniquePositionQuery(q456));
    List<MtasSpanSequenceItem> qList = new ArrayList<>();
    qList.add(new MtasSpanSequenceItem(q123, false));
    qList.add(new MtasSpanSequenceItem(q456, false));
    MtasSpanQuery q = new MtasSpanSequenceQuery(qList, null, null);
    testSimpleParse(field, prefix1, simple, new MtasSpanUniquePositionQuery(q));
    
  }
  
  @org.junit.Test
  public void singleWordPrefixMultipleEscaped() throws ParseException {
    String field = "testveld";
    String prefix = "lemma";
    String simple = "pos:LID&t:d\\&en&de";
    MtasSpanQuery q1 = new MtasSimpleParserWordQuery(field, "pos", "LID");
    MtasSpanQuery q2 = new MtasSimpleParserWordQuery(field, "t", "d&en");
    MtasSpanQuery q3 = new MtasSimpleParserWordQuery(field, "lemma", "de");
    MtasSpanQuery q = new MtasSpanAndQuery(q1,q2,q3);
    testSimpleParse(field, prefix, simple, new MtasSpanUniquePositionQuery(q));
  }
  
  @org.junit.Test
  public void singleWordQuotesEscaped() throws ParseException {
    String field = "testveld";
    String prefix = "lemma";
    String simple = "\\\"koe\\\"";
    MtasSpanQuery q = new MtasSimpleParserWordQuery(field, prefix, "\"koe\"");
    testSimpleParse(field, prefix, simple, new MtasSpanUniquePositionQuery(q));
  }
  
  @org.junit.Test(expected = ParseException.class)
  public void singleWordWronglyEscaped() throws ParseException  {
    String field = "testveld";
    String prefix = "lemma";
    String simple = "\"koe\\\"";
    MtasSimpleParser p = new MtasSimpleParser(
        new BufferedReader(new StringReader(simple)));
    p.parse(field, prefix, null, null);
  }
  
  @org.junit.Test(expected = ParseException.class)
  public void singleWordWronglyBracketed() throws ParseException  {
    String field = "testveld";
    String prefix = "lemma";
    String simple = "(koe))";
    MtasSimpleParser p = new MtasSimpleParser(
        new BufferedReader(new StringReader(simple)));
    p.parse(field, prefix, null, null);
  }
  
   @org.junit.Test
  public void singleWordSequence() throws ParseException {
    String field = "testveld";
    String prefix = "lemma";
    String simple = "\"een echte test\"";
    List<String> words = new ArrayList<>(Arrays.asList(simple.replace("\"","").split(" ")));
    List<MtasSpanSequenceItem> qList = new ArrayList<>();
    for(String word : words) {
      qList.add(new MtasSpanSequenceItem(new MtasSimpleParserWordQuery(field, prefix, word), false));
    }
    MtasSpanQuery q = new MtasSpanSequenceQuery(qList, null, null);
    testSimpleParse(field, prefix, simple, new MtasSpanUniquePositionQuery(q));
  }
  
  @org.junit.Test
  public void singleWordSequenceEscape() throws ParseException {
    String field = "testveld";
    String prefix = "lemma";
    String simple = "\"een \\\"echte\\\" test\"";
    List<MtasSpanSequenceItem> qList = new ArrayList<>();
    qList.add(new MtasSpanSequenceItem(new MtasSimpleParserWordQuery(field, prefix, "een"), false));
    qList.add(new MtasSpanSequenceItem(new MtasSimpleParserWordQuery(field, prefix, "\"echte\""), false));
    qList.add(new MtasSpanSequenceItem(new MtasSimpleParserWordQuery(field, prefix, "test"), false));
    MtasSpanQuery q = new MtasSpanSequenceQuery(qList, null, null);
    testSimpleParse(field, prefix, simple, new MtasSpanUniquePositionQuery(q));
  }
  
  @org.junit.Test
  public void singleWordSequenceBracket() throws ParseException {
    String field = "testveld";
    String prefix = "lemma";
    String simple = "\"een (echte) test\"";
    List<MtasSpanSequenceItem> qList = new ArrayList<>();
    qList.add(new MtasSpanSequenceItem(new MtasSimpleParserWordQuery(field, prefix, "een"), false));
    qList.add(new MtasSpanSequenceItem(new MtasSimpleParserWordQuery(field, prefix, "echte"), false));
    qList.add(new MtasSpanSequenceItem(new MtasSimpleParserWordQuery(field, prefix, "test"), false));
    MtasSpanQuery q = new MtasSpanSequenceQuery(qList, null, null);
    testSimpleParse(field, prefix, simple, new MtasSpanUniquePositionQuery(q));
  }
  
  @org.junit.Test
  public void multipleWords() throws ParseException {
    String field = "testveld";
    String prefix = "lemma";
    String simple = "koe paard schaap";
    List<String> words = new ArrayList<>(Arrays.asList(simple.split(" ")));
    List<MtasSpanQuery> qList = new ArrayList<>();
    for(String word : words) {
      qList.add(new MtasSpanUniquePositionQuery(new MtasSimpleParserWordQuery(field, prefix, word)));
    }
    testSimpleParse(field, prefix, simple, qList);
  }
  
  @org.junit.Test
  public void multipleWordsAndSequences() throws ParseException {
    String field = "testveld";
    String prefix = "lemma";
    String simple = "koe \"paard schaap\" geit";
    List<MtasSpanQuery> qList = new ArrayList<>();
    List<MtasSpanSequenceItem> sList = new ArrayList<>();
    sList.add(new MtasSpanSequenceItem(new MtasSimpleParserWordQuery(field, prefix, "paard"), false));
    sList.add(new MtasSpanSequenceItem(new MtasSimpleParserWordQuery(field, prefix, "schaap"), false));
    qList.add(new MtasSpanUniquePositionQuery(new MtasSimpleParserWordQuery(field, prefix, "koe")));
    qList.add(new MtasSpanUniquePositionQuery(new MtasSpanSequenceQuery(sList, null, null)));
    qList.add(new MtasSpanUniquePositionQuery(new MtasSimpleParserWordQuery(field, prefix, "geit")));
    testSimpleParse(field, prefix, simple, qList);
  }
  
 

  
}
