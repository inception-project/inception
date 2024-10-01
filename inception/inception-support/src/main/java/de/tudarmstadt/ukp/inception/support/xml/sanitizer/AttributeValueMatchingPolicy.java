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
package de.tudarmstadt.ukp.inception.support.xml.sanitizer;

import java.util.Optional;
import java.util.regex.Pattern;

import javax.xml.namespace.QName;

public class AttributeValueMatchingPolicy
    extends QNameAttributePolicy
    implements ChainableAttributePolicy
{
    private final Pattern pattern;

    private AttributePolicy delegate;

    public AttributeValueMatchingPolicy(QName aQName, AttributeAction aAction, Pattern aPattern)
    {
        super(aQName, aAction);
        pattern = aPattern;
    }

    @Override
    public void setDelegate(AttributePolicy aDelegate)
    {
        delegate = aDelegate;
    }

    @Override
    public AttributePolicy getDelegate()
    {
        return delegate;
    }

    @Override
    public Optional<AttributeAction> apply(String aValue)
    {
        if (pattern.matcher(aValue).matches()) {
            return super.apply(aValue);
        }

        return chain(aValue);
    }

    @Override
    public String toString()
    {
        var sb = new StringBuilder();
        sb.append("[");
        sb.append(pattern);
        sb.append("] -> ");
        sb.append(getAction());
        if (getDelegate() != null) {
            sb.append(" :: ");
            sb.append(getDelegate().toString());
        }
        return sb.toString();
    }
}
