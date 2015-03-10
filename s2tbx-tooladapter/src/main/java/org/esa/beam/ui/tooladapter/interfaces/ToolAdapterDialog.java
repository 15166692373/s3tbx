package org.esa.beam.ui.tooladapter.interfaces;

import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.GPF;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.descriptor.ToolAdapterOperatorDescriptor;
import org.esa.beam.framework.gpf.operators.tooladapter.ProcessOutputConsumer;
import org.esa.beam.framework.gpf.operators.tooladapter.ToolAdapterOp;
import org.esa.beam.framework.gpf.ui.OperatorMenu;
import org.esa.beam.framework.gpf.ui.OperatorParameterSupport;
import org.esa.beam.framework.gpf.ui.SingleTargetProductDialog;
import org.esa.beam.framework.ui.AppContext;
import org.esa.beam.ui.tooladapter.ExternalToolExecutionForm;
import org.esa.beam.util.logging.BeamLogManager;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

/**
 * @author Lucian Barbulescu.
 */
public class ToolAdapterDialog extends SingleTargetProductDialog {

    /**
     * Operator identifier.
     */
    private ToolAdapterOperatorDescriptor operatorDescriptor;
    /**
     * Parameters related info.
     */
    private OperatorParameterSupport parameterSupport;
    /**
     * The form used to get the user's input
     */
    private ExternalToolExecutionForm form;

    /**
     * Constructor.
     *
     * @param operatorSpi
     * @param appContext
     * @param title
     * @param helpID
     */
    public ToolAdapterDialog(ToolAdapterOperatorDescriptor operatorSpi, AppContext appContext, String title, String helpID) {
        super(appContext, title, helpID);
        this.operatorDescriptor = operatorSpi;

        this.parameterSupport = new OperatorParameterSupport(operatorSpi);

        form = new ExternalToolExecutionForm(appContext, operatorSpi, parameterSupport.getPropertySet(),
                getTargetProductSelector());
        OperatorMenu operatorMenu = new OperatorMenu(this.getJDialog(),
                operatorSpi,
                parameterSupport,
                appContext,
                helpID);
        getJDialog().setJMenuBar(operatorMenu.createDefaultMenu());
    }

    @Override
    protected void onApply() {
        if (validateUserInput()) {
            super.onApply();
        }
    }

    private boolean validateUserInput() {
        return true;
    }

    @Override
    public int show() {
        form.prepareShow();
        setContent(form);
        return super.show();
    }

    @Override
    public void hide() {
        form.prepareHide();
        super.hide();
    }


    /**
     * Creates the desired target product.
     * Usually, this method will be implemented by invoking one of the multiple {@link org.esa.beam.framework.gpf.GPF GPF}
     * {@code createProduct} methods.
     * <p/>
     * The method should throw a {@link org.esa.beam.framework.gpf.OperatorException} in order to signal "nominal" processing errors,
     * other exeption types are treated as internal errors.
     *
     * @return The target product.
     * @throws Exception if an error occurs, an {@link org.esa.beam.framework.gpf.OperatorException} is signaling "nominal" processing errors.
     */
    @Override
    protected Product createTargetProduct() throws Exception {
        //Get the selected product.
        final Product sourceProduct = form.getSourceProduct();
        Map<String, Product> sourceProducts = new HashMap<>();
        sourceProducts.put("sourceProduct", sourceProduct);

        Operator op = GPF.getDefaultInstance().createOperator(this.operatorDescriptor.getName(), parameterSupport.getParameterMap(), sourceProducts, null);
//form.targetProductSelector.getModel().getProductFile()
        // set the output consumer
        ((ToolAdapterOp) op).setConsumer(new LogOutputConsumer());

        return op.getTargetProduct();
    }

    @Override
    protected boolean canApply() {
        return true;
    }

    /**
     * Add the output of the tool to the log.
     */
    private class LogOutputConsumer implements ProcessOutputConsumer {

        /**
         * Consume a line of output obtained from a tool.
         *
         * @param line a line of output text.
         */
        @Override
        public void consumeOutput(String line) {
            BeamLogManager.getSystemLogger().log(Level.INFO, line);
        }
    }
}
