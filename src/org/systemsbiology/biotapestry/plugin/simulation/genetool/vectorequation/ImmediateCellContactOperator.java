package org.systemsbiology.biotapestry.plugin.simulation.genetool.vectorequation;

/**
 * Created by kleinone on 2/12/15.
 */
public class ImmediateCellContactOperator implements VectorEquationNode {
    private String domain_;

    public ImmediateCellContactOperator(String domain) {
        domain_ = domain;
    }

    public String getDomain() {
        return domain_;
    }

    public void accept(VectorEquationVisitor visitor) {
        visitor.visit(this);
    }
}
