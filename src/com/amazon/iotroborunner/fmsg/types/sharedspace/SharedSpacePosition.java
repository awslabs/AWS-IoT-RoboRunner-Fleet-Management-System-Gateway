/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: MIT-0
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the "Software"), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify,
 * merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A
 * PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.amazon.iotroborunner.fmsg.types.sharedspace;

import java.util.ArrayList;
import java.util.List;

import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;

/**
 * This object stores the x,y coordinate pairs that represent the position of a Shared Space.
 */
@Data
@NoArgsConstructor
public class SharedSpacePosition {
    private static final GeometryFactory GEOMETRY_FACTORY = JTSFactoryFinder.getGeometryFactory();
    private Polygon positionPolygon;

    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private List<Point> coordinates = new ArrayList<>();

    /**
     * Custom getter to avoid exposing internal representation of the object.
     */
    public List<Point> getCoordinates() {
        return List.copyOf(this.coordinates);
    }

    /**
     * Custom setter to avoid exposing internal representation of the object.
     */
    public void setCoordinates(final List<Point> coordinates) {
        this.coordinates = List.copyOf(coordinates);
    }

    /**
     * Add position x,y coordinates.
     */
    public void addCoordinates(final double x, final double y) {
        this.coordinates.add(GEOMETRY_FACTORY.createPoint(new Coordinate(x, y)));
    }

    /**
     * Custom getter to avoid exposing internal representation of the position polygon.
     *
     * @return Position Polygon.
     */
    public Polygon getPositionPolygon() {
        return (Polygon) positionPolygon.copy();
    }

    /**
     * Custom setter to avoid exposing internal representation of the position polygon.
     */
    public void setPositionPolygon(@NonNull final Polygon positionPolygon) {
        this.positionPolygon = (Polygon) positionPolygon.copy();
    }
}