import re
import np
from wordsegment import load, segment
from nltk import wordpunct_tokenize

load()

REG = r"(.+?)([A-Z])"


def splitCase(match):
    return match.group(1).lower() + "/" + match.group(2).lower()


def tokenize(word):
    if (len(word) == 0):
        return []

    camelCases = []

    # Do not predict snake_cases
    # if (word.find("_") != -1 or len(word) == 0):
    # return []

    ppWord = '%s' % word

    TAREG = re.compile("[<,>?\[\](){}&.|_=]")
    for match in re.finditer("[A-Z][A-Z\d]+", ppWord):
        result = ""
        s = match.start()
        e = match.end()
        if (e == len(ppWord) or bool(TAREG.match(ppWord[e]))):
            # CLASS => Class
            result = ppWord[s:e][1:].lower()
            result = ppWord[s:e][0] + result
        else:
            # CLASS => ClasS
            result = ppWord[s:e][1:-1].lower()
            result = ppWord[s:e][0] + result + ppWord[s:e][-1]

        ppWord = ppWord[:s] + result + ppWord[e:]

    # Split ...
    words = ppWord.split("...")

    # Split type argument character
    for w in words:
        tmpWord = ""

        for char in w:
            if (bool(TAREG.match(char))):
                camelCases.append(tmpWord)
                camelCases.append(char)
                tmpWord = ""
            else:
                tmpWord += char
        if (len(tmpWord) > 0):
            camelCases.append(tmpWord)

        camelCases.append("...")

    camelCases.pop()

    split_cases = [re.sub(REG, splitCase, w, 0).lower() for w in camelCases]
    words = [re.split("/", w) for w in split_cases]

    words = np.concatenate(words).tolist()

    result = []
    for ele in words:
        if (bool(TAREG.match(ele))):
            result.append([ele])
        else:
            result.append(segment(ele))

    return np.concatenate(result).tolist()


def remove_comments(string):
    pattern = r"(\".*?\"|\'.*?\')|(/\*.*?\*/|//[^\r\n]*$)"
    # first group captures quoted strings (double or single)
    # second group captures comments (//single-line or /* multi-line */)
    regex = re.compile(pattern, re.MULTILINE|re.DOTALL)
    def _replacer(match):
        # if the 2nd group (capturing comments) is not None,
        # it means we have captured a non-quoted (real) comment string.
        if match.group(2) is not None:
            return "" # so we will return empty to remove the comment
        else: # otherwise, we will return the 1st group
            return match.group(1) # captured quoted-string
    return regex.sub(_replacer, string)


def tokenize_subtoken(txt):
    src = remove_comments(txt)
    src = wordpunct_tokenize(txt)
    res = []
    for token in src:
        if not token[0].isalnum():
            for p in token:
                res.append(p)
        else:
            res += tokenize(token)
    return res


def tokenize_fulltoken(txt):
    src = remove_comments(txt)
    src = wordpunct_tokenize(txt)
    res = []
    for token in src:
        if not token[0].isalnum():
            for p in token:
                res.append(p)
        else:
            res.append(token)
    return res