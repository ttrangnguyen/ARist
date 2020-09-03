package flute.utils.file_processing;

/**
 * Created by Minology on 10:16 CH
 */
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Tokenizer {
    private Map<String, Integer> wordIndex;
    private String outOfVocabularyToken;

    public Tokenizer() {
        wordIndex = new HashMap<>();
    }

    public Tokenizer(String outOfVocabularyToken) {
        wordIndex = new HashMap<>();
        this.outOfVocabularyToken = outOfVocabularyToken;
        this.wordIndex.put(outOfVocabularyToken, 1);
    }

    public void fitOnTexts(String[] texts) {
        for (String text : texts) {
            if (!wordIndex.containsKey(text)) {
                wordIndex.put(text, wordIndex.size() + 1);
            }
        }
    }

    public ArrayList<ArrayList<Integer>> textsToSequences(ArrayList<ArrayList<String>> texts) {
        ArrayList<ArrayList<Integer>> sequences = new ArrayList<>();
        for (ArrayList<String> text: texts) {
            ArrayList<Integer> sequence = new ArrayList<>();
            for (String s : text) {
                if (!wordIndex.containsKey(s)) {
                    sequence.add(wordIndex.get(outOfVocabularyToken));
                } else {
                    sequence.add(wordIndex.get(s));
                }
            }
            sequences.add(sequence);
        }
        return sequences;
    }

    public Map<String, Integer> getWordIndex() {
        return wordIndex;
    }
}
