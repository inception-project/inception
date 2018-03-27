/*
 * Copyright 2017
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
package de.tudarmstadt.ukp.inception.recommendation.imls.util;

/** 
 * Helper class for calculating fibonacci specific things.
 * 
 *
 *
 */
public class MathUtil
{    
    public static int getFibonacci(int steps) {
        if (steps <= 1) {
            return 1;
        }
        return getFibonacci(steps - 1) + getFibonacci(steps - 2);
    }
    
    public static int getFibonacciSteps(int minimum) {
        if (minimum <= 1) {
            return 0;
        }
        
        int prev = 0;
        int curr = 1;
        int step = 0;
        
        while (curr < minimum) {
            int tmp = curr;
            curr += prev;
            prev = tmp;
            step++;
        }
        
        return step;
    }
    
    
}
