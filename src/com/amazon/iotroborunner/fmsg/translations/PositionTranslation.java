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

package com.amazon.iotroborunner.fmsg.translations;

import com.amazon.iotroborunner.fmsg.types.roborunner.PositionConversionCalibrationPoint;
import com.amazon.iotroborunner.fmsg.types.roborunner.ReferencePoint;

import java.util.ArrayList;
import java.util.List;

import boofcv.alg.distort.PointTransformHomography_F64;
import boofcv.alg.geo.h.HomographyDirectLinearTransform;
import boofcv.struct.geo.AssociatedPair;
import georegression.struct.point.Point2D_F64;
import lombok.NonNull;
import org.ejml.data.DMatrixRMaj;

/**
 * Class that performs the XY to lat/long transformation for each map of
 * an FMS.
 */
public class PositionTranslation {
    private PointTransformHomography_F64 homographyMatrix;

    /** Constructor using the configuration. */
    public PositionTranslation(@NonNull final List<PositionConversionCalibrationPoint> calibrationPoints) {
        homographyMatrix = initializeHomographyMatrix(calibrationPoints);
    }

    private PointTransformHomography_F64 initializeHomographyMatrix(
            @NonNull final List<PositionConversionCalibrationPoint> points) {
        final List<AssociatedPair> pairs = new ArrayList<>();

        for (PositionConversionCalibrationPoint point : points) {
            final ReferencePoint vendorPoint = point.getVendorCoordinates();
            final ReferencePoint roboRunnerPoint = point.getRoboRunnerCoordinates();
            pairs.add(
                new AssociatedPair(
                    vendorPoint.getXcoordinate(),
                    vendorPoint.getYcoordinate(),
                    roboRunnerPoint.getXcoordinate(),
                    roboRunnerPoint.getYcoordinate()
                )
            );
        }

        final DMatrixRMaj matrix = new DMatrixRMaj();
        final HomographyDirectLinearTransform transform = new HomographyDirectLinearTransform(false);
        transform.process(pairs, matrix);

        return new PointTransformHomography_F64(matrix);
    }

    /** Transform FMS co-ordinates to RoboRunner co-ordinates. */
    public double[] getRoboRunnerCoordinatesFromFmsCoordinates(final double x, final double y) throws RuntimeException {
        final Point2D_F64 roboRunnerCoordinates = new Point2D_F64();
        homographyMatrix.compute(x, y, roboRunnerCoordinates);

        return new double[]{roboRunnerCoordinates.getX(), roboRunnerCoordinates.getY()};
    }
}
