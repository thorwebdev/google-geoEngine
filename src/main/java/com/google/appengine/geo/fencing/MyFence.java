package com.google.appengine.geo.fencing;

/** The object model for the data we are sending through endpoints */
public class MyFence {

  private long id = -1;
  public String name;
  public String entityGroup;
  public String description;
  public double[][] vertices;

  public MyFence() {};

  public MyFence(long id, String name, String entityGroup, String description, double[][] vertices) {
    this.id = id;
    this.name = name;
    this.entityGroup = entityGroup;
    this.description = description;
    this.vertices = vertices;
  }

  public long getId() {
    return id;
  }

  public void setId(long id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getEntityGroup() {
    return entityGroup;
  }

  public void setEntityGroup(String entityGroup) {
    this.entityGroup = entityGroup;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public double[][] getVertices() {
    return vertices;
  }

  public void setVertices(double[][] vertices) {
    this.vertices = vertices;
  }
}