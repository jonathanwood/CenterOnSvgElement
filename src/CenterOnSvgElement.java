import org.apache.batik.anim.dom.SAXSVGDocumentFactory;
import org.apache.batik.bridge.BridgeContext;
import org.apache.batik.bridge.UserAgent;
import org.apache.batik.bridge.ViewBox;
import org.apache.batik.gvt.GraphicsNode;
import org.apache.batik.swing.JSVGCanvas;
import org.apache.batik.swing.gvt.GVTTreeRendererEvent;
import org.apache.batik.swing.gvt.GVTTreeRendererListener;
import org.apache.batik.swing.svg.LinkActivationEvent;
import org.apache.batik.swing.svg.LinkActivationListener;
import org.apache.batik.util.XMLResourceDescriptor;
import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.svg.*;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class CenterOnSvgElement extends JPanel {

    private float[] viewBoxAttrs;
    private CustomSvgCanvas customSvgCanvas;
    private SAXSVGDocumentFactory svgDocumentFactory =
            new SAXSVGDocumentFactory( XMLResourceDescriptor.getXMLParserClassName() );
    private SVGDocument svgDocument ;
    private final static String XLINK_NS = "http://www.w3.org/1999/xlink";
    private Map<String, Element> textElements = new HashMap<>();



    /*
    This class is to disable JSVGCanvas default action when a hyper link
    is clicked in an svg which is to open that url this is undesired behavior
    */
    class CustomSvgCanvas extends JSVGCanvas {
        class CustomCanvasUserAgent extends JSVGCanvas.CanvasUserAgent {
            @Override
            public void openLink(SVGAElement svgaElement) {
                String href =  svgaElement.getHref().getAnimVal();
                fireLinkActivatedEvent(svgaElement, href);
            }
        }

        @Override
        protected UserAgent createUserAgent() {
            return new CustomCanvasUserAgent();
        }

        public BridgeContext getBridgeContxt() {
            return bridgeContext;
        }
    }

    // rhis calss is just to set the frame visibile to the prefered size after finished rendering
    private class RefreshSizeRenderListener implements GVTTreeRendererListener {

        @Override
        public void gvtRenderingPrepare(GVTTreeRendererEvent gvtTreeRendererEvent) {
        }

        @Override
        public void gvtRenderingStarted(GVTTreeRendererEvent gvtTreeRendererEvent) {
        }

        @Override
        public void gvtRenderingCompleted(GVTTreeRendererEvent gvtTreeRendererEvent) {
            invalidate();
            validate();
        }

        @Override
        public void gvtRenderingCancelled(GVTTreeRendererEvent gvtTreeRendererEvent) {

        }

        @Override
        public void gvtRenderingFailed(GVTTreeRendererEvent gvtTreeRendererEvent) {
        }
    }


    private class CenterOnLinkListener implements LinkActivationListener {

        /*
        when a hyper link is selected the text of the hytper link appears dead center in the
        display of the canvas what the user sees with his eyes
         */
        @Override
        public void linkActivated(LinkActivationEvent linkActivationEvent) {

            final SVGSVGElement rootElement = svgDocument.getRootElement();
            final Element textElement = textElements.get(linkActivationEvent.getReferencedURI());
            final SVGRect textBBox = ((SVGLocatable)textElement).getBBox();
            final SVGPoint textCenter = rootElement.createSVGPoint();
            textCenter.setX(textBBox.getX() + textBBox.getWidth()/2);
            textCenter.setY(textBBox.getY() + textBBox.getHeight() / 2);

            final SVGMatrix rootTransform = ((SVGLocatable) textElement).getTransformToElement(rootElement);
            final SVGPoint textCenterOnRoot = textCenter.matrixTransform(rootTransform);

            String[] vbParts = rootElement.getAttributeNS(null, "viewBox").split(" ");
            float vbX = - (Float.parseFloat(vbParts[2])/2 - textCenterOnRoot.getX());
            float vbY = - (Float.parseFloat(vbParts[3])/2 - textCenterOnRoot.getY());
            final String newViewBox = "" + vbX + " " + vbY + " " + vbParts[2] + " " + vbParts[3];

            customSvgCanvas.getUpdateManager().getUpdateRunnableQueue().invokeLater(
                    new Runnable() {
                        @Override
                        public void run() {
                            rootElement.setAttributeNS(null, "viewBox", newViewBox);
                        }
                    }
            );

        }
    }


    public CenterOnSvgElement() throws HeadlessException {


        customSvgCanvas = new CustomSvgCanvas();
        customSvgCanvas.setDoubleBuffered(true);
        customSvgCanvas.setDocumentState(JSVGCanvas.ALWAYS_DYNAMIC);
        customSvgCanvas.addGVTTreeRendererListener( new RefreshSizeRenderListener() );
        customSvgCanvas.addLinkActivationListener(new CenterOnLinkListener() );

        add(customSvgCanvas, BorderLayout.CENTER);

    }

    public void openSvg() {
        File svgFile = new File("test.svg");
        try {
            svgDocument = svgDocumentFactory.createSVGDocument(svgFile.toURI().toString());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        // get the textElement that we will center on when the link is clicked
        NodeList anchors = svgDocument.getElementsByTagName("a");
        for(int i = 0; i < anchors.getLength(); i++) {
            Element anchor = (Element) anchors.item(i);
            Attr href = anchor.getAttributeNodeNS(XLINK_NS, "href");
            String lineNumber = href.getValue();
            NodeList textNodes = anchor.getElementsByTagName("text");
            // this is an anchor without a text element under it
            if (textNodes.getLength() != 1) continue;
            textElements.put(lineNumber, (Element) textNodes.item(0));

        }
        customSvgCanvas.setDocument( svgDocument );
        final SVGSVGElement rootElement = svgDocument.getRootElement();

        String viewBox = rootElement.getAttributeNS(null, "viewBox");

        // get the initial view box for later use
        viewBoxAttrs =
                ViewBox.parseViewBoxAttribute(rootElement, viewBox, customSvgCanvas.getBridgeContxt());


    }

    public static void main(String[] args) {
        JFrame frame = new JFrame();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        CenterOnSvgElement centerOnSvgElement = new CenterOnSvgElement();
        frame.getContentPane().add(centerOnSvgElement, BorderLayout.CENTER);
        frame.pack();
        frame.setVisible(true);
        centerOnSvgElement.openSvg();
    }

}
