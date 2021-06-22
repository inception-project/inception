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
function LogoutTimer(sessionTimeout) {
  var instance = this;

  var timeout = sessionTimeout;
  var started = null;
  var timer = null;

  this.tick = function () {
    var remaining = timeout - Math.floor((new Date() - started) / 1000);
    var remaining2 = Math.max(0, remaining);
    var min = Math.floor(remaining2 / 60).toString();

    // Calling text(val) causes a redraw of the browser contents causing constant CPU
    // load. Only calling the method if the text actually did change avoids this issue.
    var timerElement = $('#timer');
    if (timerElement.text() != min) {
      $('#timer').text(min);
    }

    // Wait an additional couple of seconds before reload to avoid reloading before the
    // server-side session has *really* expired.
    if (remaining <= -10) {
      clearInterval(instance.timer);
      location.reload(true);
    }
  };

  this.start = function () {
    const TICK_RATE = 1000; // 1 second
    if (instance.timer) {
      clearInterval(instance.timer);
    }
    started = new Date();
    instance.timer = setInterval(instance.tick, TICK_RATE);
    instance.tick();
  };

  if (typeof Wicket != 'undefined') {
    instance.start();
    Wicket.Event.subscribe('/ajax/call/beforeSend', function (attr, xhr, status) {
      instance.start()
    });
  }
}
