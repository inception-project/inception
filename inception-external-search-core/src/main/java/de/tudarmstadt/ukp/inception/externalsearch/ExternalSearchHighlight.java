/*
 * Copyright 2019
 * Ubiquitous Knowledge Processing (UKP) Lab
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
package de.tudarmstadt.ukp.inception.externalsearch;

import java.util.ArrayList;
import java.util.List;

public class ExternalSearchHighlight
{

    private String highlight; // with <em> tags

    private List<KeywordOffset> offsets = new ArrayList<>();

    public ExternalSearchHighlight(String aHighlight) {
        highlight = aHighlight;
    }

    public String getHighlight()
    {
        return highlight;
    }

    public void setHighlight(String highlight)
    {
        this.highlight = highlight;
    }

    public List<KeywordOffset> getOffsets() {
        return offsets;
    }

    public void addOffset(int start, int end) {
        offsets.add(new KeywordOffset(start, end));
    }

    public class KeywordOffset {

        private int start;

        private int end;

        public KeywordOffset(int aStart, int aEnd) {
            start = aStart;
            end = aEnd;
        }

        public int getStartOffset()
        {
            return start;
        }

        public void setStartOffset(int start)
        {
            this.start = start;
        }

        public int getEndOffset()
        {
            return end;
        }

        public void setEndOffset(int end)
        {
            this.end = end;
        }
    }
}
