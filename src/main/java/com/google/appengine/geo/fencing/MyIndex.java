package com.google.appengine.geo.fencing;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Text;



import com.google.appengine.api.memcache.MemcacheService;
import com.google.appengine.api.memcache.MemcacheServiceFactory;

import java.io.IOException;
import java.util.List;

import com.google.gson.Gson;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.index.strtree.STRtree;
import com.vividsolutions.jts.geom.LinearRing;


public class MyIndex {
    //Create the index and store it in Memcache.
    public static int buildIndex(String group) throws IOException{
        //Get all fences of group from DataStore.
        DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
        Key fenceKey = KeyFactory.createKey("Geofence", group);

        Query query = new Query("Fence", fenceKey).addSort("id", Query.SortDirection.DESCENDING);
        List<Entity> fencesFromStore = datastore.prepare(query).asList(FetchOptions.Builder.withDefaults());

        if (!fencesFromStore.isEmpty()) {
        	//Create STRTree-Index.
            STRtree index = new STRtree();
            //Loop through the fences from DataStore.
            for (Entity fenceFromStore : fencesFromStore) {
                long id = (long) fenceFromStore.getProperty("id");
                Gson gson = new Gson();
                Text vText = (Text) fenceFromStore.getProperty("vertices");
                String vString = vText.getValue();
                double[][] vertices = gson.fromJson(vString, double[][].class);

                //Store coordinates in an array.
                Coordinate[] coordinates = new Coordinate[vertices.length];
                int i = 0;
                for(double[] point : vertices){
                    Coordinate coordinate = new Coordinate(point[0],point[1]);
                    coordinates[i++] = coordinate;
                }
                //Create polygon from the coordinates.
                GeometryFactory fact = new GeometryFactory();
                LinearRing linear = new GeometryFactory().createLinearRing(coordinates);
                MyPolygon polygon = new MyPolygon(linear, null, fact, id);
                //Add polygon to index.
                index.insert(polygon.getEnvelopeInternal(), polygon);
            }
            //Build the index.
            index.build();
            //Write the index to Memcache.
            MemcacheService syncCache = MemcacheServiceFactory.getMemcacheService();
            //Last param is expiration date. Set to null to keep it in Memcache forever.
            syncCache.put(group, index, null); 
        }
        return fencesFromStore.size();
    }
}
