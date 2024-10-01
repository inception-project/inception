package mtas.codec.payload;

import java.io.IOException;
import java.util.SortedSet;
import java.util.TreeSet;

import mtas.analysis.token.MtasOffset;
import mtas.analysis.token.MtasPosition;

/**
 * The Class MtasPayloadDecoder.
 */
public class MtasPayloadDecoder
{

    /** The byte stream. */
    private MtasBitInputStream byteStream;

    /** The mtas position. */
    private MtasPosition mtasPosition;

    /** The mtas start position. */
    private int mtasStartPosition;

    /** The mtas positions. */
    private SortedSet<Integer> mtasPositions;

    /** The mtas id. */
    private Integer mtasId = null;

    /** The mtas payload value. */
    private byte[] mtasPayloadValue = null;

    /** The mtas parent id. */
    private Integer mtasParentId = null;

    /** The mtas payload. */
    private Boolean mtasPayload = null;

    /** The mtas parent. */
    private Boolean mtasParent = null;

    /** The mtas position type. */
    private String mtasPositionType = null;

    /** The mtas offset. */
    private MtasOffset mtasOffset;

    /** The mtas real offset. */
    private MtasOffset mtasRealOffset;

    /**
     * Inits the.
     *
     * @param startPosition
     *            the start position
     * @param payload
     *            the payload
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     */
    public void init(int startPosition, byte[] payload) throws IOException
    {
        byteStream = new MtasBitInputStream(payload);
        mtasStartPosition = startPosition;
        // analyse initial bits - position
        Boolean getOffset;
        Boolean getRealOffset;
        if (byteStream.readBit() == 1) {
            if (byteStream.readBit() == 1) {
                mtasPositionType = null;
            }
            else {
                mtasPositionType = MtasPosition.POSITION_RANGE;
            }
        }
        else {
            if (byteStream.readBit() == 1) {
                mtasPositionType = MtasPosition.POSITION_SET;
            }
            else {
                mtasPositionType = MtasPosition.POSITION_SINGLE;
            }
        }
        // analyze initial bits - offset
        if (byteStream.readBit() == 1) {
            getOffset = true;
        }
        else {
            getOffset = false;
        }
        // analyze initial bits - realOffset
        if (byteStream.readBit() == 1) {
            getRealOffset = true;
        }
        else {
            getRealOffset = false;
        }
        // analyze initial bits - parent
        if (byteStream.readBit() == 1) {
            mtasParent = true;
        }
        else {
            mtasParent = false;
        }
        // analyse initial bits - payload
        if (byteStream.readBit() == 1) {
            mtasPayload = true;
        }
        else {
            mtasPayload = false;
        }
        if (byteStream.readBit() == 0) {
            // string
        }
        else {
            // other
        }
        // get id
        mtasId = byteStream.readEliasGammaCodingNonNegativeInteger();
        // get position info
        if (mtasPositionType != null && mtasPositionType.equals(MtasPosition.POSITION_SINGLE)) {
            mtasPosition = new MtasPosition(mtasStartPosition);
        }
        else if (mtasPositionType != null && mtasPositionType.equals(MtasPosition.POSITION_RANGE)) {
            mtasPosition = new MtasPosition(mtasStartPosition,
                    (mtasStartPosition + byteStream.readEliasGammaCodingPositiveInteger() - 1));
        }
        else if (mtasPositionType != null && mtasPositionType.equals(MtasPosition.POSITION_SET)) {
            mtasPositions = new TreeSet<>();
            mtasPositions.add(mtasStartPosition);
            int numberOfPoints = byteStream.readEliasGammaCodingPositiveInteger();
            int[] positionList = new int[numberOfPoints];
            positionList[0] = mtasStartPosition;
            int previousPosition = 0;
            int currentPosition = mtasStartPosition;
            for (int i = 1; i < numberOfPoints; i++) {
                previousPosition = currentPosition;
                currentPosition = previousPosition
                        + byteStream.readEliasGammaCodingPositiveInteger();
                positionList[i] = currentPosition;
            }
            mtasPosition = new MtasPosition(positionList);
        }
        else {
            mtasPosition = null;
        }
        // get offset and realOffset info
        if (getOffset) {
            int offsetStart = byteStream.readEliasGammaCodingNonNegativeInteger();
            int offsetEnd = offsetStart + byteStream.readEliasGammaCodingPositiveInteger() - 1;
            mtasOffset = new MtasOffset(offsetStart, offsetEnd);
            if (getRealOffset) {
                int realOffsetStart = byteStream.readEliasGammaCodingInteger() + offsetStart;
                int realOffsetEnd = realOffsetStart
                        + byteStream.readEliasGammaCodingPositiveInteger() - 1;
                mtasRealOffset = new MtasOffset(realOffsetStart, realOffsetEnd);
            }
        }
        else if (getRealOffset) {
            int realOffsetStart = byteStream.readEliasGammaCodingNonNegativeInteger();
            int realOffsetEnd = realOffsetStart + byteStream.readEliasGammaCodingPositiveInteger()
                    - 1;
            mtasRealOffset = new MtasOffset(realOffsetStart, realOffsetEnd);
        }
        if (mtasParent) {
            mtasParentId = byteStream.readEliasGammaCodingInteger() + mtasId;
        }
        if (mtasPayload) {
            mtasPayloadValue = byteStream.readRemainingBytes();
        }
    }

    /**
     * Gets the mtas id.
     *
     * @return the mtas id
     */
    public Integer getMtasId()
    {
        return mtasId;
    }

    /**
     * Gets the mtas parent id.
     *
     * @return the mtas parent id
     */
    public Integer getMtasParentId()
    {
        return mtasParentId;
    }

    /**
     * Gets the mtas payload.
     *
     * @return the mtas payload
     */
    public byte[] getMtasPayload()
    {
        return mtasPayload ? mtasPayloadValue : null;
    }

    /**
     * Gets the mtas position.
     *
     * @return the mtas position
     */
    public MtasPosition getMtasPosition()
    {
        return mtasPosition;
    }

    /**
     * Gets the mtas offset.
     *
     * @return the mtas offset
     */
    public MtasOffset getMtasOffset()
    {
        return mtasOffset;
    }

    /**
     * Gets the mtas real offset.
     *
     * @return the mtas real offset
     */
    public MtasOffset getMtasRealOffset()
    {
        return mtasRealOffset;
    }

}
