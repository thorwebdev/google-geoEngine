# Scalable geofencing API for Google App Engine

An architecture proposal for a geofencing API on Google's [App Engine][1] using [Google Cloud Endpoints][3].
This API is able to geofence complex polygons at a large throughput using App Engine, Cloud Datastore and Memcache.
It uses the [Java Topology Suite][7] to create a spatial index which is stored in Memcache for fast querying access. 
You can download, build and deploy the project as is using the [Google App Engine Maven plugin][4].
For a detailed explanation of the architecture please refer to this [Google Developers Blogpost][8].

## Endpoints

- __add__: Add a fence to a certain group.
- __buildIndex__: Build the spatial index and write it to Memcache.
- __getById__: Get a fence's metadata by it's id.
- __list__: List all fences in a certain group.
- __point__: Get all fences that contain a certain point.
- __polygon__: Get all fences that aren't disjoint with a certain polygon.
- __polyline__: Get all fences that intersect with a certain polyline.

## Test & Deploy to App Engine

1. Update the value of `application` in `src/main/webapp/WEB-INF/appengine-web.xml` to the app
   ID you have registered in the App Engine admin console and would
   like to use to host your instance of this sample.

1. **__Optional step:__** These sub steps are not required but you need this
   if you want to have auth protected methods.

    2. Update the values in `src/main/java/com/google/appengine/geo/fencing/Constants.java`
       to reflect the respective client IDs you have registered in the
       [APIs Console][6]. 

    2. You also need to supply the web client ID you have registered
       in the [APIs Console][4] to your client of choice (web, Android,
       iOS).

1. Run the application with `mvn appengine:devserver`, and ensure it's
   running by visiting your local server (by
   default [localhost:8080][5].)

1. **__Optional step:__** Get the client library with

   `$ mvn appengine:endpoints_get_client_lib`

   It will generate a client library jar file under the
   `target/endpoints-client-libs/<api-name>/target` directory of your
   project, as well as install the artifact into your local maven
   repository.
   
   For more information on client libraries see:
   
   - [Generating Cleint Libraries][11]
   - [Client Library for JavaScript][10]

1. Deploy your application to Google App Engine with

   `$ mvn appengine:update`
   
   *Please note that you should always first test on the development server since that creates indexes for our datastore queries. Also after the first deployment App Engine takes a while to create the necessary indexes and connections, so if you get errors, just wait for a bit.*
   
## Example of using the [JavaScript Google Client Library with this API][10]

- `src/main/webapp/addFence.html` is an example of how to use the Google Maps JavaScript API [Drawing Layer][9] 
to draw fences to the map and store them to your App Engines Datastore using the __add__ endpoint.

- `src/main/webapp/query.html` shows you how to query your API for points, polylines and polygons.

These examples can also be used to test your API. You should always first test on the devserver (`mvn appengine:devserver`), 
since this automatically creates indexes that are needed for our Datastore queries.

[1]: https://developers.google.com/appengine
[2]: http://java.com/en/
[3]: https://developers.google.com/appengine/docs/java/endpoints/
[4]: https://developers.google.com/appengine/docs/java/tools/maven
[5]: http://localhost:8080/
[6]: https://console.developers.google.com/
[7]: http://www.vividsolutions.com/jts/JTSHome.htm
[8]: http://googledevelopers.blogspot.com/2014/12/building-scalable-geofencing-api-on.html
[9]: https://developers.google.com/maps/documentation/javascript/drawinglayer
[10]: https://developers.google.com/api-client-library/javascript/start/start-js
[11]: https://cloud.google.com/appengine/docs/java/endpoints/gen_clients