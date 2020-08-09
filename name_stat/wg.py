import re
import np
from wordsegment import load, segment
load()

REG = r"(.+?)([A-Z])"


def snake(match):
    return match.group(1).lower() + "_" + match.group(2).lower()


def tokenize(word):
    camelCases = []
    snake_cases = []
    # Do not predict snake_cases
    if(word.find("_") != -1 or len(word) == 0):
        return []
    TAREG = re.compile("[<,>?]")
    for match in re.finditer("[A-Z][A-Z\d]+", word):
        result = ""
        s = match.start()
        e = match.end()
        if(e == len(word) or bool(TAREG.match(word[e]))):
            # CLASS => Class
            result = word[s:e][1:].lower()
            result = word[s:e][0] + result
        else:
            # CLASS => ClasS
            result = word[s:e][1:-1].lower()
            result = word[s:e][0] + result + word[s:e][-1]

        word = word[:s] + result + word[e:]

    # Split type argument character
    tmpWord = ""
    for char in word:
        if(bool(TAREG.match(char))):
            camelCases.append(tmpWord)
            camelCases.append(char)
            tmpWord = ""
        else:
            tmpWord += char
    if(len(tmpWord) > 0):
        camelCases.append(tmpWord)

    snake_cases = [re.sub(REG, snake, w, 0).lower() for w in camelCases]
    words = [re.split("_", w) for w in snake_cases]

    words = np.concatenate(words).tolist()

    result = []
    for ele in words:
        if(bool(TAREG.match(ele))):
            result.append([ele])
        else:
            result.append(segment(ele))

    return np.concatenate(result).tolist()


# print(tokenize("HelloAreIam<HOWare,b?c>"))
