package flute.feature.ps;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import flute.candidate.Candidate;
import slp.core.util.Pair;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class PsMemberManager {
    private HashMap<String, List<Pair<String, String>>> publicStaticFieldMap;
    private HashMap<String, List<Pair<String, String>>> publicStaticMethodMap;

    public PsMemberManager(String path) {
        publicStaticFieldMap = new HashMap<>();
        publicStaticMethodMap = new HashMap<>();
        try {
            Gson gson = new Gson();
            BufferedReader br = new BufferedReader(new FileReader(path));
            Type publicStaticMemberType = new TypeToken<List<Candidate>>() {
            }.getType();
            List<Candidate> publicStaticFieldList = gson.fromJson(br.readLine(), publicStaticMemberType);
            List<Candidate> publicStaticMethodList = gson.fromJson(br.readLine(), publicStaticMemberType);
            for (Candidate publicStaticField: publicStaticFieldList) {
                if (publicStaticFieldMap.containsKey(publicStaticField.key)) {
                    publicStaticFieldMap.get(publicStaticField.key).add(
                            new Pair<>(publicStaticField.excode, publicStaticField.lexical));
                } else {
                    publicStaticFieldMap.put(publicStaticField.key,
                            new ArrayList<>(Arrays.asList(new Pair<>(publicStaticField.excode, publicStaticField.lexical))));
                }
            }
            for (Candidate publicStaticMethod: publicStaticMethodList) {
                if (publicStaticFieldMap.containsKey(publicStaticMethod.key)) {
                    publicStaticFieldMap.get(publicStaticMethod.key).add(
                            new Pair<>(publicStaticMethod.excode, publicStaticMethod.lexical));
                } else {
                    publicStaticFieldMap.put(publicStaticMethod.key,
                            new ArrayList<>(Arrays.asList(new Pair<>(publicStaticMethod.excode, publicStaticMethod.lexical))));
                }
            }
            br.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
