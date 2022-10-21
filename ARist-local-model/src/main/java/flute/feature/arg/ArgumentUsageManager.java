package flute.feature.arg;

import com.opencsv.CSVReader;
import flute.config.Config;
import flute.config.ModelConfig;
import slp.core.util.Pair;

import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;

public class ArgumentUsageManager {
    private HashMap<Argument, ArgumentUsage> argumentUsageMap;

    public ArgumentUsageManager() {
        argumentUsageMap = new HashMap<>();
        try (CSVReader csvReader = new CSVReader(new FileReader(ModelConfig.argumentUsagePath))) {
            System.out.println("Reading argument usage from file...");

            // Read first line, which is columns name
            String[] values = csvReader.readNext();

            // Second line onwards are data
            while ((values = csvReader.readNext()) != null) {
                updateUsage(new Argument(values[0], Integer.parseInt(values[1])),
                       Integer.parseInt(values[2]), Integer.parseInt(values[3]));
            }
            System.out.println("Reading argument usage from file done!");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public ArgumentUsage getUsage(Argument argument) {
        return argumentUsageMap.get(argument);
    }

    public void updateUsage(Argument argument, Integer ps, Integer nonPs) {
        if (!argumentUsageMap.containsKey(argument)) {
            argumentUsageMap.put(argument, new ArgumentUsage(ps, nonPs));
        } else {
            ArgumentUsage val = argumentUsageMap.get(argument);
            argumentUsageMap.put(argument,
                    new ArgumentUsage(val.psOccurence + ps, val.nonPsOccurence + nonPs));
        }
    }

    public boolean isPossiblyPs(Argument argument) {
        ArgumentUsage argumentUsage = argumentUsageMap.get(argument);
        if (argumentUsage == null) return true;
        return 1.0 * argumentUsage.psOccurence / (argumentUsage.psOccurence + argumentUsage.nonPsOccurence)
                >= ModelConfig.PS_RATE_REQUIREMENT;
    }
}
