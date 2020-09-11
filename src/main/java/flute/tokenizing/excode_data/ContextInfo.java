package flute.tokenizing.excode_data;

import java.util.ArrayList;
import java.util.List;

public class ContextInfo {
    private List<NodeSequenceInfo> excodes;

    public int currentPos;
    private int methodDeclarationPos;

    public ContextInfo(List<NodeSequenceInfo> excodes, int currentPos) {
        this.excodes = excodes;
        this.currentPos = currentPos;
        for (int i = currentPos; i >= 0; --i) {
            NodeSequenceInfo excode = excodes.get(i);
            if (NodeSequenceInfo.isConstructorOrMethod(excode)) {
                methodDeclarationPos = i;
                break;
            }
        }
    }

    public List<NodeSequenceInfo> getContextFromMethodDeclaration() {
        List<NodeSequenceInfo> context = new ArrayList<>();
        for (int i = methodDeclarationPos; i <= currentPos; ++i) {
            context.add(excodes.get(i));
        }
        return context;
    }
}
