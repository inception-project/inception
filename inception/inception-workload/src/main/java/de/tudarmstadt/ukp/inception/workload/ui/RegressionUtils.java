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
package de.tudarmstadt.ukp.inception.workload.ui;

import java.util.List;

final class RegressionUtils
{
    static record Point(double x, double y) {}

    static record RegressionResult(double slope, double intercept) {}

    private RegressionUtils()
    {
        // No instances
    }

    /**
     * Calculates Simple Linear Regression (Least Squares). Formula: y = mx + b
     */
    static RegressionResult calculateRegression(List<Point> aPoints)
    {
        double n = aPoints.size();
        if (n < 2) {
            // Flat line if insufficient data
            return new RegressionResult(0, aPoints.get(0).y);
        }

        double sumX = 0;
        double sumY = 0;
        double sumXY = 0;
        double sumXX = 0;

        for (var p : aPoints) {
            sumX += p.x;
            sumY += p.y;
            sumXY += p.x * p.y;
            sumXX += p.x * p.x;
        }

        double denominator = (n * sumXX - sumX * sumX);
        if (denominator == 0) {
            // Handle vertical line edge case
            return new RegressionResult(0, sumY / n);
        }

        double slope = (n * sumXY - sumX * sumY) / denominator;
        double intercept = (sumY - slope * sumX) / n;
        return new RegressionResult(slope, intercept);
    }
}
