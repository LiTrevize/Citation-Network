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
        int community;
        int size; // for super-node, size is the number of atomic node
        int loop;
        List<Node> neighbors;
        List<Integer> weights;

        Node(int id) {
            this.id = id;
            this.community = id;
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
            sb.append(id).append(" ").append(community);
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

        Community(int id, int val) {
            this.id = id;
            this.size = 1;
            sum_tot = val;
        }

        public String toString() {
            String ans = "";
            return ans + id + " " + size;
        }
    }

    private List<Node> nodes;
    private List<Node> nodes_default;
    private Map<Integer, Community> communities; // community_id -> sum_tot: sum of k_i for all nodes in c
    private int num_edges; // sum of weights of all the edges <=> number of edges for the original network

    public MyLouvain(int capacity, String nodeCsv, String edgeCsv) {
        nodes = new ArrayList<>(capacity);
        communities = new HashMap<>(capacity);

        from_csv(nodeCsv, edgeCsv);

        init_parameters();
    }

    public void execute() {
        SimpleDateFormat sdf = new SimpleDateFormat();
        sdf.applyPattern("yyyy-MM-dd HH:mm:ss a");
        System.out.println("Begining at " + sdf.format(new Date()));

        firstPhase();
        nodes_default = nodes;
        secondPhase();

        while (firstPhase()) {
            updateNodeDefault();
            secondPhase();
        }

        System.out.println("Finished at " + sdf.format(new Date()));
        writeTo("phase1.csv");
    }

    public void writeTo(String filePath) {
        try {
            System.out.println("Writing to " + filePath + " ...");
            File file = new File(filePath);
            file.createNewFile();
            BufferedWriter out = new BufferedWriter(new FileWriter(file));
            for (Node node : nodes_default) {
                out.write(node.id + "\t" + node.community + "\n");
            }

            out.flush();
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void updateNodeDefault() {
        for (Node node : nodes_default) {
            if (!communities.containsKey(node.community)) {
                for (Node superNode : nodes) {
                    if (superNode.id == node.community) {
                        node.community = superNode.community;
                    }
                }
            }
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
            superNode.community = entry.getKey();
            superNode.size = entry.getValue().size;
            nodes.add(superNode);
            id2node.put(entry.getKey(), superNode);
        }
        // update neighbors and weights
        for (Node node : nodes_old) {
            Node superNode = id2node.get(node.community);
            for (int i = 0; i < node.neighbors.size(); i++) {
                Node superNei = id2node.get(node.neighbors.get(i).community);
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
                if (p != q && p.community == q.community) {
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
                    if (node.community != nei.community) {
                        c2w.put(nei.community, c2w.getOrDefault(nei.community, 0) + w);
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
                if (dq > 0) {
//                    System.out.println(node.id);
//                    System.out.println(bestC);
//                    System.out.println(dq / num_edges);
                    flag = true;
                    Community oldCom = communities.get(node.community);
                    oldCom.size -= node.size;
//                    int old = oldCom.sum_tot;
                    if (oldCom.size == 0) communities.remove(node.community);
                    else oldCom.sum_tot -= node.k_i;
                    Community newCom = communities.get(bestC);
                    // update sum_tot
                    newCom.sum_tot += node.k_i;
                    node.community = bestC;
                    newCom.size += node.size;
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
            if (nodes.size() < 2000) {
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
            communities.put(node.id, new Community(node.id, node.k_i));
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
            File file = new File(nodeCsv);
            if (file.isFile() && file.exists()) {
                InputStreamReader read = new InputStreamReader(
                        new FileInputStream(file), encoding);
                BufferedReader bufferedReader = new BufferedReader(read);
                String line = null;

                while ((line = bufferedReader.readLine()) != null) {
                    int id = Integer.parseInt(line.split("\t")[0]);
                    Node node = new Node(id);
                    nodes.add(node);
                    tmp.put(id, node);
                }
                bufferedReader.close();
                read.close();
            }
            // read edges
            System.out.println("Reading edges...");
            num_edges = 0;
            file = new File(edgeCsv);
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
                    tmp.get(src).neighbors.add(tmp.get(tar));
                    tmp.get(src).weights.add(w);
                    tmp.get(tar).neighbors.add(tmp.get(src));
                    tmp.get(tar).weights.add(w);
                }
                bufferedReader.close();
                read.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

}
