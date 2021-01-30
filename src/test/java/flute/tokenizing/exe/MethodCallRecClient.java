package flute.tokenizing.exe;

import flute.config.Config;
import flute.tokenizing.excode_data.MethodCallRecTest;
import flute.tokenizing.excode_data.RecTest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public abstract class MethodCallRecClient extends RecClient {
    public MethodCallRecClient(String projectName) {
        super(projectName);
    }

    @Override
    public List<? extends RecTest> getTests(boolean fromSavefile, boolean doSaveTestsAfterGen) throws IOException {
        List<MethodCallRecTest> tests = (List<MethodCallRecTest>) super.getTests(fromSavefile, doSaveTestsAfterGen);

        if (Config.TEST_APIS != null && Config.TEST_APIS.length > 0) {
            List<MethodCallRecTest> tmp = new ArrayList<>();
            for (MethodCallRecTest test: tests) {
                if (test.getMethodInvocClassQualifiedName() != null) {
                    for (String targetAPI: Config.TEST_APIS) {{
                        if (test.getMethodInvocClassQualifiedName().startsWith(targetAPI + '.')) {
                            tmp.add(test);
                            break;
                        }
                    }}
                }
            }
            tests = tmp;
        }

        return tests;
    }
}
