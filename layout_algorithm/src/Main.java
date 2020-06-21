import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;

public class Main {

    // thread pool
    private static int nThreads = 6;
    private static ExecutorService fixedThreadPool = Executors.newFixedThreadPool(nThreads);
    private static ThreadPoolExecutor tpe = (ThreadPoolExecutor) fixedThreadPool;

    public static void partition(String nodeCsv, String edgeCsv, String outDir) {
//        MyLouvain lou = new MyLouvain(
//                "data/node_dblp_only.csv",
//                "data/edge_dblp_only.csv");
        File dir = new File(outDir);
        if (dir.isDirectory()) return;
        MyLouvain lou = new MyLouvain(nodeCsv, edgeCsv);
        lou.execute();
        if (!dir.isDirectory()) dir.mkdir();
        lou.ensureOneNodeCommunityID();
        lou.saveCommunityPartition(outDir + "/partition.csv");
        lou.partitionAndSaveTo(outDir + "/community");
        lou.saveMegaGraph(outDir + "/mega_graph");
//        lou.saveCommunityExternalEdges(outDir + "/external");
    }


    public static Future<?> layout(String nodePath, String edgePath, String outPath, String subDir, boolean isMega) {
        System.out.println("Layout " + edgePath + "...");
        CitationNetwork net = new CitationNetwork(nodePath, edgePath);
        net.showStat();
        if (net.getNodeCount() <= 50000) {
            if (net.getNodeCount() >= 5000)
                return fixedThreadPool.submit(new Runnable() {
                    @Override
                    public void run() {
                        System.out.println("Layout in thread...");
                        net.layout_fa2((net.getNodeCount() / 10000 + 1) * 1000, false, false);
//                        if (isMega) net.rankSizeBy("size", 5, 500);
//                        else net.rankSizeBy("degree", 5, 500);
                        net.layout_fa2((net.getNodeCount() / 10000 + 1) * 2000, false, true);
                        net.exportXYR(outPath);
                    }
                });
            else {
                net.layout_fa2((net.getNodeCount() / 10000 + 1) * 1000, false, false);
//                if (isMega) net.rankSizeBy("size", 5, 500);
//                else net.rankSizeBy("degree", 5, 500);
                net.layout_fa2((net.getNodeCount() / 10000 + 1) * 2000, false, true);
                net.exportXYR(outPath);
            }


//            net.setPreview();
//            net.exportTo("testtest.png");
        } else { // recursively partition and layout
            partition(nodePath, edgePath, subDir);
            layoutCommunity(subDir);
            // layout mega graph
            Future<?> future = layout(subDir + "/mega_graph/mega_node.csv", subDir + "/mega_graph/mega_edge.csv",
                    subDir + "/mega_graph/layout.csv", subDir + "/mega_sub", true);
            if (future != null)
                try {
                    future.get();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            // merge
            merge(subDir + "/layout", subDir + "/mega_graph/layout.csv", outPath);
        }
        return null;
    }

    public static void layoutCommunity(String baseDir) {
        File comDir = new File(baseDir + "/community");
        String[] fileNames = comDir.list();
        File layoutDir = new File(baseDir + "/layout");
        if (!layoutDir.exists()) layoutDir.mkdir();
        List<Future<?>> futures = new ArrayList<>();

        for (String fileName : fileNames) {
//            File file = new File(comDir.getPath() + "/" + fileName);
            File layoutFile = new File(baseDir + "/layout/" + fileName);
            if (layoutFile.exists()) continue;
            Future<?> future = layout(baseDir + "/community_node/" + fileName,
                    comDir.getPath() + "/" + fileName,
                    baseDir + "/layout/" + fileName,
                    baseDir + "/" + fileName.substring(0, fileName.indexOf(".")), false);
            if (future != null) futures.add(future);
        }
        for (Future<?> t : futures)
            try {
                t.get();
            } catch (Exception e) {
                e.printStackTrace();
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
            cur.r = (cur.r * na.r / maxR);
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
                        if (!file.exists()) {
                            System.out.println(cPath + " not found");
                            file = new File("data/dblp0/layout/" + cid + ".csv");
//                            continue;
                        }
//                        System.out.println(cPath);
                        BufferedReader read = new BufferedReader(new FileReader(file));
                        String line;

                        // save cur pos to map
                        Map<Integer, NodeAttr> cNodes = new HashMap<>();
                        while ((line = read.readLine()) != null) {
                            String[] p = line.split("\t");
                            float r = 1f;
                            if (p.length > 3) r = Float.parseFloat(p[3]);
                            cNodes.put(Integer.parseInt(p[0]),
                                    new NodeAttr(Float.parseFloat(p[1]), Float.parseFloat(p[2]), r));
                        }

                        // updatePost
                        updatePos(na, cNodes);
                        // save to
                        for (Map.Entry<Integer, NodeAttr> e : cNodes.entrySet()) {
                            out.write("" + e.getKey() + "\t" + e.getValue().x + "\t" + e.getValue().y + "\t" +
                                    e.getValue().r + "\t" + cid + "\n");
                        }
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
        System.out.println("Merge complete");
    }

    public static void layoutDBLP() {
        partition("data/node_dblp_only.csv", "data/edge_dblp_only.csv", "data/dblp");
        layoutCommunity("data/dblp");
        try {
            Future<?> future = layout("data/dblp/mega_graph/mega_node.csv", "data/dblp/mega_graph/mega_edge.csv",
                    "data/dblp/mega_graph/layout.csv", "data/dblp/mega_sub", true);
            if (future != null) future.get();
        } catch (Exception e) {
            e.printStackTrace();
        }

        merge("data/dblp/layout", "data/dblp/mega_graph/layout.csv", "data/dblp/layout.csv");

    }

    public static void testLayout(String nodePath, String edgePath, String outPath, String subDir, boolean isMega) {
        System.out.println("Layout " + edgePath + "...");
        CitationNetwork net = new CitationNetwork(nodePath, edgePath);
        net.showStat();

        if (isMega) net.rankSizeBy("size", 3, 300);
        else net.rankSizeBy("degree", 1, 10000);

        net.setPreview();

        for (int i = 0; i < 3; i++) {
            net.layout_fa2(1000, true, false);
            net.exportTo("testtest-" + i + ".png");
        }

        for (int i = 0; i < 3; i++) {
            net.layout_fa2(1000, true, true);
            net.exportTo("testtest-3-" + i + ".png");
        }
    }

    public static void communitySVG(int cid) {
        SVG svg = new SVG();
        svg.fromCsv("data/dblp/layout/" + cid + ".csv", "data/edge_dblp_only.csv");
        svg.exportTo("data/dblp/layout_" + cid + ".svg");
    }

    public static void re_layout_mega(int cid) {
        try {
            Future<?> future = layout("data/dblp/" + cid + "/mega_graph/mega_node.csv", "data/dblp/" + cid + "/mega_graph/mega_edge.csv",
                    "data/dblp/" + cid + "/mega_graph/layout.csv", "data/dblp/" + cid + "/mega_sub", true);
            if (future != null) future.get();
        } catch (Exception e) {
            e.printStackTrace();
        }
        SVG svg = new SVG();
        svg.fromCsv("data/dblp/" + cid + "/mega_graph/layout.csv", "data/edge_dblp_only.csv");
        svg.exportTo("data/dblp/test.svg");

        merge("data/dblp/" + cid + "/layout", "data/dblp/" + cid + "/mega_graph/layout.csv", "data/dblp/layout/" + cid + ".csv");
    }


    public static void main(String[] args) {
        layoutDBLP();

        SVG svg = new SVG();
        svg.fromCsv("data/dblp/layout.csv", "data/edge_dblp_only.csv");
        svg.exportTo("data/dblp/layout.svg");

        fixedThreadPool.shutdown();
    }
}
