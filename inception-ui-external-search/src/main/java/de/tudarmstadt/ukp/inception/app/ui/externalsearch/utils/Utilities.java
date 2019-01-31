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
package de.tudarmstadt.ukp.inception.app.ui.externalsearch.utils;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.safety.Cleaner;
import org.jsoup.safety.Whitelist;

public class Utilities
{
    public static String cleanHighlight(String aHighlight) {
        Whitelist wl = new Whitelist();
        wl.addTags("em");
        Document dirty = Jsoup.parseBodyFragment(aHighlight, "");
        Cleaner cleaner = new Cleaner(wl);
        Document clean = cleaner.clean(dirty);
        clean.select("em").tagName("mark");

        return clean.body().html();
    }
}
