package de.tudarmstadt.ukp.clarin.webanno.brat.message;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

public class NumericBooleanSerializer
    extends JsonSerializer<Boolean>
{
    @Override
    public void serialize(Boolean b, JsonGenerator jsonGenerator,
            SerializerProvider serializerProvider)
        throws IOException
    {
        jsonGenerator.writeNumber(b ? 1 : 0);
    }
}
