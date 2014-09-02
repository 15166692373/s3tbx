package org.esa.beam.dataio.s3.util;

import org.esa.beam.dataio.netcdf.util.Constants;
import org.esa.beam.dataio.netcdf.util.DataTypeUtils;
import org.esa.beam.dataio.netcdf.util.NetcdfFileOpener;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.FlagCoding;
import org.esa.beam.framework.datamodel.IndexCoding;
import org.esa.beam.framework.datamodel.MetadataAttribute;
import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.SampleCoding;
import org.esa.beam.framework.datamodel.VirtualBand;
import org.esa.beam.util.io.FileUtils;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * @author Tonio Fincke
 */
public class S3NetcdfReader {

    private static final String product_type = "product_type";
    private static final String flag_values = "flag_values";
    private static final String flag_masks = "flag_masks";
    private static final String flag_meanings = "flag_meanings";
    private static final String fillValue = "_FillValue";
    private final NetcdfFile netcdfFile;
    private final String pathToFile;

    public S3NetcdfReader(String pathToFile) throws IOException {
        netcdfFile = NetcdfFileOpener.open(pathToFile);
        this.pathToFile = pathToFile;
    }

    public Product readProduct() throws IOException {
        final String productType = readProductType();
        int productWidth = getWidth();
        int productHeight = getHeight();
        final File file = new File(pathToFile);

        final Product product = new Product(FileUtils.getFilenameWithoutExtension(file), productType, productWidth, productHeight);
        product.setFileLocation(file);
        addGlobalMetadata(product);
        addBands(product);
        addGeoCoding(product);
        for (final Band band : product.getBands()) {
            if (band instanceof VirtualBand) {
                continue;
            }
            band.setSourceImage(createSourceImage(band));
        }
        return product;
    }

    protected void addGeoCoding(Product product) {

    }

    protected String[] getSeparatingThirdDimensions() {
        return new String[0];
    }

    protected String[] getSuffixesForSeparatingThirdDimensions() {
        return new String[0];
    }

    protected String[][] getRowColumnNamePairs() {
        return new String[][]{{"rows", "columns"}, {"tie_rows", "tie_columns"}};
    }

    protected RenderedImage createSourceImage(Band band) {
        final String bandName = band.getName();
        String variableName = bandName;
        if (variableName.endsWith("_lsb")) {
            variableName = variableName.substring(0, variableName.indexOf("_lsb"));
        } else if (variableName.endsWith("_msb")) {
            variableName = variableName.substring(0, variableName.indexOf("_msb"));
        }
        Variable variable = null;
        int dimensionIndex = -1;
        String dimensionName = "";
        final String[] separatingDimensions = getSeparatingThirdDimensions();
        final String[] suffixesForSeparatingThirdDimensions = getSuffixesForSeparatingThirdDimensions();
        for (int i = 0; i < separatingDimensions.length; i++) {
            final String dimension = separatingDimensions[i];
            final String suffix = suffixesForSeparatingThirdDimensions[i];
            if (bandName.contains(suffix)) {
                variableName = bandName.substring(0, variableName.indexOf(suffix) - 1);
                variable = netcdfFile.findVariable(variableName);
                dimensionName = dimension;
                dimensionIndex = Integer.parseInt(bandName.substring(bandName.lastIndexOf("_") + 1)) - 1;
            }
        }
        if (variable == null) {
            variable = netcdfFile.findVariable(variableName);
        }
        return createImage(band, variable, dimensionName, dimensionIndex);
    }

    protected RenderedImage createImage(Band band, Variable variable, String dimensionName, int dimensionIndex) {
        return new S3MultiLevelOpImage(band, variable, dimensionName, dimensionIndex, false);
    }

    private void addGlobalMetadata(Product product) {
        final MetadataElement globalAttributesElement = new MetadataElement("Global_Attributes");
        final List<Attribute> globalAttributes = netcdfFile.getGlobalAttributes();
        for (final Attribute attribute : globalAttributes) {
            int type = DataTypeUtils.getEquivalentProductDataType(attribute.getDataType(), false, false);
            final ProductData attributeData = getAttributeData(attribute, type);
            final MetadataAttribute metadataAttribute =
                    new MetadataAttribute(attribute.getFullName(), attributeData, true);
            globalAttributesElement.addAttribute(metadataAttribute);
        }
        product.getMetadataRoot().addElement(globalAttributesElement);
        final MetadataElement variableAttributesElement = new MetadataElement("Variable_Attributes");
        product.getMetadataRoot().addElement(variableAttributesElement);
    }

    protected void addBands(Product product) {
        final List<Variable> variables = netcdfFile.getVariables();
        for (final Variable variable : variables) {
            final String[][] rowColumnNamePairs = getRowColumnNamePairs();
            for (String[] rowColumnNamePair : rowColumnNamePairs) {
                if (variable.findDimensionIndex(rowColumnNamePair[0]) != -1 &&
                        variable.findDimensionIndex(rowColumnNamePair[1]) != -1) {
                    final String variableName = variable.getFullName();
                    final String[] dimensions = getSeparatingThirdDimensions();
                    final String[] suffixes = getSuffixesForSeparatingThirdDimensions();
                    boolean variableHasBeenAdded = false;
                    for (int i = 0; i < dimensions.length; i++) {
                        String dimensionName = dimensions[i];
                        if (variable.findDimensionIndex(dimensionName) != -1) {
                            final Dimension dimension =
                                    variable.getDimension(variable.findDimensionIndex(dimensionName));
                            for (int j = 0; j < dimension.getLength(); j++) {
                                addVariableAsBand(product, variable, variableName + "_" + suffixes[i] + "_" + (j + 1), false);
                            }
                            variableHasBeenAdded = true;
                            break;
                        }
                    }
                    if (!variableHasBeenAdded) {
                        addVariableAsBand(product, variable, variableName, false);
                    }
                }
            }
            addVariableMetadata(variable, product);
        }
    }

    private static int getRasterDataType(Variable variable) {
        int rasterDataType = DataTypeUtils.getRasterDataType(variable);
        if (rasterDataType == -1 && variable.getDataType() == DataType.LONG) {
            rasterDataType = variable.isUnsigned() ? ProductData.TYPE_UINT32 : ProductData.TYPE_INT32;
        }
        return rasterDataType;
    }

    protected void addVariableAsBand(Product product, Variable variable, String variableName, boolean synthetic) {
        int type = getRasterDataType(variable);
        if (variable.getDataType() == DataType.LONG) {
            final Band lowerBand = product.addBand(variableName + "_lsb", type);
            lowerBand.setDescription(variable.getDescription() + "(least significant bytes)");
            lowerBand.setUnit(variable.getUnitsString());
            lowerBand.setScalingFactor(getScalingFactor(variable));
            lowerBand.setScalingOffset(getAddOffset(variable));
            lowerBand.setSynthetic(synthetic);
            addFillValue(lowerBand, variable);
            addSampleCodings(product, lowerBand, variable, false);
            final Band upperBand = product.addBand(variableName + "_msb", type);
            upperBand.setDescription(variable.getDescription() + "(most significant bytes)");
            upperBand.setUnit(variable.getUnitsString());
            upperBand.setScalingFactor(getScalingFactor(variable));
            upperBand.setScalingOffset(getAddOffset(variable));
            upperBand.setSynthetic(synthetic);
            addFillValue(upperBand, variable);
            addSampleCodings(product, upperBand, variable, true);
        } else {
            final Band band = product.addBand(variableName, type);
            band.setDescription(variable.getDescription());
            band.setUnit(variable.getUnitsString());
            band.setScalingFactor(getScalingFactor(variable));
            band.setScalingOffset(getAddOffset(variable));
            band.setSynthetic(synthetic);
            addFillValue(band, variable);
            addSampleCodings(product, band, variable, false);
        }
    }

    private void addFillValue(Band band, Variable variable) {
        final Attribute fillValueAttribute = variable.findAttribute(fillValue);
        if (fillValueAttribute != null) {
            //todo double is not always correct
            band.setNoDataValue(fillValueAttribute.getNumericValue().doubleValue());
            band.setNoDataValueUsed(true);
        }
    }

    private void addSampleCodings(Product product, Band band, Variable variable, boolean msb) {
        final Attribute flagValuesAttribute = variable.findAttribute(flag_values);
        final Attribute flagMasksAttribute = variable.findAttribute(flag_masks);
        final Attribute flagMeaningsAttribute = variable.findAttribute(flag_meanings);
        if (flagValuesAttribute != null && flagMasksAttribute != null) {
            final FlagCoding flagCoding =
                    getFlagCoding(product, band.getName(), flagMeaningsAttribute, flagValuesAttribute,
                                  flagMasksAttribute, msb);
            band.setSampleCoding(flagCoding);
        } else if (flagValuesAttribute != null) {
            final IndexCoding indexCoding =
                    getIndexCoding(product, band.getName(), flagMeaningsAttribute, flagValuesAttribute, msb);
            band.setSampleCoding(indexCoding);
        } else if (flagMasksAttribute != null) {
            final FlagCoding flagCoding =
                    getFlagCoding(product, band.getName(), flagMeaningsAttribute, flagMasksAttribute, msb);
            band.setSampleCoding(flagCoding);
        }
    }

    private IndexCoding getIndexCoding(Product product, String indexCodingName, Attribute flagMeaningsAttribute,
                                       Attribute flagValuesAttribute, boolean msb) {
        final IndexCoding indexCoding = new IndexCoding(indexCodingName);
        addSamples(indexCoding, flagMeaningsAttribute, flagValuesAttribute, msb);
        if (!product.getIndexCodingGroup().contains(indexCodingName)) {
            product.getIndexCodingGroup().add(indexCoding);
        }
        return indexCoding;
    }

    private FlagCoding getFlagCoding(Product product, String flagCodingName, Attribute flagMeaningsAttribute,
                                     Attribute flagMasksAttribute, boolean msb) {
        final FlagCoding flagCoding = new FlagCoding(flagCodingName);
        addSamples(flagCoding, flagMeaningsAttribute, flagMasksAttribute, msb);
        if (!product.getFlagCodingGroup().contains(flagCodingName)) {
            product.getFlagCodingGroup().add(flagCoding);
        }
        return flagCoding;
    }

    private FlagCoding getFlagCoding(Product product, String flagCodingName, Attribute flagMeaningsAttribute,
                                     Attribute flagValuesAttribute, Attribute flagMasksAttribute, boolean msb) {
        final FlagCoding flagCoding = new FlagCoding(flagCodingName);
        addSamples(flagCoding, flagMeaningsAttribute, flagValuesAttribute, flagMasksAttribute, msb);
        if (!product.getFlagCodingGroup().contains(flagCodingName)) {
            product.getFlagCodingGroup().add(flagCoding);
        }
        return flagCoding;
    }

    private static void addSamples(SampleCoding sampleCoding, Attribute sampleMeanings, Attribute sampleValues,
                                   boolean msb) {
        final String[] meanings = getSampleMeanings(sampleMeanings);
        final int sampleCount = Math.min(meanings.length, sampleValues.getLength());
        for (int i = 0; i < sampleCount; i++) {
            final String sampleName = replaceNonWordCharacters(meanings[i]);
            switch (sampleValues.getDataType()) {
                case BYTE:
                    sampleCoding.addSample(sampleName,
                                            DataType.unsignedByteToShort(sampleValues.getNumericValue(i).byteValue()),
                                            null);
                    break;
                case SHORT:
                    sampleCoding.addSample(sampleName,
                                           DataType.unsignedShortToInt(sampleValues.getNumericValue(i).shortValue()), null);
                    break;
                case INT:
                    sampleCoding.addSample(sampleName, sampleValues.getNumericValue(i).intValue(), null);
                    break;
                case LONG:
                    final long longValue = sampleValues.getNumericValue(i).longValue();
                    if (msb) {
                        long shiftedValue = longValue >>> 32;
                        if (shiftedValue > 0) {
                            sampleCoding.addSample(sampleName, (int) shiftedValue, null);
                        }
                    } else {
                        long shiftedValue = longValue & 0x00000000FFFFFFFFL;
                        if (shiftedValue > 0 || longValue == 0L) {
                            sampleCoding.addSample(sampleName, (int) shiftedValue, null);
                        }
                    }
                    break;
            }
        }
    }

    private static void addSamples(SampleCoding sampleCoding, Attribute sampleMeanings, Attribute sampleValues,
                                   Attribute sampleMasks, boolean msb) {
        final String[] meanings = getSampleMeanings(sampleMeanings);
        final int sampleCount = Math.min(meanings.length, sampleMasks.getLength());
        for (int i = 0; i < sampleCount; i++) {
            final String sampleName = replaceNonWordCharacters(meanings[i]);
            switch (sampleMasks.getDataType()) {
                case BYTE:
                    int[] byteValues = {DataType.unsignedByteToShort(sampleMasks.getNumericValue(i).byteValue()),
                            DataType.unsignedByteToShort(sampleValues.getNumericValue(i).byteValue())};
                    if(byteValues[0] == byteValues[1]) {
                        sampleCoding.addSample(sampleName, byteValues[0], null);
                    } else {
                        sampleCoding.addSamples(sampleName, byteValues, null);
                    }
                    break;
                case SHORT:
                    int[] shortValues = {DataType.unsignedShortToInt(sampleMasks.getNumericValue(i).shortValue()),
                            DataType.unsignedShortToInt(sampleValues.getNumericValue(i).shortValue())};
                    if(shortValues[0] == shortValues[1]) {
                        sampleCoding.addSample(sampleName, shortValues[0], null);
                    } else {
                        sampleCoding.addSamples(sampleName, shortValues, null);
                    }
                    break;
                case INT:
                    int[] intValues = {sampleMasks.getNumericValue(i).intValue(),
                            sampleValues.getNumericValue(i).intValue()};
                    if(intValues[0] == intValues[1]) {
                        sampleCoding.addSample(sampleName, intValues[0], null);
                    } else {
                        sampleCoding.addSamples(sampleName, intValues, null);
                    }
                    sampleCoding.addSamples(sampleName, intValues, null);
                    break;
                case LONG:
                    long[] longValues = {sampleMasks.getNumericValue(i).longValue(),
                            sampleValues.getNumericValue(i).longValue()};
                    if (msb) {
                        int[] intLongValues =
                                {(int)(longValues[0] >>> 32), (int)(longValues[1] >>> 32)};
                        if (longValues[0] > 0) {
                            if(intLongValues[0] == intLongValues[1]) {
                                sampleCoding.addSample(sampleName, intLongValues[0], null);
                            } else {
                                sampleCoding.addSamples(sampleName, intLongValues, null);
                            }
                        }
                    } else {
                        int[] intLongValues =
                                {(int)(longValues[0] & 0x00000000FFFFFFFFL), (int)(longValues[1] & 0x00000000FFFFFFFFL)};
                        if (intLongValues[0] > 0 || longValues[0] == 0L) {
                            if(intLongValues[0] == intLongValues[1]) {
                                sampleCoding.addSample(sampleName, intLongValues[0], null);
                            } else {
                                sampleCoding.addSamples(sampleName, intLongValues, null);
                            }
                        }
                    }
                    break;
            }
        }
    }

    private static String[] getSampleMeanings(Attribute sampleMeanings) {
        final int sampleMeaningsCount = sampleMeanings.getLength();
        if (sampleMeaningsCount == 0) {
            return new String[sampleMeaningsCount];
        }
        if (sampleMeaningsCount > 1) {
            // handle a common misunderstanding of CF conventions, where flag meanings are stored as array of strings
            final String[] strings = new String[sampleMeaningsCount];
            for (int i = 0; i < strings.length; i++) {
                strings[i] = sampleMeanings.getStringValue(i);
            }
            return strings;
        }
        return sampleMeanings.getStringValue().split(" ");
    }

    static String replaceNonWordCharacters(String flagName) {
        return flagName.replaceAll("\\W+", "_");
    }

    private static double getScalingFactor(Variable variable) {
        Attribute attribute = variable.findAttribute(Constants.SCALE_FACTOR_ATT_NAME);
        if (attribute == null) {
            attribute = variable.findAttribute(Constants.SLOPE_ATT_NAME);
        }
        if (attribute == null) {
            attribute = variable.findAttribute("scaling_factor");
        }
        if (attribute != null) {
            return getAttributeValue(attribute).doubleValue();
        }
        return 1.0;
    }

    private static double getAddOffset(Variable variable) {
        Attribute attribute = variable.findAttribute(Constants.ADD_OFFSET_ATT_NAME);
        if (attribute == null) {
            attribute = variable.findAttribute(Constants.INTERCEPT_ATT_NAME);
        }
        if (attribute != null) {
            return getAttributeValue(attribute).doubleValue();
        }
        return 0.0;
    }

    private static Number getAttributeValue(Attribute attribute) {
        if (attribute.isString()) {
            String stringValue = attribute.getStringValue();
            if (stringValue.endsWith("b")) {
                // Special management for bytes; Can occur in e.g. ASCAT files from EUMETSAT
                return Byte.parseByte(stringValue.substring(0, stringValue.length() - 1));
            } else {
                return Double.parseDouble(stringValue);
            }
        } else {
            return attribute.getNumericValue();
        }
    }

    protected void addVariableMetadata(Variable variable, Product product) {
        final MetadataElement variableElement = new MetadataElement(variable.getFullName());
        final List<Attribute> attributes = variable.getAttributes();
        for (Attribute attribute : attributes) {
            if (attribute.getFullName().equals("flag_meanings")) {
                final String[] flagMeanings = attribute.getStringValue().split(" ");
                for (int i = 0; i < flagMeanings.length; i++) {
                    String flagMeaning = flagMeanings[i];
                    final ProductData attributeData = ProductData.createInstance(flagMeaning);
                    final MetadataAttribute metadataAttribute =
                            new MetadataAttribute(attribute.getFullName() + "." + i, attributeData, true);
                    variableElement.addAttribute(metadataAttribute);
                }
            } else {
                int type = DataTypeUtils.getEquivalentProductDataType(attribute.getDataType(), false, false);
                if (type == -1 && attribute.getDataType() == DataType.LONG) {
                    type = variable.isUnsigned() ? ProductData.TYPE_UINT32 : ProductData.TYPE_INT32;
                }
                final ProductData attributeData = getAttributeData(attribute, type);
                final MetadataAttribute metadataAttribute =
                        new MetadataAttribute(attribute.getFullName(), attributeData, true);
                variableElement.addAttribute(metadataAttribute);
            }
        }
        product.getMetadataRoot().getElement("Variable_Attributes").addElement(variableElement);
    }

    protected ProductData getAttributeData(Attribute attribute, int type) {
        final Array attributeValues = attribute.getValues();
        ProductData productData = null;
        switch (type) {
            case ProductData.TYPE_ASCII: {
                productData = ProductData.createInstance(attributeValues.toString());
                break;
            }
            case ProductData.TYPE_INT8: {
                productData = ProductData.createInstance((byte[]) attributeValues.copyTo1DJavaArray());
                break;
            }
            case ProductData.TYPE_INT16: {
                productData = ProductData.createInstance((short[]) attributeValues.copyTo1DJavaArray());
                break;
            }
            case ProductData.TYPE_INT32: {
                Object array = attributeValues.copyTo1DJavaArray();
                if (array instanceof long[]) {
                    long[] longArray = (long[]) array;
                    int[] newArray = new int[longArray.length];
                    for (int i = 0; i < longArray.length; i++) {
                        newArray[i] = (int) longArray[i];
                    }
                    array = newArray;
                }
                productData = ProductData.createInstance((int[]) array);
                break;
            }
            case ProductData.TYPE_UINT32: {
                Object array = attributeValues.copyTo1DJavaArray();
                if (array instanceof long[]) {
                    long[] longArray = (long[]) array;
                    int[] newArray = new int[longArray.length];
                    for (int i = 0; i < longArray.length; i++) {
                        newArray[i] = (int) longArray[i];
                    }
                    array = newArray;
                }
                productData = ProductData.createInstance((int[]) array);
                break;
            }
            case ProductData.TYPE_FLOAT32: {
                productData = ProductData.createInstance((float[]) attributeValues.copyTo1DJavaArray());
                break;
            }
            case ProductData.TYPE_FLOAT64: {
                productData = ProductData.createInstance((double[]) attributeValues.copyTo1DJavaArray());
                break;
            }
            default: {
                break;
            }
        }
        return productData;
    }

    int getWidth() {
        final String[][] rowColumnNamePairs = getRowColumnNamePairs();
        for (String[] rowColumnNamePair : rowColumnNamePairs) {
            final Dimension widthDimension = netcdfFile.findDimension(rowColumnNamePair[1]);
            if (widthDimension != null) {
                return widthDimension.getLength();
            }
        }
        return 0;
    }

    int getHeight() {
        final String[][] rowColumnNamePairs = getRowColumnNamePairs();
        for (String[] rowColumnNamePair : rowColumnNamePairs) {
            final Dimension heightDimension = netcdfFile.findDimension(rowColumnNamePair[0]);
            if (heightDimension != null) {
                return heightDimension.getLength();
            }
        }
        return 0;
    }

    String readProductType() {
        Attribute typeAttribute = netcdfFile.findGlobalAttribute(product_type);
        String productType;
        if (typeAttribute != null) {
            productType = typeAttribute.getStringValue();
            if (productType != null && productType.trim().length() > 0) {
                productType = productType.trim();
            }
        } else {
            typeAttribute = netcdfFile.findGlobalAttribute("Conventions");
            if (typeAttribute != null) {
                productType = typeAttribute.getStringValue();
            } else {
                productType = Constants.FORMAT_NAME;
            }
        }
        return productType;
    }

    protected NetcdfFile getNetcdfFile() {
        return netcdfFile;
    }

}
