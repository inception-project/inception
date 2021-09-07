# Experimental Annotation-API concept for custom annotation editors

## Introduction
The following API concept facilitates the implementation of custom annotation editors by providing 
powerful endpoints that handle all back-end implementation details. Therefore, implementing a custom
annotation editor does not require knowledge of INCEpTIONs back-end. This shall enhance the overall
usability of new editors because any time spent does only need to be used for the editor itself.
Furthermore, basic JavaScript and CSS / HTML knowledge is enough to work with the API concept.
This README file shall be a guidance on what the API offers and how to implement an own custom editor. 
As INCEpTION is already a very powerful annotation tool, the API concept still contributes to
INCEpTIONs flexibility in regard to the available annotation editors. Anyone can simply create their own
custom annotation editor. Still, it is advisable to talk to the INCEpTION developer team beforehand in order
to make the implementation smoother by aquiring all the required knowledge for creating a custom annotation
editor. Furthermore, reading this README file carefully will also help to understand its fundamental concepts.
The following editor was one of the first to be implemented with the API concept:
PICTURE.



### API endpoints
This section divides between the API endpoints available to the front-end (for any custom annotation editor) 
and the backend. The most important one is obviously the front-end 'Experience API' and should be called
for any requirement that shall be handled in INCEpTIONs back-end (like creating / deleting annotations).
NOTE:  It is not advised to alter the API-concept in any way in order to guarantee a correct workflow.
However, in case the API-concept must be extended, this section will also cover a 'streamlined process' 
to do so.
#### Client-side 
Any custom annotation editor belongs to the client-side. Any data received or requested
will be handled by WebSocket and a Stomp broker. A separate section about WebSocket and Stomp is also included
in this README file.
##### Experience API
#### Server-Side
##### Process API
##### System API
####Streamlined-process

### WebSocket protocol

### Implementation of the API for a custom editor

### Extension of the API




### Additional information
When the API is invoked, a websocket connection between the client (browser) and server will
also be created automatically. You can configure the URL that is used
in the "config.json" file. 
    
## License
 Licensed to the Technische Universität Darmstadt under one
 or more contributor license agreements.  See the NOTICE file
 distributed with this work for additional information
 regarding copyright ownership.  The Technische Universität Darmstadt
 licenses this file to you under the Apache License, Version 2.0 (the
 "License"); you may not use this file except in compliance
 with the License.
 
 http://www.apache.org/licenses/LICENSE-2.0
 
 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
