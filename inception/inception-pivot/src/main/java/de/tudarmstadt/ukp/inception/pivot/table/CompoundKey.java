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
package de.tudarmstadt.ukp.inception.pivot.table;

import static java.util.Collections.unmodifiableMap;
import static org.apache.commons.text.StringEscapeUtils.escapeJson;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public final class CompoundKey
    implements Serializable, Comparable<CompoundKey>
{
    private static final long serialVersionUID = -9126990723957178977L;

    public static final CompoundKey ROW_KEY = new CompoundKey(new CompoundKeySchema(), true);

    private final CompoundKeySchema schema;
    private final Serializable[] values;
    private final int hashCode;
    private final boolean weak;

    public CompoundKey(CompoundKeySchema aSchema, boolean aWeak, Serializable... aValues)
    {
        if (aValues.length != aSchema.size()) {
            throw new IllegalArgumentException("Value count does not match schema");
        }

        weak = aWeak;
        schema = aSchema;
        values = aValues.clone();
        hashCode = Arrays.deepHashCode(values);
    }

    public boolean isMultiValue()
    {
        return schema.size() > 1;
    }

    public Object get(int index)
    {
        return values[index];
    }

    public Serializable get(String aName)
    {
        for (int i = 0; i < schema.size(); i++) {
            if (schema.getName(i).equals(aName)) {
                return values[i];
            }
        }
        return null; // or throw exception
    }

    public Map<String, Serializable> asMap()
    {
        var map = new LinkedHashMap<String, Serializable>(schema.size());
        for (var i = 0; i < schema.size(); i++) {
            map.put(schema.getName(i), values[i]);
        }
        return unmodifiableMap(map);
    }

    public List<Entry<String, Serializable>> entries()
    {
        return new ArrayList<>(asMap().entrySet());
    }

    @Override
    public int compareTo(CompoundKey aO)
    {
        for (int i = 0; i < values.length; i++) {
            Object v1 = this.values[i];
            Object v2 = aO.values[i];
            if (v1 == v2) {
                continue;
            }
            if (v1 == null) {
                return -1;
            }
            if (v2 == null) {
                return 1;
            }
            @SuppressWarnings("unchecked")
            int cmp = ((Comparable<Object>) v1).compareTo(v2);
            if (cmp != 0) {
                return cmp;
            }
        }
        return 0;
    }

    @Override
    public boolean equals(Object aOther)
    {
        if (this == aOther) {
            return true;
        }

        if (!(aOther instanceof CompoundKey)) {
            return false;
        }

        var that = (CompoundKey) aOther;

        // schema is compared by reference: same schema instance → same layout
        return schema == that.schema && Arrays.deepEquals(values, that.values);
    }

    @Override
    public int hashCode()
    {
        return hashCode;
    }

    public String toJson()
    {
        var sb = new StringBuilder();
        sb.append("{");
        for (var i = 0; i < schema.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append('"');
            sb.append(escapeJson(schema.getName(i)));
            sb.append('"');
            sb.append(':');
            if (values[i] == null) {
                sb.append("null");
            }
            else if (values[i] instanceof Number number) {
                sb.append(number);
            }
            else if (values[i] instanceof Boolean bool) {
                sb.append(bool);
            }
            else {
                sb.append('"');
                sb.append(escapeJson(String.valueOf(values[i])));
                sb.append('"');
            }
        }
        sb.append("}");
        return sb.toString();
    }

    @Override
    public String toString()
    {
        return toJson();
    }

    public boolean isWeak()
    {
        return weak || schema.isWeak();
    }

    public CompoundKeySchema getSchema()
    {
        return schema;
    }
}
