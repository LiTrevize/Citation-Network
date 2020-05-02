from gexf import Gexf
import pickle as pkl
from utils import *


def gen_node_csv(out, dblp_only=True):
    print('loading reference...')
    with open('reference.pkl', 'rb') as f:
        ref = pkl.load(f)
    print('loading titles...')
    with open('titles.pkl', 'rb') as f:
        titles = pkl.load(f)
    print('writing...')
    with open(out, 'w', encoding='utf8') as g:
        if dblp_only:
            for idx in ref:
                write_csv(g, idx, titles[idx])
        else:
            for idx in set(ref.keys()).union(*ref.values()):
                write_csv(g, idx, titles[idx])


def gen_edge_csv(out, dblp_only=True):
    print('loading reference...')
    with open('reference.pkl', 'rb') as f:
        ref = pkl.load(f)
    print('writing...')
    with open(out, 'w', encoding='utf8') as g:
        for idx in ref:
            for r in ref[idx]:
                if not dblp_only or r in ref:
                    write_csv(g, r, idx)


def gexf_from_csv(node_csv, edge_csv):
    gexf = Gexf("Jingyu", "The DBLP Citation Network")
    graph = gexf.addGraph("directed", "static", "The DBLP Citation Network")

    # attr_dblp = graph.addNodeAttribute('isDBLP', 'false', type='boolean')

    print('adding nodes...')
    tot = count_lines(node_csv)
    with open(node_csv, 'r', encoding='utf8') as f:
        for i, line in enumerate(f):
            idx, title = line.strip().split('\t')
            node = graph.addNode(str(idx), title)
            # node.addAttribute(attr_dblp, 'true')
            if i % 10000 == 0:
                print('\r{:.2f}%'.format(i*100/tot), end='')
        print()

    print('adding edges...')
    tot = count_lines(edge_csv)
    num_edge = 0
    with open(edge_csv, 'r', encoding='utf8') as f:
        for i, line in enumerate(f):
            src, tar = line.strip().split('\t')
            graph.addEdge(str(num_edge), str(src), str(tar))
            num_edge += 1
            if i % 10000 == 0:
                print('\r{:.2f}%'.format(i*100/tot), end='')
        print()

    print('writing to file...')
    with open('dblp_raw.gexf', 'wb') as g:
        gexf.write(g)


if __name__ == "__main__":
    # gen_node_csv('node_dblp_only.csv')
    # gen_edge_csv('edge_dblp_only.csv')
    # gen_node_csv('node_dblp_full.csv', False)
    # gen_edge_csv('edge_dblp_full.csv', False)
    gexf_from_csv('node_dblp_only.csv','edge_dblp_only.csv')