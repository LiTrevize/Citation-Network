from pymongo import MongoClient
import json
from utils import count_lines
import pickle as pkl
from info import *
from collections import defaultdict

client = MongoClient("localhost", 27017,
                     username="root",
                     password="pwd")

db_node = client.citationNetwork.node


def add_basic():
    nodes = []
    total = count_lines('node_dblp_only.csv')
    with open('node_dblp_only.csv', 'r', encoding='utf8') as f:
        for i, line in enumerate(f):
            nid, title = line.strip().split('\t')
            nodes.append({'_id': int(nid), 'title': title})

            if i % 10000 == 0:
                print('\r{:.4f}\t'.format(1.*i/total), end='')
                db_node.insert_many(nodes)
                nodes.clear()
        print()

    edges = defaultdict(list)
    total = count_lines('edge_dblp_only.csv')
    with open('edge_dblp_only.csv', 'r') as f:
        for i, line in enumerate(f):
            src, tar = line.strip().split('\t')
            edges[int(src)].append(int(tar))

            if i and i % 10000 == 0:
                print('\r{:.4f}\t'.format(1.*i/total), end='')
        print()

    for src in edges:
        cited = edges[src]
        db_node.update_one(
            {'_id': src}, {'$set': {'citation': len(cited), 'citedBy': cited}})


def add_layout():
    total = count_lines("dblp/layout.csv")
    with open("dblp/layout.csv", "r") as f:
        for i, line in enumerate(f):
            nid, x, y, r, cid = line.strip().split('\t')
            newvalues = {
                '$set': {'x': float(x), 'y': float(y), 'r': float(r), 'cid': int(cid)}
            }
            db_node.update_one({'_id': int(nid)}, newvalues)

            if i % 10000 == 0:
                print('\r{:.4f}\t'.format(1.*i/total), end='')
                # 0.3459
        print()


def getMaxMin():
    xmin = 0
    xmax = 0
    ymin = 0
    ymax = 0
    for node in db_node.find({"x": {"$exists": True}}):
        # print(node)
        x = node['x']
        y = node['y']
        xmin = min(x, xmin)
        xmax = max(x, xmax)
        ymin = min(y, ymin)
        ymax = max(y, ymax)
    print(xmin, xmax, ymin, ymax)
    print(xmin, ymin, xmax-xmin, ymax-ymin)


def add_all():
    nodes = defaultdict(dict)
    total = count_lines('node_dblp_only.csv')
    with open('node_dblp_only.csv', 'r', encoding='utf8') as f:
        for i, line in enumerate(f):
            nid, title = line.strip().split('\t')
            nodes[int(nid)].update({'_id': int(nid), 'title': title,'citedBy':[]})
            # db_node.insert_many(nodes)
        # print()

    # edges = defaultdict(list)
    total = count_lines('edge_dblp_only.csv')
    with open('edge_dblp_only.csv', 'r') as f:
        for i, line in enumerate(f):
            src, tar = line.strip().split('\t')
            nodes[int(src)]['citedBy'].append(int(tar))

            if i and i % 10000 == 0:
                print('\r{:.4f}\t'.format(1.*i/total), end='')
        print()

    total = count_lines("dblp/layout.csv")
    tmp = []
    with open("dblp/layout.csv", "r") as f:
        for i, line in enumerate(f):
            nid, x, y, r, cid = line.strip().split('\t')
            dic = nodes[int(nid)]
            dic.update({'x': float(x), 'y': float(y), 'r': float(r), 'cid': int(cid),'citation':len(dic['citedBy'])})
            tmp.append(dic)

            if i % 10000 == 0:
                print('\r{:.4f}\t'.format(1.*i/total), end='')
                db_node.insert_many(tmp)
                tmp.clear()
                # 0.3459
        print()


if __name__ == "__main__":
    # db_node.rename('node0')
    # add_basic()
    # add_layout()
    # getMaxMin()
    add_all()