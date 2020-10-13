package de.tudarmstadt.ukp.clarin.webanno.brat.message;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

public class NumericBooleanSerializer
    extends JsonSerializer<Boolean>
{
    @Override
    public void serialize(Boolean aValue, JsonGenerator aGenerator,
            SerializerProvider aSerializerProvider)
        throws IOException
    {
        aGenerator.writeNumber(aValue ? 1 : 0);
    }
}
