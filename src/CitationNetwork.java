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
import org.openide.util.Lookup;
import org.gephi.project.api.ProjectController;
import org.gephi.project.api.Workspace;

import java.io.File;
import java.io.IOException;

public class CitationNetwork {
    // project
    static private ProjectController pc = Lookup.getDefault().lookup(ProjectController.class);

    private Workspace workspace;
    private GraphModel graphModel;
    private PreviewModel previewModel;

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
        previewModel.getProperties().putValue(PreviewProperty.EDGE_CURVED, false);
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
        DirectedGraph graph = graphModel.getDirectedGraph();
        System.out.println("Nodes: " + graph.getNodeCount());
        System.out.println("Edges: " + graph.getEdgeCount());
    }
}

