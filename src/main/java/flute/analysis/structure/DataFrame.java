package flute.analysis.structure;

import java.util.*;

public class DataFrame {
    public class Variable {
        private Map<Double, Integer> counter = new TreeMap<Double, Integer>();

        private Integer count = 0;
        private Double sum = 0.0;
        private Double min = Double.POSITIVE_INFINITY;
        private Double max = Double.NEGATIVE_INFINITY;
        private Double mode = Double.NaN;

        public void insert(double data) {
            Integer cnt = counter.getOrDefault(data, 0);
            counter.put(data, cnt + 1);

            ++this.count;
            this.sum += data;
            this.min = Math.min(this.min, data);
            this.max = Math.max(this.max, data);
            if (mode.isNaN() || countValue(mode) < countValue(data)) mode = data;
        }

        public int getCount() {
            return this.count;
        }

        public double getMean() {
            if (getCount() == 0) return 0.0;

            return sum / getCount();
        }

        public double getVariance() {
            if (getCount() <= 1) return 0.0;

            Double mean = getMean();
            Double variance = 0.0;
            for (Double key: counter.keySet()) {
                variance += counter.get(key) * (key - mean) * (key - mean);
            }
            variance /= (getCount() - 1);
            return variance;
        }

        public double getMode() {
            return mode;
        }

        public double getStd() {
            return Math.sqrt(getVariance());
        }

        public double getMin() {
            return min;
        }

        public double getMax() {
            return max;
        }

        public double getSum() {
            return sum;
        }

        public List<Double> getMilestones(double... milestones) {
            if (milestones.length == 0) throw new IllegalArgumentException("At least one argument is required");
            for (double milestone: milestones) {
                if (!(0 <= milestone && milestone <= 1))
                    throw new IllegalArgumentException("All arguments must be between 0 and 1");
            }
            Arrays.sort(milestones);

            List<Double> results = new ArrayList<Double>();
            int milestoneId = 0;
            int sum = 0;
            for (Map.Entry<Double, Integer> entry: counter.entrySet()) {
                sum += entry.getValue();
                while (sum >= milestones[milestoneId] * getCount()) {
                    results.add(entry.getKey());
                    milestoneId++;
                    if (milestoneId == milestones.length) break;
                }
                if (milestoneId == milestones.length) break;
            }
            return results;
        }

        public int countValue(double value) {
            return counter.getOrDefault(value, 0);
        }

        public int countRange(double lower, double upper) {
            int result = 0;
            for (double key: counter.keySet()) {
                if (lower <= key && key <= upper) result += counter.get(key);
            }
            return result;
        }

        public double getProportionOfValue(double value, boolean inPercent) {
            double result = (double)countValue(value) / getCount();
            if (inPercent) result *= 100;
            return result;
        }

        public double getProportionOfValue(double value) {
            return getProportionOfValue(value, false);
        }

        public double getProportionOfRange(double lower, double upper, boolean inPercent) {
            double result = (double)countRange(lower, upper) / getCount();
            if (inPercent) result *= 100;
            return result;
        }

        public double getProportionOfRange(double lower, double upper) {
            return getProportionOfRange(lower, upper, false);
        }
    }

    private Map<String, Variable> vars = new HashMap<String, Variable>();

    public Variable getVariable(String label) {
        if (!vars.containsKey(label)) vars.put(label, new Variable());
        return vars.get(label);
    }

    public void insert(String label, double data) {
        Variable variable = getVariable(label);
        variable.insert(data);
    }

    public void insert(String label, boolean data) {
        insert(label, data? 1: 0);
    }

    public String describe(String label) {
        Variable variable = getVariable(label);
        List<Double> milestones = variable.getMilestones(0.25, 0.50, 0.75);

        StringBuilder sb = new StringBuilder();
        sb.append("Statistics on " + label + ":\n");
        sb.append(String.format("\t%-7s%20d\n", "count:", variable.getCount()));
        sb.append(String.format("\t%-7s%20f\n", "mean:", variable.getMean()));
        sb.append(String.format("\t%-7s%20f\n", "std:", variable.getStd()));
        sb.append(String.format("\t%-7s%20f\n", "mode:", variable.getMode()));
        sb.append(String.format("\t%-7s%20f\n", "min:", variable.getMin()));
        sb.append(String.format("\t%-7s%20f\n", "25%:", milestones.get(0)));
        sb.append(String.format("\t%-7s%20f\n", "50%:", milestones.get(1)));
        sb.append(String.format("\t%-7s%20f\n", "75%:", milestones.get(2)));
        sb.append(String.format("\t%-7s%20f\n", "max:", variable.getMax()));
        return sb.toString();
    }

    public static void main(String[] args) {
        DataFrame dataFrame = new DataFrame();
        String testLabel = "test set";
        dataFrame.insert(testLabel, 6.9);
        dataFrame.insert(testLabel, 70);
        dataFrame.insert(testLabel, 6.9);
        dataFrame.insert(testLabel, -1);
        System.out.println(dataFrame.describe(testLabel));
    }
}
