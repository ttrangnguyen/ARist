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

    def score_lexsim(self, lexsim):
        # lexsim=0
        if abs(lexsim) < 1e-6:
            return -1
        # lexsim>0, <0.4
        if lexsim < 0.4:
            return -0.8
        # lexsim>0.4, <0.5
        if lexsim + 1e-6 < 0.5:
            return -0.6
        if abs(lexsim - 0.5) < 1e-6:
            # lexsim=0.5
            return -0.5
        # lexsim>0.5, <0.6
        if lexsim < 0.6:
            return -0.4
        # lexsim>0.6, <1
        if lexsim + 1e-6 < 1:
            return -0.3
        else:
            # lexsim=1
            return 0
