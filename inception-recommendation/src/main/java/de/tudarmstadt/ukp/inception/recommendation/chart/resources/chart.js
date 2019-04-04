/*
#Copyright 2019
#Ubiquitous Knowledge Processing (UKP) Lab
#Technische Universität Darmstadt
#
#Licensed under the Apache License, Version 2.0 (the "License");
#you may not use this file except in compliance with the License.
#You may obtain a copy of the License at
# 
# http://www.apache.org/licenses/LICENSE-2.0
#
#Unless required by applicable law or agreed to in writing, software
#distributed under the License is distributed on an "AS IS" BASIS,
#WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#See the License for the specific language governing permissions and
#limitations under the License.
*/

function OnSuccess(a) {
    var b = [];
    for (var c = 0; c < a.length; c++) b.push(a[c]);
    var d = "category";
    if (a.length > 2) var d = "";
    var e = c3.generate({
        bindto: "#" + $("#canvas").attr("my:canvas.chartid"),
        data: {
            x: "x",
            columns: b,
            type: "step"
        },
        axis: {
            x: {
                type: d,
                tick: {
                    rotate: 0,
                    multiline: true
                }
            },
            y: {
                tick: {
                    format: function(a) {
                        return Math.round(1e4 * a) / 1e4;
                    }
                }
            }
        }
    });
}