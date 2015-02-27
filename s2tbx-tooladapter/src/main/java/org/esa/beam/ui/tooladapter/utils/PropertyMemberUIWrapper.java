package org.esa.beam.ui.tooladapter.utils;

import com.bc.ceres.swing.binding.BindingContext;
import org.esa.beam.framework.gpf.descriptor.ToolAdapterOperatorDescriptor;
import org.esa.beam.framework.gpf.descriptor.ToolParameterDescriptor;

import java.awt.*;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

/**
 * @author Ramona Manda
 */
public abstract class PropertyMemberUIWrapper implements FocusListener {

    protected String attributeName;
    protected Component UIComponent;
    protected int width = 0;
    protected ToolParameterDescriptor property;
    protected ToolAdapterOperatorDescriptor opDescriptor;
    private CallBackAfterEdit callback;
    private BindingContext context;

    public PropertyMemberUIWrapper(String attributeName, ToolParameterDescriptor property, ToolAdapterOperatorDescriptor opDescriptor, BindingContext context) {
        this.attributeName = attributeName;
        this.property = property;
        this.opDescriptor = opDescriptor;
        this.context = context;
    }

    public PropertyMemberUIWrapper(String attributeName, ToolParameterDescriptor property, ToolAdapterOperatorDescriptor opDescriptor, BindingContext context, int width, CallBackAfterEdit callback) {
        this(attributeName, property, opDescriptor, context);
        this.width = width;
        this.callback = callback;
        try {
            getUIComponent();
        }catch (Exception ex){
            //TODO
            ex.printStackTrace();
        }
    }

    public String getErrorValueMessage(Object value) {
        return null;
    }

    protected abstract void setMemberValue(Object value) throws PropertyAttributeException;

    public abstract Object getMemberValue() throws PropertyAttributeException;

    public Component getUIComponent() throws Exception {
        if (UIComponent == null) {
            buildAndLinkUIComponent();
        }
        return UIComponent;
    }

    public void buildAndLinkUIComponent() throws Exception {
        UIComponent = buildUIComponent();
        if (width != -1) {
            UIComponent.setPreferredSize(new Dimension(width, 20));
        }
        UIComponent.addFocusListener(this);
        //Object value = context.getBinding(attributeName).getPropertyValue();
        /*if(value != null) {
            if (UIComponent instanceof JTextField) {
                ((JTextField) UIComponent).setText(value.toString());
            }
            if (UIComponent instanceof JFileChooser) {
                ((JFileChooser) UIComponent).setSelectedFile(new File(value.toString()));
            }
            if (UIComponent instanceof JComboBox) {
                ((JComboBox) UIComponent).setSelectedItem(value);
            }
        }*/
    }

    protected abstract Component buildUIComponent() throws Exception;

    public void setMemberValueWithCheck(Object value) throws PropertyAttributeException {
        String msg = getErrorValueMessage(value);
        if (msg != null) {
            throw new PropertyAttributeException(msg);
        }
        setMemberValue(value);
    }

    public boolean memberUIComponentNeedsRevalidation() {
        return false;
    }

    public boolean propertyUIComponentsNeedsRevalidation() {
        return false;
    }

    public boolean contextUIComponentsNeedsRevalidation() {
        return false;
    }

    @Override
    public void focusGained(FocusEvent e) {
        //do nothing
    }

    @Override
    public void focusLost(FocusEvent e) {
        //save the value
        if (!e.isTemporary()) {
            try {
                if (getValueFromUIComponent().equals(getMemberValue())) {
                    return;
                }
                setMemberValueWithCheck(getValueFromUIComponent());
                if (callback != null) {
                    callback.doCallBack(null, property, null, attributeName);
                }
            } catch (PropertyAttributeException ex) {
                if (callback != null) {
                    callback.doCallBack(ex, property, null, attributeName);
                }
                UIComponent.requestFocusInWindow();
            }
        }
    }

    public void setCallback(CallBackAfterEdit callback) {
        this.callback = callback;
    }

    protected abstract Object getValueFromUIComponent() throws PropertyAttributeException;

    public interface CallBackAfterEdit {
        public void doCallBack(PropertyAttributeException exception, ToolParameterDescriptor oldProperty, ToolParameterDescriptor newProperty, String attributeName);
    }
}
