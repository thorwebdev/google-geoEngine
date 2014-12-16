package com.google.appengine.geo.fencing;

import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiMethod;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.Filter;
import com.google.appengine.api.datastore.Query.FilterOperator;
import com.google.appengine.api.datastore.Query.FilterPredicate;
import com.google.appengine.api.datastore.Text;
import com.google.appengine.api.memcache.MemcacheService;
import com.google.appengine.api.memcache.MemcacheServiceFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.index.strtree.STRtree;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.LineString;

import javax.inject.Named;

/**
 * API annotation, see:
 * https://cloud.google.com/appengine/docs/java/endpoints/annotations
 */@Api(
        name = "geofencing",
        version = "v1",
        description = "Store geofences and query them",
        clientIds = {
                Constants.WEB_CLIENT_ID
        })

   public class GeoFencingAPI {
    /**
     * Endpoint for adding fences to the DataStore collection.
     */@ApiMethod(name = "add", httpMethod = "post", path = "add")
    public MyFence addFence(@Named("group") String group, @Named("index") boolean buildIndex, MyFence fence) {
        //Get the last fences' id.
        DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
        Key fenceKey = KeyFactory.createKey("Geofence", group);

        long nextId;
        if (fence.getId() != -1) {
            nextId = fence.getId();
        } else {
            long lastId;
            Query query = new Query("Fence", fenceKey).addSort("id", Query.SortDirection.DESCENDING);
            List < Entity > lastFence = datastore.prepare(query).asList(FetchOptions.Builder.withLimit(1));

            if (lastFence.isEmpty()) {
                lastId = -1;
            } else {
                lastId = (long) lastFence.get(0).getProperty("id");
            }
            nextId = lastId + 1;
        }

        //Create new Entity with nextId.
        Entity fenceEntity = new Entity("Fence", fenceKey);
        fenceEntity.setProperty("id", nextId);
        fenceEntity.setProperty("name", fence.getName());
        fenceEntity.setProperty("description", fence.getDescription());

        Gson gson = new Gson();
        String jsonString = gson.toJson(fence.getVertices());
        //Convert to DataStore-Text-Object to store the vertices.
        Text jsonText = new Text(jsonString);
        fenceEntity.setProperty("vertices", jsonText);
        //Write to DataStore.
        datastore.put(fenceEntity);

        MyFence newFence = new MyFence();
        newFence.setId(nextId);

        //Rebuild the Index.
        if (buildIndex) {
            try {
                MyIndex.buildIndex(group);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return newFence;
    }

    /**
     * Endpoint for listing all fences from the DataStore collection.
     */@ApiMethod(name = "list", httpMethod = "get", path = "list")
    public ArrayList < MyFence > listFences(@Named("group") String group) {
        ArrayList < MyFence > fences = new ArrayList < MyFence > ();

        DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
        Key fenceKey = KeyFactory.createKey("Geofence", group);

        Query query = new Query("Fence", fenceKey).addSort("id", Query.SortDirection.DESCENDING);
        List < Entity > fencesFromStore = datastore.prepare(query).asList(FetchOptions.Builder.withDefaults());

        if (fencesFromStore.isEmpty()) {
            return fences;
        } else {
            for (Entity fenceFromStore: fencesFromStore) {
                long id = (long) fenceFromStore.getProperty("id");
                String name = (String) fenceFromStore.getProperty("name");
                String description = (String) fenceFromStore.getProperty("description");
                Gson gson = new Gson();
                Text vText = (Text) fenceFromStore.getProperty("vertices");
                String vString = vText.getValue();
                double[][] vertices = gson.fromJson(vString, double[][].class);

                MyFence tempFence = new MyFence(id, name, group, description, vertices);
                fences.add(tempFence);
            }
            return fences;
        }
    }

    /**
     * Endpoint for finding the fences a certain point is in.
     */@ApiMethod(name = "point", httpMethod = "get", path = "point")
    public ArrayList < MyFence > queryPoint(@Named("group") String group, @Named("lng") double lng, @Named("lat") double lat) {
        ArrayList < MyFence > fences = new ArrayList < MyFence > ();

        //Get the Index from Memcache.
        MemcacheService syncCache = MemcacheServiceFactory.getMemcacheService();
        GeometryFactory gf = new GeometryFactory();
        STRtree index = (STRtree) syncCache.get(group); // read from cache
        if (index != null) {
            Coordinate coord = new Coordinate(lng, lat);
            Point point = gf.createPoint(coord);
            List < MyPolygon > items = index.query(point.getEnvelopeInternal());
            if (!items.isEmpty()) {
                for (MyPolygon poly: items) {
                    if (poly.contains(point)) {
                        long id = poly.getID();
                        MyFence newFence = new MyFence();
                        newFence.setId(id);
                        fences.add(newFence);
                    }
                }
            }
        } else {
            try {
                MyIndex.buildIndex(group);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return fences;
    }

    /**
     * Endpoint for getting a fence's metadata by its id.
     */@ApiMethod(name = "getById", httpMethod = "get", path = "getById")
    public ArrayList < MyFence > getFenceById(@Named("group") String group, @Named("id") long id) {
        DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
        Key fenceKey = KeyFactory.createKey("Geofence", group);
        Filter propertyFilter = new FilterPredicate("id", FilterOperator.EQUAL, id);
        Query query = new Query("Fence", fenceKey).setFilter(propertyFilter);
        Entity fenceFromStore = datastore.prepare(query).asSingleEntity();

        ArrayList < MyFence > fences = new ArrayList < MyFence > ();
        if (fenceFromStore != null) {
            String name = (String) fenceFromStore.getProperty("name");
            String description = (String) fenceFromStore.getProperty("description");
            Gson gson = new Gson();
            Text vText = (Text) fenceFromStore.getProperty("vertices");
            String vString = vText.getValue();
            double[][] vertices = gson.fromJson(vString, double[][].class);

            MyFence tempFence = new MyFence(id, name, group, description, vertices);
            fences.add(tempFence);
        }
        return fences;
    }

    /**
     * Endpoint for finding the fences that intersect with a polyline.
     */@ApiMethod(name = "polyline", httpMethod = "post", path = "polyline")
    public ArrayList < MyFence > queryPolyLine(@Named("group") String group, MyPolyLine polyline) {
        ArrayList < MyFence > fences = new ArrayList < MyFence > ();

        //Get the index from Memcache.
        MemcacheService syncCache = MemcacheServiceFactory.getMemcacheService();
        GeometryFactory gf = new GeometryFactory();
        STRtree index = (STRtree) syncCache.get(group); // read from cache
        if (index != null) {
            //Create coordinate array.
            double[][] points = polyline.getCoordinates();
            Coordinate[] coordinates = new Coordinate[points.length];
            int i = 0;
            for (double[] point: points) {
                Coordinate coordinate = new Coordinate(point[0], point[1]);
                coordinates[i++] = coordinate;
            }
            //Create polyline.
            GeometryFactory fact = new GeometryFactory();
            LineString linestring = new GeometryFactory().createLineString(coordinates);

            List < MyPolygon > items = index.query(linestring.getEnvelopeInternal());
            if (!items.isEmpty()) {
                for (MyPolygon poly: items) {
                    if (linestring.crosses(poly) || poly.contains(linestring)) {
                        long id = poly.getID();
                        MyFence newFence = new MyFence();
                        newFence.setId(id);
                        fences.add(newFence);
                    }
                }
            }
        } else {
            try {
                MyIndex.buildIndex(group);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return fences;
    }

    /**
     * Endpoint for finding the fences that intersect with a polygon.
     */@ApiMethod(name = "polygon", httpMethod = "post", path = "polygon")
    public ArrayList < MyFence > queryPolygon(@Named("group") String group, MyPolyLine polyline) {
        ArrayList < MyFence > fences = new ArrayList < MyFence > ();

        //Get index from Memcache
        MemcacheService syncCache = MemcacheServiceFactory.getMemcacheService();
        STRtree index = (STRtree) syncCache.get(group); // read from cache
        if (index != null) {
            //Create coordinate array.
            double[][] points = polyline.getCoordinates();
            Coordinate[] coordinates = new Coordinate[points.length];
            int i = 0;
            for (double[] point: points) {
                Coordinate coordinate = new Coordinate(point[0], point[1]);
                coordinates[i++] = coordinate;
            }
            //Create polygon.
            GeometryFactory fact = new GeometryFactory();
            LinearRing linear = new GeometryFactory().createLinearRing(coordinates);
            Polygon polygon = new Polygon(linear, null, fact);

            List < MyPolygon > items = index.query(polygon.getEnvelopeInternal());
            if (!items.isEmpty()) {
                for (MyPolygon poly: items) {
                    if (polygon.contains(poly) || !polygon.disjoint(poly)) {
                        long id = poly.getID();
                        MyFence newFence = new MyFence();
                        newFence.setId(id);
                        fences.add(newFence);
                    }
                }
            }
        } else {
            try {
                MyIndex.buildIndex(group);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return fences;
    }

    /**
     * Endpoint for building and storing the index for a certain group to Memcache.
     */@ApiMethod(name = "buildIndex", httpMethod = "get", path = "buildIndex")
    public MyFence buildIndex(@Named("group") String group) {
        MyFence fence = new MyFence();

        long id;
        try {
            id = (long) MyIndex.buildIndex(group);
            fence.setId(id);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return fence;
    }

    //TODO delete group
}