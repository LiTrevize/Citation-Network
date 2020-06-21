from collections import defaultdict
import matplotlib.pyplot as plt
from utils import *


def hist_size():
    communities = defaultdict(int)

    with open('partition.csv', 'r') as f:
        for line in f:
            nid, cid = filter(int, line.strip().split('\t'))
            communities[cid] += 1


    plt.hist(communities.values(),bins=30,log=True,cumulative=-1)
    plt.xlabel('Size of communities')
    plt.ylabel('Counts')
    plt.show()


def add_citation():
    citation = defaultdict(int)
    total = count_lines('edge_dblp_only.csv')
    with open('edge_dblp_only.csv', 'r') as f:
        for i, line in enumerate(f):
            src, tar = line.strip().split('\t')
            citation[int(src)] += 1

            if i and i % 10000 == 0:
                print('\r{:.4f}\t'.format(1.*i/total), end='')
        print()
    # print(citation)
    with open('layout.csv','r') as f,open('layout_cite.csv','w') as g:
        for line in f:
            nid = int(line.strip().split('\t')[0])
            line = line.strip()+'\t'+str(citation[nid])+'\n'
            g.write(line)


def hist_citation():
    citation = defaultdict(int)

    with open('layout_cite.csv', 'r') as f:
        for line in f:
            c = int(line.strip().split('\t')[-1])
            citation[c] += 1


    plt.hist(citation.values(),bins=100,log=True,cumulative=-1,range=(0,100))
    plt.xlabel('Citation')
    plt.ylabel('Cumulative Counts')
    plt.show()


def show_com():
    communities = defaultdict(int)

    with open('partition.csv', 'r') as f:
        for line in f:
            nid, cid = filter(int, line.strip().split('\t'))
            communities[cid] += 1

    remove = []
    for cid in communities:
        if communities[cid]<100000:
            remove.append(cid)

    for cid in remove:
        del communities[cid]

    print(communities)
    print([int(x) for x in communities.keys()])

show_com()