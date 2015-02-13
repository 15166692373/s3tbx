package org.esa.nest.gpf.geometric;

import org.apache.commons.math3.util.FastMath;
import org.esa.beam.framework.datamodel.*;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.nest.dataio.dem.ElevationModel;
import org.esa.snap.datamodel.AbstractMetadata;
import org.esa.snap.datamodel.OrbitStateVector;
import org.esa.snap.eo.Constants;
import org.esa.snap.eo.GeoUtils;
import org.esa.snap.eo.LocalGeometry;
import org.esa.snap.gpf.OperatorUtils;
import org.esa.snap.gpf.TileGeoreferencing;
import org.esa.snap.util.Maths;

/**
 * Common SAR utilities for Geocoding
 */
public class SARGeocoding {

    public static final double NonValidZeroDopplerTime = -99999.0;
    public static final double NonValidIncidenceAngle = -99999.0;

    public static boolean isNearRangeOnLeft(final TiePointGrid incidenceAngle, final int sourceImageWidth) {
        // for products without incidence angle tpg just assume left facing
        if (incidenceAngle == null) return true;

        final double incidenceAngleToFirstPixel = incidenceAngle.getPixelDouble(0, 0);
        final double incidenceAngleToLastPixel = incidenceAngle.getPixelDouble(sourceImageWidth - 1, 0);
        return (incidenceAngleToFirstPixel < incidenceAngleToLastPixel);
    }

    /**
     * Compute zero Doppler time for given earth point using bisection method.
     *
     * @param firstLineUTC     The zero Doppler time for the first range line.
     * @param lineTimeInterval The line time interval.
     * @param wavelength       The radar wavelength.
     * @param earthPoint       The earth point in xyz coordinate.
     * @param sensorPosition   Array of sensor positions for all range lines.
     * @param sensorVelocity   Array of sensor velocities for all range lines.
     * @return The zero Doppler time in days if it is found, -1 otherwise.
     * @throws OperatorException The operator exception.
     */
    public static double getEarthPointZeroDopplerTime(final double firstLineUTC,
                                                      final double lineTimeInterval, final double wavelength,
                                                      final double[] earthPoint, final double[][] sensorPosition,
                                                      final double[][] sensorVelocity) throws OperatorException {

        // binary search is used in finding the zero doppler time
        int lowerBound = 0;
        int upperBound = sensorPosition.length - 1;
        double lowerBoundFreq = getDopplerFrequency(
                earthPoint, sensorPosition[lowerBound], sensorVelocity[lowerBound], wavelength);
        double upperBoundFreq = getDopplerFrequency(
                earthPoint, sensorPosition[upperBound], sensorVelocity[upperBound], wavelength);

        if (Double.compare(lowerBoundFreq, 0.0) == 0) {
            return firstLineUTC + lowerBound * lineTimeInterval;
        } else if (Double.compare(upperBoundFreq, 0.0) == 0) {
            return firstLineUTC + upperBound * lineTimeInterval;
        } else if (lowerBoundFreq * upperBoundFreq > 0.0) {
            return NonValidZeroDopplerTime;
        }

        // start binary search
        double midFreq;
        while (upperBound - lowerBound > 1) {

            final int mid = (int) ((lowerBound + upperBound) / 2.0);
            midFreq = sensorVelocity[mid][0] * (earthPoint[0] - sensorPosition[mid][0]) +
                    sensorVelocity[mid][1] * (earthPoint[1] - sensorPosition[mid][1]) +
                    sensorVelocity[mid][2] * (earthPoint[2] - sensorPosition[mid][2]);

            if (midFreq * lowerBoundFreq > 0.0) {
                lowerBound = mid;
                lowerBoundFreq = midFreq;
            } else if (midFreq * upperBoundFreq > 0.0) {
                upperBound = mid;
                upperBoundFreq = midFreq;
            } else if (Double.compare(midFreq, 0.0) == 0) {
                return firstLineUTC + mid * lineTimeInterval;
            }
        }

        final double y0 = lowerBound - lowerBoundFreq * (upperBound - lowerBound) / (upperBoundFreq - lowerBoundFreq);
        return firstLineUTC + y0 * lineTimeInterval;
    }

    /**
     * Compute zero Doppler time for given earth point using Newton's method.
     *
     * @param firstLineUTC     The zero Doppler time for the first range line.
     * @param lineTimeInterval The line time interval.
     * @param wavelength       The radar wavelength.
     * @param earthPoint       The earth point in xyz coordinate.
     * @param sensorPosition   Array of sensor positions for all range lines.
     * @param sensorVelocity   Array of sensor velocities for all range lines.
     * @return The zero Doppler time in days if it is found, NonValidZeroDopplerTime otherwise.
     * @throws OperatorException The operator exception.
     */
    public static double getEarthPointZeroDopplerTimeNewton(final double firstLineUTC,
                                                            final double lineTimeInterval, final double wavelength,
                                                            final double[] earthPoint, final double[][] sensorPosition,
                                                            final double[][] sensorVelocity) throws OperatorException {
        final int lowerBound = 0;
        final int upperBound = sensorPosition.length - 1;
        final double lowerBoundFreq = getDopplerFrequency(earthPoint, sensorPosition[lowerBound],
                sensorVelocity[lowerBound], wavelength);
        final double upperBoundFreq = getDopplerFrequency(earthPoint, sensorPosition[upperBound],
                sensorVelocity[upperBound], wavelength);
        if (Double.compare(lowerBoundFreq, 0.0) == 0) {
            return firstLineUTC + lowerBound * lineTimeInterval;
        } else if (Double.compare(upperBoundFreq, 0.0) == 0) {
            return firstLineUTC + upperBound * lineTimeInterval;
        } else if (lowerBoundFreq * upperBoundFreq > 0.0) {
            return NonValidZeroDopplerTime;
        }
        int yOld = 0, yOld1;
        int yNew = sensorPosition.length / 2, yNew1 = 0;
        final int yMax = sensorPosition.length - 1;
        double fOld = 0, fOld1 = 0, fNew = 0, fNew1 = 0, d = 0, y0;
        while (Math.abs(yNew - yOld) > 2) {
            yOld = yNew;
            yOld1 = yOld + 1;
            if (yOld1 > yMax) {
                yOld1 = yOld - 1;
            }
            fOld = getDopplerFrequency(earthPoint, sensorPosition[yOld], sensorVelocity[yOld], wavelength);
            fOld1 = getDopplerFrequency(earthPoint, sensorPosition[yOld1], sensorVelocity[yOld1], wavelength);
            d = (fOld1 - fOld) / (yOld1 - yOld);
            yNew = (int) (yOld - fOld / d);
            if (yNew < 0) {
                yNew = 0;
            } else if (yNew > yMax) {
                yNew = yMax;
            }
        }
        fNew = getDopplerFrequency(earthPoint, sensorPosition[yNew], sensorVelocity[yNew], wavelength);
        yNew1 = yNew + 1;
        fNew1 = getDopplerFrequency(earthPoint, sensorPosition[yNew1], sensorVelocity[yNew1], wavelength);
        if (fNew * fNew1 > 0.0) {
            yNew1 = yNew - 1;
            fNew1 = getDopplerFrequency(earthPoint, sensorPosition[yNew1], sensorVelocity[yNew1], wavelength);
        }
        y0 = yNew - fNew * (yNew1 - yNew) / (fNew1 - fNew);
        return firstLineUTC + y0 * lineTimeInterval;
    }

    public static double getEarthPointZeroDopplerTimeNewton(
            final double firstLineUTC, final double lineTimeInterval, final double wavelength,
            final double[] earthPoint, final SARGeocoding.Orbit orbit) throws OperatorException {

        final int numOrbitVec = orbit.orbitStateVectors.length;
        double[] sensorPosition = new double[3];
        double[] sensorVelocity = new double[3];

        sensorPosition[0] = orbit.orbitStateVectors[0].x_pos;
        sensorPosition[1] = orbit.orbitStateVectors[0].y_pos;
        sensorPosition[2] = orbit.orbitStateVectors[0].z_pos;
        sensorVelocity[0] = orbit.orbitStateVectors[0].x_vel;
        sensorVelocity[1] = orbit.orbitStateVectors[0].y_vel;
        sensorVelocity[2] = orbit.orbitStateVectors[0].z_vel;
        final double firstVecFreq = getDopplerFrequency(earthPoint, sensorPosition, sensorVelocity, wavelength);
        final double firstVecTime = orbit.orbitStateVectors[0].time_mjd;

        sensorPosition[0] = orbit.orbitStateVectors[numOrbitVec - 1].x_pos;
        sensorPosition[1] = orbit.orbitStateVectors[numOrbitVec - 1].y_pos;
        sensorPosition[2] = orbit.orbitStateVectors[numOrbitVec - 1].z_pos;
        sensorVelocity[0] = orbit.orbitStateVectors[numOrbitVec - 1].x_vel;
        sensorVelocity[1] = orbit.orbitStateVectors[numOrbitVec - 1].y_vel;
        sensorVelocity[2] = orbit.orbitStateVectors[numOrbitVec - 1].z_vel;
        final double lastVecFreq = getDopplerFrequency(earthPoint, sensorPosition, sensorVelocity, wavelength);
        final double lastVecTime = orbit.orbitStateVectors[numOrbitVec - 1].time_mjd;

        if (firstVecFreq == 0.0) {
            return firstVecTime;
        } else if (lastVecFreq == 0.0) {
            return lastVecTime;
        } else if (firstVecFreq * lastVecFreq > 0.0) {
            return NonValidZeroDopplerTime;
        }

        double oldTime = firstVecTime, oldTimeDel;
        double oldFreq = firstVecFreq;
        double newTime = (firstVecTime + lastVecTime) / 2.0, oldFreqDel = 0.0;
        orbit.getPositionVelocity(newTime, sensorPosition, sensorVelocity);
        double newFreq = getDopplerFrequency(earthPoint, sensorPosition, sensorVelocity, wavelength);

        double d;
        int numIter = 0;
        while (Math.abs(newFreq) > 0.001 && numIter <= 10) {
            oldTime = newTime;
            oldFreq = newFreq;

            orbit.getPositionVelocity(oldTime + lineTimeInterval, sensorPosition, sensorVelocity);
            oldFreqDel = getDopplerFrequency(earthPoint, sensorPosition, sensorVelocity, wavelength);

            d = (oldFreqDel - oldFreq) / lineTimeInterval;

            newTime = oldTime - oldFreq / d;
            if (newTime < 0.0) {
                newTime = 0.0;
            } else if (newTime > lastVecTime) {
                newTime = lastVecTime;
            }

            orbit.getPositionVelocity(newTime, sensorPosition, sensorVelocity);
            newFreq = getDopplerFrequency(earthPoint, sensorPosition, sensorVelocity, wavelength);
            numIter++;
        }

        return newTime;
    }

    /**
     * Compute zero Doppler time for given point with the product orbit state vectors using bisection method.
     *
     * @param firstLineUTC     The zero Doppler time for the first range line.
     * @param lineTimeInterval The line time interval.
     * @param wavelength       The radar wavelength.
     * @param earthPoint       The earth point in xyz coordinate.
     * @param orbit            The object holding orbit state vectors.
     * @return The zero Doppler time in days if it is found, NonValidZeroDopplerTime otherwise.
     * @throws OperatorException The operator exception.
     */
    public static double getZeroDopplerTime(final double firstLineUTC, final double lineTimeInterval,
                                            final double wavelength, final double[] earthPoint,
                                            final SARGeocoding.Orbit orbit) throws OperatorException {

        // loop through all orbit state vectors to find the adjacent two vectors
        final int numOrbitVec = orbit.orbitStateVectors.length;
        double[] sensorPosition = new double[3];
        double[] sensorVelocity = new double[3];
        double firstVecTime = 0.0;
        double secondVecTime = 0.0;
        double firstVecFreq = 0.0;
        double secondVecFreq = 0.0;

        for (int i = 0; i < numOrbitVec; i++) {
            sensorPosition[0] = orbit.orbitStateVectors[i].x_pos;
            sensorPosition[1] = orbit.orbitStateVectors[i].y_pos;
            sensorPosition[2] = orbit.orbitStateVectors[i].z_pos;

            sensorVelocity[0] = orbit.orbitStateVectors[i].x_vel;
            sensorVelocity[1] = orbit.orbitStateVectors[i].y_vel;
            sensorVelocity[2] = orbit.orbitStateVectors[i].z_vel;

            final double currentFreq = getDopplerFrequency(earthPoint, sensorPosition, sensorVelocity, wavelength);
            if (i == 0 || firstVecFreq * currentFreq > 0) {
                firstVecTime = orbit.orbitStateVectors[i].time_mjd;
                firstVecFreq = currentFreq;
            } else {
                secondVecTime = orbit.orbitStateVectors[i].time_mjd;
                secondVecFreq = currentFreq;
                break;
            }
        }

        if (firstVecFreq * secondVecFreq >= 0.0) {
            return NonValidZeroDopplerTime;
        }

        // find the exact time using Doppler frequency and bisection method
        double lowerBoundTime = firstVecTime;
        double upperBoundTime = secondVecTime;
        double lowerBoundFreq = firstVecFreq;
        double upperBoundFreq = secondVecFreq;
        double midTime = 0.0;
        double midFreq = 0.0;
        double diffTime = Math.abs(upperBoundTime - lowerBoundTime);
        while (diffTime > Math.abs(lineTimeInterval)) {

            midTime = (upperBoundTime + lowerBoundTime) / 2.0;
            orbit.getPositionVelocity(midTime, sensorPosition, sensorVelocity);
            midFreq = getDopplerFrequency(earthPoint, sensorPosition, sensorVelocity, wavelength);

            if (midFreq * lowerBoundFreq > 0.0) {
                lowerBoundTime = midTime;
                lowerBoundFreq = midFreq;
            } else if (midFreq * upperBoundFreq > 0.0) {
                upperBoundTime = midTime;
                upperBoundFreq = midFreq;
            } else if (Double.compare(midFreq, 0.0) == 0) {
                return midTime;
            }

            diffTime = Math.abs(upperBoundTime - lowerBoundTime);
        }
        /*
        midTime = (upperBoundTime + lowerBoundTime) / 2.0;
        orbit.getPositionVelocity(midTime, sensorPosition, sensorVelocity);
        midFreq = getDopplerFrequency(earthPoint, sensorPosition, sensorVelocity, wavelength);
        final double[] freqArray = {lowerBoundFreq, midFreq, upperBoundFreq};
        final double[][] tmp = {{1, -1, 1}, {1,0,0}, {1,1,1}};
        final Matrix A = new Matrix(tmp);
        final double[] c = Maths.polyFit(A, freqArray);
        double t1 = (-c[1] + Math.sqrt(c[1]*c[1] - 4.0*c[0]*c[2]))/ (2.0*c[2]);
        double t2 = (-c[1] - Math.sqrt(c[1]*c[1] - 4.0*c[0]*c[2]))/ (2.0*c[2]);
        if (t1 >= -1 && t1 <= 1) {
            return 0.5*(1 - t1)*lowerBoundTime + 0.5*(1 + t1)*upperBoundTime;//return t1;
        } else {
            return 0.5*(1 - t2)*lowerBoundTime + 0.5*(1 + t2)*upperBoundTime;//return t2;
        }*/

        double time = lowerBoundTime - lowerBoundFreq * (upperBoundTime - lowerBoundTime) / (upperBoundFreq - lowerBoundFreq);
        return time;
    }

    /**
     * Compute Doppler frequency for given earthPoint and sensor position.
     *
     * @param earthPoint     The earth point in xyz coordinate.
     * @param sensorPosition Array of sensor positions for all range lines.
     * @param sensorVelocity Array of sensor velocities for all range lines.
     * @param wavelength     The radar wavelength.
     * @return The Doppler frequency in Hz.
     */
    private static double getDopplerFrequency(
            final double[] earthPoint, final double[] sensorPosition,
            final double[] sensorVelocity, final double wavelength) {

        final double xDiff = earthPoint[0] - sensorPosition[0];
        final double yDiff = earthPoint[1] - sensorPosition[1];
        final double zDiff = earthPoint[2] - sensorPosition[2];
        final double distance = Math.sqrt(xDiff * xDiff + yDiff * yDiff + zDiff * zDiff);

        return 2.0 * (sensorVelocity[0] * xDiff + sensorVelocity[1] * yDiff + sensorVelocity[2] * zDiff) / (distance * wavelength);
    }

    /**
     * Compute slant range distance for given earth point and given time.
     *
     * @param time       The given time in days.
     * @param orbit      The orbit.
     * @param earthPoint The earth point in xyz coordinate.
     * @param sensorPos  The sensor position.
     * @return The slant range distance in meters.
     */
    public static double computeSlantRange(
            final double time, final SARGeocoding.Orbit orbit, final double[] earthPoint, final double[] sensorPos) {

        final double[] sensorVel = new double[3];
        orbit.getPositionVelocity(time, sensorPos, sensorVel);

        final double xDiff = sensorPos[0] - earthPoint[0];
        final double yDiff = sensorPos[1] - earthPoint[1];
        final double zDiff = sensorPos[2] - earthPoint[2];

        return Math.sqrt(xDiff * xDiff + yDiff * yDiff + zDiff * zDiff);
    }

    /**
     * Compute range index in source image for earth point with given zero Doppler time and slant range.
     *
     * @param zeroDopplerTime The zero Doppler time in MJD.
     * @param slantRange      The slant range in meters.
     * @return The range index.
     */
    public static double computeRangeIndex(
            final boolean srgrFlag, final int sourceImageWidth, final double firstLineUTC, final double lastLineUTC,
            final double rangeSpacing, final double zeroDopplerTime, final double slantRange,
            final double nearEdgeSlantRange, final AbstractMetadata.SRGRCoefficientList[] srgrConvParams) {

        if (zeroDopplerTime < Math.min(firstLineUTC, lastLineUTC) ||
                zeroDopplerTime > Math.max(firstLineUTC, lastLineUTC)) {
            return -1.0;
        }

        if (srgrFlag) { // ground detected image

            double groundRange = 0.0;

            if (srgrConvParams.length == 1) {
                groundRange = computeGroundRange(sourceImageWidth, rangeSpacing, slantRange,
                        srgrConvParams[0].coefficients, srgrConvParams[0].ground_range_origin);
                if (groundRange < 0.0) {
                    return -1.0;
                } else {
                    return (groundRange - srgrConvParams[0].ground_range_origin) / rangeSpacing;
                }
            }

            int idx = 0;
            for (int i = 0; i < srgrConvParams.length && zeroDopplerTime >= srgrConvParams[i].timeMJD; i++) {
                idx = i;
            }

            final double[] srgrCoefficients = new double[srgrConvParams[idx].coefficients.length];
            if (idx == srgrConvParams.length - 1) {
                idx--;
            }

            final double mu = (zeroDopplerTime - srgrConvParams[idx].timeMJD) /
                    (srgrConvParams[idx + 1].timeMJD - srgrConvParams[idx].timeMJD);
            for (int i = 0; i < srgrCoefficients.length; i++) {
                srgrCoefficients[i] = Maths.interpolationLinear(srgrConvParams[idx].coefficients[i],
                        srgrConvParams[idx + 1].coefficients[i], mu);
            }
            groundRange = computeGroundRange(sourceImageWidth, rangeSpacing, slantRange,
                    srgrCoefficients, srgrConvParams[idx].ground_range_origin);
            if (groundRange < 0.0) {
                return -1.0;
            } else {
                return (groundRange - srgrConvParams[idx].ground_range_origin) / rangeSpacing;
            }

        } else { // slant range image

            return (slantRange - nearEdgeSlantRange) / rangeSpacing;
        }
    }

    /**
     * Compute ground range for given slant range.
     *
     * @param sourceImageWidth    The source image width.
     * @param rangeSpacing        The range spacing.
     * @param slantRange          The salnt range in meters.
     * @param srgrCoeff           The SRGR coefficients for converting ground range to slant range.
     *                            Here it is assumed that the polinomial is given by
     *                            c0 + c1*x + c2*x^2 + ... + cn*x^n, where {c0, c1, ..., cn} are the SRGR coefficients.
     * @param ground_range_origin The ground range origin.
     * @return The ground range in meters.
     */
    public static double computeGroundRange(final int sourceImageWidth, final double rangeSpacing,
                                            final double slantRange, final double[] srgrCoeff,
                                            final double ground_range_origin) {

        // binary search is used in finding the ground range for given slant range
        double lowerBound = ground_range_origin;
        double upperBound = ground_range_origin + sourceImageWidth * rangeSpacing;
        final double lowerBoundSlantRange = Maths.computePolynomialValue(lowerBound, srgrCoeff);
        final double upperBoundSlantRange = Maths.computePolynomialValue(upperBound, srgrCoeff);

        if (slantRange < lowerBoundSlantRange || slantRange > upperBoundSlantRange) {
            return -1.0;
        }

        // start binary search
        double midSlantRange;
        while (upperBound - lowerBound > 0.0) {

            final double mid = (lowerBound + upperBound) / 2.0;
            midSlantRange = Maths.computePolynomialValue(mid, srgrCoeff);
            if (Math.abs(midSlantRange - slantRange) < 0.1) {
                return mid;
            } else if (midSlantRange < slantRange) {
                lowerBound = mid;
            } else if (midSlantRange > slantRange) {
                upperBound = mid;
            }
        }

        return -1.0;
    }

    /**
     * Compute projected local incidence angle (in degree).
     *
     * @param lg                               Object holding local geometry information.
     * @param saveLocalIncidenceAngle          Boolean flag indicating saving local incidence angle.
     * @param saveProjectedLocalIncidenceAngle Boolean flag indicating saving projected local incidence angle.
     * @param saveSigmaNought                  Boolean flag indicating applying radiometric calibration.
     * @param x0                               The x coordinate of the pixel at the upper left corner of current tile.
     * @param y0                               The y coordinate of the pixel at the upper left corner of current tile.
     * @param x                                The x coordinate of the current pixel.
     * @param y                                The y coordinate of the current pixel.
     * @param localDEM                         The local DEM.
     * @param localIncidenceAngles             The local incidence angle and projected local incidence angle.
     */
    public static void computeLocalIncidenceAngle(
            final LocalGeometry lg, final double demNoDataValue, final boolean saveLocalIncidenceAngle,
            final boolean saveProjectedLocalIncidenceAngle, final boolean saveSigmaNought, final int x0,
            final int y0, final int x, final int y, final double[][] localDEM, final double[] localIncidenceAngles) {

        // Note: For algorithm and notation of the following implementation, please see Andrea's email dated
        //       May 29, 2009 and Marcus' email dated June 3, 2009, or see Eq (14.10) and Eq (14.11) on page
        //       321 and 323 in "SAR Geocoding - Data and Systems".
        //       The Cartesian coordinate (x, y, z) is represented here by a length-3 array with element[0]
        //       representing x, element[1] representing y and element[2] representing z.

        for (int i = 0; i < 3; i++) {
            final int yy = y - y0 + i;
            for (int j = 0; j < 3; j++) {
                if (localDEM[yy][x - x0 + j] == demNoDataValue) {
                    return;
                }
            }
        }

        final int yy = y - y0;
        final int xx = x - x0;
        final double rightPointHeight = (localDEM[yy][xx + 2] +
                localDEM[yy + 1][xx + 2] +
                localDEM[yy + 2][xx + 2]) / 3.0;

        final double leftPointHeight = (localDEM[yy][xx] +
                localDEM[yy + 1][xx] +
                localDEM[yy + 2][xx]) / 3.0;

        final double upPointHeight = (localDEM[yy][xx] +
                localDEM[yy][xx + 1] +
                localDEM[yy][xx + 2]) / 3.0;

        final double downPointHeight = (localDEM[yy + 2][xx] +
                localDEM[yy + 2][xx + 1] +
                localDEM[yy + 2][xx + 2]) / 3.0;

        final double[] rightPoint = new double[3];
        final double[] leftPoint = new double[3];
        final double[] upPoint = new double[3];
        final double[] downPoint = new double[3];

        GeoUtils.geo2xyzWGS84(lg.rightPointLat, lg.rightPointLon, rightPointHeight, rightPoint);
        GeoUtils.geo2xyzWGS84(lg.leftPointLat, lg.leftPointLon, leftPointHeight, leftPoint);
        GeoUtils.geo2xyzWGS84(lg.upPointLat, lg.upPointLon, upPointHeight, upPoint);
        GeoUtils.geo2xyzWGS84(lg.downPointLat, lg.downPointLon, downPointHeight, downPoint);

        final double[] a = {rightPoint[0] - leftPoint[0], rightPoint[1] - leftPoint[1], rightPoint[2] - leftPoint[2]};
        final double[] b = {downPoint[0] - upPoint[0], downPoint[1] - upPoint[1], downPoint[2] - upPoint[2]};
        final double[] c = {lg.centrePoint[0], lg.centrePoint[1], lg.centrePoint[2]};

        final double[] n = {a[1] * b[2] - a[2] * b[1],
                a[2] * b[0] - a[0] * b[2],
                a[0] * b[1] - a[1] * b[0]}; // ground plane normal

        Maths.normalizeVector(n);
        if (Maths.innerProduct(n, c) < 0) {
            n[0] = -n[0];
            n[1] = -n[1];
            n[2] = -n[2];
        }

        final double[] s = {lg.sensorPos[0] - lg.centrePoint[0],
                lg.sensorPos[1] - lg.centrePoint[1],
                lg.sensorPos[2] - lg.centrePoint[2]};
        Maths.normalizeVector(s);

        if (saveLocalIncidenceAngle) { // local incidence angle
            final double nsInnerProduct = Maths.innerProduct(n, s);
            localIncidenceAngles[0] = FastMath.acos(nsInnerProduct) * Constants.RTOD;
        }

        if (saveProjectedLocalIncidenceAngle || saveSigmaNought) { // projected local incidence angle
            final double[] m = {s[1] * c[2] - s[2] * c[1], s[2] * c[0] - s[0] * c[2], s[0] * c[1] - s[1] * c[0]}; // range plane normal
            Maths.normalizeVector(m);
            final double mnInnerProduct = Maths.innerProduct(m, n);
            final double[] n1 = {n[0] - m[0] * mnInnerProduct, n[1] - m[1] * mnInnerProduct, n[2] - m[2] * mnInnerProduct};
            Maths.normalizeVector(n1);
            localIncidenceAngles[1] = FastMath.acos(Maths.innerProduct(n1, s)) * Constants.RTOD;
        }
    }

    public static void computeLocalIncidenceAngle(
            final LocalGeometry lg, final double demNoDataValue, final boolean saveLocalIncidenceAngle,
            final boolean saveProjectedLocalIncidenceAngle, final boolean saveSigmaNought, final int x0,
            final int y0, final int x, final int y, final double[][] localDEM, final double[] localIncidenceAngles,
            final TileGeoreferencing tileGeoRef, ElevationModel dem) throws Exception {

        // Note: For algorithm and notation of the following implementation, please see Andrea's email dated
        //       May 29, 2009 and Marcus' email dated June 3, 2009, or see Eq (14.10) and Eq (14.11) on page
        //       321 and 323 in "SAR Geocoding - Data and Systems".
        //       The Cartesian coordinate (x, y, z) is represented here by a length-3 array with element[0]
        //       representing x, element[1] representing y and element[2] representing z.
        try {

            final int yy = y - y0;
            final int xx = x - x0;
            final int maxX = localDEM[0].length - 1;
            final int maxY = localDEM.length - 1;
            final int numN = 3;
            final GeoPos geo = new GeoPos();
            double alt;

            double rightPointHeight = 0, leftPointHeight = 0, upPointHeight = 0, downPointHeight = 0;

            int cnt = 0;
            for (int n = 0; n < numN; ++n) {
                if (xx + n > maxX) {
                    tileGeoRef.getGeoPos(xx + n, yy, geo);
                    alt = dem.getElevation(geo);
                } else {
                    alt = localDEM[yy][xx + n];
                }
                if (alt != demNoDataValue) {
                    rightPointHeight += alt;
                    ++cnt;
                }
            }
            if (cnt == 0) return;
            rightPointHeight /= (double) cnt;

            cnt = 0;
            for (int n = 0; n < numN; ++n) {
                if (xx - n < 0) {
                    tileGeoRef.getGeoPos(xx - n, yy, geo);
                    alt = dem.getElevation(geo);
                } else {
                    alt = localDEM[yy][xx - n];
                }
                if (alt != demNoDataValue) {
                    leftPointHeight += alt;
                    ++cnt;
                }
            }
            if (cnt == 0) return;
            leftPointHeight /= (double) cnt;

            cnt = 0;
            for (int n = 0; n < numN; ++n) {
                if (yy - n < 0) {
                    tileGeoRef.getGeoPos(xx, yy - n, geo);
                    alt = dem.getElevation(geo);
                } else {
                    alt = localDEM[yy - n][xx];
                }
                if (alt != demNoDataValue) {
                    upPointHeight += alt;
                    ++cnt;
                }
            }
            if (cnt == 0) return;
            upPointHeight /= (double) cnt;

            cnt = 0;
            for (int n = 0; n < numN; ++n) {
                if (yy + n > maxY) {
                    tileGeoRef.getGeoPos(xx, yy + n, geo);
                    alt = dem.getElevation(geo);
                } else {
                    alt = localDEM[yy + n][xx];
                }
                if (alt != demNoDataValue) {
                    downPointHeight += alt;
                    ++cnt;
                }
            }
            if (cnt == 0) return;
            downPointHeight /= (double) cnt;

            final double[] rightPoint = new double[3];
            final double[] leftPoint = new double[3];
            final double[] upPoint = new double[3];
            final double[] downPoint = new double[3];
            final double[] centrePoint = new double[3];

            GeoUtils.geo2xyzWGS84(lg.rightPointLat, lg.rightPointLon, rightPointHeight, rightPoint);
            GeoUtils.geo2xyzWGS84(lg.leftPointLat, lg.leftPointLon, leftPointHeight, leftPoint);
            GeoUtils.geo2xyzWGS84(lg.upPointLat, lg.upPointLon, upPointHeight, upPoint);
            GeoUtils.geo2xyzWGS84(lg.downPointLat, lg.downPointLon, downPointHeight, downPoint);

            tileGeoRef.getGeoPos(xx, yy, geo);
            final double centerHeight = localDEM[yy][xx];
            GeoUtils.geo2xyzWGS84(geo.getLat(), geo.lon, centerHeight, centrePoint);

            final double[] a = {rightPoint[0] - leftPoint[0], rightPoint[1] - leftPoint[1], rightPoint[2] - leftPoint[2]};
            final double[] b = {downPoint[0] - upPoint[0], downPoint[1] - upPoint[1], downPoint[2] - upPoint[2]};
            //final double[] c = {lg.centrePoint[0], lg.centrePoint[1], lg.centrePoint[2]};
            final double[] c = {centrePoint[0], centrePoint[1], centrePoint[2]};

            final double[] n = {a[1] * b[2] - a[2] * b[1],
                    a[2] * b[0] - a[0] * b[2],
                    a[0] * b[1] - a[1] * b[0]}; // ground plane normal

            Maths.normalizeVector(n);
            if (Maths.innerProduct(n, c) < 0) {
                n[0] = -n[0];
                n[1] = -n[1];
                n[2] = -n[2];
            }

            final double[] s = {lg.sensorPos[0] - centrePoint[0],
                    lg.sensorPos[1] - centrePoint[1],
                    lg.sensorPos[2] - centrePoint[2]};
            Maths.normalizeVector(s);

            if (saveLocalIncidenceAngle) { // local incidence angle
                final double nsInnerProduct = Maths.innerProduct(n, s);
                localIncidenceAngles[0] = FastMath.acos(nsInnerProduct) * Constants.RTOD;
            }

            if (saveProjectedLocalIncidenceAngle || saveSigmaNought) { // projected local incidence angle
                final double[] m = {s[1] * c[2] - s[2] * c[1], s[2] * c[0] - s[0] * c[2], s[0] * c[1] - s[1] * c[0]}; // range plane normal
                Maths.normalizeVector(m);
                final double mnInnerProduct = Maths.innerProduct(m, n);
                final double[] n1 = {n[0] - m[0] * mnInnerProduct, n[1] - m[1] * mnInnerProduct, n[2] - m[2] * mnInnerProduct};
                Maths.normalizeVector(n1);
                localIncidenceAngles[1] = FastMath.acos(Maths.innerProduct(n1, s)) * Constants.RTOD;
            }
        } catch (Exception e) {
            throw e;
        }
    }

    /**
     * Get azimuth pixel spacing (in m).
     *
     * @param srcProduct The source product.
     * @return The azimuth pixel spacing.
     * @throws Exception The exception.
     */
    public static double getAzimuthPixelSpacing(final Product srcProduct) throws Exception {
        final MetadataElement abs = AbstractMetadata.getAbstractedMetadata(srcProduct);
        return AbstractMetadata.getAttributeDouble(abs, AbstractMetadata.azimuth_spacing);
    }

    /**
     * Get range pixel spacing (in m).
     *
     * @param srcProduct The source product.
     * @return The range pixel spacing.
     * @throws Exception The exception.
     */
    public static double getRangePixelSpacing(final Product srcProduct) throws Exception {
        final MetadataElement abs = AbstractMetadata.getAbstractedMetadata(srcProduct);
        final double rangeSpacing = AbstractMetadata.getAttributeDouble(abs, AbstractMetadata.range_spacing);
        final boolean srgrFlag = AbstractMetadata.getAttributeBoolean(abs, AbstractMetadata.srgr_flag);
        if (srgrFlag) {
            return rangeSpacing;
        } else {
            return rangeSpacing / FastMath.sin(getIncidenceAngleAtCentreRangePixel(srcProduct));
        }
    }

    /**
     * Compute pixel spacing (in m).
     *
     * @param srcProduct The source product.
     * @return The pixel spacing.
     * @throws Exception The exception.
     */
    public static double getPixelSpacing(final Product srcProduct) throws Exception {
        final MetadataElement abs = AbstractMetadata.getAbstractedMetadata(srcProduct);
        final double rangeSpacing = AbstractMetadata.getAttributeDouble(abs, AbstractMetadata.range_spacing);
        final double azimuthSpacing = AbstractMetadata.getAttributeDouble(abs, AbstractMetadata.azimuth_spacing);
        final boolean srgrFlag = AbstractMetadata.getAttributeBoolean(abs, AbstractMetadata.srgr_flag);
        if (srgrFlag) {
            return Math.min(rangeSpacing, azimuthSpacing);
        } else {
            return Math.min(rangeSpacing / FastMath.sin(getIncidenceAngleAtCentreRangePixel(srcProduct)), azimuthSpacing);
        }
    }

    /**
     * Get incidence angle at centre range pixel (in radian).
     *
     * @param srcProduct The source product.
     * @return The incidence angle.
     * @throws OperatorException The exceptions.
     */
    private static double getIncidenceAngleAtCentreRangePixel(final Product srcProduct) throws OperatorException {

        final int sourceImageWidth = srcProduct.getSceneRasterWidth();
        final int sourceImageHeight = srcProduct.getSceneRasterHeight();
        final int x = sourceImageWidth / 2;
        final int y = sourceImageHeight / 2;
        final TiePointGrid incidenceAngle = OperatorUtils.getIncidenceAngle(srcProduct);
        if (incidenceAngle == null) {
            throw new OperatorException("incidence_angle tie point grid not found in product");
        }
        return incidenceAngle.getPixelDouble(x, y) * Constants.DTOR;
    }

    /**
     * Compute pixel spacing in degrees.
     *
     * @param pixelSpacingInMeter Pixel spacing in meters.
     * @return The pixel spacing in degrees.
     */
    public static double getPixelSpacingInDegree(final double pixelSpacingInMeter) {
        return pixelSpacingInMeter / Constants.semiMajorAxis * Constants.RTOD;
//        return pixelSpacingInMeter / Constants.MeanEarthRadius * Constants.RTOD;
    }

    /**
     * Compute pixel spacing in meters.
     *
     * @param pixelSpacingInDegree Pixel spacing in degrees.
     * @return The pixel spacing in meters.
     */
    public static double getPixelSpacingInMeter(final double pixelSpacingInDegree) {
        return pixelSpacingInDegree * Constants.semiMinorAxis * Constants.DTOR;
//        return pixelSpacingInDegree * Constants.MeanEarthRadius * Constants.DTOR;
    }

    public static boolean isValidCell(final double rangeIndex, final double azimuthIndex,
                                      final double lat, final double lon,
                                      final TileGeoreferencing tileGeoRef,
                                      final int srcMaxRange, final int srcMaxAzimuth, final double[] sensorPos) {

        if (rangeIndex < 0.0 || rangeIndex >= srcMaxRange || azimuthIndex < 0.0 || azimuthIndex >= srcMaxAzimuth) {
            return false;
        }

        final GeoPos sensorGeoPos = new GeoPos();
        GeoUtils.xyz2geo(sensorPos, sensorGeoPos, GeoUtils.EarthModel.WGS84);
        final double delLatMax = Math.abs(lat - sensorGeoPos.lat);
        double delLonMax;
        if (lon < 0 && sensorGeoPos.lon > 0) {
            delLonMax = Math.min(Math.abs(360 + lon - sensorGeoPos.lon), sensorGeoPos.lon - lon);
        } else if (lon > 0 && sensorGeoPos.lon < 0) {
            delLonMax = Math.min(Math.abs(360 + sensorGeoPos.lon - lon), lon - sensorGeoPos.lon);
        } else {
            delLonMax = Math.abs(lon - sensorGeoPos.lon);
        }

        final GeoPos geoPos = new GeoPos();
        tileGeoRef.getGeoPos(new PixelPos(rangeIndex, azimuthIndex), geoPos);
        final double delLat = Math.abs(lat - geoPos.getLat());
        final double srcLon = geoPos.getLon();

        double delLon;
        if (lon < 0 && srcLon > 0) {
            delLon = Math.min(Math.abs(360 + lon - srcLon), srcLon - lon);
        } else if (lon > 0 && srcLon < 0) {
            delLon = Math.min(Math.abs(360 + srcLon - lon), lon - srcLon);
        } else {
            delLon = Math.abs(lon - srcLon);
        }

        return (delLat + delLon <= delLatMax + delLonMax);
    }

    public static void addLookDirection(final String name, final MetadataElement lookDirectionListElem, final int index,
                                        final int num, final int sourceImageWidth, final int sourceImageHeight,
                                        final double firstLineUTC, final double lineTimeInterval,
                                        final boolean nearRangeOnLeft, final TiePointGrid latitude,
                                        final TiePointGrid longitude) {

        final MetadataElement lookDirectionElem = new MetadataElement(name + index);

        int xHead, xTail, y;
        if (num == 1) {
            y = sourceImageHeight / 2;
        } else if (num > 1) {
            y = (index - 1) * sourceImageHeight / (num - 1);
        } else {
            throw new OperatorException("Invalid number of look directions");
        }

        final double time = firstLineUTC + y * lineTimeInterval;
        lookDirectionElem.setAttributeUTC("time", new ProductData.UTC(time));

        if (nearRangeOnLeft) {
            xHead = sourceImageWidth - 1;
            xTail = 0;
        } else {
            xHead = 0;
            xTail = sourceImageWidth - 1;
        }
        lookDirectionElem.setAttributeDouble("head_lat", latitude.getPixelDouble(xHead, y));
        lookDirectionElem.setAttributeDouble("head_lon", longitude.getPixelDouble(xHead, y));
        lookDirectionElem.setAttributeDouble("tail_lat", latitude.getPixelDouble(xTail, y));
        lookDirectionElem.setAttributeDouble("tail_lon", longitude.getPixelDouble(xTail, y));
        lookDirectionListElem.addElement(lookDirectionElem);
    }


    public static class Orbit {

        public OrbitStateVector[] orbitStateVectors = null;
        public double[][] sensorPosition = null; // sensor position for all range lines
        public double[][] sensorVelocity = null; // sensor velocity for all range lines

        public Orbit(OrbitStateVector[] orbitStateVectors,
                     double firstLineUTC, double lineTimeInterval, int sourceImageHeight) {

            this.orbitStateVectors = new OrbitStateVector[orbitStateVectors.length];
            System.arraycopy(orbitStateVectors, 0, this.orbitStateVectors, 0, orbitStateVectors.length);

            this.sensorPosition = new double[sourceImageHeight][3];
            this.sensorVelocity = new double[sourceImageHeight][3];
            for (int i = 0; i < sourceImageHeight; i++) {
                final double time = firstLineUTC + i * lineTimeInterval;
                getPositionVelocity(time, sensorPosition[i], sensorVelocity[i]);
            }
        }

        public Orbit(OrbitStateVector[] orbitStateVectors) {

            this.orbitStateVectors = new OrbitStateVector[orbitStateVectors.length];
            System.arraycopy(orbitStateVectors, 0, this.orbitStateVectors, 0, orbitStateVectors.length);
        }

        public void getPositionVelocity(final double time, double[] position, double[] velocity) {

            final int[] adjVecIndices = findAdjacentVectors(time);

            final int numVectors = adjVecIndices.length;
            double[] timeArray = new double[numVectors];
            double[] xPosArray = new double[numVectors];
            double[] yPosArray = new double[numVectors];
            double[] zPosArray = new double[numVectors];
            double[] xVelArray = new double[numVectors];
            double[] yVelArray = new double[numVectors];
            double[] zVelArray = new double[numVectors];

            for (int j = 0; j < numVectors; j++) {
                timeArray[j] = orbitStateVectors[adjVecIndices[j]].time_mjd;
                xPosArray[j] = orbitStateVectors[adjVecIndices[j]].x_pos;
                yPosArray[j] = orbitStateVectors[adjVecIndices[j]].y_pos;
                zPosArray[j] = orbitStateVectors[adjVecIndices[j]].z_pos;
                xVelArray[j] = orbitStateVectors[adjVecIndices[j]].x_vel;
                yVelArray[j] = orbitStateVectors[adjVecIndices[j]].y_vel;
                zVelArray[j] = orbitStateVectors[adjVecIndices[j]].z_vel;
            }

            //lagrangeInterpolatingPolynomial
            position[0] = 0;
            position[1] = 0;
            position[2] = 0;
            velocity[0] = 0;
            velocity[1] = 0;
            velocity[2] = 0;
            for (int i = 0; i < numVectors; ++i) {
                double weight = 1;
                for (int j = 0; j < numVectors; ++j) {
                    if (j != i) {
                        weight *= (time - timeArray[j]) / (timeArray[i] - timeArray[j]);
                    }
                }
                position[0] += weight * xPosArray[i];
                position[1] += weight * yPosArray[i];
                position[2] += weight * zPosArray[i];
                velocity[0] += weight * xVelArray[i];
                velocity[1] += weight * yVelArray[i];
                velocity[2] += weight * zVelArray[i];
            }
        }

        public double getVelocity(final double time) {

            final double[] position = new double[3];
            final double[] velocity = new double[3];
            getPositionVelocity(time, position, velocity);
            return Math.sqrt(velocity[0]*velocity[0] + velocity[1]*velocity[1] + velocity[2]*velocity[2]);
        }

        private int[] findAdjacentVectors(final double time) {

            int[] vectorIndices = null;
            final int nv = 8;
            final int totalVectors = orbitStateVectors.length;
            if (totalVectors <= nv) {
                vectorIndices = new int[totalVectors];
                for (int i = 0; i < totalVectors; i++) {
                    vectorIndices[i] = i;
                }
                return vectorIndices;
            }

            int idx = 0;
            for (int i = 0; i < totalVectors; i++) {
                if (time < orbitStateVectors[i].time_mjd) {
                    idx = i - 1;
                    break;
                }
            }

            if (idx == -1) {
                vectorIndices = new int[nv];
                for (int i = 0; i < nv; i++) {
                    vectorIndices[i] = i;
                }
            } else if (idx == 0) {
                vectorIndices = new int[nv];
                for (int i = 0; i < nv; i++) {
                    vectorIndices[i] = totalVectors - nv + i;
                }
            } else {
                final int stIdx = Math.max(idx - nv/2 + 1, 0);
                final int edIdx = Math.min(idx + nv/2, totalVectors - 1);
                vectorIndices = new int[edIdx - stIdx + 1];
                for (int i = 0; i < vectorIndices.length; i++) {
                    vectorIndices[i] = stIdx + i;
                }
            }

            return vectorIndices;
        }
    }

}
