class ModelManager:
    def recreate(self, result, data):
        origin = []
        for candidate_ids in result:
            candidate_text = ""
            for i in range(len(candidate_ids)):
                candidate_text += data['next_lex'][i][candidate_ids[i][0]][candidate_ids[i][1]]
                if i < len(candidate_ids) - 1:
                    candidate_text += ", "
            origin.append(candidate_text)
        return origin

    def is_valid_param(self, param):
        invalid_phrases = ['null', '', 'true', 'false', '0']
        return param not in invalid_phrases
