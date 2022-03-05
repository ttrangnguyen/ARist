def prepare_sentence(sentence, n):
    assert len(sentence) <= n
    if len(sentence) == n:
        return sentence
    else:
        return ['<s>'] * (n - len(sentence)) + sentence


def score_ngram(model, sentence, n, start_pos):
    total_score = 0
    for i in range(start_pos, len(sentence)):
        prep = prepare_sentence(sentence[max(0, i - n + 1):i], n - 1)
        score = model.logscore(sentence[i], prep)
        total_score += max(score, -25)
    return total_score
