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
package de.tudarmstadt.ukp.inception.io.bioc.model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlTransient;

@XmlTransient
public abstract class BioCObject
{
    private List<BioCInfon> infons;

    public List<BioCInfon> getInfons()
    {
        return infons;
    }

    @XmlElement(name = "infon")
    public void setInfons(List<BioCInfon> aInfons)
    {
        infons = aInfons;
    }

    public void addInfon(String aKey, String aValue)
    {
        if (infons == null) {
            infons = new ArrayList<>();
        }
        infons.add(new BioCInfon(aKey, aValue));
    }

    public Optional<String> infon(String aKey)
    {
        if (infons == null) {
            return Optional.empty();
        }

        return infons.stream() //
                .filter(i -> i.getKey().equals(aKey)) //
                .findFirst() //
                .map(BioCInfon::getValue);
    }

    public Map<String, List<String>> infonMap()
    {
        var map = new LinkedHashMap<String, List<String>>();

        for (var infon : infons) {
            var list = map.computeIfAbsent(infon.getKey(), $ -> new ArrayList<>());
            list.add(infon.getValue());
        }

        return map;
    }
}
