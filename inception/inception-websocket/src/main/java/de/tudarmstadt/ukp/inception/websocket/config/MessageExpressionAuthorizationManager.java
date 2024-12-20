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
package de.tudarmstadt.ukp.inception.websocket.config;

import static java.util.Objects.requireNonNull;

import java.util.function.Supplier;

import org.springframework.expression.Expression;
import org.springframework.security.access.expression.ExpressionUtils;
import org.springframework.security.access.expression.SecurityExpressionHandler;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.messaging.access.intercept.MessageAuthorizationContext;

public class MessageExpressionAuthorizationManager
    implements AuthorizationManager<MessageAuthorizationContext<?>>
{
    private final SecurityExpressionHandler<MessageAuthorizationContext<?>> expressionHandler;

    private final Expression expression;

    public static MessageExpressionAuthorizationManager expression(
            SecurityExpressionHandler<MessageAuthorizationContext<?>> expressionHandler,
            String aExpression)
    {
        return new MessageExpressionAuthorizationManager(expressionHandler, aExpression);
    }

    private MessageExpressionAuthorizationManager(
            SecurityExpressionHandler<MessageAuthorizationContext<?>> aExpressionHandler,
            String aExpression)
    {
        requireNonNull(aExpressionHandler, "expressionHandler cannot be null");
        requireNonNull(aExpression, "expression cannot be null");
        expressionHandler = aExpressionHandler;
        expression = expressionHandler.getExpressionParser().parseExpression(aExpression);
    }

    @Override
    public void verify(Supplier<Authentication> aAuthentication,
            MessageAuthorizationContext<?> aObject)
    {
        var context = expressionHandler.createEvaluationContext(aAuthentication, aObject);
        var granted = ExpressionUtils.evaluateAsBoolean(expression, context);
        if (!granted) {
            throw new AuthorizationDeniedException("Access Denied");
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public AuthorizationDecision check(Supplier<Authentication> aAuthentication,
            MessageAuthorizationContext<?> aObject)
    {
        try {
            verify(aAuthentication, aObject);
            return new AuthorizationDecision(true);
        }
        catch (AuthorizationDeniedException e) {
            return new AuthorizationDecision(false);
        }
    }
}
