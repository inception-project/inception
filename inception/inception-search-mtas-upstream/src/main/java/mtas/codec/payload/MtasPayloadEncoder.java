package mtas.codec.payload;

import java.io.IOException;
import java.util.Arrays;

import org.apache.lucene.util.BytesRef;

import mtas.analysis.token.MtasPosition;
import mtas.analysis.token.MtasToken;
import mtas.analysis.token.MtasTokenString;

/**
 * The Class MtasPayloadEncoder.
 */

/**
 * The Class MtasPayloadEncoder.
 */
public class MtasPayloadEncoder
{

    /** The mtas token. */
    private MtasToken mtasToken;

    /** The byte stream. */
    private MtasBitOutputStream byteStream;

    /** The encoding flags. */
    private int encodingFlags;

    /** The Constant ENCODE_PAYLOAD. */
    public static final int ENCODE_PAYLOAD = 1;

    /** The Constant ENCODE_OFFSET. */
    public static final int ENCODE_OFFSET = 2;

    /** The Constant ENCODE_REALOFFSET. */
    public static final int ENCODE_REALOFFSET = 4;

    /** The Constant ENCODE_PARENT. */
    public static final int ENCODE_PARENT = 8;

    /** The Constant ENCODE_DEFAULT. */
    public static final int ENCODE_DEFAULT = ENCODE_PAYLOAD | ENCODE_OFFSET | ENCODE_PARENT;

    /** The Constant ENCODE_ALL. */
    public static final int ENCODE_ALL = ENCODE_PAYLOAD | ENCODE_OFFSET | ENCODE_REALOFFSET
            | ENCODE_PARENT;

    /**
     * Instantiates a new mtas payload encoder.
     *
     * @param token
     *            the token
     * @param flags
     *            the flags
     */
    public MtasPayloadEncoder(MtasToken token, int flags)
    {
        mtasToken = token;
        byteStream = new MtasBitOutputStream();
        encodingFlags = flags;
    }

    /**
     * Instantiates a new mtas payload encoder.
     *
     * @param token
     *            the token
     */
    public MtasPayloadEncoder(MtasToken token)
    {
        this(token, ENCODE_DEFAULT);
    }

    /**
     * Gets the payload.
     *
     * @return the payload
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     */
    public BytesRef getPayload() throws IOException
    {

        // initial bits - position
        if (mtasToken.checkPositionType(MtasPosition.POSITION_SINGLE)) {
            byteStream.writeBit(0);
            byteStream.writeBit(0);
        }
        else if (mtasToken.checkPositionType(MtasPosition.POSITION_RANGE)) {
            byteStream.writeBit(1);
            byteStream.writeBit(0);
        }
        else if (mtasToken.checkPositionType(MtasPosition.POSITION_SET)) {
            byteStream.writeBit(0);
            byteStream.writeBit(1);
        }
        else {
            byteStream.writeBit(1);
            byteStream.writeBit(1);
        }
        // initial bits - offset
        if ((encodingFlags & ENCODE_OFFSET) == ENCODE_OFFSET && mtasToken.checkOffset()) {
            byteStream.writeBit(1);
        }
        else {
            byteStream.writeBit(0);
        }
        // initial bits - realOffset
        if ((encodingFlags & ENCODE_REALOFFSET) == ENCODE_REALOFFSET
                && mtasToken.checkRealOffset()) {
            byteStream.writeBit(1);
        }
        else {
            byteStream.writeBit(0);
        }
        // initial bits - parentId
        if ((encodingFlags & ENCODE_PARENT) == ENCODE_PARENT && mtasToken.checkParentId()) {
            byteStream.writeBit(1);
        }
        else {
            byteStream.writeBit(0);
        }
        // initial bits - original payload
        if ((encodingFlags & ENCODE_PAYLOAD) == ENCODE_PAYLOAD && mtasToken.getPayload() != null) {
            byteStream.writeBit(1);
        }
        else {
            byteStream.writeBit(0);
        }
        if (mtasToken.getType().equals(MtasTokenString.TOKEN_TYPE)) {
            byteStream.writeBit(0);
        }
        else {
            // to add other token types later on
            byteStream.writeBit(1);
        }
        // add id (EliasGammaCoding)
        byteStream.writeEliasGammaCodingNonNegativeInteger(mtasToken.getId());
        // add position info (EliasGammaCoding)
        if (mtasToken.checkPositionType(MtasPosition.POSITION_SINGLE)) {
            // do nothing
        }
        else if (mtasToken.checkPositionType(MtasPosition.POSITION_RANGE)) {
            // write length
            byteStream.writeEliasGammaCodingPositiveInteger(
                    1 + mtasToken.getPositionEnd() - mtasToken.getPositionStart());
        }
        else if (mtasToken.checkPositionType(MtasPosition.POSITION_SET)) {
            // write number of positions
            int[] positionList = mtasToken.getPositions();
            byteStream.writeEliasGammaCodingPositiveInteger(positionList.length);
            int previousPosition = positionList[0];
            for (int i = 1; i < positionList.length; i++) {
                byteStream.writeEliasGammaCodingPositiveInteger(positionList[i] - previousPosition);
                previousPosition = positionList[i];
            }
        }
        else {
            // do nothing
        }
        // add offset info (EliasGammaCoding)
        if ((encodingFlags & ENCODE_OFFSET) == ENCODE_OFFSET && mtasToken.checkOffset()) {
            byteStream.writeEliasGammaCodingNonNegativeInteger(mtasToken.getOffsetStart());
            byteStream.writeEliasGammaCodingPositiveInteger(
                    1 + mtasToken.getOffsetEnd() - mtasToken.getOffsetStart());
        }
        // add realOffset info (EliasGammaCoding)
        if ((encodingFlags & ENCODE_REALOFFSET) == ENCODE_REALOFFSET
                && mtasToken.checkRealOffset()) {
            if ((encodingFlags & ENCODE_OFFSET) == ENCODE_OFFSET && mtasToken.checkOffset()) {
                byteStream.writeEliasGammaCodingInteger(
                        mtasToken.getRealOffsetStart() - mtasToken.getOffsetStart());
                byteStream.writeEliasGammaCodingPositiveInteger(
                        1 + mtasToken.getRealOffsetEnd() - mtasToken.getRealOffsetStart());
            }
            else {
                byteStream.writeEliasGammaCodingNonNegativeInteger(mtasToken.getRealOffsetStart());
                byteStream.writeEliasGammaCodingPositiveInteger(
                        1 + mtasToken.getRealOffsetEnd() - mtasToken.getRealOffsetStart());
            }
        }
        // add parent info (EliasGammaCoding)
        if ((encodingFlags & ENCODE_PARENT) == ENCODE_PARENT && mtasToken.checkParentId()) {
            byteStream.writeEliasGammaCodingInteger(mtasToken.getParentId() - mtasToken.getId());
        }
        // add minimal number of zero-bits to get round number of bytes
        byteStream.createByte();
        // finally add original payload bytes
        if ((encodingFlags & ENCODE_PAYLOAD) == ENCODE_PAYLOAD && mtasToken.getPayload() != null) {
            BytesRef payload = mtasToken.getPayload();
            byteStream.write(Arrays.copyOfRange(payload.bytes, payload.offset,
                    (payload.offset + payload.length)));
        }
        // construct new payload
        return new BytesRef(byteStream.toByteArray());
    }

}
