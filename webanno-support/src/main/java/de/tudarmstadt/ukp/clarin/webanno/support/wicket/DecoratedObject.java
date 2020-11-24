/*
 * Copyright 2017
 * Ubiquitous Knowledge Processing (UKP) Lab and FG Language Technology
 * Technische Universität Darmstadt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tudarmstadt.ukp.clarin.webanno.support.wicket;

import java.io.Serializable;

public class DecoratedObject<T>
    implements Serializable
{
    private static final long serialVersionUID = -878886282182894983L;

    private T delegate;

    private String label;

    private String color;

    public DecoratedObject(T aDelegate)
    {
        delegate = aDelegate;
    }

    public String getLabel()
    {
        return label;
    }

    public void setLabel(String aLabel)
    {
        label = aLabel;
    }

    public void setLabel(String aFormat, Object... aValues)
    {
        label = String.format(aFormat, aValues);
    }

    public String getColor()
    {
        return color;
    }

    public void setColor(String aColor)
    {
        color = aColor;
    }

    public T get()
    {
        return delegate;
    }

    public static <T> DecoratedObject<T> of(T aDelegate)
    {
        return new DecoratedObject<>(aDelegate);
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((delegate == null) ? 0 : delegate.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        DecoratedObject other = (DecoratedObject) obj;
        if (delegate == null) {
            if (other.delegate != null) {
                return false;
            }
        }
        else if (!delegate.equals(other.delegate)) {
            return false;
        }
        return true;
    }
}
