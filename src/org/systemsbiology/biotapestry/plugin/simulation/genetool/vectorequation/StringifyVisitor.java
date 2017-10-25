package org.systemsbiology.biotapestry.plugin.simulation.genetool.vectorequation;

public class StringifyVisitor implements VectorEquationVisitor {
	private StringBuilder builder_;

	public StringifyVisitor() {
		builder_ = new StringBuilder();
	}

	public String getValue() {
		return builder_.toString();
	}

	public void visit(AndNode node) {
		StringifyVisitor leftVisitor = new StringifyVisitor();
		VectorEquationNode left = node.getLeft();
		left.accept(leftVisitor);

		StringifyVisitor rightVisitor = new StringifyVisitor();
		VectorEquationNode right = node.getRight();
		right.accept(rightVisitor);

		builder_.append(leftVisitor.getValue());
		builder_.append(" AND ");
		builder_.append(rightVisitor.getValue());
	}

	public void visit(OrNode node) {
		StringifyVisitor leftVisitor = new StringifyVisitor();
		VectorEquationNode left = node.getLeft();
		left.accept(leftVisitor);

		StringifyVisitor rightVisitor = new StringifyVisitor();
		VectorEquationNode right = node.getRight();
		right.accept(rightVisitor);

		builder_.append(leftVisitor.getValue());
		builder_.append(" OR ");
		builder_.append(rightVisitor.getValue());
	}

	public void visit(NotNode node) {
		StringifyVisitor visitor = new StringifyVisitor();
		node.getChild().accept(visitor);

		builder_.append("NOT");
		builder_.append(" ");
		builder_.append(visitor.getValue());
	}

	public void visit(GeneValueNode node) {
		builder_.append("G:");
		builder_.append(node.getGeneName());
		builder_.append("=");
		builder_.append(node.getValue() ? "1" : "0");
	}

	public void visit(TimePointOperator node) {
		StringifyVisitor visitor = new StringifyVisitor();
		node.getChild().accept(visitor);
		int time = node.getTime();

		builder_.append("AT");
		if (time >= 0) {
			builder_.append(time > 0 ? "+" : "-");
		}
		builder_.append(time);
		builder_.append(" ");
		builder_.append(visitor.getValue());
	}

	public void visit(GreaterThanTimeOperator node) {
		builder_.append(">");
		builder_.append(node.getTime());
	}

	public void visit(LessThanTimeOperator node) {
		builder_.append("<");
		builder_.append(node.getTime());
	}

	public void visit(InRegionOperator node) {
		builder_.append("IN D:");
		builder_.append(node.getRegion());
	}

    public void visit(ImmediateCellContactOperator node) {
        builder_.append("IN CC D:");
        builder_.append(node.getDomain());
    }

    public void visit(ModifierNode node) {
        StringifyVisitor leftVisitor = new StringifyVisitor();
        VectorEquationNode left = node.getLeft();
        left.accept(leftVisitor);

        StringifyVisitor rightVisitor = new StringifyVisitor();
        VectorEquationNode right = node.getRight();
        right.accept(rightVisitor);

        builder_.append(leftVisitor.getValue());
        builder_.append(" ");
        builder_.append(rightVisitor.getValue());
    }
}
