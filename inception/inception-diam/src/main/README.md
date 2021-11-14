# Experimental Annotation-API concept for custom annotation editors in INCEpTION

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
to make the implementation smoother by acquiring all the required knowledge for creating a custom annotation
editor. Furthermore, reading this README file carefully will also help to understand its fundamental concepts.
The following editor was one of the first to be implemented with the API concept:

![](resources/META-INF/asciidoc/user-guide/images/word_alignment_editor.png?raw=true "Title")


### API endpoints
This section divides between the API endpoints available to the front-end (for any custom annotation editor) 
and the backend. The most important one is obviously the front-end 'Experience API' and should be called
for any requirement that shall be handled in INCEpTIONs back-end (like creating / deleting annotations).
NOTE:  It is not advised to alter the API-concept in any way in order to guarantee a correct workflow.
However, in case the API-concept must be extended, this section will also cover a 'streamlined process' 
to do so.
#### Client-side 
Any custom annotation editor belongs to the client-side. Any data received or requested
will be handled by WebSocket and the Stomp broker. A separate section about WebSocket and Stomp 
are also in this README file included.
##### Experience API
The frontend 'Experience API' is written in TypeScript.  An interface class with the name 
AnnotationExperienceAPI.ts serves as the endpoint for any developer / researcher. 
The implementation can be found in the same directory with the name AnnotationExperienceAPIImpl.ts. 
The Experience API has a model package which represent different models required in INCEpTION to represent
different annotations like spans or relations. A 'Viewport' class can also be found in the model package
which serves as a support class containing all the visible text and annotations.
Another package called 'messages' is also contained in the Experience API. 
Every message type can be found here. Separated message types were chosen in order to keep the messages 
payloads as tiny as possible. Overall, the 'Experience API' primarily consist of three different types 
of functions that serve different purposes:
 - connect(): This functions is unique unlike the other two function-types and is already 
 called in the constructor. There is no need to call this method at any time. 
 Obviously, the 'connect()' function creates the connection with WebSocket between the client 
 (custom annotation editor) and the server. The Stomp Broker automatically subscribes to multiple channels 
 that are unique to the client and independent of the current viewport and document. 
 - request(): This type of function always sends a new request to the server. 'request()' functions 
 publish a new message to a specific topic that the server listens to. Input parameters, 
 which will be added as a JSON string to the payload, represent the required data by the server and are therefore mandatory.
 TheJSON string will be created from the corresponding class of the request.
 - on():Functions of the 'on()' type process the data received from the server. Their implementation 
 detail varies a lot. However, in general these functions are called whenever data is received on 
 the specific topic for which the 'Experience API' is subscribed to. 
 
#### Server-Side
Even though the server-side implementation of the API shall also never be altered and is in 
comparison to the 'Experience API' never called by custom annotation editors, we still want
to give some insight into how the back-end API concept works. 
NOTE: As the back-end APIs are never called directly from any custom annotation editors  (only via the
'Experience API') this part can be skipped if there is no interest in their doings.
 
##### Process API
The Process API is the direct communication endpoint for the frontends 'Experience API'. 
It only receives and forwards requests between the other two APIs. Overall, the 'Process API' 
defines from which topics the server receives messages and to which topic it publishes data.
These topics are 'static final variables' defined after the class declaration. 
Whenever a new specific topic is required, it must be added here. In general, the 'Process API'
primarily consists of two different methods:
 - receiveRequest(): Methods listen to data published to specific topics by clients. This is
  ensured by the annotation above their method declaration ’@MessageMap(CONSTANT_TOPIC_VALUE)’. 
  Whenever data is received on a specific topic, the corresponding function will be called.
  This request is then forwarded to a method in the 'System API' which always starts with ’handle()’.
  The call always contains the data that was send by the client, which was transformed beforehand 
  into a specific java class by reading the JSON argument.
  - sendResponse(): sendResponse() Methods always receive a JSON string representation of a class 
  and a desired location to which the data shall be send to. The only statement executed 
  by any of these methods is 'convertAndSend()' together with the correct location to which 
  the data will be send and the respective JSON payload.
  
##### System API
The 'System API' handles all requests made by any client or forwards data after specific events
 happening in INCEpTIONs back-end. The 'System API' has two different kind of methods 
 - handle(): All handle() methods process the desired request by any client.
 - onEventHandler(): This kind of methods always have a '@EventListener' above their header. 
 This indicates, that whenever a specific event happens on the server side (which is declared in 
 the methods arguments) the corresponding method will fire. The event itself carries the 
 information needed. From this event, a new Class-Object is created and forwared 
 to the 'Process API'.
 
 Furthermore, the 'System API' has some private support methods, that are needed to 
 process different requests on the server side. All of these functions are crucial. 
 The following methods are part of the 'System APIs' support functions:
 - getCasForDocument(): This short but straight forward helper methods retrieves the CAS
  for a specific document of a specific user in a specific project. Even though it 
  is such a simple function, it is used by all of the other handle() methods. 
  - getAnnotations(): This is the by far most complex methods of all within the 'System API'. 
  It returns a Pair consisting of a list of span annotations and a list of arc annotations 
  for specific layers for a document. Even though this sounds straightforward,
  Types, FeatureStructures and Features are a highly complex structure in INCEpTION. 
  - getColorForAnnotation(): This method explains its purpose already in its methods name. 
  INCEpTION has coloring strategies which can be customized by project administrators. 
  Therefore, this method shall simply retrieve the correct color for an annotation.

### WebSocket protocol
Any communication done between you custom annotation editor and the back-end of INCEpTION is handled by 
WebSocket. The connection will be created automatically when your annotation editor invokes the 'Experience-API'.
WebSocket is like an open channel between client and server, through which data can be forwarded to each other
at any time. In addition to WebSocket, the API-concept also uses a Stomp Broker. This works like a typical
publish/subscribe mechanism where anyone who is connected can sent data to specific topics / destinations which
then can be retrieved by other client(s) or the server.


### Implementation of the API for a custom editor
Now comes the most important and interesting part of this README. We guide you to create your custom annotation editor
implementation. We will put this into several steps that should be followed precisely. In the end, you will be able to
open your custom editor from the annotation pages 'settings'. Thereafter, feel free to play with the endpoints which the 
'Experience API' offers to your for your custom annotation editor.

Of course, how your editor works and looks like is completely up to you!

1. Create a new module that contains your editor files. For the sake of example, we'll create a word_alignment editor. Therefore,
the name of the module will be 'inception-word-alignment-editor'.
2. Create the following package structure as seen in the picture

![](resources/META-INF/asciidoc/user-guide/images/package_structure.png?raw=true "Title")

HINT: Simply look into 
    -       'inception-experimental-editor/src/main/java/.../editor'
     for the configuration- (in '/config') and resource files (in '/resources')
3. Create a 'spring.factories' class in the 'META-INF' folder. The file should contain the following line:
    -       org.springframework.boot.autoconfigure.EnableAutoConfiguration=\de.tudarmstadt.ukp.inception.wordalignment.editor.config.WordAlignmentEditorAutoConfiguration
4. Create a 'WordAlignmentEditorFactory' class in '/config' 
5. Create the 'WordAlignmentEditorAutoConfiguration' class in '/config' and create a '@Bean' to create your
factory. Be advised, if you add a constraint to this '@Configuration' file do not forget to activate it 
in the 'application.yml' file in 
    -       inception-app-webapp/src/main/resources/application.yml
6. Create a 'WordAlignmentEditorReference' in the '/resources' folder. The referenced JavaScript file path must point
to your JavaScript file. It is recommended to update your package structure with a '/js' package structure
7. Create your 'WordAlignmentEditor' class which must extend 'AnnotationEditorBase'
8. Add to the 'renderHead()' method in your 'WordAlignmentEditor' the following lines
    -      aResponse.render(forReference(WordAlignmentEditorReference.get()));
    -      aResponse.render(OnDomReadyHeaderItem.forScript(setupExperienceAPI()));
    The setupExperienceAPI() call can be copied from
    -      inception-experimental-editor/src/main/java/../../editor/advanced/ExperimentalAdvancedEditor
9. Create a 'WordAlignmentEditor.html' containing at least the following code
    -       <html xmlns="http://www.w3.org/1999/xhtml"
             xmlns:wicket="http://wicket.apache.org/dtds.data/wicket-xhtml1.4-strict.dtd">
            <body>
                <wicket:panel>
                    <div class="scrolling flex-content fit-child-loose text-center" style="min-height: unset;">
                        <form class="editor">
                    </div>
                </wicket:panel>
            </body>    
10. Congratulation you have just created your own editor!
When you now navigate to the annotation page of INCEpTION, simply open the 'settings' by clicking on the cogwheel and
enable your custom editor!

From now on, you can add everything you need in the JavaScript- and the HTML file whatever you want and need. HINT: You can 
add multiple resource references and therewith use multiple JavaScript files if you like.     


### Extension of the API
If there shall really be the point where the API has be extended, it is highly recommended to ask
the INCEpTION team for support in advance. Furthermore, we really want to encourage, that 
extending the API shall be done following the Streamlined-process in the next section.
####Streamlined-process
The streamlined-process consists of nine simple steps and should be followed precisely in order to ensure
proper working of the new functionality.
1. Add the new required topic / channel to the 'Experience-APIs' 'onConnect()' methods in the same
way, the other channels are subscribed to.
2. Create a new 'on()' methods within the 'Experience-API' that is used for the callback. You can later 
add its functionality, but it is recommended to initially only add a 'console.log("Message Retrieved")'.
3. Create TWO new classes that reflect the payload of your new request. The request must end with 
'..Request.ts', the response must end with ’..Message.ts' respectively.
4. Create now the 'request' method in the 'Experience API' and stick to the implementation detail of the
others. The topic, to which the data is published must be the same as defined in the 'Process API'
later on.
REMARK: Whenever the CAS needs to be retrieved in the backend, always add the
'annotatorName', 'projectId' and the 'documentId' in the request. Otherwise, the back-end cannot 
retrieve the correct CAS. 
5. Add the new topics to the 'Process API' in the 'final static string' section in the very top. 
It is highly advised to stick to the naming strategy.
6. Add a 'receive...Request()' and 'send...Response()' method. Their implementation detail
should again be the same as for all the others. Do NOT forget the '@MessageMapping()' above the
methods header. Otherwise, the data won't be received by the 'Process API'.
7. Add TWO new classes that reflect the payload of the new request with the same names as it was done 
for the 'Experience API' in the '/request' and '/response' folders respectively.
8. Add the coressponding handle..() method in the 'Process API'. This method handles everything 
that should be done in INCEpTIONs backend. REMARK: When the new operation type does NOT exist
in INCEpTION yet, there will not be created an event on the server side that can be listened to. 
Step (9) can be omitted. Still, in order to send data back, add the implementation detail 
for any of the on..EventHandler() methods at the end of your new handle..() method.
9. Add an on..EventHandler() method to the 'System API'. Stick to their overall implementation detail.
Do NOT forget to add the @EventListener() notation above the method.

NOTE: All APIs are implementations of their respective interfaces. Therefore, please extend the interfaces.

Overall, this streamlined process can also be seen in the following figure.
    
![](resources/META-INF/asciidoc/user-guide/images/streamlined_process.png?raw=true "Title")

If you encounter any issues, please contact the INCEpTION team.

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
