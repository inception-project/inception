package mtas.analysis.token;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.lucene.analysis.payloads.PayloadHelper;
import org.apache.lucene.util.BytesRef;
import mtas.analysis.util.MtasParserException;

/**
 * The Class MtasTokenCollection.
 */
public class MtasTokenCollection {

  /** The token collection. */
  private HashMap<Integer, MtasToken> tokenCollection = new HashMap<>();

  /** The token collection index. */
  private ArrayList<Integer> tokenCollectionIndex = new ArrayList<>();

  /**
   * Instantiates a new mtas token collection.
   */
  public MtasTokenCollection() {
    clear();
  }

  /**
   * Adds the.
   *
   * @param token the token
   * @return the integer
   */
  public Integer add(MtasToken token) {
    Integer id = token.getId();
    tokenCollection.put(id, token);
    return id;
  }

  /**
   * Gets the.
   *
   * @param id the id
   * @return the mtas token
   */
  public MtasToken get(Integer id) {
    return tokenCollection.get(id);
  }

  /**
   * Iterator.
   *
   * @return the iterator
   * @throws MtasParserException the mtas parser exception
   */
  public Iterator<MtasToken> iterator() throws MtasParserException {
    checkTokenCollectionIndex();
    return new Iterator<MtasToken>() {

      private Iterator<Integer> indexIterator = tokenCollectionIndex.iterator();

      @Override
      public boolean hasNext() {
        return indexIterator.hasNext();
      }

      @Override
      public MtasToken next() {
        return tokenCollection.get(indexIterator.next());
      }

      @Override
      public void remove() {
        throw new UnsupportedOperationException();
      }
    };
  }

  /**
   * Prints the.
   *
   * @throws MtasParserException the mtas parser exception
   */
  public void print() throws MtasParserException {
    Iterator<MtasToken> it = this.iterator();
    while (it.hasNext()) {
      MtasToken token = it.next();
      System.out.println(token);
    }
  }

  /**
   * Gets the list.
   *
   * @return the list
   * @throws MtasParserException the mtas parser exception
   */
  public String[][] getList() throws MtasParserException {
    String[][] result = new String[(tokenCollection.size() + 1)][];
    result[0] = new String[] { "id", "start real offset", "end real offset",
        "provide real offset", "start offset", "end offset", "provide offset",
        "start position", "end position", "multiple positions", "parent",
        "provide parent", "payload", "prefix", "postfix" };
    int number = 1;
    Iterator<MtasToken> it = this.iterator();
    while (it.hasNext()) {
      MtasToken token = it.next();
      String[] row = new String[15];
      row[0] = token.getId().toString();
      if (token.getRealOffsetStart() != null) {
        row[1] = token.getRealOffsetStart().toString();
        row[2] = token.getRealOffsetEnd().toString();
        row[3] = token.getProvideRealOffset() ? "1" : null;
      }
      if (token.getOffsetStart() != null) {
        row[4] = token.getOffsetStart().toString();
        row[5] = token.getOffsetEnd().toString();
        row[6] = token.getProvideOffset() ? "1" : null;
      }
      if (token.getPositionLength() != null) {
        if (token.getPositionStart().equals(token.getPositionEnd())) {
          row[7] = token.getPositionStart().toString();
          row[8] = token.getPositionEnd().toString();
          row[9] = null;
        } else if ((token.getPositions() == null)
            || (token.getPositions().length == (1 + token.getPositionEnd()
                - token.getPositionStart()))) {
          row[7] = token.getPositionStart().toString();
          row[8] = token.getPositionEnd().toString();
          row[9] = null;
        } else {
          row[7] = null;
          row[8] = null;
          row[9] = Arrays.toString(token.getPositions());
        }
      }
      if (token.getParentId() != null) {
        row[10] = token.getParentId().toString();
        row[11] = token.getProvideParentId() ? "1" : null;
      }
      if (token.getPayload() != null) {
        BytesRef payload = token.getPayload();
        row[12] = Float.toString(PayloadHelper.decodeFloat(Arrays.copyOfRange(
            payload.bytes, payload.offset, (payload.offset + payload.length))));
      }
      row[13] = token.getPrefix();
      row[14] = token.getPostfix();
      result[number] = row;
      number++;
    }
    return result;
  }

  /**
   * Check.
   *
   * @param autoRepair the auto repair
   * @param makeUnique the make unique
   * @throws MtasParserException the mtas parser exception
   */
  public void check(Boolean autoRepair, Boolean makeUnique)
      throws MtasParserException {
    if (autoRepair) {
      autoRepair();
    }
    if (makeUnique) {
      makeUnique();
    }
    checkTokenCollectionIndex();
    for (Integer i : tokenCollectionIndex) {
      // minimal properties
      if (tokenCollection.get(i).getId() == null
          || tokenCollection.get(i).getPositionStart() == null
          || tokenCollection.get(i).getPositionEnd() == null
          || tokenCollection.get(i).getValue() == null) {
        clear();
        break;
      }
    }
  }

  /**
   * Make unique.
   */
  private void makeUnique() {
    HashMap<String, ArrayList<MtasToken>> currentPositionTokens = new HashMap<>();
    ArrayList<MtasToken> currentValueTokens;
    int currentStartPosition = -1;
    MtasToken currentToken = null;
    for (Entry<Integer, MtasToken> entry : tokenCollection.entrySet()) {
      currentToken = entry.getValue();
      if (currentToken.getPositionStart() > currentStartPosition) {
        currentPositionTokens.clear();
        currentStartPosition = currentToken.getPositionStart();
      } else {
        if (currentPositionTokens.containsKey(currentToken.getValue())) {
          currentValueTokens = currentPositionTokens
              .get(currentToken.getValue());

        } else {
          currentValueTokens = new ArrayList<>();
          currentPositionTokens.put(currentToken.getValue(),
              currentValueTokens);
        }
        currentValueTokens.add(currentToken);
      }
    }
  }

  /**
   * Auto repair.
   */
  private void autoRepair() {
    ArrayList<Integer> trash = new ArrayList<>();
    HashMap<Integer, Integer> translation = new HashMap<>();
    HashMap<Integer, MtasToken> newTokenCollection = new HashMap<>();
    Integer parentId;
    Integer maxId = null;
    Integer minId = null;
    Integer startOffset;
    Integer endOffset;
    MtasToken token;
    // check id, position and value
    for (Entry<Integer, MtasToken> entry : tokenCollection.entrySet()) {
      token = entry.getValue();
      boolean putInTrash;
      putInTrash = token.getId() == null;
      putInTrash |= (token.getPositionStart() == null)
          || (token.getPositionEnd() == null);
      putInTrash |= token.getValue() == null || (token.getValue().isEmpty());
      putInTrash |= token.getPrefix() == null || (token.getPrefix().isEmpty());
      if (putInTrash) {
        trash.add(entry.getKey());
      }
    }
    // check parentId and offset
    for (Entry<Integer, MtasToken> entry : tokenCollection.entrySet()) {
      token = entry.getValue();
      parentId = token.getParentId();
      if (parentId != null && (!tokenCollection.containsKey(parentId)
          || trash.contains(parentId))) {
        token.setParentId(null);
      }
    }
    // empty bin
    if (!trash.isEmpty()) {
      for (Integer i : trash) {
        tokenCollection.remove(i);
      }
    }
    // always check ids
    if (tokenCollection.size() > 0) {
      for (Integer i : tokenCollection.keySet()) {
        maxId = ((maxId == null) ? i : Math.max(maxId, i));
        minId = ((minId == null) ? i : Math.min(minId, i));
      }
      // check
      if ((minId > 0) || ((1 + maxId - minId) != tokenCollection.size())) {
        int newId = 0;
        // create translation
        for (Integer i : tokenCollection.keySet()) {
          translation.put(i, newId);
          newId++;
        }
        // translate objects
        for (Entry<Integer, MtasToken> entry : tokenCollection.entrySet()) {
          token = entry.getValue();
          parentId = token.getParentId();
          token.setId(translation.get(entry.getKey()));
          if (parentId != null) {
            token.setParentId(translation.get(parentId));
          }
        }
        // new tokenCollection
        Iterator<Map.Entry<Integer, MtasToken>> iter = tokenCollection
            .entrySet().iterator();
        while (iter.hasNext()) {
          Map.Entry<Integer, MtasToken> entry = iter.next();
          newTokenCollection.put(translation.get(entry.getKey()),
              entry.getValue());
          iter.remove();
        }
        tokenCollection = newTokenCollection;
      }
    }
  }

  /**
   * Check token collection index.
   *
   * @throws MtasParserException the mtas parser exception
   */
  private void checkTokenCollectionIndex() throws MtasParserException {
    if (tokenCollectionIndex.size() != tokenCollection.size()) {
      MtasToken token;
      Integer maxId = null;
      Integer minId = null;
      tokenCollectionIndex.clear();
      for (Entry<Integer, MtasToken> entry : tokenCollection.entrySet()) {
        token = entry.getValue();
        maxId = ((maxId == null) ? entry.getKey()
            : Math.max(maxId, entry.getKey()));
        minId = ((minId == null) ? entry.getKey()
            : Math.min(minId, entry.getKey()));
        if (token.getId() == null) {
          throw new MtasParserException(
              "no id for token (" + token.getValue() + ")");
        } else if ((token.getPositionStart() == null)
            || (token.getPositionEnd() == null)) {
          throw new MtasParserException("no position for token with id "
              + token.getId() + " (" + token.getValue() + ")");
        } else if (token.getValue() == null || (token.getValue().equals(""))) {
          throw new MtasParserException(
              "no value for token with id " + token.getId());
        } else if (token.getPrefix() == null
            || (token.getPrefix().equals(""))) {
          throw new MtasParserException(
              "no prefix for token with id " + token.getId());
        } else if ((token.getParentId() != null)
            && !tokenCollection.containsKey(token.getParentId())) {
          throw new MtasParserException(
              "missing parentId for token with id " + token.getId());
        } else if ((token.getOffsetStart() == null)
            || (token.getOffsetEnd() == null)) {
          throw new MtasParserException("missing offset for token with id "
              + token.getId() + " (" + token.getValue() + ")");
        }
        tokenCollectionIndex.add(entry.getKey());
      }
      if ((tokenCollection.size() > 0)
          && ((minId > 0) || ((1 + maxId - minId) != tokenCollection.size()))) {
        throw new MtasParserException("missing ids");
      }
      Collections.sort(tokenCollectionIndex, getCompByName());
    }
  }

  /**
   * Gets the comp by name.
   *
   * @return the comp by name
   */
  public Comparator<Integer> getCompByName() {
    return new Comparator<Integer>() {
      @Override
      public int compare(Integer t1, Integer t2) {
        Integer p1 = tokenCollection.get(t1).getPositionStart();
        Integer p2 = tokenCollection.get(t2).getPositionStart();
        assert p1 != null : "no position for " + tokenCollection.get(t1);
        assert p2 != null : "no position for " + tokenCollection.get(t2);
        if (p1.equals(p2)) {
          Integer o1 = tokenCollection.get(t1).getOffsetStart();
          Integer o2 = tokenCollection.get(t2).getOffsetStart();
          if (o1 != null && o2 != null) {
            if (o1.equals(o2)) {
              return tokenCollection.get(t1).getValue()
                  .compareTo(tokenCollection.get(t2).getValue());
            } else {
              return o1.compareTo(o2);
            }
          } else {
            return tokenCollection.get(t1).getValue()
                .compareTo(tokenCollection.get(t2).getValue());
          }
        }
        return p1.compareTo(p2);
      }
    };
  }

  /**
   * Clear.
   */
  private void clear() {
    tokenCollectionIndex.clear();
    tokenCollection.clear();
  }

}
