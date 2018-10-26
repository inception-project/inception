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
package de.tudarmstadt.ukp.inception.externalsearch.elastic.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ElasticSearchHit
{
    private String _index;
    private String _type;
    private String _id;
    private Double _score;
    private ElasticSearchSource _source;
    private ElasticSearchHighlight highlight;

    public ElasticSearchHighlight getHighlight()
    {
        return highlight;
    }

    public void setHighlight(ElasticSearchHighlight highlight)
    {
        this.highlight = highlight;
    }

    public String get_index()
    {
        return _index;
    }

    public void set_index(String _index)
    {
        this._index = _index;
    }

    public String get_type()
    {
        return _type;
    }

    public void set_type(String _type)
    {
        this._type = _type;
    }

    public String get_id()
    {
        return _id;
    }

    public void set_id(String _id)
    {
        this._id = _id;
    }

    public Double get_score()
    {
        return _score;
    }

    public void set_score(Double _score)
    {
        this._score = _score;
    }

    public ElasticSearchSource get_source()
    {
        return _source;
    }

    public void set_source(ElasticSearchSource _source)
    {
        this._source = _source;
    }

    public String toString()
    {
        return String.format("{index: %s, type: %s, id: %s, score: %f}", this._index, this._type,
                this._id, this._score);
    }

}
