# Get data from acemap.sjtu.edu.cn

from pymongo import MongoClient
import json
from utils import count_lines
import pickle as pkl
from info import *

client = MongoClient(ADDR, PORT,
                     username=USERNAME,
                     password=PASSWORD)


def get_mapping():
    """get the mapping of dblp_id to acemap_id"""
    with open('dblp2am.csv', 'w') as f:
        for mapping in client.crawlerMapping.paper_mapping.find(
            {'_id': {'$regex': 'dblp'}}
        ):
            f.write('{}\t{}\n'.format(*mapping.values()))


def get_dblp():
    """get all dblp_id"""
    with open('dblp.csv', 'w') as f:
        for mapping in client.crawlerPaper.dblp.find():
            f.write('{}\n'.format(mapping['PaperID']))


def get_dblp_no_mapping():
    """get dblp id with no mapping"""
    has_mapping = set()
    with open('dblp2am.csv', 'r') as f:
        for line in f:
            has_mapping.add(line.strip().split('\t')[0])
    with open('dblp.csv', 'r') as f, open('dblp_no_mapping.csv', 'w') as g:
        for line in f:
            idx = line.strip()
            if idx not in has_mapping:
                g.write('{}\n'.format(idx))


def find_missing_mapping():
    """find the missing mapping by title"""
    with open('dblp_no_mapping.csv', 'r') as f, open('dblp2am_rem.csv', 'w') as g:
        for line in f:
            idx = line.strip()
            dblp = client.crawlerPaper.dblp.find_one({'PaperID': idx})
            try:
                title = dblp['Title']
            except:
                print(dblp)
                continue
            am = client.acemap.paper.find_one({'title': title})
            if am:
                g.write('{}\t{}\n'.format(idx, am['_id']))
                continue
            am = client.acemap.paper.find_one({'title': title.rstrip('.')})
            if am:
                g.write('{}\t{}\n'.format(idx, am['_id']))


def get_dblp_reference():
    """get id->[ref1,ref2,...] mapping"""
    ids = []
    ref = {}
    total = count_lines('dblp2am.csv')
    with open('dblp2am.csv', 'r') as f:
        for i, line in enumerate(f):
            _id = int(line.strip().split('\t')[1])
            ids.append(_id)

    i = 0
    p = 1000
    for j in range(len(ids)//p+1):
        for paper in client.acemap.paper.find(
            {'_id': {
                '$in': ids[p*j:p*(j+1)]}},
                ['reference_list']):

            ref[paper['_id']] = paper['reference_list']
            # g.write('{}\n'.format(paper))

            i += 1
            if i and i % 1000 == 0:
                print('\r{:.2f}%'.format(i/total*100), end='')
                # break
    print()
    print(total, i, len(ref.keys()))

    with open('dblp_ref.pkl', 'wb') as g:
        pkl.dump(ref, g)


def get_titles():
    """get id->title mapping"""
    with open('reference.pkl', 'rb') as f:
        ref = pkl.load(f)
    ids = set(list(ref.keys()))
    print(len(ids))
    ids.update(*ref.values())
    print(len(ids))
    ids = list(ids)
    total = len(ids)
    titles = {}
    i = 0
    p = 10000
    for j in range(len(ids)//p+1):
        for paper in client.acemap.paper.find(
            {'_id': {
                '$in': ids[p*j:p*(j+1)]}},
                ['title']):

            titles[paper['_id']] = paper['title']
            # g.write('{}\n'.format(paper))

            i += 1
            if i and i % 1000 == 0:
                print('\r{:.2f}%'.format(i/total*100), end='')
        # break
    print()
    print(len(titles.keys()))

    with open('titles.pkl', 'wb') as g:
        pkl.dump(titles, g)


if __name__ == "__main__":
    # get_dblp()
    # get_dblp_no_mapping()
    # get_reference()
    # get_titles()
    pass
