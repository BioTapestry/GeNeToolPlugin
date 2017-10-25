package org.systemsbiology.biotapestry.plugin.simulation.genetool.vectorequation;

public class GreaterThanTimeOperator implements VectorEquationNode {
    private int time_;

    public GreaterThanTimeOperator(int time) {
        time_ = time;
    }

    public int getTime() {
        return time_;
    }

    public void accept(VectorEquationVisitor visitor) {
        visitor.visit(this);
    }
}
