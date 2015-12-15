package org.esa.s3tbx.slstr.pdu.stitching.manifest;

import com.sun.org.apache.xerces.internal.dom.TextImpl;
import org.esa.s3tbx.slstr.pdu.stitching.PDUStitchingException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

import java.util.List;

/**
 * @author Tonio Fincke
 */
public class DumpInformationMerger extends AbstractElementMerger {

    @Override
    public void mergeNodes(List<Node> fromParents, Element toParent, Document toDocument) throws PDUStitchingException {
        final Node actualParent = toParent.getParentNode();
        actualParent.removeChild(toParent);
        for (Node fromParent : fromParents) {
            appendLineBreakAndIndent(actualParent, toDocument, 6);
            final Node origElement = fromParent;
            final Element newDumpElement = toDocument.createElement(origElement.getNodeName());
            final NodeList origChildNodes = origElement.getChildNodes();
            for (int j = 0; j < origChildNodes.getLength(); j++) {
                final Node origChild = origChildNodes.item(j);
                if (!(origChild instanceof TextImpl) && !origChild.getTextContent().contains("\n")) {
                    appendLineBreakAndIndent(newDumpElement, toDocument, 7);
                    final Element dumpChildElement = toDocument.createElement(origChild.getNodeName());
                    final String textContent = origChild.getTextContent();
                    final Text textNode = toDocument.createTextNode(textContent);
                    dumpChildElement.appendChild(textNode);
                    newDumpElement.appendChild(dumpChildElement);
                }
            }
            appendLineBreakAndIndent(newDumpElement, toDocument, 6);
            actualParent.appendChild(newDumpElement);
        }
    }
}
