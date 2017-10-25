package org.systemsbiology.biotapestry.plugin.simulation.genetool.vectorequation;

public interface VectorEquationVisitor {
	public void visit(AndNode node);
	public void visit(OrNode node);
	public void visit(NotNode node);
	public void visit(GeneValueNode node);
	public void visit(TimePointOperator node);
	public void visit(GreaterThanTimeOperator node);
	public void visit(LessThanTimeOperator node);
	public void visit(InRegionOperator node);
    public void visit(ImmediateCellContactOperator node);
    public void visit(ModifierNode node);
}
