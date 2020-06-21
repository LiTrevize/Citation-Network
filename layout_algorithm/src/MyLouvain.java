import javafx.util.Pair;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Implement the Louvain algorithm for undirected graph
 */
public class MyLouvain {
    private class Node {
        int id;
        int k_i; // sum of edge weight of all its neighbors, including self-loop
        int cid;
        int size; // for super-node, size is the number of atomic node
        int loop;
        List<Node> neighbors;
        List<Integer> weights;

        Node(int id) {
            this.id = id;
            this.cid = id;
            neighbors = new ArrayList<>();
            weights = new ArrayList<>();
            size = 1;
            loop = 0;
        }

        int get_weight(Node node) {
            for (int i = 0; i < neighbors.size(); i++) {
                if (neighbors.get(i) == node)
                    return weights.get(i);
            }
            return 0;
        }

        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append(id).append(" ").append(cid);
            sb.append(" [");
            for (Node node : neighbors) sb.append(node.id).append(" ");
            sb.append("]");
            return sb.toString();

        }

    }

    private class Community {
        int id;
        int sum_tot;
        int size;
        int size_default;

        Community(int id, int val, int size) {
            this.id = id;
            this.size = size;
            sum_tot = val;
            this.size_default = size;
        }

        public String toString() {
            String ans = "";
            return ans + id + " " + size;
        }
    }

    private String nodeCsv, edgeCsv;

    private List<Node> nodes;
    private List<Node> nodes_default;
    private Map<Integer, Community> communities; // community_id -> sum_tot: sum of k_i for all nodes in c
    private int num_edges; // sum of weights of all the edges <=> number of edges for the original network

    private int MAX_COMMUNITY_SIZE = -1;
    private int DESIRED = 50000;

    public MyLouvain(String nodeCsv, String edgeCsv) {
        this.nodeCsv = nodeCsv;
        this.edgeCsv = edgeCsv;

        allocate(nodeCsv, edgeCsv);
        from_csv(nodeCsv, edgeCsv);

        init_parameters();

//        MAX_COMMUNITY_SIZE = nodes.size() / 2;
//        System.out.println(MAX_COMMUNITY_SIZE);
    }

    private void allocate(String nodeCsv, String edgeCsv) {
        nodes = new ArrayList<>();
        communities = new HashMap<>();
    }

    public void saveMegaGraph(String dirName) {
        // check for output directory
        File dir = new File(dirName);
        if (dir.isDirectory()) dir.delete();
        dir.mkdir();

        Set<Integer> s = new HashSet<>();

        try {
            File edgeFile = new File(dirName + "/mega_edge.csv");
            edgeFile.createNewFile();
            BufferedWriter writer = new BufferedWriter(new FileWriter(edgeFile));
            for (Node node : nodes) {
                for (int i = 0; i < node.neighbors.size(); i++) {
                    s.add(communities.get(node.cid).id);
                    s.add(communities.get(node.neighbors.get(i).cid).id);
                    writer.write("" + communities.get(node.cid).id + "\t" +
                            communities.get(node.neighbors.get(i).cid).id + "\t" + node.weights.get(i) + "\n");
                }
            }

            writer.flush();
            writer.close();

        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            File nodeFile = new File(dirName + "/mega_node.csv");
            nodeFile.createNewFile();
            BufferedWriter writer = new BufferedWriter(new FileWriter(nodeFile));
            for (Community c : communities.values())
                if (communities.size() <= DESIRED || s.contains(c.id) || c.size_default > 1)
                    writer.write("" + c.id + "\t" + c.size_default + "\n");
            writer.flush();
            writer.close();

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void ensureOneNodeCommunityID() {
        // non-consistent pairs
        Map<Integer, Integer> cid2nid = new HashMap<>(communities.size());
        for (Node node : nodes_default) {
            if (communities.get(node.cid).size == 1 && communities.get(node.cid).id != node.id)
                cid2nid.put(node.cid, node.id);
        }
        System.out.println(cid2nid.size());

        int nextID = 0;
        for (Map.Entry<Integer, Integer> e : cid2nid.entrySet()) {
            int cid = e.getKey();
            int nid = e.getValue();
            if (!cid2nid.containsKey(nid) && communities.containsKey(nid)) { // assign new cid for original nid
                while (communities.containsKey(nextID) || cid2nid.containsValue(nextID)) nextID++;
                communities.get(nid).id = nextID;
                nextID++;
            }
            // modify one-node community id
            communities.get(cid).id = nid;
        }
    }

    public void saveCommunityPartition(String filePath) {
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(filePath));
            for (Node node : nodes_default)
                writer.write("" + node.id + "\t" + communities.get(node.cid).id + "\n");

            writer.flush();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void partitionAndSaveTo(String dirName) {
        System.out.println("Partitioning and saving to " + dirName + "...");

        // check for output directory
        File dir = new File(dirName);
        if (dir.isDirectory()) dir.delete();
        dir.mkdir();

        Map<Integer, Integer> node2com = new HashMap<>(nodes_default.size());
        for (Node node : nodes_default) node2com.put(node.id, node.cid);

        //
        Map<Integer, List<Pair<Integer, Integer>>> cid2edge = new HashMap<>(communities.size());
        for (int cid : communities.keySet()) {
            cid2edge.put(cid, new ArrayList<>());
        }
        try {
            // select from edge files
            File eFile = new File(edgeCsv);
            InputStreamReader read = new InputStreamReader(
                    new FileInputStream(eFile), "utf8");
            BufferedReader bufferedReader = new BufferedReader(read);
            String line = null;

            while ((line = bufferedReader.readLine()) != null) {
                String[] p = line.split("\t");
                int src = Integer.parseInt(p[0]);
                int tar = Integer.parseInt(p[1]);
                if (node2com.get(src).equals(node2com.get(tar))) {
                    cid2edge.get(node2com.get(src)).add(new Pair<>(src, tar));
                }
            }
            bufferedReader.close();
            read.close();


            // open out file
            for (Map.Entry<Integer, List<Pair<Integer, Integer>>> entry : cid2edge.entrySet()) {
                int cid = entry.getKey();
                if (entry.getValue().size() == 0) continue;
                File f = new File(dirName + "/" + communities.get(cid).id + ".csv");
                f.createNewFile();
                BufferedWriter writer = new BufferedWriter(new FileWriter(f));

                for (Pair<Integer, Integer> p : entry.getValue()) {
                    writer.write("" + p.getKey() + "\t" + p.getValue() + "\n");
                }
                // flush and close
                writer.flush();
                writer.close();
            }

            // check if need to save node file
            boolean flag = false;
            for (Node node : nodes_default) {
                if (node.size > 1) {
                    flag = true;
                    break;
                }
            }
            if (flag) {
                Map<Integer, List<Node>> cid2node = new HashMap<>(communities.size());
                for (int cid : communities.keySet()) {
                    cid2node.put(cid, new ArrayList<>());
                }
                for (Node node : nodes_default) {
                    cid2node.get(communities.get(node.cid).id).add(node);
                }
                for (Map.Entry<Integer, List<Node>> entry : cid2node.entrySet()) {
                    int cid = entry.getKey();
                    String newDir = new File(dirName).getParent() + "/community_node";
                    new File(newDir).mkdir();
                    File f = new File(newDir + "/" + communities.get(cid).id + ".csv");
                    f.createNewFile();
                    BufferedWriter writer = new BufferedWriter(new FileWriter(f));

                    for (Node node : entry.getValue()) {
                        writer.write("" + node.id + "\t" + node.size + "\n");
                    }
                    // flush and close
                    writer.flush();
                    writer.close();
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }


    }

    public void saveCommunityExternalEdges(String dirName) {
        System.out.println("Saving out edges to " + dirName + "...");

        // check for output directory
        File dir = new File(dirName);
        if (dir.isDirectory()) dir.delete();
        dir.mkdir();

        Map<Integer, Map<Pair<Integer, Integer>, Integer>> cid2edge = new HashMap<>(communities.size());

        for (int cid : communities.keySet()) {
            cid2edge.put(cid, new HashMap<>());
        }
        for (Node node : nodes_default) {
            for (Node nei : node.neighbors) {
                if (node.cid != nei.cid) {
                    Pair<Integer, Integer> p = new Pair<>(node.id, nei.cid);
                    Map<Pair<Integer, Integer>, Integer> edges = cid2edge.get(node.cid);
                    edges.put(p, edges.getOrDefault(p, 0) + 1);
                }
            }
        }
        try {
            // select from nodes_default

            for (Map.Entry<Integer, Map<Pair<Integer, Integer>, Integer>> entry : cid2edge.entrySet()) {
                int cid = entry.getKey();
                Map<Pair<Integer, Integer>, Integer> edges = entry.getValue();

                // open out file
                File f = new File(dirName + "/" + cid + ".csv");
                f.createNewFile();
                BufferedWriter writer = new BufferedWriter(new FileWriter(f));

                for (Map.Entry<Pair<Integer, Integer>, Integer> e : edges.entrySet()) {
                    writer.write("" + e.getKey().getKey() + "\t" + e.getKey().getValue() + "\t" + e.getValue() + "\n");
                }

                // flush and close
                writer.flush();
                writer.close();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void execute() {
        SimpleDateFormat sdf = new SimpleDateFormat();
        sdf.applyPattern("yyyy-MM-dd HH:mm:ss a");
        System.out.println("Begining at " + sdf.format(new Date()));

        firstPhase();
        nodes_default = nodes;
//        logTo("log/round0.csv");
        secondPhase();

        int round = 0;
        while (firstPhase()) {
            round++;
            updateNodesDefault();
//            logTo("log/round" + round + ".csv");
            secondPhase();
        }

        System.out.println("Finished at " + sdf.format(new Date()));

    }

    public void logTo(String filePath) {
        try {
            System.out.println("Writing to " + filePath + " ...");
            File file = new File(filePath);
            file.createNewFile();
            BufferedWriter out = new BufferedWriter(new FileWriter(file));
            for (Node node : nodes_default) {
                out.write(node.id + "\t" + node.cid + "\n");
            }

            out.flush();
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void updateNodesDefault() {
        System.out.println("Updating nodes default...");
        Map<Integer, Integer> cc = new HashMap<>();
        for (Node superNode : nodes) cc.put(superNode.id, superNode.cid);
        for (Node node : nodes_default) {
            node.cid = cc.get(node.cid);
        }
    }

    // contract all nodes in one community to a super-node with self-loop
    private void secondPhase() {
        System.out.println("Second phase...");
        List<Node> nodes_old = nodes;
        nodes = new ArrayList<>(communities.size());
        Map<Integer, Node> id2node = new HashMap<>(communities.size()); // tmp map
        for (Map.Entry<Integer, Community> entry : communities.entrySet()) {
            Node superNode = new Node(entry.getKey());
            superNode.k_i = entry.getValue().sum_tot;
            superNode.cid = entry.getKey();
            superNode.size = entry.getValue().size_default;
            nodes.add(superNode);
            id2node.put(entry.getKey(), superNode);
        }
        // update neighbors and weights
        for (Node node : nodes_old) {
            Node superNode = id2node.get(node.cid);
            for (int i = 0; i < node.neighbors.size(); i++) {
                Node superNei = id2node.get(node.neighbors.get(i).cid);
                if (superNode == superNei) continue;
                int idx = superNode.neighbors.indexOf(superNei);
                if (idx >= 0) {
                    superNode.weights.set(idx, superNode.weights.get(idx) + node.weights.get(i));
                } else {
                    superNode.neighbors.add(superNei);
                    superNode.weights.add(node.weights.get(i));
                }
            }
        }
        // update self loop
        for (Node superNode : nodes) {
            superNode.loop = superNode.k_i;
            for (int w : superNode.weights) superNode.loop -= w;
//            System.out.println(superNode.loop);
        }
    }

    public double modularity() {
        double ans = 0;
        for (Node p : nodes) {
            for (Node q : nodes) {
                if (p != q && p.cid == q.cid) {
                    double tmp = 0;
                    tmp += p.get_weight(q) - (double) p.k_i * q.k_i / num_edges / 2;
                    ans += tmp / num_edges / 2;
                } else if (p == q) {
                    double tmp = 0;
                    tmp += p.loop - (double) p.k_i * q.k_i / num_edges / 2;
                    ans += tmp / num_edges / 2;
                }
            }
        }
        return ans;
    }

    /*
    return whether to continue
     */
    private boolean firstPhase() {
        System.out.println("First phase...");
        boolean flag = true;
        double q = -2;
        double avg = -1;
        int num_same = 0;
        int num_iter = 0;
        while (flag) {
            flag = false;
            num_iter++;
            System.out.println("Iteration: " + num_iter);
//            System.out.println(nodes);
            for (Node node : nodes) {
                // get k_i_in for node about all its adjacent communities
                Map<Integer, Integer> c2w = new HashMap<>();
                for (int i = 0; i < node.neighbors.size(); i++) {
                    Node nei = node.neighbors.get(i);
                    int w = node.weights.get(i);
                    // path reduce
                    if (node.cid != nei.cid) {
                        c2w.put(nei.cid, c2w.getOrDefault(nei.cid, 0) + w);
                    }
                }
                double dq = 0;
                int bestC = -1;
                for (int c : c2w.keySet()) {
                    int k_i_in = c2w.get(c);
                    double cur = (double) k_i_in * 2 - (double) communities.get(c).sum_tot * node.k_i / num_edges;
                    if (cur > dq) {
                        dq = cur;
                        bestC = c;
                    }
                }
                // add node to communities bestC
                if (dq > 0 && (MAX_COMMUNITY_SIZE < 0 || communities.get(bestC).size + 1 <= MAX_COMMUNITY_SIZE)) {
//                    System.out.println(node.id);
//                    System.out.println(bestC);
//                    System.out.println(dq / num_edges);
                    flag = true;
                    Community oldCom = communities.get(node.cid);
                    oldCom.size -= 1;
                    oldCom.size_default -= node.size;
//                    int old = oldCom.sum_tot;
                    if (oldCom.size == 0 || oldCom.size_default == 0) communities.remove(node.cid);
                    else oldCom.sum_tot -= node.k_i;
                    Community newCom = communities.get(bestC);
                    // update sum_tot
                    newCom.sum_tot += node.k_i;
                    node.cid = bestC;
                    newCom.size += 1;
                    newCom.size_default += node.size;
                }

            }
            // stat
            System.out.println("# of communities: " + communities.size());
            int max = 0, sum = 0, min = Integer.MAX_VALUE;
            for (Community c : communities.values()) {
                if (c.size > max) max = c.size;
                if (c.size < min) min = c.size;
                sum += c.size;
            }
            System.out.println("max community size: " + max);
            System.out.println("min community size: " + min);
            System.out.println("avg community size: " + (double) sum / communities.size());

            // if the result oscillate

            // if network is small, use modularity to stop
            if (nodes.size() < 10000) {
                double m = modularity();
                System.out.println("Modularity: " + m);

                // stop
                if (m == q) {
                    break;
                }
                q = m;
            }

            // use average community size
            // in case result oscillate
            if ((double) sum / communities.size() == avg)
                num_same++;
            else {
                num_same = 0;
                avg = (double) sum / communities.size();
            }
            if (num_same == 5) break;


        }
        return num_iter > 1;
    }

    /*
    calculate k_i and initialize communities with sum_tot=k_i
     */
    private void init_parameters() {
        for (Node node : nodes) {
            node.k_i = 0;
            for (int w : node.weights) node.k_i += w;
            node.k_i += node.loop;
            communities.put(node.id, new Community(node.id, node.k_i, 1));
            communities.get(node.id).size_default = node.size;
        }
    }

    public void from_csv(String nodeCsv, String edgeCsv) {
        /**
         * import directed graph from csv
         */
        try {
            Map<Integer, Node> tmp = new HashMap<>(nodes.size());
            // read node
            System.out.println("Reading nodes...");
            String encoding = "utf8";
            if (nodeCsv != null && new File(nodeCsv).exists()) {
                File file = new File(nodeCsv);
                InputStreamReader read = new InputStreamReader(
                        new FileInputStream(file), encoding);
                BufferedReader bufferedReader = new BufferedReader(read);
                String line = null;

                while ((line = bufferedReader.readLine()) != null) {
                    String[] p = line.split("\t");
                    int id = Integer.parseInt(p[0]);
                    Node node = new Node(id);
                    if (p.length > 1)
                        try {
                            node.size = Integer.parseInt(p[1]);
                        } catch (Exception e) {

                        }
                    nodes.add(node);
                    tmp.put(id, node);
                }
                bufferedReader.close();
                read.close();

            } else { // read node from edge file
                File file = new File(edgeCsv);
                if (file.isFile() && file.exists()) {
                    InputStreamReader read = new InputStreamReader(
                            new FileInputStream(file), encoding);
                    BufferedReader bufferedReader = new BufferedReader(read);
                    String line = null;

                    while ((line = bufferedReader.readLine()) != null) {
                        String[] p = line.split("\t");
                        int src = Integer.parseInt(p[0]);
                        int tar = Integer.parseInt(p[1]);
                        if (!tmp.containsKey(src)) {
                            Node node = new Node(src);
                            nodes.add(node);
                            tmp.put(src, node);
                        }
                        if (!tmp.containsKey(tar)) {
                            Node node = new Node(tar);
                            nodes.add(node);
                            tmp.put(tar, node);
                        }

                    }
                    bufferedReader.close();
                    read.close();
                }
            }

            // read edges
            System.out.println("Reading edges...");
            num_edges = 0;
            File file = new File(edgeCsv);
            if (file.isFile() && file.exists()) {
                InputStreamReader read = new InputStreamReader(
                        new FileInputStream(file), encoding);
                BufferedReader bufferedReader = new BufferedReader(read);
                String line = null;

                while ((line = bufferedReader.readLine()) != null) {
                    num_edges++;
                    String[] p = line.split("\t");
                    int src = Integer.parseInt(p[0]);
                    int tar = Integer.parseInt(p[1]);
                    int w = p.length > 2 ? Integer.parseInt(p[2]) : 1;
                    if (src != tar) {
                        tmp.get(src).neighbors.add(tmp.get(tar));
                        tmp.get(src).weights.add(w);
                        tmp.get(tar).neighbors.add(tmp.get(src));
                        tmp.get(tar).weights.add(w);
                    } else { // self loop
                        tmp.get(src).loop += w;
                    }

                }
                bufferedReader.close();
                read.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

}
