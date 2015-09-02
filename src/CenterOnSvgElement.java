import org.apache.batik.anim.dom.SAXSVGDocumentFactory;
import org.apache.batik.bridge.BridgeContext;
import org.apache.batik.bridge.UserAgent;
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
import org.w3c.dom.svg.SVGAElement;
import org.w3c.dom.svg.SVGDocument;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.io.File;

public class CenterOnSvgElement extends JPanel {

    private Element textElement;
    private CustomSvgCanvas customSvgCanvas;
    private SAXSVGDocumentFactory svgDocumentFactory =
            new SAXSVGDocumentFactory( XMLResourceDescriptor.getXMLParserClassName() );
    private SVGDocument svgDocument ;
    private final static String XLINK_NS = "http://www.w3.org/1999/xlink";



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
            // TODO : I have no idea how to center on textElement

            GraphicsNode graphicsNode = customSvgCanvas.getBridgeContxt().getGraphicsNode(textElement);
            AffineTransform gnt = new AffineTransform(customSvgCanvas.getRenderingTransform());
            gnt.concatenate(graphicsNode.getGlobalTransform());

            Shape rect = gnt.createTransformedShape(graphicsNode.getPrimitiveBounds());
            Rectangle bounds = rect.getBounds();

            double dx = -bounds.getX() - bounds.getWidth() / 2;
            double dy = -bounds.getY() - bounds.getHeight() / 2;

            AffineTransform tx = AffineTransform.getTranslateInstance(dx, dy);

            Dimension canvasSize = customSvgCanvas.getSize();

            tx.preConcatenate(AffineTransform.getTranslateInstance
                    (canvasSize.width/2, canvasSize.height/2));

            AffineTransform rt = (AffineTransform) customSvgCanvas.getRenderingTransform().clone();

            rt.preConcatenate(tx);

            customSvgCanvas.setRenderingTransform(rt);

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
            Integer lineNumber = Integer.valueOf(href.getValue());
            NodeList textNodes = anchor.getElementsByTagName("text");
            // this is an anchor withot a text lement under it
            if (textNodes.getLength() != 1) continue;
            textElement = (Element) textNodes.item(0);
            break;
        }
        customSvgCanvas.setDocument( svgDocument );
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
