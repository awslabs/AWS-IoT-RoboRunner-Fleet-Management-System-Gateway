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

package com.amazon.iotroborunner.fmsg.testhelpers.sharedspace;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.geotools.geometry.jts.JTSFactoryFinder;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;

/**
 * SharedSpaceTestConstants contains constants that can be used for shared space testing.
 */
public final class SharedSpaceTestConstants {
    private static final GeometryFactory GEOMETRY_FACTORY = JTSFactoryFinder.getGeometryFactory();

    public static final String POLYGON_JSON_1 =
        "{\"polygon\":[{\"y\":0.123,\"x\":4.567},{\"y\":8.6,\"x\":12.3},{\"y\":45.67,\"x\":8.90}]}";
    public static final String POLYGON_JSON_2 = 
        "{\"polygon\":[{\"y\":11.1,\"x\":22.2},{\"y\":33.3,"
         + "\"x\":44.4},{\"y\":55.5,\"x\":66.6},{\"y\":77.7,\"x\":88.8}]}";
    public static final String POLYGON_JSON_3 =
        "{\"polygon\":[{\"y\":0,\"x\":1},{\"y\":0,"
            + "\"x\":0},{\"y\":1,\"x\":0},{\"y\":1,\"x\":1}]}";

    public static final List<Point> POLYGON_COORDS_EMPTY =
            Collections.unmodifiableList(new ArrayList());
    public static final List<Point> POLYGON_COORDS_1 =
            Collections.unmodifiableList(new ArrayList<Point>() {{
                    add(GEOMETRY_FACTORY.createPoint(new Coordinate(4.567, 0.123)));
                    add(GEOMETRY_FACTORY.createPoint(new Coordinate(12.3, 8.6)));
                    add(GEOMETRY_FACTORY.createPoint(new Coordinate(8.90, 45.67)));
                }
            });
    public static final List<Point> POLYGON_COORDS_2 =
            Collections.unmodifiableList(new ArrayList<Point>() {{
                    add(GEOMETRY_FACTORY.createPoint(new Coordinate(22.2, 11.1)));
                    add(GEOMETRY_FACTORY.createPoint(new Coordinate(44.4, 33.3)));
                    add(GEOMETRY_FACTORY.createPoint(new Coordinate(66.6, 55.5)));
                    add(GEOMETRY_FACTORY.createPoint(new Coordinate(88.8, 77.7)));
                }
            });
    public static final List<Point> POLYGON_COORDS_SAME =
            Collections.unmodifiableList(new ArrayList<Point>() {{
                    add(GEOMETRY_FACTORY.createPoint(new Coordinate(4.567, 0.123)));
                    add(GEOMETRY_FACTORY.createPoint(new Coordinate(12.3, 8.6)));
                    add(GEOMETRY_FACTORY.createPoint(new Coordinate(8.90, 45.67)));
                    add(GEOMETRY_FACTORY.createPoint(new Coordinate(4.567, 0.123)));
                }
            });
    public static final List<Point> POLYGON_COORDS_SINGLE =
            Collections.unmodifiableList(new ArrayList<Point>() {{
                    add(GEOMETRY_FACTORY.createPoint(new Coordinate(4.567, 0.123)));
                }
            });
    public static final List<Point> POLYGON_COORDS_TWO_SAME =
            Collections.unmodifiableList(new ArrayList<Point>() {{
                    add(GEOMETRY_FACTORY.createPoint(new Coordinate(4.567, 0.123)));
                    add(GEOMETRY_FACTORY.createPoint(new Coordinate(4.567, 0.123)));
                }
            });
    public static final List<Point> POLYGON_COORDS_TWO_DIFF =
            Collections.unmodifiableList(new ArrayList<Point>() {{
                    add(GEOMETRY_FACTORY.createPoint(new Coordinate(4.567, 0.123)));
                    add(GEOMETRY_FACTORY.createPoint(new Coordinate(12.3, 8.6)));
                }
            });
    public static final Double ROBOT_POSITION_X = 22.377197265625;
    public static final Double ROBOT_POSITION_Y = 10.37197494506836;
    public static final Double ROBOT_VENDOR_X = 6.092332363128662;
    public static final Double ROBOT_VENDOR_Y = 2.8254685401916504;

    public static final String WORKER_RESPONSE =
            "tst/com/amazon/iotroborunner/fmsg/connectors/worker_response.json";
    public static final String WORKER_RESPONSE_2 =
        "tst/com/amazon/iotroborunner/fmsg/connectors/worker_response_2.json";
    public static final String WORKER_WAITING_RESPONSE =
            "tst/com/amazon/iotroborunner/fmsg/connectors/worker_waiting_response.json";
    public static final String AREA_MAP_RESPONSE =
            "tst/com/amazon/iotroborunner/fmsg/connectors/mir_area_map_response.json";

    public static final Duration VENDOR_POLLING_DURATION = Duration.ofSeconds(6);

    /**
     * Utility/Constant class, don't allow instantiation.
     */
    private SharedSpaceTestConstants() {
        throw new UnsupportedOperationException("This class is for holding constants and should not be instantiated");
    }
}
