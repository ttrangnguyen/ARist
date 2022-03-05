package flute.analysis.structure;

import java.util.*;

public class StringCounter {
    private Map<String, Integer> counter = new TreeMap<String, Integer>();

    private Integer count = 0;

    public void add(String s) {
        Integer cnt = counter.getOrDefault(s, 0);
        counter.put(s, cnt + 1);

        ++this.count;
    }

    public int getCount(String s) {
        return counter.getOrDefault(s, 0);
    }

    public double getProportion(String s, boolean inPercent) {
        double result = (double)getCount(s) / this.count;
        if (inPercent) result *= 100;
        return result;
    }

    public Set<String> getDistinctStrings() {
        return counter.keySet();
    }

    public String[] getTop(int num) {
        List<String> list = new ArrayList<String>(counter.keySet());
        Collections.sort(list, new Comparator<String>() {
            @Override
            public int compare(String o1, String o2) {
                return getCount(o2) - getCount(o1);
            }
        });
        String[] top = new String[Math.min(num, list.size())];
        for (int i = 0; i < top.length; ++i) top[i] = list.get(i);
        return top;
    }

    public String describe(int mostCommonOnly) {
        List<String> list = new ArrayList<String>(counter.keySet());
        Collections.sort(list, new Comparator<String>() {
            @Override
            public int compare(String o1, String o2) {
                return getCount(o2) - getCount(o1);
            }
        });
        int lenMax = 0;
        if (mostCommonOnly <= 0) mostCommonOnly = list.size();
        for (int i = 0; i < Math.min(list.size(), mostCommonOnly); ++i) {
            lenMax = Math.max(lenMax, list.get(i).length());
        }
        StringBuilder sb = new StringBuilder();
        sb.append("Frequency distribution:\n");
        for (int i = 0; i < Math.min(list.size(), mostCommonOnly); ++i) {
            sb.append(String.format("\t%-"+lenMax+"s: %5.2f%%\n", list.get(i), getProportion(list.get(i), true)));
        }
        return sb.toString();
    }

    public String describe() {
        return describe(0);
    }
}
