import np
import wg

# statistic

def statistic(inputFile, outputFile):
  file = open("input/"+inputFile, "r")
  fileData = file.read()
  file.close()
  words = fileData.splitlines()
  results = []

  process = 0
  for index, word in enumerate(words):
    if(index/len(words) - process > 0.001):
      process = index/len(words)
      print(str(process*100)+"%")
    results.append(wg.tokenize(word))
  
  results = np.concatenate(results).tolist() 
  
  stat = dict()

  for w in results:
    if w in stat:
      stat[w] += 1
    else:
      stat[w] = 1

  stat = sorted(stat.items(), key=lambda kv: kv[1], reverse=True)

  file = open("output/"+ outputFile, "x")

  for item in stat:
    file.write(item[0] + ": "+ str(item[1]) + "\n")

  file.close()


statistic("method_names.txt", "method_names_stat.txt")