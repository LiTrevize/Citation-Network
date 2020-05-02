public class Main {

    public static void main(String[] args) {
        String pathname = "../material/LesMiserables.gexf";
        CitationNetwork net = new CitationNetwork(pathname);
        net.showStat();
        net.layout_fa2(100);
        net.setPreview();
        net.exportTo("test.svg");
    }
}
