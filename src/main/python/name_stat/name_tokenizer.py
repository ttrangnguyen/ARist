import re
import np
from wordsegment import load, segment

load()

REG = r"(.+?)([A-Z])"


def splitCase(match):
    return match.group(1).lower() + "/" + match.group(2).lower()


def tokenize(word):
    camelCases = []
    split_cases = []

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

print(tokenize("a = b."))
