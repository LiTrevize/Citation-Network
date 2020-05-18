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
            nid, x, y, cid = line.strip().split('\t')
            newvalues = {
                '$set': {'x': float(x), 'y': float(y), 'cid': int(cid)}
            }
            db_node.update_one({'_id': int(nid)}, newvalues)

            if i % 10000 == 0:
                print('\r{:.4f}\t'.format(1.*i/total), end='')
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


if __name__ == "__main__":
    # add_basic()
    # add_layout()
    getMaxMin()