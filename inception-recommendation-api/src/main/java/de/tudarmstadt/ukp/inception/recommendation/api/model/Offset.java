/*
 * Copyright 2017
 * Ubiquitous Knowledge Processing (UKP) Lab
 * Technische Universität Darmstadt
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tudarmstadt.ukp.inception.recommendation.api.model;

import java.io.Serializable;

public class Offset
    implements Comparable<Offset>, Serializable
{
    private static final long serialVersionUID = -3084534351646334021L;

    private int beginCharacter = -1;
    private int endCharacter = -1;
    
    @Deprecated
    private int beginToken = -1;
    
    @Deprecated
    private int endToken = -1;

    @Deprecated
    public Offset()
    {
    }

    public Offset(int beginCharacter, int endCharacter, int beginToken, int endToken)
    {
        this.beginCharacter = beginCharacter;
        this.endCharacter = endCharacter;
        this.beginToken = beginToken;
        this.endToken = endToken;
    }

    @Override
    public String toString()
    {
        return "Char: (" + beginCharacter + "," + endCharacter + "), Token: (" + beginToken + ","
                + endToken + ")";
    }

    public int getBeginCharacter()
    {
        return beginCharacter;
    }

    public void setBeginCharacter(int beginCharacter)
    {
        this.beginCharacter = beginCharacter;
    }

    @Deprecated
    public int getBeginToken()
    {
        return beginToken;
    }

    @Deprecated
    public void setBeginToken(int beginToken)
    {
        this.beginToken = beginToken;
    }

    public int getEndCharacter()
    {
        return endCharacter;
    }

    public void setEndCharacter(int endCharacter)
    {
        this.endCharacter = endCharacter;
    }

    @Deprecated
    public int getEndToken()
    {
        return endToken;
    }

    @Deprecated
    public void setEndToken(int endToken)
    {
        this.endToken = endToken;
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + beginCharacter;
        result = prime * result + beginToken;
        result = prime * result + endCharacter;
        result = prime * result + endToken;
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
        Offset other = (Offset) obj;
        if (beginCharacter != other.beginCharacter) {
            return false;
        }
        if (beginToken != other.beginToken) {
            return false;
        }
        if (endCharacter != other.endCharacter) {
            return false;
        }
        if (endToken != other.endToken) {
            return false;
        }
        return true;
    }

    @Override
    public int compareTo(Offset o)
    {
        if (o == null) {
            return 1;
        }
        if (this.equals(o)) {
            return 0;
        }
        if (this.getBeginCharacter() < o.getBeginCharacter()) {
            return -1;
        }
        if (this.getBeginToken() < o.getBeginToken()) {
            return -1;
        }
        if (this.getBeginCharacter() == o.getBeginCharacter()
                && this.getEndCharacter() < o.getEndCharacter()) {
            return -1;
        }
        if (this.getBeginToken() == o.getBeginToken() && this.getEndToken() < this.getEndToken()) {
            return -1;
        }

        return 1;
    }
}
