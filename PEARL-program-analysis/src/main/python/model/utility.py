def read_file(filepath):
    with open(filepath) as f:
        str_text = f.read()
    return str_text


def is_not_empty_list(list):
    return len(list) > 0
