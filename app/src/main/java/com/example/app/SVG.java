package com.example.app;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import java.io.File;
import java.io.StringWriter;

public class SVG {

    private Document document;
    Element svg;
    Element eNode, eEdge;
    float multiplier = 0.02f;

    float xmin = 0, xmax = 0, ymin = 0, ymax = 0;

    public SVG() {
        try {
            // 创建解析器工厂
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = factory.newDocumentBuilder();
            document = db.newDocument();
            // 不显示standalone="no"
            document.setXmlStandalone(true);

            svg = document.createElement("svg");
            svg.setAttribute("xmlns", "http://www.w3.org/2000/svg");
            svg.setAttribute("version", "1.1");
            svg.setAttribute("viewBox", "-11033.525 -13722.238 20532.134 29905.457");
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

    public void updateXYRange(float x, float y) {
        if (x < xmin) xmin = x;
        if (x > xmax) xmax = x;
        if (y < ymin) ymin = y;
        if (y > ymax) ymax = y;
    }

    public void updateView() {
        svg.setAttribute("viewBox", "" + xmin + " " + ymin + " " + (xmax - xmin) + " " + (ymax - ymin));
    }

    public void addNode(Node node) {
        Element e = document.createElement("circle");
        e.setAttribute("cx", String.valueOf(node.x));
        e.setAttribute("cy", String.valueOf(node.y));
        e.setAttribute("r", String.valueOf(multiplier * node.citation));
        e.setAttribute("fill", "black");
        e.setAttribute("fill-opacity", "0.5");
        e.setAttribute("title", node.title);
        e.setAttribute("onclick", "sub(event)");

        eNode.appendChild(e);

        updateXYRange(node.x, node.y);

    }

    public void addEdge(Node src, Node tar) {
        Element e = document.createElement("line");
        e.setAttribute("x1", String.valueOf(src.x));
        e.setAttribute("y1", String.valueOf(src.y));
        e.setAttribute("x2", String.valueOf(tar.x));
        e.setAttribute("y2", String.valueOf(tar.y));
        e.setAttribute("stroke", "black");
        e.setAttribute("stroke-width", "1");
        e.setAttribute("stroke-opacity", "0.3");
        eEdge.appendChild(e);
    }

    public void exportTo(String filePath) {
        updateView();
        try {
            TransformerFactory tff = TransformerFactory.newInstance();

            Transformer tf = tff.newTransformer();

            tf.setOutputProperty(OutputKeys.INDENT, "yes");

            tf.transform(new DOMSource(document), new StreamResult(new File(filePath)));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public String toString() {
        try {
            TransformerFactory tff = TransformerFactory.newInstance();
            Transformer tf = tff.newTransformer();
            // create a StringWriter for the output
            StringWriter outWriter = new StringWriter();
            StreamResult result = new StreamResult(outWriter);
            tf.transform(new DOMSource(document), result);
            String resXml = outWriter.getBuffer().toString();
            return resXml;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }
}
