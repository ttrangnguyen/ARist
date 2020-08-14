classNames = []
fieldNames = []
methodNames = []

file = open("output/class_names_stat.txt", "r")
fileData = file.read()
classNames = fileData.splitlines()
file.close()

file = open("output/field_names_stat.txt", "r")
fileData = file.read()
fieldNames = fileData.splitlines()
file.close()

file = open("output/method_names_stat.txt", "r")
fileData = file.read()
methodNames = fileData.splitlines()
file.close()

threshold = 4


def filter(arr, result):
    for ele in arr:
        name = ele.split(":")[0]
        amount = int(ele.split(":")[1])
        if(amount >= threshold and name.isalpha() and (name not in result)):
            result.append(name)


def gen():
    dictList = []
    filter(classNames, dictList)
    filter(fieldNames, dictList)
    filter(methodNames, dictList)

    file = open("output/dict.txt", "x")

    for item in dictList:
        file.write(item+"\n")

    file.close()


gen()
