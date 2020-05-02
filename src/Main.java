public class Main {

    public static void main(String[] args) {
//        String pathname = "../material/LesMiserables.gexf";
//        CitationNetwork net = new CitationNetwork(pathname);
//        net.showStat();
//        net.layout_fa2(100);
//        net.setPreview();
//        net.exportTo("test.svg");
//        MyLouvain lou = new MyLouvain(4031392,
//                "data/node_dblp_only.csv",
//                "data/edge_dblp_only.csv");
        MyLouvain lou = new MyLouvain(1000,
                "data/node_test.csv",
                "data/edge_test.csv");
        lou.execute();
    }
}
