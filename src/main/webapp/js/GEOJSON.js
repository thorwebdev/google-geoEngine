/**
 * Created by schaeff on 9/25/14.
 */

var GEOJSON = (function(gj){
    gj.Polygon = function(properties, inner, outer){
        this.type = "Feature";
        this.properties = properties;
        this.geometry = {
            "type": "Polygon",
            "coordinates": outer ? [inner,outer] : [inner]
        }
    };

    //EXPORT
    return gj;
}(GEOJSON || {}));