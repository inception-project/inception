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
package de.tudarmstadt.ukp.clarin.webanno.webapp.config;

import java.io.PrintStream;

import org.springframework.boot.Banner;
import org.springframework.boot.ansi.AnsiColor;
import org.springframework.boot.ansi.AnsiOutput;
import org.springframework.boot.ansi.AnsiStyle;
import org.springframework.core.env.Environment;

import de.tudarmstadt.ukp.clarin.webanno.support.SettingsUtil;

public class WebAnnoBanner
    implements Banner
{
    // @formatter:off
    private static final String[] BANNER = { "",
            "  _      __    __   ___                ",
            " | | /| / /__ / /  / _ | ___  ___  ___ ",
            " | |/ |/ / -_) _ \\/ __ |/ _ \\/ _ \\/ _ \\",
            " |__/|__/\\__/_.__/_/ |_/_//_/_//_/\\___/"};
    // @formatter:on

    @Override
    public void printBanner(Environment environment, Class<?> sourceClass, PrintStream printStream)
    {
        for (String line : BANNER) {
            printStream.println(line);
        }
        String version = SettingsUtil.getVersionString();
        printStream.println(AnsiOutput.toString(AnsiColor.GREEN, AnsiStyle.FAINT, version));
        printStream.println();
    }
}
