package mtas.codec.util.collector;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import mtas.codec.util.CodecUtil;

/**
 * The Class MtasDataItemDoubleFull.
 */
public class MtasDataItemDoubleFull extends MtasDataItemFull<Double, Double> {

  /** The Constant serialVersionUID. */
  private static final long serialVersionUID = 1L;

  /** The fp argument. */
  private static Pattern fpArgument = Pattern.compile("([^=,]+)=([^,]*)");

  /**
   * Instantiates a new mtas data item double full.
   *
   * @param value the value
   * @param sub the sub
   * @param statsItems the stats items
   * @param sortType the sort type
   * @param sortDirection the sort direction
   * @param errorNumber the error number
   * @param errorList the error list
   * @param sourceNumber the source number
   */
  public MtasDataItemDoubleFull(double[] value, MtasDataCollector<?, ?> sub,
      Set<String> statsItems, String sortType, String sortDirection,
      int errorNumber, Map<String, Integer> errorList, int sourceNumber) {
    super(Arrays.stream(value).boxed().toArray(Double[]::new), sub, statsItems, sortType, sortDirection,
        errorNumber, errorList, new MtasDataDoubleOperations(), sourceNumber);
  }

  /**
   * Gets the number of decimals.
   *
   * @param ds the ds
   * @return the number of decimals
   */
  private int getNumberOfDecimals(String ds) {
    if (!ds.contains(".")) {
      return 0;
    } else {
      return (ds.length() - ds.indexOf(".") - 1);
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * mtas.codec.util.DataCollector.MtasDataItemFull#getDistribution(java.lang.
   * String)
   */
  @Override
  protected HashMap<String, Object> getDistribution(String argument) {
    HashMap<String, Object> result = new LinkedHashMap<>();
    Double start = null;
    Double end = null;
    Double step = null;
    Integer d = null;
    Integer number = null;
    if (argument != null) {
      Matcher m = fpArgument.matcher(argument);
      // get settings
      while (m.find()) {
        if (m.group(1).trim().equals("start")) {
          start = Double.parseDouble(m.group(2));
          d = (d == null) ? getNumberOfDecimals(m.group(2))
              : Math.max(d, getNumberOfDecimals(m.group(2)));
        } else if (m.group(1).trim().equals("end")) {
          end = Double.parseDouble(m.group(2));
          d = (d == null) ? getNumberOfDecimals(m.group(2))
              : Math.max(d, getNumberOfDecimals(m.group(2)));
        } else if (m.group(1).trim().equals("step")) {
          step = Double.parseDouble(m.group(2));
          d = (d == null) ? getNumberOfDecimals(m.group(2))
              : Math.max(d, getNumberOfDecimals(m.group(2)));
        } else if (m.group(1).trim().equals("number")) {
          number = Integer.parseInt(m.group(2));
        }
      }
    }
    // always exactly one of (positive) number and (positive) step, other null
    if ((number == null || number < 1) && (step == null || step <= 0)) {
      number = 10;
      step = null;
    } else if (step != null && step <= 0) {
      step = null;
    } else if (number != null && number < 1) {
      number = null;
    } else if (step != null) {
      number = null;
    }
    // sanity checks start/end
    createStats();
    double tmpStart = stats.getMin();
    double tmpEnd = stats.getMax();
    if (start != null && end != null && start > end) {
      return null;
    } else if (start != null && start > tmpEnd) {
      return null;
    } else if (end != null && end < tmpStart) {
      return null;
    }
    // check start and end
    if (start == null && end == null) {
      if (step == null) {
        step = (tmpEnd - tmpStart) / number;
      }
      number = Double.valueOf(Math.ceil((tmpEnd - tmpStart) / step)).intValue();
      start = tmpStart;
      end = start + (number * step);
    } else if (start == null) {
      if (step == null) {
        step = (end - tmpStart) / number;
      }
      number = Double.valueOf(Math.ceil((end - tmpStart) / step)).intValue();
      start = end - (number * step);
    } else if (end == null) {
      if (step == null) {
        step = (tmpEnd - start) / number;
      }
      number = Double.valueOf(Math.ceil((tmpEnd - start) / step)).intValue();
      end = start + (number * step);
    } else {
      if (step == null) {
        step = (end - start) / number;
      }
      number = Double.valueOf(Math.ceil((end - start) / step)).intValue();
    }
    // round step to agreeable format and recompute number
    int tmpD = Double.valueOf(Math.max(0, 1 + Math.ceil(-1 * Math.log10(step))))
        .intValue();
    d = (d == null) ? tmpD : Math.max(d, tmpD);
    double tmp = Math.pow(10.0, d);
    step = Math.round(step * tmp) / tmp;
    number = Double.valueOf(Math.ceil((end - start) / step)).intValue();

    // compute distribution
    long[] list = new long[number];
    for (Double v : fullValues) {
      if (v >= start && v <= end) {
        int i = Math.min(
            Double.valueOf(Math.floor((v - start) / step)).intValue(),
            (number - 1));
        list[i]++;
      }
    }
    Double l;
    Double r;
    String ls;
    String lsFormat;
    String rs;
    String rsFormat;
    for (int i = 0; i < number; i++) {
      l = start + i * step;
      r = Math.min(end, l + step);
      lsFormat = "%." + d + "f";
      ls = String.format(lsFormat, l);
      rsFormat = "%." + d + "f";
      rs = String.format(rsFormat, r);
      String key = "[" + ls + "," + rs
          + ((i == (number - 1) && r >= tmpEnd && l <= tmpEnd) ? "]" : ")");
      result.put(key, list[i]);
    }
    return result;
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.lang.Comparable#compareTo(java.lang.Object)
   */
  @SuppressWarnings({ "rawtypes", "unchecked" })
  public int compareTo(MtasDataItem<Double, Double> o) {
    int compare = 0;
    if (o instanceof MtasDataItemDoubleFull) {
      MtasDataItemDoubleFull to = (MtasDataItemDoubleFull) o;
      MtasDataItemNumberComparator c1 = getComparableValue();
      MtasDataItemNumberComparator c2 = to.getComparableValue();
      compare = (c1 != null && c2 != null) ? c1.compareTo(c2.getValue()) : 0;
    }
    return sortDirection.equals(CodecUtil.SORT_DESC) ? -1 * compare : compare;
  }

  /*
   * (non-Javadoc)
   * 
   * @see mtas.codec.util.collector.MtasDataItem#getCompareValue1()
   */
  @Override
  public MtasDataItemNumberComparator<Double> getCompareValue1() {
    createStats();
    switch (sortType) {
    case CodecUtil.STATS_TYPE_SUM:
      return new MtasDataItemNumberComparator<>(stats.getSum(), sortDirection);
    case CodecUtil.STATS_TYPE_MAX:
      return new MtasDataItemNumberComparator<>(stats.getMax(), sortDirection);
    case CodecUtil.STATS_TYPE_MIN:
      return new MtasDataItemNumberComparator<>(stats.getMin(), sortDirection);
    case CodecUtil.STATS_TYPE_SUMSQ:
      return new MtasDataItemNumberComparator<>(stats.getSumsq(),
          sortDirection);
    default:
      return null;
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see mtas.codec.util.collector.MtasDataItem#getCompareValue2()
   */
  @Override
  public MtasDataItemNumberComparator<Double> getCompareValue2() {
    createStats();
    switch (sortType) {
    case CodecUtil.STATS_TYPE_SUMOFLOGS:
      return new MtasDataItemNumberComparator<>(
          stats.getN() * Math.log(stats.getGeometricMean()), sortDirection);
    case CodecUtil.STATS_TYPE_MEAN:
      return new MtasDataItemNumberComparator<>(stats.getMean(), sortDirection);
    case CodecUtil.STATS_TYPE_GEOMETRICMEAN:
      return new MtasDataItemNumberComparator<>(stats.getGeometricMean(),
          sortDirection);
    case CodecUtil.STATS_TYPE_STANDARDDEVIATION:
      return new MtasDataItemNumberComparator<>(stats.getStandardDeviation(),
          sortDirection);
    case CodecUtil.STATS_TYPE_VARIANCE:
      return new MtasDataItemNumberComparator<>(stats.getVariance(),
          sortDirection);
    case CodecUtil.STATS_TYPE_POPULATIONVARIANCE:
      return new MtasDataItemNumberComparator<>(stats.getPopulationVariance(),
          sortDirection);
    case CodecUtil.STATS_TYPE_QUADRATICMEAN:
      return new MtasDataItemNumberComparator<>(
          Math.sqrt(stats.getSumsq() / stats.getN()), sortDirection);
    case CodecUtil.STATS_TYPE_KURTOSIS:
      return new MtasDataItemNumberComparator<>(stats.getKurtosis(),
          sortDirection);
    case CodecUtil.STATS_TYPE_MEDIAN:
      return new MtasDataItemNumberComparator<>(stats.getPercentile(50),
          sortDirection);
    case CodecUtil.STATS_TYPE_SKEWNESS:
      return new MtasDataItemNumberComparator<>(stats.getSkewness(),
          sortDirection);
    default:
      return null;
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.lang.Object#toString()
   */
  public String toString() {
    return this.getClass().getSimpleName() + "[" + fullValues.length + "]";
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.lang.Object#equals(java.lang.Object)
   */
  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    MtasDataItemDoubleFull that = (MtasDataItemDoubleFull) obj;
    MtasDataItemNumberComparator<?> c1 = getComparableValue();
    MtasDataItemNumberComparator<?> c2 = that.getComparableValue();
    return (c1 != null && c2 != null && c1.equals(c2));
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    return Objects.hash(this.getClass().getSimpleName(), getComparableValue());   
  }

}
