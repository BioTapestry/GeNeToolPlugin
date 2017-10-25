package org.systemsbiology.biotapestry.plugin.simulation.genetool.tests;

import org.junit.*;

import org.systemsbiology.biotapestry.plugin.simulation.genetool.vectorequation.*;

public class StringifyTests {

	@Test
	public void singleGeneTest() {
		GeneValueNode gnv = new GeneValueNode("Ets1", true, 0);
		StringifyVisitor visitor = new StringifyVisitor();
		
		gnv.accept(visitor);

		Assert.assertEquals("G:Ets1=1", visitor.getValue());
	}

	@Test
	public void singleNotGeneTest() {
		GeneValueNode gnv = new GeneValueNode("Ets1", true, 0);
		NotNode not = new NotNode(gnv);
		StringifyVisitor visitor = new StringifyVisitor();
		
		not.accept(visitor);

		Assert.assertEquals("NOT G:Ets1=1", visitor.getValue());
	}

	@Test
	public void singleTimePointNotGeneTest() {
		GeneValueNode gnv = new GeneValueNode("Ets1", true, 0);
		NotNode not = new NotNode(gnv);
		TimePointOperator tpo = new TimePointOperator(not, -3);
		
		StringifyVisitor visitor = new StringifyVisitor();
		tpo.accept(visitor);

		Assert.assertEquals("AT-3 NOT G:Ets1=1", visitor.getValue());
	}

	@Test
	public void twoGenesAndTest() {
		GeneValueNode gnv1 = new GeneValueNode("Ets1", true, 0);
		GeneValueNode gnv2 = new GeneValueNode("HesC", true, 0);
		AndNode andNode = new AndNode(gnv1, gnv2);
		
		StringifyVisitor visitor = new StringifyVisitor();
		
		andNode.accept(visitor);

		Assert.assertEquals("G:Ets1=1 AND G:HesC=1", visitor.getValue());
	}

	@Test
	public void twoGenesOrTest() {
		GeneValueNode gnv1 = new GeneValueNode("Ets1", true, 0);
		GeneValueNode gnv2 = new GeneValueNode("HesC", true, 0);
		OrNode orNode = new OrNode(gnv1, gnv2);
		
		StringifyVisitor visitor = new StringifyVisitor();
		
		orNode.accept(visitor);

		Assert.assertEquals("G:Ets1=1 OR G:HesC=1", visitor.getValue());
	}
}
