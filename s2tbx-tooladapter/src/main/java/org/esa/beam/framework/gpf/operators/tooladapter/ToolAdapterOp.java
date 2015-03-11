package org.esa.beam.framework.gpf.operators.tooladapter;

import com.bc.ceres.binding.Property;
import com.bc.ceres.core.ProgressMonitor;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.app.VelocityEngine;
import org.esa.beam.framework.dataio.ProductIO;
import org.esa.beam.framework.dataio.ProductIOPlugInManager;
import org.esa.beam.framework.dataio.ProductWriterPlugIn;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.GPF;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.descriptor.ToolAdapterOperatorDescriptor;
import org.esa.beam.framework.gpf.internal.OperatorContext;
import org.esa.beam.jai.ImageManager;
import org.esa.beam.util.logging.BeamLogManager;
import org.esa.beam.utils.CollectionHelper;
import org.esa.beam.utils.PrivilegedAccessor;

import java.io.*;
import java.util.*;
import java.util.logging.Level;

/**
 * Tool Adapter operator
 *
 * @author Lucian Barbulescu
 */
@OperatorMetadata(alias = "ToolAdapterOp",
        category = "Tools",
        version = "1.0",
        description = "Tool Adapter Operator")
public class ToolAdapterOp extends Operator {

    private static final String INTERMEDIATE_PRODUCT_NAME = "interimProduct";
    private static final String[] DEFAULT_EXTENSIONS = { ".tif", ".tiff", ".nc", ".hdf", ".pgx", ".png", ".gif", ".jpg", ".bmp", ".pnm", ".pbm", ".pgm", ".ppm" };
    public static final String VELOCITY_LINE_SEPARATOR = "\r\n|\n";
    /**
     * Consume the output created by a tool.
     */
    private ProcessOutputConsumer consumer;

    /**
     * Stop the tool's execution.
     */
    private boolean stop;

    /**
     * Synchronization lock.
     */
    private final Object lock;

    private ToolAdapterOperatorDescriptor descriptor;

    /**
     * The folder where the tool descriptors reside.
     */
    private File adapterFolder;
    private OperatorContext accessibleContext;

    /**
     * Constructor.
     */
    public ToolAdapterOp() {
        super();
        this.consumer = null;
        this.stop = false;
        this.lock = new Object();
        try {
            accessibleContext = (OperatorContext) PrivilegedAccessor.getValue(this, "context");
        } catch (Exception e) {
            getLogger().severe(e.getMessage());
        }
        Velocity.init();
        //this.descriptor = ((ToolAdapterOperatorDescriptor) accessibleContext.getOperatorSpi().getOperatorDescriptor());
    }

    /**
     * Set a consumer for the tool's output.
     *
     * @param consumer the output consumer.
     */
    public void setConsumer(ProcessOutputConsumer consumer) {
        this.consumer = consumer;
    }

    /**
     * Command to stop the tool.
     * <p>
     * This method is synchronized.
     * </p>
     */
    public void stopTool() {
        synchronized (this.lock) {
            this.stop = true;
        }
    }

    /**
     * Check if a stop command was issued.
     * <p>
     * This method is synchronized.
     * </p>
     *
     * @return true if the execution of the tool must be stopped.
     */
    private boolean isStopped() {
        synchronized (this.lock) {
            return this.stop;
        }
    }

    public void setAdapterFolder(File folder) {
        this.adapterFolder = folder;
    }

    /**
     * Initialise and run the defined tool.
     * <p>
     * This method will block until the tool finishes its execution.
     * </p>
     *
     * @throws org.esa.beam.framework.gpf.OperatorException
     */
    @Override
    public void initialize() throws OperatorException {
        Date currentTime = new Date();
        if (descriptor == null) {
            descriptor = ((ToolAdapterOperatorDescriptor) accessibleContext.getOperatorSpi().getOperatorDescriptor());
        }
        //Validate the input
        validateDescriptor();
        //Prepare tool run
        beforeExecute();
        //Run tool
        execute();
        if (this.consumer != null) {
            Date finalDate = new Date();
            this.consumer.consumeOutput("Finished tool execution in " + (finalDate.getTime() - currentTime.getTime()) / 1000 + " seconds");
        }
        //Try to load target product
        postExecute();
    }

    /**
     * Verify that the data provided withing the operator descriptor is valid.
     *
     * @throws OperatorException in case of an error
     */
    private void validateDescriptor() throws OperatorException {

        //Get the tool file
        File toolFile = descriptor.getMainToolFileLocation();
        if (toolFile == null) {
            throw new OperatorException("Tool file not defined!");
        }
        // check if the tool file exists
        if (!toolFile.exists() || !toolFile.isFile()) {
            throw new OperatorException(String.format("Invalid tool file: '%s'!", toolFile.getAbsolutePath()));
        }

        //Get the tool's working directory
        File toolWorkingDirectory = descriptor.getWorkingDir();
        if (toolWorkingDirectory == null) {
            throw new OperatorException("Tool working directory not defined!");
        }
        // check if the tool file exists
        if (!toolWorkingDirectory.exists() || !toolWorkingDirectory.isDirectory()) {
            throw new OperatorException(String.format("Invalid tool working directory: '%s'!", toolWorkingDirectory.getAbsolutePath()));
        }

    }

    /**
     * Fill the templates with data and prepare the source product.
     *
     * @throws org.esa.beam.framework.gpf.OperatorException in case of an error
     */
    private void beforeExecute() throws OperatorException {
        if (descriptor.shouldWriteBeforeProcessing()) {
            String sourceFormatName = descriptor.getProcessingWriter();
            if (sourceFormatName != null) {
                ProductIOPlugInManager registry = ProductIOPlugInManager.getInstance();
                Iterator<ProductWriterPlugIn> writerPlugIns = registry.getWriterPlugIns(sourceFormatName);
                ProductWriterPlugIn writerPlugIn = writerPlugIns.next();
                Product selectedProduct = getSourceProduct();
                String sourceDefaultExtension = writerPlugIn.getDefaultFileExtensions()[0];
                File outFile = new File(descriptor.getWorkingDir(), INTERMEDIATE_PRODUCT_NAME + sourceDefaultExtension);
                if (outFile.exists() && outFile.canWrite()) {
                    if (outFile.delete()) {
                        getLogger().warning("Could not delete previous temporary image");
                    }
                }
                GPF.writeProduct(selectedProduct, outFile, sourceFormatName, true, ProgressMonitor.NULL);
                if (outFile.exists()) {
                    try {
                        Product product = ProductIO.readProduct(outFile);
                        setSourceProducts(product);
                        product.closeIO();
                    } catch (IOException e) {
                        getLogger().severe("Cannot read from the selected format");
                    }
                }
            }
        }
    }

    /**
     * Run the tool.
     *
     * @return the return value of the process.
     * @throws OperatorException in case of an error.
     */
    private int execute() throws OperatorException {
        Process process = null;
        BufferedReader outReader = null;
        int ret = -1;
        try {
            if (this.consumer == null) {
                this.consumer = new LogOutputConsumer();
            }
            //initialise stop flag.
            synchronized (this.lock) {
                this.stop = false;
            }
            List<String> cmdLine = getCommandLineTokens();
            logCommandLine(cmdLine);
            ProcessBuilder pb = new ProcessBuilder(cmdLine);
            //redirect the error of the tool to the standard output
            pb.redirectErrorStream(true);
            //set the working directory
            pb.directory(descriptor.getWorkingDir());
            //start the process
            process = pb.start();
            //get the process output
            outReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            while (!isStopped()) {
                while (outReader.ready()) {
                    //read the process output line by line
                    String line = outReader.readLine();
                    //consume the line if possible
                    if (line != null && !"".equals(line.trim())) {
                        this.consumer.consumeOutput(line);
                    }
                }
                // check if the project finished execution
                if (!process.isAlive()) {
                    //stop the loop
                    stopTool();
                } else {
                    //yield the control to other threads
                    Thread.yield();
                }
            }
        } catch (IOException e) {
            throw new OperatorException("Error running tool " + descriptor.getName(), e);
        } finally {
            if (process != null) {
                // if the process is still running, force it to stop
                if (process.isAlive()) {
                    //destroy the process
                    process.destroyForcibly();
                }
                try {
                    //wait for the project to end.
                    ret = process.waitFor();
                } catch (InterruptedException e) {
                    //noinspection ThrowFromFinallyBlock
                    throw new OperatorException("Error running tool " + descriptor.getName(), e);
                }

                //close the reader
                closeStream(outReader);
                //close all streams
                closeStream(process.getErrorStream());
                closeStream(process.getInputStream());
                closeStream(process.getOutputStream());
            }
        }

        return ret;
    }

    /**
     * Load the result of the tool's execution.
     *
     * @throws OperatorException in case of an error
     */
    private void postExecute() throws OperatorException {
        File input = (File) getParameter(ToolAdapterConstants.TOOL_TARGET_PRODUCT_FILE);
        if (input == null) {
            //no target product, means the source product was changed
            //TODO all input files should be (re)-loaded since we do not know which one was changed
            input = getSourceProduct().getFileLocation();
        } else {
            try {
                Product[] sourceProducts = getSourceProducts();
                Product tmpProduct = CollectionHelper.firstOrDefault(sourceProducts, p -> {
                    return p.getFileLocation().getName().contains(INTERMEDIATE_PRODUCT_NAME);
                });
                if (tmpProduct != null) {
                    tmpProduct.closeIO();
                    if (!tmpProduct.getFileLocation().delete()) {
                        getLogger().warning("Temporary image could not be deleted");
                    }
                }
                Product target = ProductIO.readProduct(input);
                for (Band band : target.getBands()) {
                    ImageManager.getInstance().getSourceImage(band, 0);
                }
                setTargetProduct(target);
            } catch (IOException e) {
                throw new OperatorException("Error reading product '" + input.getPath() + "'");
            }
        }
    }

    /**
     * Close any stream without triggering exceptions.
     *
     * @param stream input or output stream.
     */
    private void closeStream(Closeable stream) {
        if (stream != null) {
            try {
                stream.close();
            } catch (IOException e) {
                //nothing to do.
            }
        }
    }

    /**
     * Add the tool's command line to the log.
     *
     * @param cmdLine the command line
     */
    private void logCommandLine(List<String> cmdLine) {
        StringBuilder sb = new StringBuilder();
        sb.append("Executing tool '").append(this.descriptor.getName()).append("' with command line: ");
        sb.append('\'').append(cmdLine.get(0));
        for (int i = 1; i < cmdLine.size(); i++) {
            sb.append(' ').append(cmdLine.get(i));
        }
        sb.append('\'');

        getLogger().log(Level.INFO, sb.toString());
    }

    /**
     * Build the list of command line parameters.
     * <p>
     * If no command line template defined then only the tool's file is returned
     * </p>
     *
     * @return the list of command line parameters
     * @throws org.esa.beam.framework.gpf.OperatorException in case of an error.
     */
    private List<String> getCommandLineTokens() throws OperatorException {
        final List<String> tokens = new ArrayList<>();
        String templateFile = ((ToolAdapterOperatorDescriptor) (getSpi().getOperatorDescriptor())).getTemplateFileLocation();
        if (templateFile != null) {
            tokens.add(descriptor.getMainToolFileLocation().getAbsolutePath());
            if (templateFile.endsWith(ToolAdapterConstants.TOOL_VELO_TEMPLATE_SUFIX)) {
                tokens.addAll(transformTemplate(new File(this.adapterFolder, templateFile)));
            } else {
                throw new OperatorException("Invalid Velocity template");
            }
        }
        return tokens;
    }

    private List<String> transformTemplate(File templateFile) {
        VelocityEngine ve = new VelocityEngine();
        ve.setProperty("file.resource.loader.path", templateFile.getParent());
        ve.init();
        Template t = ve.getTemplate(templateFile.getName());
        VelocityContext velContext = new VelocityContext();
        Property[] params = accessibleContext.getParameterSet().getProperties();
        for (Property param : params) {
            velContext.put(param.getName(), param.getValue());
        }
        Product[] sourceProducts = getSourceProducts();
        velContext.put(ToolAdapterConstants.TOOL_SOURCE_PRODUCT_ID,
                       sourceProducts.length == 1 ? sourceProducts[0] : sourceProducts);
        File[] rasterFiles = new File[sourceProducts.length];
        for (int i = 0; i < sourceProducts.length; i++) {
            File productFile = sourceProducts[i].getFileLocation();
            rasterFiles[i] = productFile.isFile() ? productFile : selectCandidateRasterFile(productFile);
        }
        velContext.put(ToolAdapterConstants.TOOL_SOURCE_PRODUCT_FILE,
                       rasterFiles.length == 1 ? rasterFiles[0] : rasterFiles);

        StringWriter writer = new StringWriter();
        t.merge(velContext, writer);
        String result = writer.toString();
        return Arrays.asList(result.split(VELOCITY_LINE_SEPARATOR));
    }

    private File selectCandidateRasterFile(File folder) {
        File rasterFile = null;
        List<File> candidates = getRasterFiles(folder);
        int numFiles = candidates.size() - 1;
        if (numFiles >= 0) {
            candidates.sort(Comparator.comparingLong(File::length));
            rasterFile = candidates.get(numFiles);
            getLogger().info(rasterFile.getName() + " was selected as raster file");
        }
        return rasterFile;
    }

    private List<File> getRasterFiles(File folder) {
        List<File> rasters = new ArrayList<>();
        for (String extension : DEFAULT_EXTENSIONS) {
            File[] files = folder.listFiles((File dir, String name) -> name.endsWith(extension));
            if (files != null) {
                rasters.addAll(Arrays.asList(files));
            }
            File[] subFolders = folder.listFiles(File::isDirectory);
            for(File subFolder : subFolders) {
                File subCandidate = selectCandidateRasterFile(subFolder);
                if (subCandidate != null) {
                    rasters.add(subCandidate);
                }
            }
        }
        return rasters;
    }

    /**
     * Add the output of the tool to the log.
     */
    public static class LogOutputConsumer implements ProcessOutputConsumer {

        /**
         * Consume a line of output obtained from a tool.
         *
         * @param line a line of output text.
         */
        @Override
        public void consumeOutput(String line) {
            BeamLogManager.getSystemLogger().info(line);
        }
    }
}
