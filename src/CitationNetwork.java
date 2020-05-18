import org.gephi.appearance.api.AppearanceController;
import org.gephi.appearance.api.AppearanceModel;
import org.gephi.appearance.api.Function;
import org.gephi.appearance.api.Ranking;
import org.gephi.appearance.plugin.RankingElementColorTransformer;
import org.gephi.appearance.plugin.RankingNodeSizeTransformer;
import org.gephi.appearance.spi.Transformer;
import org.gephi.graph.api.*;
import org.gephi.layout.plugin.force.StepDisplacement;
import org.gephi.layout.plugin.force.yifanHu.YifanHuLayout;
import org.gephi.layout.plugin.forceAtlas2.ForceAtlas2;
import org.gephi.io.exporter.api.ExportController;
import org.gephi.io.importer.api.Container;
import org.gephi.io.importer.api.EdgeDirectionDefault;
import org.gephi.io.importer.api.ImportController;
import org.gephi.io.processor.plugin.DefaultProcessor;
import org.gephi.layout.plugin.forceAtlas2.ForceAtlas2Builder;
import org.gephi.layout.plugin.noverlap.NoverlapLayout;
import org.gephi.layout.plugin.noverlap.NoverlapLayoutBuilder;
import org.gephi.preview.api.PreviewController;
import org.gephi.preview.api.PreviewModel;
import org.gephi.preview.api.PreviewProperty;
import org.gephi.statistics.plugin.GraphDistance;
import org.openide.util.Lookup;
import org.gephi.project.api.ProjectController;
import org.gephi.project.api.Workspace;

import java.awt.*;
import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class CitationNetwork {
    // project
    static private ProjectController pc = Lookup.getDefault().lookup(ProjectController.class);

    private Workspace workspace;
    private GraphModel graphModel;
    private PreviewModel previewModel;

    private AppearanceController appearanceController;
    private AppearanceModel appearanceModel;
    DirectedGraph graph;


    public CitationNetwork() {
        init();
    }

    /*
    filePath can be csv or gexf/gml/...
     */
    public CitationNetwork(String filePath) {
        init();
        loadFile(filePath);
    }

    public CitationNetwork(String nodePath, String edgePath) {
        init();

        if (nodePath != null && new File(nodePath).exists())
            fromCsv(nodePath, edgePath);
        else fromCsv(edgePath);
    }

    public CitationNetwork(File file) {
        init();
        loadFile(file);
    }

    public void exportXYR(String filePath, boolean exportR) {
        try {
            System.out.println("Writing to " + filePath + " ...");
            File file = new File(filePath);
            file.createNewFile();
            BufferedWriter out = new BufferedWriter(new FileWriter(file));

            Column sizeCol = graphModel.getNodeTable().getColumn("size");

            for (Node node : graph.getNodes().toArray()) {
                float x = node.x();
                float y = node.y();
                if (!exportR)
                    out.write(node.getId().toString() + "\t" + x + "\t" + y + "\n");
                else {
                    float size = 0;
                    if ((int) node.getAttribute(sizeCol) > 1) size = node.size();
                    out.write(node.getId().toString() + "\t" + x + "\t" + y + "\t" + size + "\n");
                }
            }

            out.flush();
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void getXY() {
        float xmax = 0, xmin = 0, ymax = 0, ymin = 0;
        for (Node node : graph.getNodes().toArray()) {
            float x = node.x();
            float y = node.y();
//            System.out.println("" + x + "," + y);
            if (x > xmax) xmax = x;
            if (x < xmin) xmin = x;
            if (y > ymax) ymax = y;
            if (y < ymin) ymin = y;
        }
        System.out.println("X" + "," + xmin + "," + xmax);
        System.out.println("Y" + "," + ymin + "," + ymax);
    }

    public void layout_fa2(int num_iter, boolean linLog, boolean adjustSize) {
        System.out.println("Layout...");
        ForceAtlas2 fa2 = new ForceAtlas2(null);
        fa2.setGraphModel(graphModel);
        fa2.setAdjustSizes(adjustSize);
        if (linLog) {
            fa2.setLinLogMode(true);
            fa2.setScalingRatio(0.2);
        } else {
            fa2.setLinLogMode(false);
            fa2.setScalingRatio(50d);
        }

        fa2.initAlgo();
        for (int i = 0; i < num_iter && fa2.canAlgo(); i++) {
            System.out.print("\r" + i);
            fa2.goAlgo();
        }
        fa2.endAlgo();
        System.out.println();
    }

    public void layout_yfh(int num_iter) {
        System.out.println("Layout...");
        //Run YifanHuLayout for 100 passes - The layout always takes the current visible view
        YifanHuLayout layout = new YifanHuLayout(null, new StepDisplacement(1f));
        layout.setGraphModel(graphModel);
        layout.resetPropertiesValues();
        layout.setOptimalDistance(200f);

        layout.initAlgo();
        for (int i = 0; i < num_iter && layout.canAlgo(); i++) {
            System.out.print("\r" + i);
            layout.goAlgo();
        }
        layout.endAlgo();
        System.out.println();
    }

    public void rankColor() {
        //Rank color by Degree
        Function colorDegreeRanking = appearanceModel.getNodeFunction(graph, AppearanceModel.GraphFunction.NODE_OUTDEGREE, RankingElementColorTransformer.class);
        RankingElementColorTransformer colorDegreeTransformer = (RankingElementColorTransformer) colorDegreeRanking.getTransformer();
        colorDegreeTransformer.setColors(new Color[]{new Color(0x00EAFF), new Color(0x3C8CE7)});
//        colorDegreeTransformer.setColors(new Color[]{new Color(0x17e0ff),new Color(0x66aaff),new Color(0x7971ff),new Color(0x7a1eff)});
        colorDegreeTransformer.setColorPositions(new float[]{0f, 1f});
        appearanceController.transform(colorDegreeRanking);
    }

    public void rankSizeBy(String attr, int minSize, int maxSize) {
        // Rank size by degree
        Function sizeDegreeRanking;
        if (attr.equals("degree"))
            sizeDegreeRanking = appearanceModel.getNodeFunction(graph, AppearanceModel.GraphFunction.NODE_OUTDEGREE, RankingNodeSizeTransformer.class);
        else {
            Column attrCol = graphModel.getNodeTable().getColumn(attr);
            sizeDegreeRanking = appearanceModel.getNodeFunction(graph, attrCol, RankingNodeSizeTransformer.class);
        }
        RankingNodeSizeTransformer sizeDegreeTransformer = (RankingNodeSizeTransformer) sizeDegreeRanking.getTransformer();
        sizeDegreeTransformer.setMinSize(minSize);
        sizeDegreeTransformer.setMaxSize(maxSize);
        appearanceController.transform(sizeDegreeRanking);
    }

    public void setPreview() {
        System.out.println("Setting preview...");

//        rankColor();
//
//        rankSizeBy("degree", 1, 10000);

        // straight line
        previewModel.getProperties().putValue(PreviewProperty.EDGE_CURVED, false);
        previewModel.getProperties().putValue(PreviewProperty.EDGE_THICKNESS, 1);
        previewModel.getProperties().putValue(PreviewProperty.EDGE_OPACITY, 50);
        previewModel.getProperties().putValue(PreviewProperty.NODE_OPACITY, 50);
        previewModel.getProperties().putValue(PreviewProperty.NODE_BORDER_WIDTH, 0);
        previewModel.getProperties().putValue(PreviewProperty.ARROW_SIZE, 0);
    }

    public void exportTo(String pathname) {
        System.out.println("Exporting to " + pathname);
        ExportController ec = Lookup.getDefault().lookup(ExportController.class);
        try {
            ec.exportFile(new File(pathname));
        } catch (IOException ex) {
            ex.printStackTrace();
            return;
        }
    }

    /**
     * init workspace
     */
    private void init() {
        pc.newProject();
        workspace = pc.getCurrentWorkspace();
        graphModel = Lookup.getDefault().lookup(GraphController.class).getGraphModel();
        previewModel = Lookup.getDefault().lookup(PreviewController.class).getModel();
        graph = graphModel.getDirectedGraph();
        appearanceController = Lookup.getDefault().lookup(AppearanceController.class);
        appearanceModel = appearanceController.getModel();
    }

    private void fromCsv(String nodeCsv, String edgeCsv) {
        File nodeFile = new File(nodeCsv);
        File edgeFile = new File(edgeCsv);
        InputStreamReader read;
        BufferedReader bufferedReader;

        // load node
        Map<String, Node> nodeMap = new HashMap<>();

        //Add boolean column
        Column sizeCol = graphModel.getNodeTable().addColumn("size", Integer.class);
        try {
            read = new InputStreamReader(new FileInputStream(nodeFile), "utf8");
            bufferedReader = new BufferedReader(read);
            String line = null;

            while ((line = bufferedReader.readLine()) != null) {
                String[] p = line.split("\t");
                String nid = p[0];
                int size = Integer.parseInt(p[1]);
                if (!nodeMap.containsKey(nid)) {
                    Node n1 = graphModel.factory().newNode(nid);
                    // add size attribute
                    n1.setAttribute(sizeCol, size);

                    nodeMap.put(nid, n1);
                }
            }
            graph.addAllNodes(nodeMap.values());

            // load edge
            read = new InputStreamReader(new FileInputStream(edgeFile), "utf8");
            bufferedReader = new BufferedReader(read);
            while ((line = bufferedReader.readLine()) != null) {
                String[] p = line.split("\t");
                String src = p[0];
                String tar = p[1];
                double w = 1f;
//                if (p.length > 2) w = Double.parseDouble(p[2]);
                Edge e = graphModel.factory().newEdge(nodeMap.get(src), nodeMap.get(tar), 0, w, true);
                graph.addEdge(e);
            }

            bufferedReader.close();
            read.close();


        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void fromCsv(String edgeCsv) {
        fromCsv(new File(edgeCsv));
    }

    private void fromCsv(File edgeFile) {
        InputStreamReader read;
        BufferedReader bufferedReader;
        Map<String, Node> nodeMap = new HashMap<>();
        try {
            if (edgeFile.isFile() && edgeFile.exists()) {
                read = new InputStreamReader(new FileInputStream(edgeFile), "utf8");
                bufferedReader = new BufferedReader(read);
                bufferedReader.mark((int) edgeFile.length() + 1);
                String line = null;

                while ((line = bufferedReader.readLine()) != null) {
                    String[] p = line.split("\t");
                    String src = p[0];
                    String tar = p[1];
                    if (!nodeMap.containsKey(src)) {
                        Node n1 = graphModel.factory().newNode(src);
                        nodeMap.put(src, n1);
                    }
                    if (!nodeMap.containsKey(tar)) {
                        Node n2 = graphModel.factory().newNode(tar);
                        nodeMap.put(tar, n2);
                    }
                }
                graph.addAllNodes(nodeMap.values());

                bufferedReader.reset();
                while ((line = bufferedReader.readLine()) != null) {
                    String[] p = line.split("\t");
                    String src = p[0];
                    String tar = p[1];
                    double w = 1f;
//                    if (p.length > 2) w = Double.parseDouble(p[2]);
                    Edge e = graphModel.factory().newEdge(nodeMap.get(src), nodeMap.get(tar), 0, w, true);
                    graph.addEdge(e);
                }

                bufferedReader.close();
                read.close();
            }


        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void fromGF(String filePath) {
        File file = new File(filePath);
        fromGF(file);
    }

    private void fromGF(File file) {
        ImportController importController = Lookup.getDefault().lookup(ImportController.class);
        //Import file
        Container container;
        try {
            container = importController.importFile(file);
            container.getLoader().setEdgeDefault(EdgeDirectionDefault.DIRECTED);   //Force DIRECTED
        } catch (Exception ex) {
            ex.printStackTrace();
            return;
        }

        //Append imported data to GraphAPI
        importController.process(container, new DefaultProcessor(), workspace);
    }

    private void loadFile(String filePath) {
        if (filePath.matches(".*csv")) {
            fromCsv(filePath);
        } else fromGF(filePath);
    }

    private void loadFile(File file) {
        String filePath = file.getPath();
        if (filePath.matches(".*csv")) {
            fromCsv(file);
        } else fromGF(file);
    }


    public void showStat() {
        System.out.println("Nodes: " + graph.getNodeCount());
        System.out.println("Edges: " + graph.getEdgeCount());
    }

    public int getNodeCount() {
        return graph.getNodeCount();
    }
}

