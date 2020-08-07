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
  if(word.find("_") != -1):
    return []
  for match in re.finditer("[A-Z][A-Z\d]+", word):
      result = ""
      s = match.start()
      e = match.end()
      if(e == len(word)):
        result = word[s:e][1:].lower()
        result = word[s:e][0] + result
      else:
        result = word[s:e][1:-1].lower()
        result = word[s:e][0] + result + word[s:e][-1]
      
      word = word[:s] + result + word[e:]

  camelCases.append(word)
  
  snake_cases = [re.sub(REG, snake, w, 0) for w in camelCases]
  words = [re.split("_", w) for w in snake_cases] 
  words = np.concatenate(words).tolist()
  
  words = [segment(w) for w in snake_cases]

  return np.concatenate(words).tolist()  
  