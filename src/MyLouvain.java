import java.io.*;
import java.util.*;

/**
 * Implement the Louvain algorithm for undirected graph
 */
public class MyLouvain {
    private class Node {
        int id;
        int k_i; // sum of edge weight of all its neighbors
        int community;
        List<Node> neighbors;

        Node(int id) {
            this.id = id;
            this.community = id;
            neighbors = new ArrayList<>();
        }

        int num_in(int c) {
            int ans = 0;
            for (Node node : neighbors) {
                if (node.community == c)
                    ans++;
            }
            return ans;
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
    }

    private List<Node> nodes;
    private Map<Integer, Community> communities; // community_id -> sum_tot: sum of k_i for all nodes in c
    private int num_edges;

    public MyLouvain(int capacity, String nodeCsv, String edgeCsv) {
        nodes = new ArrayList<>(capacity);
        communities = new HashMap<>(capacity);

        from_csv(nodeCsv, edgeCsv);

        init_parameters();
    }

    public void execute() {
        firstPhase();
        writeTo("phase1.csv");
    }

    public void writeTo(String filePath) {
        try {
            System.out.println("Writing to " + filePath + " ...");
            File file = new File(filePath);
            file.createNewFile();
            BufferedWriter out = new BufferedWriter(new FileWriter(file));
            for (Node node : nodes) {
                out.write(node.id + "\t" + node.community + "\n");
            }

            out.flush();
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public double modularity() {
        double ans = 0;
        for (Node p : nodes) {
            for (Node q : nodes) {
                if (p != q && p.community == q.community) {
                    double tmp = 0;
                    tmp -= (double) p.neighbors.size() * q.neighbors.size() / num_edges / 2;
                    if (p.neighbors.contains(q)) tmp += 1;
                    ans += tmp / num_edges / 2;
                }
            }
        }
        return ans;
    }

    private void firstPhase() {
        System.out.println("First phase...");
        boolean flag = true;
        double q = -2;
        while (flag) {
            flag = false;
            for (Node node : nodes) {
                Map<Integer, Integer> c2w = new HashMap<>();
                for (Node nei : node.neighbors) {
                    if (node.community != nei.community) {
                        c2w.put(nei.community, c2w.getOrDefault(nei.community, 0) + 1);
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
                if (dq > 0) {
                    flag = true;
                    communities.get(node.community).size--;
                    int old = communities.get(node.community).sum_tot;
                    if (old == node.k_i) communities.remove(node.community);
                    else communities.get(node.community).sum_tot = old - node.k_i;
                    communities.get(bestC).sum_tot += node.k_i;
                    node.community = bestC;
                    communities.get(bestC).size++;
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
            double m = modularity();
            System.out.println("Modularity: " + m);

            // stop
            if (m == q) {
                break;
            }
            q = m;
        }

    }

    private void init_parameters() {
        for (Node node : nodes) {
            node.k_i = node.neighbors.size();
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
                    tmp.get(src).neighbors.add(tmp.get(tar));
                    tmp.get(tar).neighbors.add(tmp.get(src));
                }
                bufferedReader.close();
                read.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

}
