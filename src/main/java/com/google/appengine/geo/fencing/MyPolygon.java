package com.google.appengine.geo.fencing;

import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.Polygon;

public class MyPolygon extends Polygon {
    private long ID;

    public MyPolygon(LinearRing shell, LinearRing[] holes, GeometryFactory factory, long ID) {
        super(shell, holes, factory);
        this.ID = ID;
    }

    public long getID() {
        return ID;
    }

    public void setID(long ID) {
        this.ID = ID;
    }
}
