package org.esa.beam.dataio.deimos;

import org.esa.beam.dataio.deimos.dimap.DeimosConstants;
import org.esa.beam.dataio.deimos.dimap.DeimosMetadata;
import org.esa.beam.dataio.readers.GeoTiffBasedReader;
import org.esa.beam.framework.dataio.ProductReaderPlugIn;
import org.esa.beam.framework.datamodel.GeoCoding;
import org.esa.beam.framework.datamodel.Mask;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.TiePointGeoCoding;
import org.esa.beam.framework.datamodel.TiePointGrid;

/**
 * This product reader is intended for reading DEIMOS-1 files
 * from compressed archive files, from tar files or from (uncompressed) file system.
 *
 * @author  Cosmin Cara
 */
public class DeimosProductReader extends GeoTiffBasedReader<DeimosMetadata> {

    protected DeimosProductReader(ProductReaderPlugIn readerPlugIn) {
        super(readerPlugIn);
    }

    @Override
    protected String getMetadataExtension() {
        return DeimosConstants.METADATA_EXTENSION;
    }

    @Override
    protected String getMetadataProfile() {
        if (metadata != null && metadata.size() > 0) {
            return metadata.get(0).getMetadataProfile();
        } else {
            return DeimosConstants.VALUE_NOT_AVAILABLE;
        }
    }

    @Override
    protected String getProductGenericName() {
        if (metadata != null && metadata.size() > 0) {
            return metadata.get(0).getProductName();
        } else {
            return DeimosConstants.VALUE_NOT_AVAILABLE;
        }
    }

    @Override
    protected String getMetadataFileSuffix() {
        return DeimosConstants.METADATA_EXTENSION;
    }

    @Override
    protected String[] getBandNames() {
        if (metadata != null && metadata.size() > 0) {
            return metadata.get(0).getBandNames();
        } else {
            return new String[] { };
        }
    }

    @Override
    protected void addMetadataMasks(Product product, DeimosMetadata componentMetadata) {
        logger.info("Create masks");
        int noDataValue,saturatedValue;
        if ((noDataValue = componentMetadata.getNoDataValue()) >= 0) {
            product.getMaskGroup().add(Mask.BandMathsType.create(DeimosConstants.NODATA_VALUE,
                    DeimosConstants.NODATA_VALUE,
                    product.getSceneRasterWidth(),
                    product.getSceneRasterHeight(),
                    String.valueOf(noDataValue),
                    componentMetadata.getNoDataColor(),
                    0.5));
        }
        if ((saturatedValue = componentMetadata.getSaturatedPixelValue()) >= 0) {
            product.getMaskGroup().add(Mask.BandMathsType.create(DeimosConstants.SATURATED_VALUE,
                    DeimosConstants.SATURATED_VALUE,
                    product.getSceneRasterWidth(),
                    product.getSceneRasterHeight(),
                    String.valueOf(saturatedValue),
                    componentMetadata.getSaturatedColor(),
                    0.5));
        }
    }

    @Override
    protected void addBands(Product product, DeimosMetadata componentMetadata, int componentIndex) {
        super.addBands(product, componentMetadata, componentIndex);
        if (DeimosConstants.PROCESSING_1R.equals(componentMetadata.getProcessingLevel())) {
            initGeoCoding(product);
        }
    }

    private void initGeoCoding(Product product) {
        DeimosMetadata deimosMetadata = metadata.get(0);
        DeimosMetadata.InsertionPoint[] geopositionPoints = deimosMetadata.getGeopositionPoints();
        if (geopositionPoints != null) {
            int numPoints = geopositionPoints.length;
            if (numPoints > 1 && (int)(numPoints / Math.sqrt((double)numPoints)) == numPoints) {
                float stepX = geopositionPoints[1].stepX - geopositionPoints[0].stepX;
                float stepY = geopositionPoints[1].stepY - geopositionPoints[0].stepY;
                float[] latitudes = new float[numPoints];
                float[] longitudes = new float[numPoints];
                for (int i = 0; i < numPoints; i++) {
                    latitudes[i] = geopositionPoints[i].y;
                    longitudes[i] = geopositionPoints[i].x;
                }
                TiePointGrid latGrid = addTiePointGrid(stepX, stepY, product, DeimosConstants.LATITUDE_BAND_NAME, latitudes);
                TiePointGrid lonGrid = addTiePointGrid(stepX, stepY, product, DeimosConstants.LONGITUDE_BAND_NAME, longitudes);
                GeoCoding geoCoding = new TiePointGeoCoding(latGrid, lonGrid);
                product.setGeoCoding(geoCoding);
            }
        }
    }

    private TiePointGrid addTiePointGrid(float subSamplingX, float subSamplingY, Product product, String gridName, float[] tiePoints) {
        int gridDim = (int) Math.sqrt(tiePoints.length);
        final TiePointGrid tiePointGrid = createTiePointGrid(gridName, gridDim, gridDim, 0, 0, subSamplingX, subSamplingY, tiePoints);
        product.addTiePointGrid(tiePointGrid);
        return tiePointGrid;
    }
}
