package flute.jdtparser.callsequence;

import flute.data.MethodInvocationModel;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

public class MethodCallNode {
    private List<MethodCallNode> childNode = new ArrayList<>();

    private MethodInvocationModel value;

    private static int arriveCnt = 0;
    private static int leaveCnt = 0;
    private Integer arriveId;
    private Integer leaveId;

    public MethodCallNode(MethodInvocationModel value) {
        this.value = value;
    }

    public MethodInvocationModel getValue() {
        return value;
    }

    public void setValue(MethodInvocationModel value) {
        this.value = value;
    }

    public List<MethodCallNode> getChildNode() {
        return childNode;
    }

    public void addChildNode(MethodCallNode node) {
        childNode.add(node);
    }

    public void uniqueChildNode() {
        childNode = new ArrayList<>(new HashSet<>(getChildNode()));
    }

    public MethodCallNode copy() {
        return new MethodCallNode(getValue());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MethodCallNode that = (MethodCallNode) o;
        return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    public void setArriveId(Integer arriveId) {
        this.arriveId = arriveId;
    }

    public void setLeaveId(Integer leaveId) {
        this.leaveId = leaveId;
    }

    public void markArrive() {
        arriveId = arriveCnt++;
    }

    public void markLeave() {
        leaveId = leaveCnt++;
    }

    public boolean isAscendanceOf(MethodCallNode other) {
        if (arriveId == null || leaveId == null) return false;
        if (other.arriveId == null || other.leaveId == null) return false;
        return arriveId <= other.arriveId && leaveId >= other.leaveId;
    }
}
