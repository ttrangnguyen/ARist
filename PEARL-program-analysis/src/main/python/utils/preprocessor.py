import re


def empty_string_literal(source_code):
    sb = []
    inside_string_literal = False
    for s in source_code.split("\\\""):
        t = s.replace("'\"'", "''").split("\"")
        for i in range(len(t)):
            if not inside_string_literal:
                sb.append(t[i].replace("''", "'\"'"))
            if i < len(t) - 1:
                sb.append('"')
                inside_string_literal = not inside_string_literal
    return ''.join(sb)


def remove_array_access_index(source_code):
    stack = []
    for s in re.findall("\\[|\\]|[^\\[\\]]+", source_code):
        if s == "]":
            while True:
                if len(stack) == 0 or stack.pop() == "[":
                    break
        stack.append(s)
    sb = []
    for s in stack:
        if s == "]":
            sb.append("[")
        sb.append(s)
    return ''.join(sb)


def normalize_method_invocation(s):
    bal = 0
    for i in range(len(s) - 1, -1, -1):
        if s[i] == '(':
            bal = bal + 1
        if s[i] == ')':
            bal = bal - 1
        if bal >= 0:
            s = s[:i + 1]
            break
    return s
