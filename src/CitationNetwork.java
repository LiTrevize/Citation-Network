import org.gephi.appearance.api.AppearanceController;
import org.gephi.appearance.api.AppearanceModel;
import org.gephi.appearance.api.Function;
import org.gephi.appearance.api.Ranking;
import org.gephi.appearance.plugin.RankingElementColorTransformer;
import org.gephi.appearance.plugin.RankingNodeSizeTransformer;
import org.gephi.appearance.spi.Transformer;
import org.gephi.graph.api.Column;
import org.gephi.layout.plugin.forceAtlas2.ForceAtlas2;
import org.gephi.graph.api.DirectedGraph;
import org.gephi.graph.api.GraphController;
import org.gephi.graph.api.GraphModel;
import org.gephi.io.exporter.api.ExportController;
import org.gephi.io.importer.api.Container;
import org.gephi.io.importer.api.EdgeDirectionDefault;
import org.gephi.io.importer.api.ImportController;
import org.gephi.io.processor.plugin.DefaultProcessor;
import org.gephi.preview.api.PreviewController;
import org.gephi.preview.api.PreviewModel;
import org.gephi.preview.api.PreviewProperty;
import org.gephi.statistics.plugin.GraphDistance;
import org.openide.util.Lookup;
import org.gephi.project.api.ProjectController;
import org.gephi.project.api.Workspace;

import java.awt.*;
import java.io.File;
import java.io.IOException;

public class CitationNetwork {
    // project
    static private ProjectController pc = Lookup.getDefault().lookup(ProjectController.class);

    private Workspace workspace;
    private GraphModel graphModel;
    private PreviewModel previewModel;
    DirectedGraph graph;

    public CitationNetwork() {
        init();
    }

    public CitationNetwork(String pathname) {
        init();
        loadFile(pathname);
    }

    public void layout_fa2(int num_iter) {
        ForceAtlas2 fa2 = new ForceAtlas2(null);
        fa2.setGraphModel(graphModel);
        fa2.setLinLogMode(true);
        fa2.initAlgo();
        for (int i = 0; i < num_iter && fa2.canAlgo(); i++) {
            fa2.goAlgo();
        }
        fa2.endAlgo();
    }

    public void setPreview() {
        AppearanceController appearanceController = Lookup.getDefault().lookup(AppearanceController.class);
        AppearanceModel appearanceModel = appearanceController.getModel();

        //Rank color by Degree
        Function colorDegreeRanking = appearanceModel.getNodeFunction(graph, AppearanceModel.GraphFunction.NODE_INDEGREE, RankingElementColorTransformer.class);
        RankingElementColorTransformer colorDegreeTransformer = (RankingElementColorTransformer) colorDegreeRanking.getTransformer();
        colorDegreeTransformer.setColors(new Color[]{new Color(0x00EAFF),new Color(0x3C8CE7)});
//        colorDegreeTransformer.setColors(new Color[]{new Color(0x17e0ff),new Color(0x66aaff),new Color(0x7971ff),new Color(0x7a1eff)});
        colorDegreeTransformer.setColorPositions(new float[]{0f, 1f});
        appearanceController.transform(colorDegreeRanking);

        // Rank size by degree
        Function sizeDegreeRanking = appearanceModel.getNodeFunction(graph, AppearanceModel.GraphFunction.NODE_INDEGREE, RankingNodeSizeTransformer.class);
        RankingNodeSizeTransformer sizeDegreeTransformer = (RankingNodeSizeTransformer) sizeDegreeRanking.getTransformer();
        sizeDegreeTransformer.setMinSize(10);
        sizeDegreeTransformer.setMaxSize(50);
        appearanceController.transform(sizeDegreeRanking);

        // Get Centrality
//        GraphDistance distance = new GraphDistance();
//        distance.setDirected(true);
//        distance.execute(graphModel);
        // Rank size by centrality
//        Column centralityColumn = graphModel.getNodeTable().getColumn(GraphDistance.BETWEENNESS);
//        Function centralityRanking = appearanceModel.getNodeFunction(graph, centralityColumn, RankingNodeSizeTransformer.class);
//        RankingNodeSizeTransformer centralityTransformer = (RankingNodeSizeTransformer) centralityRanking.getTransformer();
//        centralityTransformer.setMinSize(10);
//        centralityTransformer.setMaxSize(50);
//        appearanceController.transform(centralityRanking);
        // straight line
        previewModel.getProperties().putValue(PreviewProperty.EDGE_CURVED, false);
        previewModel.getProperties().putValue(PreviewProperty.EDGE_THICKNESS,1);
        previewModel.getProperties().putValue(PreviewProperty.EDGE_OPACITY,60);
        previewModel.getProperties().putValue(PreviewProperty.NODE_BORDER_WIDTH,0);
    }

    public void exportTo(String pathname) {
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
    }

    private void loadFile(String pathname) {
        ImportController importController = Lookup.getDefault().lookup(ImportController.class);
        //Import file
        Container container;
        try {
            File file = new File(pathname);
            container = importController.importFile(file);
            container.getLoader().setEdgeDefault(EdgeDirectionDefault.DIRECTED);   //Force DIRECTED
        } catch (Exception ex) {
            ex.printStackTrace();
            return;
        }

        //Append imported data to GraphAPI
        importController.process(container, new DefaultProcessor(), workspace);
    }


    public void showStat() {
        System.out.println("Nodes: " + graph.getNodeCount());
        System.out.println("Edges: " + graph.getEdgeCount());
    }
}

