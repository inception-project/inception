package mtas.codec.util;

import java.io.IOException;
import java.util.SortedSet;

import mtas.codec.util.collector.MtasDataCollector;
import mtas.codec.util.collector.MtasDataDoubleAdvanced;
import mtas.codec.util.collector.MtasDataDoubleBasic;
import mtas.codec.util.collector.MtasDataDoubleFull;
import mtas.codec.util.collector.MtasDataLongAdvanced;
import mtas.codec.util.collector.MtasDataLongBasic;
import mtas.codec.util.collector.MtasDataLongFull;

/**
 * The Class DataCollector.
 */
public class DataCollector
{

    /** The Constant COLLECTOR_TYPE_LIST. */
    public static final String COLLECTOR_TYPE_LIST = "list";

    /** The Constant COLLECTOR_TYPE_DATA. */
    public static final String COLLECTOR_TYPE_DATA = "data";

    /**
     * Instantiates a new data collector.
     */
    private DataCollector()
    {
        // don't do anything
    }

    /**
     * Gets the collector.
     *
     * @param collectorType
     *            the collector type
     * @param dataType
     *            the data type
     * @param statsType
     *            the stats type
     * @param statsItems
     *            the stats items
     * @param sortType
     *            the sort type
     * @param sortDirection
     *            the sort direction
     * @param start
     *            the start
     * @param number
     *            the number
     * @param segmentRegistration
     *            the segment registration
     * @param boundary
     *            the boundary
     * @return the collector
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     */
    public static MtasDataCollector<?, ?> getCollector(String collectorType, String dataType,
            String statsType, SortedSet<String> statsItems, String sortType, String sortDirection,
            Integer start, Integer number, String segmentRegistration, String boundary)
        throws IOException
    {
        return getCollector(collectorType, dataType, statsType, statsItems, sortType, sortDirection,
                start, number, null, null, null, null, null, null, null, null, segmentRegistration,
                boundary);
    }

    /**
     * Gets the collector.
     *
     * @param collectorType
     *            the collector type
     * @param dataType
     *            the data type
     * @param statsType
     *            the stats type
     * @param statsItems
     *            the stats items
     * @param sortType
     *            the sort type
     * @param sortDirection
     *            the sort direction
     * @param start
     *            the start
     * @param number
     *            the number
     * @param subCollectorTypes
     *            the sub collector types
     * @param subDataTypes
     *            the sub data types
     * @param subStatsTypes
     *            the sub stats types
     * @param subStatsItems
     *            the sub stats items
     * @param subSortTypes
     *            the sub sort types
     * @param subSortDirections
     *            the sub sort directions
     * @param subStart
     *            the sub start
     * @param subNumber
     *            the sub number
     * @param segmentRegistration
     *            the segment registration
     * @param boundary
     *            the boundary
     * @return the collector
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     */
    public static MtasDataCollector<?, ?> getCollector(String collectorType, String dataType,
            String statsType, SortedSet<String> statsItems, String sortType, String sortDirection,
            Integer start, Integer number, String[] subCollectorTypes, String[] subDataTypes,
            String[] subStatsTypes, SortedSet<String>[] subStatsItems, String[] subSortTypes,
            String[] subSortDirections, Integer[] subStart, Integer[] subNumber,
            String segmentRegistration, String boundary)
        throws IOException
    {
        if (dataType != null && dataType.equals(CodecUtil.DATA_TYPE_LONG)) {
            if (statsType.equals(CodecUtil.STATS_BASIC)) {
                return new MtasDataLongBasic(collectorType, statsItems, sortType, sortDirection,
                        start, number, subCollectorTypes, subDataTypes, subStatsTypes,
                        subStatsItems, subSortTypes, subSortDirections, subStart, subNumber,
                        segmentRegistration, boundary);
            }
            else if (statsType.equals(CodecUtil.STATS_ADVANCED)) {
                return new MtasDataLongAdvanced(collectorType, statsItems, sortType, sortDirection,
                        start, number, subCollectorTypes, subDataTypes, subStatsTypes,
                        subStatsItems, subSortTypes, subSortDirections, subStart, subNumber,
                        segmentRegistration, boundary);
            }
            else if (statsType.equals(CodecUtil.STATS_FULL)) {
                return new MtasDataLongFull(collectorType, statsItems, sortType, sortDirection,
                        start, number, subCollectorTypes, subDataTypes, subStatsTypes,
                        subStatsItems, subSortTypes, subSortDirections, subStart, subNumber,
                        segmentRegistration, boundary);
            }
            else {
                throw new IOException("unknown statsType " + statsType);
            }
        }
        else if (dataType != null && dataType.equals(CodecUtil.DATA_TYPE_DOUBLE)) {
            if (statsType.equals(CodecUtil.STATS_BASIC)) {
                return new MtasDataDoubleBasic(collectorType, statsItems, sortType, sortDirection,
                        start, number, subCollectorTypes, subDataTypes, subStatsTypes,
                        subStatsItems, subSortTypes, subSortDirections, subStart, subNumber,
                        segmentRegistration, boundary);
            }
            else if (statsType.equals(CodecUtil.STATS_ADVANCED)) {
                return new MtasDataDoubleAdvanced(collectorType, statsItems, sortType,
                        sortDirection, start, number, subCollectorTypes, subDataTypes,
                        subStatsTypes, subStatsItems, subSortTypes, subSortDirections, subStart,
                        subNumber, segmentRegistration, boundary);
            }
            else if (statsType.equals(CodecUtil.STATS_FULL)) {
                return new MtasDataDoubleFull(collectorType, statsItems, sortType, sortDirection,
                        start, number, subCollectorTypes, subDataTypes, subStatsTypes,
                        subStatsItems, subSortTypes, subSortDirections, subStart, subNumber,
                        segmentRegistration, boundary);
            }
            else {
                throw new IOException("unknown statsType " + statsType);
            }
        }
        else {
            throw new IOException("unknown dataType " + dataType);
        }
    }

}
