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
package de.tudarmstadt.ukp.inception.support.about;

import static java.lang.String.join;

import java.util.Objects;

public abstract class AbstractDependency
    implements Dependency
{
    @Override
    public boolean equals(Object other)
    {
        if (!(other instanceof Dependency)) {
            return false;
        }
        Dependency castOther = (Dependency) other;
        return Objects.equals(getName(), castOther.getName())
                && Objects.equals(getVersion(), castOther.getVersion())
                && Objects.equals(getSource(), castOther.getSource())
                && Objects.equals(getLicenses(), castOther.getLicenses())
                && Objects.equals(getUrl(), castOther.getUrl());
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(getName(), getVersion(), getSource(), getLicenses(), getUrl());
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(getName());
        if (getVersion() != null) {
            sb.append(" ").append(getVersion());
        }
        if (getSource() != null) {
            sb.append(" by ").append(getSource());
        }
        if (getLicenses() != null && !getLicenses().isEmpty()) {
            sb.append(" licensed as ");
            sb.append(join(", ", getLicenses()));
        }
        return sb.toString();
    }
}
