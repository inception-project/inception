package de.tudarmstadt.ukp.inception.diam.model.compactv2;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import de.tudarmstadt.ukp.inception.support.json.BeanAsArraySerializer;

@JsonSerialize(using = BeanAsArraySerializer.class)
@JsonPropertyOrder(value = { "comment", "type" })
public class CompactComment
{
    public static final String INFO = "I";
    public static final String ERROR = "E";

    private final String type;
    private final String comment;

    public CompactComment(String aComment)
    {
        type = null;
        comment = aComment;
    }

    public CompactComment(String aComment, String aType)
    {
        type = INFO.equals(aType) ? null : aType;
        comment = aComment;
    }

    public String getComment()
    {
        return comment;
    }

    /**
     * @return optional type. If empty, type {@code I} is assumed (information).
     */
    public String getType()
    {
        return type;
    }
}
