package com.gallatinsystems.gis.map.domain;

import java.util.ArrayList;

import javax.jdo.annotations.PersistenceCapable;

import com.gallatinsystems.framework.domain.BaseDomain;
import com.google.appengine.api.datastore.Text;

@PersistenceCapable
public class Geometry extends BaseDomain {

	/**
	 * 
	 */
	private static final long serialVersionUID = -694616882164561459L;
	private GeometryType type = null;
	private ArrayList<Coordinate> coordinates = null;
	private Text wktText = null;
	private Double centroidLat = null;
	private Double centroidLon = null;
	

	public Double getCentroidLat() {
		return centroidLat;
	}

	public void setCentroidLat(Double centroidLat) {
		this.centroidLat = centroidLat;
	}

	public Double getCentroidLon() {
		return centroidLon;
	}

	public void setCentroidLon(Double centroidLon) {
		this.centroidLon = centroidLon;
	}

	public GeometryType getType() {
		return type;
	}

	public void setType(GeometryType type) {
		this.type = type;
	}

	public ArrayList<Coordinate> getCoordinates() {
		return coordinates;
	}

	public void setCoordinates(ArrayList<Coordinate> coordinates) {
		this.coordinates = coordinates;
	}

	public void addCoordinate(Double x, Double y) {
		if (coordinates == null)
			coordinates = new ArrayList<Coordinate>();
		Coordinate coordinate = new Coordinate(x, y);
		coordinates.add(coordinate);
	}

	public void setWktText(String wktText) {
		this.wktText = new Text(wktText);
	}

	public String getWktText() {
		return wktText.getValue();
	}

	public enum GeometryType{
		MULITPOLYGON,POLYGON, POINT,RECTANGLE
	}
}
