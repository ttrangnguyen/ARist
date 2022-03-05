from name_stat.name_tokenizer import tokenize


def lcs(X, Y):
    # find the length of the strings
    m = len(X)
    n = len(Y)

    # declaring the array for storing the dp values
    L = [[None] * (n + 1) for i in range(m + 1)]

    """Following steps build L[m + 1][n + 1] in bottom up fashion 
    Note: L[i][j] contains length of LCS of X[0..i-1] 
    and Y[0..j-1]"""
    for i in range(m + 1):
        for j in range(n + 1):
            if i == 0 or j == 0:
                L[i][j] = 0
            elif X[i - 1] == Y[j - 1]:
                L[i][j] = L[i - 1][j - 1] + 1
            else:
                L[i][j] = max(L[i - 1][j], L[i][j - 1])
                # L[m][n] contains the length of LCS of X[0..n-1] & Y[0..m-1]
    return L[m][n]


def comterms(s1, s2):
    return lcs(tokenize(s1), tokenize(s2))


def lexSim(s1, s2):
    if s1 is None or s2 is None:
        return 0
    tokenZize = len(tokenize(s1)) + len(tokenize(s2))
    if(tokenZize == 0):
        return 0
    return (comterms(s1, s2) + comterms(s2, s1)) / tokenZize
