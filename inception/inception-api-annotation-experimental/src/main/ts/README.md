<p align="center">
  <br/>
  <br/><br/>
</p>

A Typescript API for text annotation. 

<br/>
<br/>

## Using the API
In order to use the API for your custom annotation editor, create a typescript file
that imports the index.ts file located in:

"inception-api-annotation-experimental/src/main/ts"

and assign a variable to create the object. 

var API = new Experimental()



### API endpoints
Several API endpoints (that are also customizable in the index.ts file) can be called.
The event library used by the API is "tiny-emitter" 
- https://www.npmjs.com/package/tiny-emitter)

Sending and receiving updates for annotations are created by
- send_*_annotation (where the * denotes "create" / "select" / "delete" ), or by
- receive_*_annotation

events.


### Additional information
When the API is invoked, a websocket connection between the client (browser) and server will
also be created automatically. You can configure the URL that is used
in the "config.json" file. 
    
## License

