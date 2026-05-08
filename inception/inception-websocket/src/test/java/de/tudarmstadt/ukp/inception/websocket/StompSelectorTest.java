/*
 * Licensed to the Technische Universität Darmstadt under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The Technische Universität Darmstadt 
 * licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.
 *  
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tudarmstadt.ukp.inception.websocket;

import static org.apache.commons.lang3.reflect.MethodUtils.invokeMethod;
import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.broker.DefaultSubscriptionRegistry;
import org.springframework.messaging.support.GenericMessage;

public class StompSelectorTest
{
    private DefaultSubscriptionRegistry sut;

    @BeforeEach
    void setup()
    {
        sut = new DefaultSubscriptionRegistry();
    }

    @Test
    void test() throws Exception
    {
        var msg = new GenericMessage<>("", Map.of( //
                "format", "compact", //
                "extensions", "cur,other"));

        assertThat(matches("headers['format'] == 'compact'", msg)).isTrue();
        assertThat(matches("headers['format'] == 'legacy'", msg)).isFalse();
        assertThat(matches("headers['extensions'] matches '.*\\bcur\\b.*'", msg)).isTrue();
        assertThat(matches("headers['extensions'] matches '.*\\brec\\b.*'", msg)).isFalse();
    }

    private boolean matches(String aExpression, Message<String> aMessage)
        throws NoSuchMethodException, IllegalAccessException, InvocationTargetException
    {
        var parser = new SpelExpressionParser();
        var ex = parser.parseExpression(aExpression);
        var result = (boolean) invokeMethod( //
                sut, // The object instance
                true, // forceAccess: MUST be true for private methods
                "evaluateExpression", // The exact method name as a String
                ex, // Parameter 1
                aMessage // Parameter 2
        );
        return result;
    }
}
