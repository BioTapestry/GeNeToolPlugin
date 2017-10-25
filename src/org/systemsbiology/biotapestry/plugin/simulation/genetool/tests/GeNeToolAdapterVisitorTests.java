package org.systemsbiology.biotapestry.plugin.simulation.genetool.tests;

import grnboolmodel.*;
import org.junit.Assert;
import org.junit.Test;
import org.systemsbiology.biotapestry.plugin.simulation.genetool.vectorequation.*;

import java.util.ArrayList;


public class GeNeToolAdapterVisitorTests {

	@Test
	public void geneRuleTest() {
		/*
		 * Set up GeNeTool model
		 */
		GRNBoolModel model = new GRNBoolModel();
		model.pm.initOperators();

		// Regions
		Region region1 = new Region(model, "region1", 0, 0, 1);

		// Domains
		Domain domain1 = new Domain(model, "region1");

		// Genes
		Gene geneA = new Gene(model, "geneA");
		Gene geneB = new Gene(model, "geneB");
		Gene geneX = new Gene(model, "geneX");


		model.gm.addGeneForUnitTest(geneA);
		model.gm.addGeneForUnitTest(geneB);
		model.gm.addGeneForUnitTest(geneX);

		// Intermediate representation for adapter
		GeneValueNode nodeGeneA = new GeneValueNode("geneA", true, 0);
		GeneValueNode nodeGeneB = new GeneValueNode("geneB", true, 0);
		GeneValueNode nodeGeneX = new GeneValueNode("geneX", true, 0);

		OrNode orNode = new OrNode(new TimePointOperator(nodeGeneA, 0), new TimePointOperator(nodeGeneB, 0));

		StringifyVisitor stringifyVisitor = new StringifyVisitor();
		orNode.accept(stringifyVisitor);

		/*
		 * Stringify the vector equation from the intermediate representation,
		 * and check that the string representation is the same as it would
		 * be in a GeNeTool .XML model.
		 *
		 * The generated string rule can be passed to GeNeTool RuleReader to produce
		 * a reference vector equation data structure for testing the GeNeToolAdapterVisitor.
		*/

		String ruleX = stringifyVisitor.getValue();
		Assert.assertEquals("", ruleX, "AT-0 G:geneA=1 OR AT-0 G:geneB=1");


		// Generate the vector equation using GeNeTool logic
		ArrayList<Object> objects = model.om.DecodeObjects(geneX, ruleX);
		Logic logic = model.rr.DecodeRule(objects);

		// Generate the vector equation using the GeNeTool adapter logic
		GeNeToolAdapterVisitor gntVisitor = new GeNeToolAdapterVisitor(model);
		orNode.accept(gntVisitor);
		Logic adapterLogic = gntVisitor.getLogic();
	}
}
