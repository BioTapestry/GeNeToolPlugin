/*
 **    Copyright (C) 2003-2014 Institute for Systems Biology
 **                            Seattle, Washington, USA.
 **
 **    This library is free software; you can redistribute it and/or
 **    modify it under the terms of the GNU Lesser General Public
 **    License as published by the Free Software Foundation; either
 **    version 2.1 of the License, or (at your option) any later version.
 **
 **    This library is distributed in the hope that it will be useful,
 **    but WITHOUT ANY WARRANTY; without even the implied warranty of
 **    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 **    Lesser General Public License for more details.
 **
 **    You should have received a copy of the GNU Lesser General Public
 **    License along with this library; if not, write to the Free Software
 **    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
*/

package org.systemsbiology.biotapestry.plugin.simulation.genetool.vectorequation;

import grnboolmodel.GRNBoolModel;
import grnboolmodel.Gene;
import grnboolmodel.Logic;
import grnboolmodel.LogicGene;
import grnboolmodel.LogicOne;
import grnboolmodel.LogicTwo;
import grnboolmodel.Operator;

public class GeNeToolAdapterVisitor implements VectorEquationVisitor {
	private static int LOGIC_OP_NOT = 0;
	private static int LOGIC_OP_AND = 1;
	private static int LOGIC_OP_OR = 2;
	private static int LOGIC_OP_AT = 8;
	
	private GRNBoolModel p_;	
	
	private Logic logic_;
	
	public GeNeToolAdapterVisitor(GRNBoolModel model) {
		p_ = model;
	}
	
	public Operator getNotLogicOperator() {
		return p_.pm.LogicOperator[LOGIC_OP_NOT];
	}

	public Operator getAndLogicOperator() {
		return p_.pm.LogicOperator[LOGIC_OP_AND];
	}

	public Operator getOrLogicOperator() {
		return p_.pm.LogicOperator[LOGIC_OP_OR];
	}

	public Operator getAtLogicOperator() {
		return p_.pm.LogicOperator[LOGIC_OP_AT];
	}
	
	public Logic getLogic() {
		return logic_;
	}
	
	public void visit(AndNode node) {
		GeNeToolAdapterVisitor leftVisitor = new GeNeToolAdapterVisitor(p_);
		VectorEquationNode left = node.getLeft();
		left.accept(leftVisitor);
		
		GeNeToolAdapterVisitor rightVisitor = new GeNeToolAdapterVisitor(p_);
		VectorEquationNode right = node.getRight();
		right.accept(rightVisitor);
		
		Logic leftLogic = leftVisitor.getLogic();
		Logic rightLogic = rightVisitor.getLogic();
		
		Operator andOperator = getAndLogicOperator();
		
		logic_ = new LogicTwo(p_, leftLogic, andOperator, rightLogic);		
	}

	public void visit(OrNode node) {
		GeNeToolAdapterVisitor leftVisitor = new GeNeToolAdapterVisitor(p_);
		VectorEquationNode left = node.getLeft();
		left.accept(leftVisitor);
		
		GeNeToolAdapterVisitor rightVisitor = new GeNeToolAdapterVisitor(p_);
		VectorEquationNode right = node.getRight();
		right.accept(rightVisitor);
		
		Logic leftLogic = leftVisitor.getLogic();
		Logic rightLogic = rightVisitor.getLogic();
		
		Operator orOperator = getOrLogicOperator();
		
		logic_ = new LogicTwo(p_, leftLogic, orOperator, rightLogic);		
	}

	public void visit(NotNode node) {
		GeNeToolAdapterVisitor visitor = new GeNeToolAdapterVisitor(p_);
		node.getChild().accept(visitor);
	
		Operator notOp = getNotLogicOperator();
		
		logic_ = new LogicOne(p_, notOp, visitor.getLogic());	
	}

	public void visit(GeneValueNode node) {
		Gene gene = p_.gm.getGene(node.getGeneName());
		logic_ = new LogicGene(p_, gene, node.getValue(), "");
	}

	public void visit(TimePointOperator node) {
		GeNeToolAdapterVisitor visitor = new GeNeToolAdapterVisitor(p_);
		node.getChild().accept(visitor);
	
		Operator atOp = getAtLogicOperator();
		atOp.step = node.getTime();
		
		logic_ = new LogicOne(p_, atOp, visitor.getLogic());
	}

    // TODO implement
	public void visit(GreaterThanTimeOperator node) {

	}

    // TODO implement
	public void visit(LessThanTimeOperator node) {

	}

    // TODO implement
	public void visit(InRegionOperator node) {

	}

    // TODO implement
    public void visit(ImmediateCellContactOperator node) {

    }

    // TODO implement
    public void visit(ModifierNode node) {

    }
}
