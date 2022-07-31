package de.tudarmstadt.ukp.inception.diam.model.compact;

import static de.tudarmstadt.ukp.clarin.webanno.support.JSONUtil.fromJsonString;
import static de.tudarmstadt.ukp.clarin.webanno.support.JSONUtil.toJsonString;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.exc.MismatchedInputException;

class CompactRangeTest
{
    @Test
    void thatSerializationWorks() throws Exception
    {
        assertThat(toJsonString(new CompactRange(10, 20))).isEqualTo("[10,20]");
    }

    @Test
    void thatDeserializationWorks() throws Exception
    {
        assertThat(fromJsonString(CompactRange.class, "[10,20]"))
                .isEqualTo(new CompactRange(10, 20));
    }

    @Test
    void thatDeserializationFails() throws Exception
    {
        assertThatExceptionOfType(MismatchedInputException.class)
                .isThrownBy(() -> fromJsonString(CompactRange.class, "[false,20]"))
                .withMessageContaining("Expecting begin offset as integer");
    }
}
