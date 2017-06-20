package org.esa.s2tbx.fcc.mahalanobis;

import it.unimi.dsi.fastutil.ints.Int2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2FloatOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import org.esa.s2tbx.fcc.ForestCoverChangeOp;
import org.esa.s2tbx.fcc.intern.PixelSourceBands;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by jcoravu on 6/6/2017.
 */
public class MahalanobisDistance {
    private static final Logger logger = Logger.getLogger(MahalanobisDistance.class.getName());

    public static Object2FloatOpenHashMap<PixelSourceBands> computeMahalanobisSquareMatrix(Collection<PixelSourceBands> points) {
        if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, ""); // add an empty line
            logger.log(Level.FINE, "Compute the Mahalanobis distance for " + points.size() + " regions");
        }

        float meanValueB4Band = 0.0f;
        float meanValueB8Band = 0.0f;
        float meanValueB11Band = 0.0f;
        float standardDeviationValueB11Band = 0.0f;
        for (PixelSourceBands point : points) {
            meanValueB4Band += point.getMeanValueB4Band();
            meanValueB8Band += point.getMeanValueB8Band();
            meanValueB11Band += point.getMeanValueB11Band();
            standardDeviationValueB11Band += point.getStandardDeviationValueB8Band();
        }

        int numberOfPoints = points.size();

        meanValueB4Band = meanValueB4Band / (float)numberOfPoints;
        meanValueB8Band = meanValueB8Band / (float)numberOfPoints;
        meanValueB11Band = meanValueB11Band / (float)numberOfPoints;
        standardDeviationValueB11Band = standardDeviationValueB11Band / (float)numberOfPoints;

        if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, ""); // add an empty line
            logger.log(Level.FINE, "The centroid points are : meanValueB4Band="+meanValueB4Band+", meanValueB8Band="+meanValueB8Band+", meanValueB11Band="+meanValueB11Band+", standardDeviationValueB11Band="+standardDeviationValueB11Band+", numberOfPoints="+numberOfPoints);
        }

        Matrix matrix = new Matrix(numberOfPoints, 4);
        Int2ObjectMap<PixelSourceBands> map = new Int2ObjectLinkedOpenHashMap<PixelSourceBands>();
        int index = -1;
        for (PixelSourceBands point : points) {
            index++;
            map.put(index, point);
            matrix.setValueAt(index, 0, (point.getMeanValueB4Band() - meanValueB4Band));
            matrix.setValueAt(index, 1, (point.getMeanValueB8Band() - meanValueB8Band));
            matrix.setValueAt(index, 2, (point.getMeanValueB11Band() - meanValueB11Band));
            matrix.setValueAt(index, 3, (point.getStandardDeviationValueB8Band() - standardDeviationValueB11Band));
        }

        Matrix inverseMatrix = computeInverseMatrix(matrix);
        if (inverseMatrix != null) {
            Matrix squaredMahalanobisMatrix = computeSquaredMahalanobisMatrix(matrix, inverseMatrix);
            Object2FloatOpenHashMap<PixelSourceBands> result = new Object2FloatOpenHashMap<PixelSourceBands>();
            int matrixSize = squaredMahalanobisMatrix.getColumnCount();
            for (int i=0; i<matrixSize; i++) {
                float value = squaredMahalanobisMatrix.getValueAt(i, i);
                PixelSourceBands point = map.get(i);
                result.put(point, (float)Math.sqrt(value));
            }
            return result;
        }
        return null;
    }

    private static Matrix computeSquaredMahalanobisMatrix(Matrix matrix, Matrix inverseMatrix) {
        TransposeMatrix transposeMatrix = new TransposeMatrix(matrix);
        Matrix resultMatrix = MatrixUtils.multiply(matrix, inverseMatrix);
        return MatrixUtils.multiply(resultMatrix, transposeMatrix);
    }

    private static Matrix computeInverseMatrix(Matrix matrix) {
        TransposeMatrix transposeMatrix = new TransposeMatrix(matrix);
        Matrix quadraticMatrix = MatrixUtils.multiply(transposeMatrix, matrix);
        Matrix covarianceMatrix = computeCovariance(quadraticMatrix, matrix.getRowCount());
        return MatrixUtils.inverse(covarianceMatrix);
    }

    private static Matrix computeCovariance(Matrix matrix, int inputRowCount) {
        float value = 1.0f / (float)(inputRowCount-1);
        int rowCount = matrix.getRowCount();
        int columnCount = matrix.getColumnCount();
        Matrix result = new Matrix(rowCount, columnCount);
        for (int i=0; i<rowCount; i++) {
            for (int j=0; j<columnCount; j++) {
                result.setValueAt(i, j, value * matrix.getValueAt(i, j));
            }
        }
        return result;
    }

    public static void main(String args[]) {
        System.out.println("MahalanobisDistance main method");
//        COMPUTE X=t({1,2,3,4,5,6,7,8,9,10}).
//        COMPUTE Y=t({1,2,2,3,3,3,4,4,4,4}).

        List<PixelSourceBands> points = new ArrayList<PixelSourceBands>();
        PixelSourceBands p1 = new PixelSourceBands(1, 1, 0, 0);
        PixelSourceBands p2 = new PixelSourceBands(2, 2, 0, 0);
        PixelSourceBands p3 = new PixelSourceBands(3, 2, 0, 0);
        PixelSourceBands p4 = new PixelSourceBands(4, 3, 0, 0);
        PixelSourceBands p5 = new PixelSourceBands(5, 3, 0, 0);
        PixelSourceBands p6 = new PixelSourceBands(6, 3, 0, 0);
        PixelSourceBands p7 = new PixelSourceBands(7, 4, 0, 0);
        PixelSourceBands p8 = new PixelSourceBands(8, 4, 0, 0);
        PixelSourceBands p9 = new PixelSourceBands(9, 4, 0, 0);
        PixelSourceBands p10 = new PixelSourceBands(10, 4, 0, 0);

        points.add(p1);
        points.add(p2);
        points.add(p3);
        points.add(p4);
        points.add(p5);
        points.add(p6);
        points.add(p7);
        points.add(p8);
        points.add(p9);
        points.add(p10);

        computeMahalanobisSquareMatrix(points);
    }
}
