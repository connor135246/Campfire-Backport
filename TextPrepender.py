# 2020-06-28
# just prepending some simple text to the start of each line in a text file automatically.

def main():
    # input file
    descText = open('detailed.txt', 'r').read().splitlines()

    # output file
    result = open('detailedRes.txt', 'w')

    for i in range(len(descText)):
        result.write("campfirebackport.config.explanation." + str(i) + "=" + descText[i] + "\n")

