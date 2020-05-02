def count_lines(filepath):
    with open(filepath, 'rb') as f:
        count = 0
        last_data = '\n'
        while True:
            data = f.read(0x400000)
            if not data:
                break
            count += data.count(b'\n')
            last_data = data
        if last_data[-1:] != b'\n':
            count += 1  # Remove this if a wc-like count is needed
    return count


def count_and_increment(dic, key, val=1):
    if key != 0 and not key:
        key = 'none'
    try:
        dic[key] += val
    except:
        dic[key] = val


def write_csv(f, *args):
    if len(args) == 0:
        return
    f.write(str(args[0]))
    for arg in args[1:]:
        f.write('\t{}'.format(arg))
    f.write('\n')


def show_dict(dic):
    print('{')
    for k in dic:
        print('\t{}: {}'.format(k, dic[k]))
    print('}')
