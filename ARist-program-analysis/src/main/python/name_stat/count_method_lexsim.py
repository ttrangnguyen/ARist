from similarly import lexSim
import csv


def count_method_lexsim_from_file(inp, out):
    file = open(inp, 'r')
    lines = file.readlines()
    file.close()
    thresh_count = [0, 0, 0, 0, 0, 0, 0, 0, 0, 0]
    index = ['[0,0.1)', '[0.1,0.2)', '[0.2,0.3)', '[0.3,0.4)', '[0.4,0.5)', '[0.5,0.6)',
             '[0.6,0.7)', '[0.7,0.8)', '[0.8,0.9)', '[0.9,1]']
    for line in lines:
        method_names = line.split(' ')
        lexsim = lexSim(method_names[0], method_names[1])
        thresh_count[min(int(lexsim*10), 9)] += 1
    with open(out, 'w', newline='') as csv_file:
        writer = csv.writer(csv_file)
        writer.writerow(index)
        writer.writerow(thresh_count)


if __name__ == "__main__":
    input = 'input/method_lexsim.txt'
    output = 'output/method_lexsim.csv'
    count_method_lexsim_from_file(input, output)
