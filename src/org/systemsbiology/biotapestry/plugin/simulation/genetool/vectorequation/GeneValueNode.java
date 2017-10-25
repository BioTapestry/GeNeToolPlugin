/*
 **    Copyright (C) 2003-2015 Institute for Systems Biology
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

public class GeneValueNode implements VectorEquationNode {
	private String gene_;
	private Boolean value_;
	private int time_;
	
	public GeneValueNode(String gene) {
		gene_ = gene;
		value_ = true;
		time_ = 0;
	}
	
	public GeneValueNode(String gene, Boolean value, int time) {
		gene_ = gene;
		value_ = value;
		time_ = time;
	}
	
	public String getGeneName() {
		return gene_;
	}
	
	public int getTime() {
		return time_;
	}
	
	public Boolean getValue() {
		return value_;
	}
	
	public void accept(VectorEquationVisitor visitor) {
		visitor.visit(this);
	}
}
