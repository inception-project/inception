package mtas.codec.util;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import mtas.analysis.token.MtasToken;
import mtas.codec.MtasCodecPostingsFormat;
import mtas.parser.function.util.MtasFunctionParserFunction;
import mtas.search.spans.util.MtasSpanQuery;
import mtas.codec.util.CodecComponent.ComponentField;
import mtas.codec.util.CodecComponent.ComponentCollection;

import org.apache.lucene.index.FieldInfo;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreMode;
import org.apache.lucene.queries.spans.SpanWeight;

/**
 * The Class CodecUtil.
 */
public class CodecUtil {

  /** The Constant STATS_TYPE_GEOMETRICMEAN. */
  public static final String STATS_TYPE_GEOMETRICMEAN = "geometricmean";

  /** The Constant STATS_TYPE_KURTOSIS. */
  public static final String STATS_TYPE_KURTOSIS = "kurtosis";

  /** The Constant STATS_TYPE_MAX. */
  public static final String STATS_TYPE_MAX = "max";

  /** The Constant STATS_TYPE_MEAN. */
  public static final String STATS_TYPE_MEAN = "mean";

  /** The Constant STATS_TYPE_MIN. */
  public static final String STATS_TYPE_MIN = "min";

  /** The Constant STATS_TYPE_N. */
  public static final String STATS_TYPE_N = "n";

  /** The Constant STATS_TYPE_MEDIAN. */
  public static final String STATS_TYPE_MEDIAN = "median";

  /** The Constant STATS_TYPE_POPULATIONVARIANCE. */
  public static final String STATS_TYPE_POPULATIONVARIANCE = "populationvariance";

  /** The Constant STATS_TYPE_QUADRATICMEAN. */
  public static final String STATS_TYPE_QUADRATICMEAN = "quadraticmean";

  /** The Constant STATS_TYPE_SKEWNESS. */
  public static final String STATS_TYPE_SKEWNESS = "skewness";

  /** The Constant STATS_TYPE_STANDARDDEVIATION. */
  public static final String STATS_TYPE_STANDARDDEVIATION = "standarddeviation";

  /** The Constant STATS_TYPE_SUM. */
  public static final String STATS_TYPE_SUM = "sum";

  /** The Constant STATS_TYPE_SUMSQ. */
  public static final String STATS_TYPE_SUMSQ = "sumsq";

  /** The Constant STATS_TYPE_SUMOFLOGS. */
  public static final String STATS_TYPE_SUMOFLOGS = "sumoflogs";

  /** The Constant STATS_TYPE_VARIANCE. */
  public static final String STATS_TYPE_VARIANCE = "variance";

  /** The Constant STATS_TYPE_ALL. */
  public static final String STATS_TYPE_ALL = "all";

  /** The Constant STATS_FUNCTION_DISTRIBUTION. */
  public static final String STATS_FUNCTION_DISTRIBUTION = "distribution";

  /** The Constant SORT_TERM. */
  public static final String SORT_TERM = "term";

  /** The Constant SORT_ASC. */
  public static final String SORT_ASC = "asc";

  /** The Constant SORT_DESC. */
  public static final String SORT_DESC = "desc";

  /** The Constant STATS_FUNCTIONS. */
  private static final List<String> STATS_FUNCTIONS = Arrays
      .asList(STATS_FUNCTION_DISTRIBUTION);

  /** The Constant STATS_TYPES. */
  private static final List<String> STATS_TYPES = Arrays.asList(
      STATS_TYPE_GEOMETRICMEAN, STATS_TYPE_KURTOSIS, STATS_TYPE_MAX,
      STATS_TYPE_MEAN, STATS_TYPE_MIN, STATS_TYPE_N, STATS_TYPE_MEDIAN,
      STATS_TYPE_POPULATIONVARIANCE, STATS_TYPE_QUADRATICMEAN,
      STATS_TYPE_SKEWNESS, STATS_TYPE_STANDARDDEVIATION, STATS_TYPE_SUM,
      STATS_TYPE_SUMSQ, STATS_TYPE_SUMOFLOGS, STATS_TYPE_VARIANCE);

  /** The Constant STATS_BASIC_TYPES. */
  private static final List<String> STATS_BASIC_TYPES = Arrays
      .asList(STATS_TYPE_N, STATS_TYPE_SUM, STATS_TYPE_MEAN);

  /** The Constant STATS_ADVANCED_TYPES. */
  private static final List<String> STATS_ADVANCED_TYPES = Arrays.asList(
      STATS_TYPE_MAX, STATS_TYPE_MIN, STATS_TYPE_SUMSQ, STATS_TYPE_SUMOFLOGS,
      STATS_TYPE_GEOMETRICMEAN, STATS_TYPE_STANDARDDEVIATION,
      STATS_TYPE_VARIANCE, STATS_TYPE_POPULATIONVARIANCE,
      STATS_TYPE_QUADRATICMEAN);

  /** The Constant STATS_FULL_TYPES. */
  private static final List<String> STATS_FULL_TYPES = Arrays
      .asList(STATS_TYPE_KURTOSIS, STATS_TYPE_MEDIAN, STATS_TYPE_SKEWNESS);

  /** The Constant STATS_BASIC. */
  public static final String STATS_BASIC = "basic";

  /** The Constant STATS_ADVANCED. */
  public static final String STATS_ADVANCED = "advanced";

  /** The Constant STATS_FULL. */
  public static final String STATS_FULL = "full";

  /** The Constant DATA_TYPE_LONG. */
  public static final String DATA_TYPE_LONG = "long";

  /** The Constant DATA_TYPE_DOUBLE. */
  public static final String DATA_TYPE_DOUBLE = "double";

  /** The fp stats items. */
  private static Pattern fpStatsItems = Pattern
      .compile("(([^\\(,]+)(\\([^\\)]*\\))?)");

  /** The fp stats function items. */
  private static Pattern fpStatsFunctionItems = Pattern
      .compile("(([^\\(,]+)(\\(([^\\)]*)\\)))");

  /**
   * Instantiates a new codec util.
   */
  private CodecUtil() {
    // don't do anything
  }

  /**
   * Checks if is single position prefix.
   *
   * @param fieldInfo
   *          the field info
   * @param prefix
   *          the prefix
   * @return true, if is single position prefix
   * @throws IOException
   *           Signals that an I/O exception has occurred.
   */
  public static boolean isSinglePositionPrefix(FieldInfo fieldInfo,
      String prefix) throws IOException {
    if (fieldInfo == null) {
      throw new IOException("no fieldInfo");
    } else {
      String info = fieldInfo.getAttribute(
          MtasCodecPostingsFormat.MTAS_FIELDINFO_ATTRIBUTE_PREFIX_SINGLE_POSITION);
      if (info == null) {
        throw new IOException("no "
            + MtasCodecPostingsFormat.MTAS_FIELDINFO_ATTRIBUTE_PREFIX_SINGLE_POSITION);
      } else {
        return Arrays.asList(info.split(Pattern.quote(MtasToken.DELIMITER)))
            .contains(prefix);
      }
    }
  }

  /**
   * Term value.
   *
   * @param term
   *          the term
   * @return the string
   */
  public static String termValue(String term) {
    int i = term.indexOf(MtasToken.DELIMITER);
    String value = null;
    if (i >= 0) {
      value = term.substring((i + MtasToken.DELIMITER.length()));
      value = (value.length() > 0) ? value : null;
    }
    return (value == null) ? null : value.replace("\u0000", "");
  }

  /**
   * Term prefix.
   *
   * @param term
   *          the term
   * @return the string
   */
  public static String termPrefix(String term) {
    int i = term.indexOf(MtasToken.DELIMITER);
    String prefix = term;
    if (i >= 0) {
      prefix = term.substring(0, i);
    }
    return prefix.replace("\u0000", "");
  }

  /**
   * Term prefix value.
   *
   * @param term
   *          the term
   * @return the string
   */
  public static String termPrefixValue(String term) {
    return (term == null) ? null : term.replace("\u0000", "");
  }

  /**
   * Collect field.
   *
   * @param field
   *          the field
   * @param searcher
   *          the searcher
   * @param rawReader
   *          the raw reader
   * @param fullDocList
   *          the full doc list
   * @param fullDocSet
   *          the full doc set
   * @param fieldStats
   *          the field stats
   * @throws IllegalAccessException
   *           the illegal access exception
   * @throws IllegalArgumentException
   *           the illegal argument exception
   * @throws InvocationTargetException
   *           the invocation target exception
   * @throws IOException
   *           Signals that an I/O exception has occurred.
   */
  public static void collectField(String field, IndexSearcher searcher,
      IndexReader rawReader, ArrayList<Integer> fullDocList,
      ArrayList<Integer> fullDocSet, ComponentField fieldStats, Status status)
      throws IllegalAccessException, IllegalArgumentException,
      InvocationTargetException, IOException {
    if (fieldStats != null) {
      IndexReader reader = searcher.getIndexReader();
      HashMap<MtasSpanQuery, SpanWeight> spansQueryWeight = new HashMap<>();
      // only if spanQueryList is not empty
      if (fieldStats.spanQueryList.size() > 0) {
        final float boost = 0;
        for (MtasSpanQuery sq : fieldStats.spanQueryList) {
          spansQueryWeight.put(sq, ((MtasSpanQuery) sq.rewrite(reader))
              .createWeight(searcher, ScoreMode.COMPLETE_NO_SCORES, boost));
        }
      }
      // collect
      CodecCollector.collectField(field, searcher, reader, rawReader,
          fullDocList, fullDocSet, fieldStats, spansQueryWeight, status);
    }
  }

  /**
   * Collect collection.
   *
   * @param reader
   *          the reader
   * @param fullDocSet
   *          the full doc set
   * @param collectionInfo
   *          the collection info
   * @throws IOException
   *           Signals that an I/O exception has occurred.
   */
  public static void collectCollection(IndexReader reader,
      List<Integer> fullDocSet, ComponentCollection collectionInfo)
      throws IOException {
    if (collectionInfo != null) {
      CodecCollector.collectCollection(reader, fullDocSet, collectionInfo);
    }
  }

  /**
   * Creates the stats items.
   *
   * @param statsType
   *          the stats type
   * @return the sorted set
   * @throws IOException
   *           Signals that an I/O exception has occurred.
   */
  static SortedSet<String> createStatsItems(String statsType)
      throws IOException {
    SortedSet<String> statsItems = new TreeSet<>();
    SortedSet<String> functionItems = new TreeSet<>();
    if (statsType != null) {
      Matcher m = fpStatsItems.matcher(statsType.trim());
      while (m.find()) {
        String tmpStatsItem = m.group(2).trim();
        if (STATS_TYPES.contains(tmpStatsItem)) {
          statsItems.add(tmpStatsItem);
        } else if (tmpStatsItem.equals(STATS_TYPE_ALL)) {
          for (String type : STATS_TYPES) {
            statsItems.add(type);
          }
        } else if (STATS_FUNCTIONS.contains(tmpStatsItem)) {
          if (m.group(3) == null) {
            throw new IOException("'" + tmpStatsItem + "' should be called as '"
                + tmpStatsItem + "()' with an optional argument");
          } else {
            functionItems.add(m.group(1).trim());
          }
        } else {
          throw new IOException("unknown statsType '" + tmpStatsItem + "'");
        }
      }
    }
    if (statsItems.size() == 0 && functionItems.size() == 0) {
      statsItems.add(STATS_TYPE_SUM);
      statsItems.add(STATS_TYPE_N);
      statsItems.add(STATS_TYPE_MEAN);
    }
    if (functionItems.size() > 0) {
      statsItems.addAll(functionItems);
    }
    return statsItems;
  }

  /**
   * Creates the stats type.
   *
   * @param statsItems
   *          the stats items
   * @param sortType
   *          the sort type
   * @param functionParser
   *          the function parser
   * @return the string
   */
  static String createStatsType(Set<String> statsItems, String sortType,
      MtasFunctionParserFunction functionParser) {
    String statsType = STATS_BASIC;
    for (String statsItem : statsItems) {
      if (STATS_FULL_TYPES.contains(statsItem)) {
        statsType = STATS_FULL;
        break;
      } else if (STATS_ADVANCED_TYPES.contains(statsItem)) {
        statsType = STATS_ADVANCED;
      } else if (statsType != STATS_ADVANCED
          && STATS_BASIC_TYPES.contains(statsItem)) {
        statsType = STATS_BASIC;
      } else {
        Matcher m = fpStatsFunctionItems.matcher(statsItem.trim());
        if (m.find()) {
          if (STATS_FUNCTIONS.contains(m.group(2).trim())) {
            statsType = STATS_FULL;
            break;
          }
        }
      }
    }
    if (sortType != null && STATS_TYPES.contains(sortType)) {
      if (STATS_FULL_TYPES.contains(sortType)) {
        statsType = STATS_FULL;
      } else if (STATS_ADVANCED_TYPES.contains(sortType)) {
        statsType = (statsType == null || statsType != STATS_FULL)
            ? STATS_ADVANCED : statsType;
      }
    }
    return statsType;
  }

  /**
   * Checks if is stats type.
   *
   * @param type
   *          the type
   * @return true, if is stats type
   */
  public static boolean isStatsType(String type) {
    return STATS_TYPES.contains(type);
  }

}
