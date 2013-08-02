package org.esa.beam.dataio.s2.update;

import jp2.Box;
import jp2.BoxReader;
import org.esa.beam.framework.dataio.ProductReaderPlugIn;
import org.esa.beam.framework.datamodel.Product;

import javax.imageio.stream.FileImageInputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;

import static jp2.BoxType.encode4b;

/**
 * Represents information of a Sentinel 2 band
 *
 * @author Tonio Fincke
 * @author Norman Fomferra
 */
public class S2L1CProductReader extends S2ProductReader {

    static S2SpatialResolution[] resolutions = {S2SpatialResolution.R60M, S2SpatialResolution.R10M,
            S2SpatialResolution.R10M, S2SpatialResolution.R10M, S2SpatialResolution.R20M, S2SpatialResolution.R20M,
            S2SpatialResolution.R20M, S2SpatialResolution.R10M, S2SpatialResolution.R20M, S2SpatialResolution.R60M,
            S2SpatialResolution.R60M, S2SpatialResolution.R20M, S2SpatialResolution.R20M};

    static final String productType = "L1C";

    /**
     * Constructs a new abstract product reader.
     *
     * @param readerPlugIn the reader plug-in which created this reader, can be <code>null</code> for internal reader
     *                     implementations
     */
    protected S2L1CProductReader(ProductReaderPlugIn readerPlugIn) {
        super(readerPlugIn);
    }

    public BandInfo getBandInfo(File file, Matcher matcher, String tileIndex, String bandName) {
        //todo read width and height from jpeg file
        try {
            final FileImageInputStream inputStream = new FileImageInputStream(file);
            final BoxReader boxReader = new BoxReader(inputStream, file.length(), new MyListener());
            List<Box> boxes = new ArrayList<Box>();
            Box box;
            while((box = boxReader.readBox()) != null) {
                boxes.add(box);
            }
            boxes = null;
        } catch (IOException e) {
            e.printStackTrace();
        }
        S2WavebandInfo wavebandInfo = S2WaveBandInfoProvider.getWaveBandInfo(bandName);
        return new BandInfo(tileIndex, file, bandName, wavebandInfo, resolutions[wavebandInfo.bandId], false);
    }

    @Override
    public String getProductType() {
        return productType;
    }

    @Override
    public void readMasks(Product product, String granulePath) {
        // do nothing...not yet, at least
    }

    @Override
    public Product readProductNodes(File metadataFile) throws IOException {
        //todo read metadata
        final String parentDirectory = metadataFile.getParent();
        final File granuleDirectory = new File(parentDirectory + "/GRANULE");
        final File[] granules = granuleDirectory.listFiles();
        String productName;
        if(S2Config.METADATA_NAME_1C_PATTERN.matcher(metadataFile.getName()).matches() ||
                S2Config.METADATA_NAME_1C_PATTERN_ALT.matcher(metadataFile.getName()).matches()) {
            productName = createProductNameFromValidMetadataName(metadataFile.getName());
        } else {
            productName = metadataFile.getParentFile().getName();
        }
        if (granules != null) {
            if (granules.length > 1) {
                //todo how to align multiple tiles -> tile consolidation?
            } else if (granules.length == 1) {
                return readSingleTile(granules[0], productName);
//            final Matcher matcher = metadataName1CPattern.matcher(metadataFile.getName());
//            if (matcher.matches()) {
//                final String regex = matcher.group(1) + "_" + matcher.group(2) + "_([A-Z]{3})_" + matcher.group(4)
//                        + "_TL_([A-Z0-9]{4})" + matcher.group(6) + "T" + matcher.group(7) + "_" + matcher.group(5) +
//                        "_[0-9]{2}[A-Z]{3}";
//                final Pattern directoryPattern = Pattern.compile(regex);
//                for (File granule : granules) {
//                    if (granule.isDirectory() && directoryPattern.matcher(granule.getName()).matches()) {
//                        File tileMetadataFile = new File(granule.getPath() + "\\" + granule.getName() + ".xml");
//                        L1cMetadata tileMetadata;
//                        try {
//                            tileMetadata = L1cMetadata.parseHeader(tileMetadataFile);
//                        } catch (JDOMException e) {
//                            e.printStackTrace();
//                        }
//                    }
//                }
//            }
            }
        }
        return null;
    }

    private static class MyListener implements BoxReader.Listener {
        @Override
        public void knownBoxSeen(Box box) {
            System.out.println("known box: " + encode4b(box.getCode()));
        }

        @Override
        public void unknownBoxSeen(Box box) {
            System.out.println("unknown box: " + encode4b(box.getCode()));
        }
    }

}
