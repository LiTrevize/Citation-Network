import javafx.util.Pair;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SVG {

    private Document document;
    Element svg;
    Element eNode, eEdge;
    float multiplier = 0.02f;

    float xmin = 0, xmax = 0, ymin = 0, ymax = 0;

    class XY {
        float x, y;

        public XY(float x, float y) {
            this.x = x;
            this.y = y;
        }
    }

    public SVG() {

        try {
            // create the builder factory
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = factory.newDocumentBuilder();
            document = db.newDocument();
            // do not display standalone="no"
            document.setXmlStandalone(true);

            svg = document.createElement("svg");
            svg.setAttribute("xmlns", "http://www.w3.org/2000/svg");
            svg.setAttribute("version", "1.1");
//            svg.setAttribute("viewBox", "-11033.525 -13722.238 20532.134 29905.457");
            eEdge = document.createElement("g");
            eEdge.setAttribute("id", "edges");
            svg.appendChild(eEdge);
            eNode = document.createElement("g");
            eNode.setAttribute("id", "nodes");
            svg.appendChild(eNode);

            document.appendChild(svg);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void updateXYRang(float x, float y, float r) {
//            System.out.println(r);
        if (x < xmin) xmin = x;
        if (x > xmax) xmax = x;
        if (y < ymin) ymin = y;
        if (y > ymax) ymax = y;
    }

    public void updateView() {
        svg.setAttribute("viewBox", "" + xmin + " " + ymin + " " + (xmax - xmin) + " " + (ymax - ymin));
        System.out.println("" + xmin + " " + ymin + " " + xmax + " " + ymax);
    }

    public void fromCsv(String layoutPath, String edgePath) {
        Map<Integer, XY> xys = new HashMap<>();
        try {
            File file = new File(layoutPath);
            InputStreamReader read = new InputStreamReader(
                    new FileInputStream(layoutPath), "utf8");
            BufferedReader bufferedReader = new BufferedReader(read);
            String line = null;

            while ((line = bufferedReader.readLine()) != null) {
                String[] p = line.split("\t");
                String nid = p[0];
                String x = p[1];
                String y = p[2];
                String r = p[3];
                if (p.length > 5) {
                    int cite = Integer.parseInt(p[5]);
                    if (cite > 50) {
                        addNode(nid, x, y, r);
                        updateXYRang(Float.parseFloat(x), Float.parseFloat(y), Float.parseFloat(r));
                        xys.put(Integer.parseInt(nid), new XY(Float.parseFloat(x), Float.parseFloat(y)));
                    }
                } else {
                    addNode(nid, x, y, r);
                    updateXYRang(Float.parseFloat(x), Float.parseFloat(y), Float.parseFloat(r));
                    xys.put(Integer.parseInt(nid), new XY(Float.parseFloat(x), Float.parseFloat(y)));
                }

            }
            bufferedReader.close();
            read.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println("Node: " + xys.size());

        updateView();

        // add edges
        int num_edges = 0;
        if (edgePath != null) {
            try {
                File file = new File(edgePath);
                InputStreamReader read = new InputStreamReader(
                        new FileInputStream(edgePath), "utf8");
                BufferedReader bufferedReader = new BufferedReader(read);
                String line = null;

                while ((line = bufferedReader.readLine()) != null) {
                    String[] p = line.split("\t");
                    int src = Integer.parseInt(p[0]);
                    int tar = Integer.parseInt(p[1]);
                    if (xys.containsKey(src) && xys.containsKey(tar)) {
                        addEdge(xys.get(src), xys.get(tar));
                        num_edges++;
                        if(num_edges>=2*xys.size()) break;
                    }
                }
                bufferedReader.close();
                read.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        System.out.println("Edges: " + num_edges);


    }

    public void addNode(String id, String x, String y, String r) {
        Element e = document.createElement("circle");
        e.setAttribute("id", id);
        e.setAttribute("cx", x);
        e.setAttribute("cy", y);
        e.setAttribute("r", r);
        e.setAttribute("fill", "black");
        e.setAttribute("fill-opacity", "0.5");

        eNode.appendChild(e);

    }

//    public void addNode(Node node) {
//        Element e = document.createElement("circle");
//        e.setAttribute("cx", String.valueOf(node.y));
//        e.setAttribute("cy", String.valueOf(node.x));
//        e.setAttribute("r", String.valueOf(multiplier * node.citation));
//        e.setAttribute("fill", "black");
//        e.setAttribute("fill-opacity", "0.5");
//
//        eNode.appendChild(e);
//
//    }

    public void addEdge(XY src, XY tar) {
        Element e = document.createElement("line");
        e.setAttribute("x1", String.valueOf(src.x));
        e.setAttribute("y1", String.valueOf(src.y));
        e.setAttribute("x2", String.valueOf(tar.x));
        e.setAttribute("y2", String.valueOf(tar.y));
        e.setAttribute("stroke", "black");
        e.setAttribute("stroke-width", "0.1");
        e.setAttribute("stroke-opacity", "0.1");
        eEdge.appendChild(e);
    }

    public void exportTo(String filePath) {
        try {
            // create TransformerFactory
            TransformerFactory tff = TransformerFactory.newInstance();
            // create Transformer
            Transformer tf = tff.newTransformer();

            // whether a line break
            tf.setOutputProperty(OutputKeys.INDENT, "yes");
            // create xml file and write data
            tf.transform(new DOMSource(document), new StreamResult(new File(filePath)));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
