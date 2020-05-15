import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class Main {

    public static void partition(String nodeCsv, String edgeCsv, String outDir) {
//        MyLouvain lou = new MyLouvain(
//                "data/node_dblp_only.csv",
//                "data/edge_dblp_only.csv");
        MyLouvain lou = new MyLouvain(nodeCsv, edgeCsv);
        lou.execute();
        File dir = new File(outDir);
        if (!dir.isDirectory()) dir.mkdir();
        lou.ensureOneNodeCommunityID();
        lou.saveCommunityPartition(outDir + "/partition.csv");
        lou.partitionAndSaveTo(outDir + "/community");
        lou.saveMegaGraph(outDir + "/mega_graph");
//        lou.saveCommunityExternalEdges(outDir + "/external");
    }


    public static void layout(String nodePath, String edgePath, String outPath, String subDir, boolean isMega) {
        System.out.println("Layout " + edgePath + "...");
        CitationNetwork net = new CitationNetwork(nodePath, edgePath);
        net.showStat();
        if (net.getNodeCount() <= 20000) {
            net.layout_fa2(500, true, false);
            if (isMega) net.rankSizeBy("size", 1, 10000);
            net.layout_fa2(1500, true, true);
            // net.setPreview();
            // net.exportTo(outPath);
            net.exportXYR(outPath, isMega);
        } else { // recursively partition and layout
            partition(null, edgePath, subDir);
            layoutCommunity(subDir);
            // layout mega graph
            layout(subDir + "/mega_graph/mega_node.csv", subDir + "/mega_graph/mega_edge.csv",
                    subDir + "/mega_graph/layout.csv", subDir + "/mega_sub", true);
            // merge
            merge(subDir + "/layout", subDir + "/mega_graph/layout.csv", outPath);
        }
    }

    public static void layoutCommunity(String baseDir) {
        File comDir = new File(baseDir + "/community");
        String[] fileNames = comDir.list();
        new File(baseDir + "/layout").mkdir();
        for (String fileName : fileNames) {
            File file = new File(comDir.getPath() + "/" + fileName);
            if (file.length() == 0) {
                System.out.print("\rdeleting " + file.getName());
                file.delete();
            } else {
                layout(null, comDir.getPath() + "/" + fileName, baseDir + "/layout/" + file.getName(),
                        baseDir + "/" + file.getName().substring(0, file.getName().indexOf(".")), false);
            }
        }
    }

    static class NodeAttr {
        float x, y, r;

        NodeAttr(float x, float y, float r) {
            this.x = x;
            this.y = y;
            this.r = r;
        }
    }

    public static void updatePos(NodeAttr na, Map<Integer, NodeAttr> cNodes) {
        // find center
        float cx = 0, cy = 0;
        int i = 0;
        for (NodeAttr cur : cNodes.values()) {
            if (i == 0) {
                cx = cur.x;
                cy = cur.y;
            } else {
                cx = cx / (i + 1) * i + cur.x / (i + 1);
                cy = cy / (i + 1) * i + cur.y / (i + 1);
            }
        }
        // find max radius
        float maxR = 0;
        for (NodeAttr cur : cNodes.values()) {
            float curR = (float) Math.sqrt((cur.x - cx) * (cur.x - cx) + (cur.y - cy) * (cur.y - cy));
            if (curR > maxR) maxR = curR;
        }
        // update position
        for (NodeAttr cur : cNodes.values()) {
            cur.x = na.x + (cur.x - cx) / maxR * na.r;
            cur.y = na.y + (cur.y - cy) / maxR * na.r;
        }
    }

    public static void merge(String cDir, String megaLayoutPath, String outPath) {
        // load mega graph
        System.out.println("Loading mega nodes...");
        Map<Integer, NodeAttr> megaNodes = new HashMap<>();
        try {
            BufferedReader read = new BufferedReader(new FileReader(megaLayoutPath));
            String line;
            while ((line = read.readLine()) != null) {
                String[] p = line.split("\t");
                megaNodes.put(Integer.parseInt(p[0]),
                        new NodeAttr(Float.parseFloat(p[1]), Float.parseFloat(p[2]), Float.parseFloat(p[3])));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println("Updating and saving all nodes...");
        // output
        try {
            BufferedWriter out = new BufferedWriter(new FileWriter(outPath));

            // calculate new position
            for (Map.Entry<Integer, NodeAttr> entry : megaNodes.entrySet()) {
                int cid = entry.getKey();
                NodeAttr na = entry.getValue();
                if (na.r > 0) {
                    try {
                        String cPath = cDir + "/" + cid + ".csv";
                        File file = new File(cPath);
//                        System.out.println(cPath);
                        BufferedReader read = new BufferedReader(new FileReader(file));
                        String line;

                        // save cur pos to map
                        Map<Integer, NodeAttr> cNodes = new HashMap<>();
                        while ((line = read.readLine()) != null) {
                            String[] p = line.split("\t");
                            cNodes.put(Integer.parseInt(p[0]),
                                    new NodeAttr(Float.parseFloat(p[1]), Float.parseFloat(p[2]), 0));
                        }

                        // updatePost
                        updatePos(na, cNodes);
                        // save to
                        for (Map.Entry<Integer, NodeAttr> e : cNodes.entrySet()) {
                            out.write("" + e.getKey() + "\t" + e.getValue().x + "\t" + e.getValue().y + "\t" + cid + "\n");
                        }

                        //
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else { // if 0, one-node community, save directly
                    try {
                        out.write("" + cid + "\t" + na.x + "\t" + na.y + "\t" + cid + "\n");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                out.flush();


            }
            out.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public static void main(String[] args) {
//        String pathname = "../material/LesMiserables.gexf";
//        layout("data/dblp/community/108979965.csv", "test0.png");


        File dir = new File("data/dblp0/community");
        File[] files = dir.listFiles();
        for (File file : files) {
            if (file.length() == 0) {
                System.out.print("\rdeleting " + file.getName());
                file.delete();
            }
        }

//        layoutCommunity("data/dblp");

//        partition();

//        460513896
//        MyLouvain lou = new MyLouvain(612761,
//                null,
//                "data/dblp0/community/89294878.csv");
//        MyLouvain lou = new MyLouvain(null, "data/dblp0/community/108979965.csv");
//        lou.execute();
//        String outDir = "data/subgraph";
//        lou.partitionAndSaveTo(outDir + "/community");
//        lou.saveMegaGraph(outDir + "/mega_graph");

//        lou.saveCommunityExternalEdges(outDir + "/external");


//        CitationNetwork net = new CitationNetwork("data/dblp0/community/460513896.csv");
//        CitationNetwork net = new CitationNetwork("../material/LesMiserables.gexf");

        partition("data/node_dblp_only.csv", "data/edge_dblp_only.csv", "data/dblp");
//        layoutCommunity("data/dblp");
//        layout("data/dblp/mega_graph/mega_node.csv", "data/dblp/mega_graph/mega_edge.csv",
//                "data/dblp/mega_graph/layout.csv", "data/dblp/mega_sub", true);
//        merge("data/dblp/layout", "data/dblp/mega_graph/layout.csv", "data/dblp/layout.csv");

//        layout(null, "data/test/108979965.csv", "data/test/layout.csv",
//                "data/test/108979965", false);

    }
}
