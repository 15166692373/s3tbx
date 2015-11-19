/*
 *
 * Copyright (C) 2013-2014 Brockmann Consult GmbH (info@brockmann-consult.de)
 * Copyright (C) 2014-2015 CS SI
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 *  This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 *
 */

package org.esa.s2tbx.dataio.s2.l2a;

import https.psd_12_sentinel2_eo_esa_int.dico._1_0.pdgs.dimap.*;
import https.psd_12_sentinel2_eo_esa_int.psd.s2_pdi_level_2a_tile_metadata.Level2A_Tile;
import https.psd_12_sentinel2_eo_esa_int.psd.user_product_level_2a.Level2A_User_Product;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Transformer;
import org.esa.s2tbx.dataio.s2.*;
import org.esa.s2tbx.dataio.s2.filepatterns.S2DatastripDirFilename;
import org.esa.s2tbx.dataio.s2.filepatterns.S2DatastripFilename;
import org.esa.s2tbx.dataio.s2.ortho.filepatterns.S2OrthoDatastripFilename;
import org.esa.snap.core.datamodel.IndexCoding;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;

/**
 * @author opicas-p
 */
public class L2aMetadataProc extends S2MetadataProc {

    public static JAXBContext getJaxbContext() throws JAXBException, FileNotFoundException {
        ClassLoader s2c = Level2A_User_Product.class.getClassLoader();
        return JAXBContext.newInstance(S2MetadataType.L2A, s2c);
    }

    private static String makeSpectralBandImageFileTemplate(String bandFileId) {
        /* Sample :
        MISSION_ID : S2A
        SITECENTRE : MTI_
        CREATIONDATE : 20150813T201603
        ABSOLUTEORBIT : A000734
        TILENUMBER : T32TQR
        RESOLUTION : 10 | 20 | 60
         */
        return String.format("IMG_DATA%sR{{RESOLUTION}}m%s{{MISSION_ID}}_USER_MSI_L2A_TL_{{SITECENTRE}}_{{CREATIONDATE}}_{{ABSOLUTEORBIT}}_{{TILENUMBER}}_%s_{{RESOLUTION}}m.jp2",
                File.separator, File.separator, bandFileId);
    }

    private static String makeAOTFileTemplate() {
        return String.format("IMG_DATA%sR{{RESOLUTION}}m%s{{MISSION_ID}}_USER_AOT_L2A_TL_{{SITECENTRE}}_{{CREATIONDATE}}_{{ABSOLUTEORBIT}}_{{TILENUMBER}}_{{RESOLUTION}}m.jp2",
                File.separator, File.separator);
    }

    private static String makeWVPFileTemplate() {
        return String.format("IMG_DATA%sR{{RESOLUTION}}m%s{{MISSION_ID}}_USER_WVP_L2A_TL_{{SITECENTRE}}_{{CREATIONDATE}}_{{ABSOLUTEORBIT}}_{{TILENUMBER}}_{{RESOLUTION}}m.jp2",
                File.separator, File.separator);
    }

    private static String makeSCLFileTemplate() {
        return String.format("IMG_DATA%s{{MISSION_ID}}_USER_SCL_L2A_TL_{{SITECENTRE}}_{{CREATIONDATE}}_{{ABSOLUTEORBIT}}_{{TILENUMBER}}_{{RESOLUTION}}m.jp2", File.separator);
    }

    private static String makeCLDFileTemplate() {
        return String.format("QI_DATA%s{{MISSION_ID}}_USER_CLD_L2A_TL_{{SITECENTRE}}_{{CREATIONDATE}}_{{ABSOLUTEORBIT}}_{{TILENUMBER}}_{{RESOLUTION}}m.jp2", File.separator);
    }

    private static String makeSNWFileTemplate() {
        return String.format("QI_DATA%s{{MISSION_ID}}_USER_SNW_L2A_TL_{{SITECENTRE}}_{{CREATIONDATE}}_{{ABSOLUTEORBIT}}_{{TILENUMBER}}_{{RESOLUTION}}m.jp2", File.separator);
    }

    public static L2aMetadata.ProductCharacteristics getProductOrganization(Level2A_User_Product product, S2SpatialResolution resolution) {

        L2aMetadata.ProductCharacteristics characteristics = new L2aMetadata.ProductCharacteristics();
        characteristics.setSpacecraft(product.getGeneral_Info().getL2A_Product_Info().getDatatake().getSPACECRAFT_NAME());
        characteristics.setDatasetProductionDate(product.getGeneral_Info().getL2A_Product_Info().getDatatake().getDATATAKE_SENSING_START().toString());
        characteristics.setProcessingLevel(product.getGeneral_Info().getL2A_Product_Info().getPROCESSING_LEVEL().getValue().value());

        List<S2BandInformation> aInfo = new ArrayList<>();

        IndexCoding sclCoding = new IndexCoding("quality_scene_classification");
        sclCoding.addIndex("NODATA", 0, "No data");
        sclCoding.addIndex("SATURATED_DEFECTIVE", 1, "Saturated or defective");
        sclCoding.addIndex("DARK_FEATURE_SHADOW", 2, "Dark feature shadow");
        sclCoding.addIndex("CLOUD_SHADOW", 3, "Cloud shadow");
        sclCoding.addIndex("VEGETATION", 4, "Vegetation");
        sclCoding.addIndex("BARE_SOIL_DESERT", 5, "Bare soil / Desert");
        sclCoding.addIndex("WATER", 6, "Water");
        sclCoding.addIndex("CLOUD_LOW_PROBA", 7, "Cloud (low probability)");
        sclCoding.addIndex("CLOUD_MEDIUM_PROBA", 8, "Cloud (medium probability)");
        sclCoding.addIndex("CLOUD_HIGH_PROBA", 9, "Cloud (high probability)");
        sclCoding.addIndex("THIN_CIRRUS", 10, "Thin cirrus");
        sclCoding.addIndex("SNOW_ICE", 11, "Snow or Ice");

        switch(resolution) {
            case R10M:
                aInfo.add(new S2SpectralInformation("B1", S2SpatialResolution.R60M, makeSpectralBandImageFileTemplate("B01"), 0, 414, 472, 443));
                aInfo.add(new S2SpectralInformation("B2", S2SpatialResolution.R10M, makeSpectralBandImageFileTemplate("B02"), 1, 425, 555, 490));
                aInfo.add(new S2SpectralInformation("B3", S2SpatialResolution.R10M, makeSpectralBandImageFileTemplate("B03"), 2, 510, 610, 560));
                aInfo.add(new S2SpectralInformation("B4", S2SpatialResolution.R10M, makeSpectralBandImageFileTemplate("B04"), 3, 617, 707, 665));
                aInfo.add(new S2SpectralInformation("B5", S2SpatialResolution.R20M, makeSpectralBandImageFileTemplate("B05"), 4, 625, 722, 705));
                aInfo.add(new S2SpectralInformation("B6", S2SpatialResolution.R20M, makeSpectralBandImageFileTemplate("B06"), 5, 720, 760, 740));
                aInfo.add(new S2SpectralInformation("B7", S2SpatialResolution.R20M, makeSpectralBandImageFileTemplate("B07"), 6, 741, 812, 783));
                aInfo.add(new S2SpectralInformation("B8", S2SpatialResolution.R10M, makeSpectralBandImageFileTemplate("B08"), 7, 752, 927, 842));
                aInfo.add(new S2SpectralInformation("B8A", S2SpatialResolution.R20M, makeSpectralBandImageFileTemplate("B8A"), 8, 823, 902, 865));
                aInfo.add(new S2SpectralInformation("B9", S2SpatialResolution.R60M, makeSpectralBandImageFileTemplate("B09"), 9, 903, 982, 945));
                aInfo.add(new S2SpectralInformation("B11", S2SpatialResolution.R20M, makeSpectralBandImageFileTemplate("B11"), 11, 1532, 1704, 1610));
                aInfo.add(new S2SpectralInformation("B12", S2SpatialResolution.R20M, makeSpectralBandImageFileTemplate("B12"), 12, 2035, 2311, 2190));

                aInfo.add(new S2BandInformation("quality_aot", S2SpatialResolution.R10M, makeAOTFileTemplate()));
                aInfo.add(new S2BandInformation("quality_wvp", S2SpatialResolution.R10M, makeWVPFileTemplate()));
                aInfo.add(new S2BandInformation("quality_cloud_confidence", S2SpatialResolution.R20M, makeCLDFileTemplate()));
                aInfo.add(new S2BandInformation("quality_snow_confidence", S2SpatialResolution.R20M, makeSNWFileTemplate()));

                // SCL only generated at 20m and 60m. upsample the 20m version
                aInfo.add(new S2IndexBandInformation("quality_scene_classification", S2SpatialResolution.R20M, makeSCLFileTemplate(), sclCoding));
                break;
            case R20M:
                aInfo.add(new S2SpectralInformation("B1", S2SpatialResolution.R60M, makeSpectralBandImageFileTemplate("B01"), 0, 414, 472, 443));
                aInfo.add(new S2SpectralInformation("B2", S2SpatialResolution.R20M, makeSpectralBandImageFileTemplate("B02"), 1, 425, 555, 490));
                aInfo.add(new S2SpectralInformation("B3", S2SpatialResolution.R20M, makeSpectralBandImageFileTemplate("B03"), 2, 510, 610, 560));
                aInfo.add(new S2SpectralInformation("B4", S2SpatialResolution.R20M, makeSpectralBandImageFileTemplate("B04"), 3, 617, 707, 665));
                aInfo.add(new S2SpectralInformation("B5", S2SpatialResolution.R20M, makeSpectralBandImageFileTemplate("B05"), 4, 625, 722, 705));
                aInfo.add(new S2SpectralInformation("B6", S2SpatialResolution.R20M, makeSpectralBandImageFileTemplate("B06"), 5, 720, 760, 740));
                aInfo.add(new S2SpectralInformation("B7", S2SpatialResolution.R20M, makeSpectralBandImageFileTemplate("B07"), 6, 741, 812, 783));
                aInfo.add(new S2SpectralInformation("B8", S2SpatialResolution.R10M, makeSpectralBandImageFileTemplate("B08"), 7, 752, 927, 842));
                aInfo.add(new S2SpectralInformation("B8A", S2SpatialResolution.R20M, makeSpectralBandImageFileTemplate("B8A"), 8, 823, 902, 865));
                aInfo.add(new S2SpectralInformation("B9", S2SpatialResolution.R60M, makeSpectralBandImageFileTemplate("B09"), 9, 903, 982, 945));
                aInfo.add(new S2SpectralInformation("B11", S2SpatialResolution.R20M, makeSpectralBandImageFileTemplate("B11"), 11, 1532, 1704, 1610));
                aInfo.add(new S2SpectralInformation("B12", S2SpatialResolution.R20M, makeSpectralBandImageFileTemplate("B12"), 12, 2035, 2311, 2190));

                aInfo.add(new S2BandInformation("quality_aot", S2SpatialResolution.R20M, makeAOTFileTemplate()));
                aInfo.add(new S2BandInformation("quality_wvp", S2SpatialResolution.R20M, makeWVPFileTemplate()));
                aInfo.add(new S2BandInformation("quality_cloud_confidence", S2SpatialResolution.R20M, makeCLDFileTemplate()));
                aInfo.add(new S2BandInformation("quality_snow_confidence", S2SpatialResolution.R20M, makeSNWFileTemplate()));

                aInfo.add(new S2IndexBandInformation("quality_scene_classification", S2SpatialResolution.R20M, makeSCLFileTemplate(), sclCoding));
                break;
            case R60M:
                aInfo.add(new S2SpectralInformation("B1", S2SpatialResolution.R60M, makeSpectralBandImageFileTemplate("B01"), 0, 414, 472, 443));
                aInfo.add(new S2SpectralInformation("B2", S2SpatialResolution.R60M, makeSpectralBandImageFileTemplate("B02"), 1, 425, 555, 490));
                aInfo.add(new S2SpectralInformation("B3", S2SpatialResolution.R60M, makeSpectralBandImageFileTemplate("B03"), 2, 510, 610, 560));
                aInfo.add(new S2SpectralInformation("B4", S2SpatialResolution.R60M, makeSpectralBandImageFileTemplate("B04"), 3, 617, 707, 665));
                aInfo.add(new S2SpectralInformation("B5", S2SpatialResolution.R60M, makeSpectralBandImageFileTemplate("B05"), 4, 625, 722, 705));
                aInfo.add(new S2SpectralInformation("B6", S2SpatialResolution.R60M, makeSpectralBandImageFileTemplate("B06"), 5, 720, 760, 740));
                aInfo.add(new S2SpectralInformation("B7", S2SpatialResolution.R60M, makeSpectralBandImageFileTemplate("B07"), 6, 741, 812, 783));
                aInfo.add(new S2SpectralInformation("B8", S2SpatialResolution.R10M, makeSpectralBandImageFileTemplate("B08"), 7, 752, 927, 842));
                aInfo.add(new S2SpectralInformation("B8A", S2SpatialResolution.R60M, makeSpectralBandImageFileTemplate("B8A"), 8, 823, 902, 865));
                aInfo.add(new S2SpectralInformation("B9", S2SpatialResolution.R60M, makeSpectralBandImageFileTemplate("B09"), 9, 903, 982, 945));
                aInfo.add(new S2SpectralInformation("B11", S2SpatialResolution.R60M, makeSpectralBandImageFileTemplate("B11"), 11, 1532, 1704, 1610));
                aInfo.add(new S2SpectralInformation("B12", S2SpatialResolution.R60M, makeSpectralBandImageFileTemplate("B12"), 12, 2035, 2311, 2190));

                aInfo.add(new S2BandInformation("quality_aot", S2SpatialResolution.R60M, makeAOTFileTemplate()));
                aInfo.add(new S2BandInformation("quality_water_vapour", S2SpatialResolution.R60M, makeWVPFileTemplate()));
                aInfo.add(new S2BandInformation("quality_cloud_confidence", S2SpatialResolution.R60M, makeCLDFileTemplate()));
                aInfo.add(new S2BandInformation("quality_snow_confidence", S2SpatialResolution.R60M, makeSNWFileTemplate()));

                aInfo.add(new S2IndexBandInformation("quality_scene_classification", S2SpatialResolution.R60M, makeSCLFileTemplate(), sclCoding));
                break;
        }
        int size = aInfo.size();
        characteristics.setBandInformations(aInfo.toArray(new S2BandInformation[size]));

        return characteristics;
    }

    public static Collection<String> getTiles(Level2A_User_Product product) {
        A_L2A_Product_Info.L2A_Product_Organisation info = product.getGeneral_Info().getL2A_Product_Info().getL2A_Product_Organisation();

        List<A_L2A_Product_Info.L2A_Product_Organisation.Granule_List> aGranuleList = info.getGranule_List();

        Transformer tileSelector = o -> {
            A_L2A_Product_Info.L2A_Product_Organisation.Granule_List ali = (A_L2A_Product_Info.L2A_Product_Organisation.Granule_List) o;
            A_PRODUCT_ORGANIZATION_2A.Granules gr = ali.getGranules();
            return gr.getGranuleIdentifier();
        };

        return CollectionUtils.collect(aGranuleList, tileSelector);
    }

    public static S2DatastripFilename getDatastrip(Level2A_User_Product product) {
        A_L2A_Product_Info.L2A_Product_Organisation info = product.getGeneral_Info().getL2A_Product_Info().getL2A_Product_Organisation();

        String dataStripMetadataFilenameCandidate = info.getGranule_List().get(0).getGranules().getDatastripIdentifier();
        S2DatastripDirFilename dirDatastrip = S2DatastripDirFilename.create(dataStripMetadataFilenameCandidate, null);

        if (dirDatastrip != null) {
            String fileName = dirDatastrip.getFileName(null);
            return S2OrthoDatastripFilename.create(fileName);
        } else {
            return null;
        }
    }

    public static S2DatastripDirFilename getDatastripDir(Level2A_User_Product product) {
        A_L2A_Product_Info.L2A_Product_Organisation info = product.getGeneral_Info().getL2A_Product_Info().getL2A_Product_Organisation();
        String dataStripMetadataFilenameCandidate = info.getGranule_List().get(0).getGranules().getDatastripIdentifier();

        return S2DatastripDirFilename.create(dataStripMetadataFilenameCandidate, null);
    }

    public static Map<S2SpatialResolution, L2aMetadata.TileGeometry> getTileGeometries(Level2A_Tile product) {

        A_GEOMETRIC_INFO_TILE info = product.getGeometric_Info();
        A_GEOMETRIC_INFO_TILE.Tile_Geocoding tgeo = info.getTile_Geocoding();


        List<A_TILE_DESCRIPTION.Geoposition> poss = tgeo.getGeoposition();
        List<A_TILE_DESCRIPTION.Size> sizz = tgeo.getSize();

        Map<S2SpatialResolution, L2aMetadata.TileGeometry> resolutions = new HashMap<>();

        for (A_TILE_DESCRIPTION.Geoposition gpos : poss) {
            S2SpatialResolution resolution = S2SpatialResolution.valueOfResolution(gpos.getResolution());
            L2aMetadata.TileGeometry tgeox = new L2aMetadata.TileGeometry();
            tgeox.setUpperLeftX(gpos.getULX());
            tgeox.setUpperLeftY(gpos.getULY());
            tgeox.setxDim(gpos.getXDIM());
            tgeox.setyDim(gpos.getYDIM());
            resolutions.put(resolution, tgeox);
        }

        for (A_TILE_DESCRIPTION.Size asize : sizz) {
            S2SpatialResolution resolution = S2SpatialResolution.valueOfResolution(asize.getResolution());
            L2aMetadata.TileGeometry tgeox = resolutions.get(resolution);
            tgeox.setNumCols(asize.getNCOLS());
            tgeox.setNumRows(asize.getNROWS());
        }

        return resolutions;
    }

    public static L2aMetadata.AnglesGrid getSunGrid(Level2A_Tile product) {

        A_GEOMETRIC_INFO_TILE.Tile_Angles ang = product.getGeometric_Info().getTile_Angles();
        A_SUN_INCIDENCE_ANGLE_GRID sun = ang.getSun_Angles_Grid();

        int azrows = sun.getAzimuth().getValues_List().getVALUES().size();
        int azcolumns = sun.getAzimuth().getValues_List().getVALUES().get(0).getValue().size();

        int zenrows = sun.getZenith().getValues_List().getVALUES().size();
        int zencolumns = sun.getZenith().getValues_List().getVALUES().size();

        L2aMetadata.AnglesGrid ag = new L2aMetadata.AnglesGrid();
        ag.setAzimuth(new float[azrows][azcolumns]);
        ag.setZenith(new float[zenrows][zencolumns]);

        for (int rowindex = 0; rowindex < azrows; rowindex++) {
            List<Float> azimuths = sun.getAzimuth().getValues_List().getVALUES().get(rowindex).getValue();
            for (int colindex = 0; colindex < azcolumns; colindex++) {
                ag.getAzimuth()[rowindex][colindex] = azimuths.get(colindex);
            }
        }

        for (int rowindex = 0; rowindex < zenrows; rowindex++) {
            List<Float> zeniths = sun.getZenith().getValues_List().getVALUES().get(rowindex).getValue();
            for (int colindex = 0; colindex < zencolumns; colindex++) {
                ag.getZenith()[rowindex][colindex] = zeniths.get(colindex);
            }
        }

        return ag;
    }

    public static L2aMetadata.AnglesGrid[] getAnglesGrid(Level2A_Tile product) {
        A_GEOMETRIC_INFO_TILE.Tile_Angles ang = product.getGeometric_Info().getTile_Angles();
        List<AN_INCIDENCE_ANGLE_GRID> incilist = ang.getViewing_Incidence_Angles_Grids();

        L2aMetadata.AnglesGrid[] darr = new L2aMetadata.AnglesGrid[incilist.size()];
        for (int index = 0; index < incilist.size(); index++) {
            AN_INCIDENCE_ANGLE_GRID angleGrid = incilist.get(index);

            int azrows2 = angleGrid.getAzimuth().getValues_List().getVALUES().size();
            int azcolumns2 = angleGrid.getAzimuth().getValues_List().getVALUES().get(0).getValue().size();

            int zenrows2 = angleGrid.getZenith().getValues_List().getVALUES().size();
            int zencolumns2 = angleGrid.getZenith().getValues_List().getVALUES().get(0).getValue().size();


            L2aMetadata.AnglesGrid ag2 = new L2aMetadata.AnglesGrid();
            ag2.setAzimuth(new float[azrows2][azcolumns2]);
            ag2.setZenith(new float[zenrows2][zencolumns2]);

            for (int rowindex = 0; rowindex < azrows2; rowindex++) {
                List<Float> azimuths = angleGrid.getAzimuth().getValues_List().getVALUES().get(rowindex).getValue();
                for (int colindex = 0; colindex < azcolumns2; colindex++) {
                    ag2.getAzimuth()[rowindex][colindex] = azimuths.get(colindex);
                }
            }

            for (int rowindex = 0; rowindex < zenrows2; rowindex++) {
                List<Float> zeniths = angleGrid.getZenith().getValues_List().getVALUES().get(rowindex).getValue();
                for (int colindex = 0; colindex < zencolumns2; colindex++) {
                    ag2.getZenith()[rowindex][colindex] = zeniths.get(colindex);
                }
            }

            ag2.setBandId(Integer.parseInt(angleGrid.getBandId()));
            ag2.setDetectorId(Integer.parseInt(angleGrid.getDetectorId()));
            darr[index] = ag2;
        }

        return darr;
    }

    public static S2Metadata.MaskFilename[] getMasks(Level2A_Tile aTile, File file) {
        A_QUALITY_INDICATORS_INFO_TILE_L2A qualityInfo = aTile.getQuality_Indicators_Info();

        S2Metadata.MaskFilename[] maskFileNamesArray = null;
        if (qualityInfo != null) {
            List<A_MASK_LIST.MASK_FILENAME> masks = aTile.getQuality_Indicators_Info().getL1C_Pixel_Level_QI().getMASK_FILENAME();
            List<L2aMetadata.MaskFilename> aMaskList = new ArrayList<>();
            for (A_MASK_LIST.MASK_FILENAME filename : masks) {
                File QIData = new File(file.getParent(), "QI_DATA");
                File GmlData = new File(QIData, filename.getValue());
                aMaskList.add(new L2aMetadata.MaskFilename(filename.getBandId(), filename.getType(), GmlData));
            }

            maskFileNamesArray = aMaskList.toArray(new L2aMetadata.MaskFilename[aMaskList.size()]);
        }
        return maskFileNamesArray;
    }
}
