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

package org.systemsbiology.biotapestry.plugin.simulation.genetool;

import java.util.*;

import grnboolmodel.*;
import org.systemsbiology.biotapestry.analysis.Link;
import org.systemsbiology.biotapestry.plugin.simulation.genetool.vectorequation.*;
import org.systemsbiology.biotapestry.simulation.ModelExpressionEntry;
import org.systemsbiology.biotapestry.simulation.ModelExpressionEntry.Source;
import org.systemsbiology.biotapestry.simulation.ModelLink;
import org.systemsbiology.biotapestry.simulation.ModelNode;
import org.systemsbiology.biotapestry.simulation.ModelRegion;
import org.systemsbiology.biotapestry.simulation.ModelRegionTopologyForTime;
import org.systemsbiology.biotapestry.simulation.ModelSource;
import org.systemsbiology.biotapestry.simulation.ModelExpressionEntry.Level;
import org.systemsbiology.biotapestry.util.DataUtil;

public class GeNeToolModelAdapter {
	private final int GENETOOL_EXPRESSION_INVALID = 5;

	private ModelSource mSrc_;
	private GRNBoolModel p;
	private int maxTimePoint_;

	private ArrayList<Domain> domains_;
	private Map<String, Domain> domainMap_;

	private Map<String, String> nodeIDsToNames_;
	private Map<String, String> nodeIDsToModelSourceNames_;
	private Map<String, String> geneNameMap_;
	private Map<String, String> nonGeneNameMap_;

	private Map<String, String> namesToNodeID_;
    private Map<String, String> nodeIDToExpressionGeneMap_;

    private Map<String, ModelNode> nodeIDToModelNode_;

	private Map<String, VectorEquationNode> gntGeneNodes_;

	private static int LOGIC_OP_AND = 1;
	private static int LOGIC_OP_OR = 2;
	
	private static int LOGIC_OP_CC = 4;
	private static int LOGIC_OP_NCC_N = 5;
	private static int LOGIC_OP_NCC_D = 6;

	private static int GENE_DELAY = -1;

    private static boolean ENABLE_UBIQUITOUS_DETECTION = false;

	public GeNeToolModelAdapter(ModelSource mSrc, GRNBoolModel grnBM) {
		mSrc_ = mSrc;
		p = grnBM;
		maxTimePoint_ = -1;

		domains_ = new ArrayList<Domain>();
		domainMap_ = new HashMap<String, Domain>();
		nodeIDsToNames_ = new HashMap<String, String>();
		nodeIDsToModelSourceNames_ = new HashMap<String, String>();
		geneNameMap_ = new HashMap<String, String>();
		nonGeneNameMap_ = new HashMap<String, String>();

		namesToNodeID_ = new HashMap<String, String>();
        nodeIDToExpressionGeneMap_ = new HashMap<String, String>();

        nodeIDToModelNode_ = new HashMap<String, ModelNode>();

		gntGeneNodes_ = new HashMap<String, VectorEquationNode>();
	}
	
	/***************************************************************************
	 *
	 * Builds a model into a GRNBoolModel instance by converting
	 * from a BioTapestry model.
	 *
	 */
	public void convert() {
		System.out.println("GeNeToolModelAdapter::convert");
		createNodeIdToNameMapping();

		deleteModel();

		buildRegions();
		buildDomains();
		buildDomainTopology();
		nodesToGenes();
		buildVectorEquationGeneNodes();
		buildGeneExpression();
		buildVectorEquationsForGenes();
		buildVectorEquationsForInputs();
		
		updateDomainsFromGenes();
	}
	
	/***************************************************************************
	 *
	 * Clears a GRNBoolModel instance.
	 * Implementation from grnboolmodel.RuleReader.deleteModel
	 *
	 */
	private void deleteModel() {
		p.gm.Genes = new ArrayList<Gene>();
		p.gm.nbGene = 0;
		p.dm.Domains = new ArrayList<Domain>();
		p.dm.nbDomain = 0;
		p.lm.MyModels = null;
		p.lm.MyModel = null;
		p.rm.Regions = new ArrayList<Region>();
		p.lm.lastModelLoaded = "";
		p.rm.MaxTime = 0;
	}

	/***************************************************************************
	 *
	 * Builds the progenitors (Regions) into the GeNeTool model.
	 *
	 */
	private void buildRegions() {
		List<ModelRegion> mrl = mSrc_.getRegions();

		// The maximum time point has to be known and set
		// inside the RegionManager before doing anything else.
		for (ModelRegion mr : mrl) {
			int endTime = mr.getRegionEnd();

			if (endTime > maxTimePoint_) {
				maxTimePoint_ = endTime;
			}
		}

		p.rm.MaxTime = maxTimePoint_;
		int regionCounter = 0;
		for (ModelRegion mr : mrl) {
			String name = mr.getRegionName();
			int startTime = mr.getRegionStart();
			int endTime = mr.getRegionEnd();

			Region region = new Region(p, name, regionCounter, startTime, endTime);
			System.out.println("Tree " + regionCounter + " " + name + " " + startTime + " -> " + endTime);
			p.rm.addRegion(region);
			regionCounter += 1;
		}
	}

	 /***************************************************************************
	  *
	  * Builds the domains into the GeNeTool model.
	  *
	  * The GeNeTool domains are the terminal leaves of the lineage tree. The ModelSource
	  * interface returns a list of all regions (progenitors), and with each region the
	  * the lineage down to that region. The domains are found by first going through all the
	  * lineages, and marking each region that has any descendants. After this, the domains
	  * are the lineages ending the regions not marked in the previous step.
	  * 
	  * Can be called after the progenitors (Regions) have been created into the GeNeTool model.
	  */
	private void buildDomains() {		
		List<ModelRegion> mrl = mSrc_.getRegions();
		Set<String> regionsWithDescendants = new HashSet<String>();
		Map<String, List<String>> regionToLineageMap = new HashMap<String, List<String>>();
				
		for (ModelRegion mr : mrl) {
			ArrayList<String> lineages = new ArrayList<String>(mr.getLineage());
				
			if (lineages.size() > 1) {
				// The current region will be the last one in the lineage list
				Iterator<String> linIterator = lineages.subList(0, lineages.size() - 1).iterator();
				while (linIterator.hasNext()) {
					String lr = linIterator.next();
					regionsWithDescendants.add(lr);
				}
			}
			
			regionToLineageMap.put(mr.getRegionName(), mr.getLineage());
		}
		
		for (ModelRegion mr : mrl) {
			String lineageKey = mr.getRegionName();
			
			if (! regionsWithDescendants.contains(lineageKey)) {
				Domain domain = new Domain(p, lineageKey);

				List<String> lin = regionToLineageMap.get(lineageKey);
				for (String lr : lin) {
					domain.addTree(lr);
				}
				
				domains_.add(domain);
				domainMap_.put(lineageKey, domain);
				p.dm.addDomain(domain);
			}
		}
	}
	
	/***************************************************************************
	 *
	 * Creates a GeNeTool representation of a single link between two domains
	 * with the logic operator and temporal information.
	 * 
	 * The link is stored into a mapping of domain identifiers to links.
	 *
	 */
	private void buildLinkBetweenDomains(Map<String, List<Objet[]>> domainLinkMap , int minTime, int maxTime, String source, String target) {
		Operator operator1 = new Operator(p.pm.LogicOperator[LOGIC_OP_CC]);
		operator1.hmin = minTime;
		operator1.hmax = maxTime;
		
		Domain domain = domainMap_.get(target);
		
		Objet[] gntLink = new Objet[2];
		gntLink[0] = new Objet(p, operator1);
		gntLink[1] = new Objet(p, domain);
		
		List<Objet[]> links;
		if (!domainLinkMap.containsKey(source)) {
			links = new ArrayList<Objet[]>();
			domainLinkMap.put(source, links);
		} else {
			links = domainLinkMap.get(source);
		}
		
		links.add(gntLink);
	}
	
	/***************************************************************************
	 * 
	 * In GeNeTool, each domain knows every domain that links to it.
	 *
	 */
	private void buildDomainTopology() {
		Map<String, List<Objet[]>> domainLinkMap = new HashMap<String, List<Objet[]>>();
		Iterator<ModelRegionTopologyForTime> lit = mSrc_.getRegionTopology();
		while (lit.hasNext()) {
			ModelRegionTopologyForTime topo = lit.next();
			int minTime = topo.getMinTime();
			int maxTime = topo.getMaxTime();

			Iterator<ModelRegionTopologyForTime.TopoLink> tlit = topo.getLinks();
			while (tlit.hasNext()) {
				ModelRegionTopologyForTime.TopoLink tlink = tlit.next();
				String region1 = tlink.getRegion1();
				String region2 = tlink.getRegion2();
				
				System.out.println("Domain link " + region1 + " -> " + region2);
				if (!domainMap_.containsKey(region1)) {
					System.out.println("  " + region1 + " is not a domain, skipping link.");
					continue;
				}
				if (!domainMap_.containsKey(region2)) {
					System.out.println("  " + region2 + " is not a domain, skipping link.");
					continue;
				}

				////////////
				// A -> B //
				////////////
				buildLinkBetweenDomains(domainLinkMap, minTime, maxTime, region1, region2);
				
				////////////
				// B -> A //
				////////////
                buildLinkBetweenDomains(domainLinkMap, minTime, maxTime, region2, region1);
			}
		}

        /*
         * Each link from one domain to another is represented as interlaced arrays
         * of the GeNeTool class Objet.
        */
		Iterator<String> linkIter = domainLinkMap.keySet().iterator();
		while (linkIter.hasNext()) {
			String domainID = linkIter.next();
			List<Objet[]> links = domainLinkMap.get(domainID);
			Objet[][] linksArray = new Objet[links.size()][2];
			links.toArray(linksArray);
			
			Domain domain = domainMap_.get(domainID);
			domain.DefObjets = linksArray;
		}
	}

	/***************************************************************************
	 *
	 * Refreshes the data inside the Domain objects.
	 * 
	 * Must be called after Genes are created into the GeNeTool model and
	 * expression data has been set for the Genes. 
	 *
	 */
	private void updateDomainsFromGenes() {
		for (Domain domain : domains_) {
			domain.computeData();
		}
	}
	  
	/***************************************************************************
	 *
	 * Answers if given BioTapestry node will be marked "ubiquitous"
	 * in the GeNeTool model.
	 *
	 */
	private boolean isUbiquitousNode(String id) {
		String name = getUniqueNodeName(id);
		return DataUtil.normKey(name).contains("UBIQ");
	}

	private String normalizeName(String name) {
		return DataUtil.normKey(name);
	}

	/***************************************************************************
	 *
	 * Creates a unique name for each BioTapestry model node.
	 * DataUtil.normKey is used for removing white space from node names.
	 *
	 * TODO: Ensure that usage of DataUtil.normKey doesn't result in aliasing.
	 */
	private void createNodeIdToNameMapping() {
		// Generate unique names first
		Iterator<ModelNode> nit = mSrc_.getRootModelNodes();
		while (nit.hasNext()) {
			ModelNode modelNode = nit.next();
			String uniqueName = normalizeName(modelNode.getUniqueName());
			String uniqueInternalID = modelNode.getUniqueInternalID();
			nodeIDsToModelSourceNames_.put(uniqueInternalID, modelNode.getUniqueName());
			nodeIDsToNames_.put(uniqueInternalID, uniqueName);
			namesToNodeID_.put(uniqueName, uniqueInternalID);

			// If the node is a gene, store the BT name to GeNeTool mapping separately.
			// This is needed when converting the expression data to the GeNeTool model.
			if (modelNode.getType() == ModelNode.Type.GENE) {
				geneNameMap_.put(modelNode.getName(), uniqueName);
			}
			else {
				nonGeneNameMap_.put(modelNode.getName(), uniqueName);
			}
		}
	}
	
	/***************************************************************************
	 *
	 * Returns the GeNeTool name of a node, given a BioTapestry node ID.
	 * 
	 */
	private String getUniqueNodeName(String id) {
		return nodeIDsToNames_.get(id);
	}
	
	/***************************************************************************
	 *
	 * Returns the GeNeTool Gene name, given the name of a BioTapestry
	 * "expression gene" instance. 
	 * 
	 */
	private String getUniqueGeneName(String name) {
		return geneNameMap_.get(name);
	}
	
	/***************************************************************************
	 *
	 * Returns the GeNeTool Gene name, given the name of a BioTapestry
	 * "expression gene" instance that does not map to a BioTapestry model
	 * gene.
	 * 
	 */	
	private String getUniqueNonGeneName(String name) {
		return nonGeneNameMap_.get(name);
	}
	
	/***************************************************************************
	 * 
	 * Builds the Genes inside a GeNeTool model.
	 * 
	 * The network in GeNeTool is represented by Gene instances. The "isGene"
	 * property of the Genes is used distinguish BioTapestry model nodes that
	 * are genes from nodes that are not.
	 *
	 * The "isUbiquitous" property of each Gene is also set, not depending
	 * on whether or not the corresponding node is a gene in the BioTapestry
	 * model.
	 * 
	 */
	private void nodesToGenes() {
		Iterator<ModelNode> nit = mSrc_.getRootModelNodes();

		while (nit.hasNext()) {
			ModelNode node = nit.next();
			String id = node.getUniqueInternalID();
			String name = getUniqueNodeName(id);
            nodeIDToModelNode_.put(id, node);

            ModelNode.Type type = node.getType();
			Gene gene = new Gene(p, name);

			if (type == ModelNode.Type.GENE) {
				System.out.println("GENE " + id + " / " + name);
				gene.setIsGene(true);
			} else {
				System.out.println(type.toString() + " " + id + " / " + name);
				gene.setIsGene(false);
			}
			
			boolean isUbiq = isUbiquitousNode(id) && ENABLE_UBIQUITOUS_DETECTION;
			gene.setIsUbiquitous(isUbiq);
			
			p.gm.addGene(gene);
		}
	}

	private void buildVectorEquationGeneNodes() {
        for (String nodeID : nodeIDsToNames_.keySet()) {
            ModelNode.Type type = nodeIDToModelNode_.get(nodeID).getType();
            VectorEquationNode node = null;
            GeneValueNode geneValueNode = new GeneValueNode(nodeIDsToNames_.get(nodeID), true, 0);

            // Intercell nodes -> IN CC:R
            if (type == ModelNode.Type.INTERCELL) {
                node = new ModifierNode(geneValueNode, new ImmediateCellContactOperator("R"));
            }
            else {
                node = geneValueNode;
            }

			gntGeneNodes_.put(nodeID, node);
		}
	}

	/***************************************************************************
	 * 
	 * Maps a ModelExpressionEntry to a gene expression value to be used
	 * in the GeNeTool model.
	 *
	 */
	int mapExpressionLevel(ModelExpressionEntry mre) {
		ModelExpressionEntry.Level level = mre.getLevel();
		ModelExpressionEntry.Source source = mre.getSource();

		if (source == Source.MATERNAL_SOURCE) {
			return 4;
		} else if (level == Level.NOT_EXPRESSED) {
			return 1;
		} else if (level == Level.EXPRESSED) {
			return 3;
		} else if (level == Level.NO_DATA) {
			return 0;
		} else if (level == Level.WEAK_EXPRESSION) {
			return 2;
		} else {
			return GENETOOL_EXPRESSION_INVALID;
		}
	}

	/***************************************************************************
	 *
	 * Builds the gene expression data into the GeNeTool model.
	 *
	 */
	private void buildGeneExpression() {
		SortedSet<String> mrl = new TreeSet<String>(mSrc_.getExpressionGenes());
		SortedSet<Integer> times = mSrc_.getExpressionTimes();
		ArrayList<String> mrn = new ArrayList<String>();
		for (ModelRegion mr : mSrc_.getRegions()) {
			mrn.add(mr.getRegionName());
		}
		for (String geneName : mrl) {
			// Gene in GeNeTool model
			String uniqueGeneName = getUniqueInternalNameForExpressionGene(geneName);
			nodeIDToExpressionGeneMap_.put(namesToNodeID_.get(uniqueGeneName), geneName);
            Gene geneObject = p.gm.getGene(uniqueGeneName);

			if (geneObject == null) {
				System.out.println("Gene \'" + uniqueGeneName + "\' not found in GeNeTool model, skipping.");
				continue;
			}

			for (String reg : mrn) {
				// Region in GeNeTool
				Region region = p.rm.getRegion(reg);

				for (Integer time : times) {
					ModelExpressionEntry mre = mSrc_.getExpressionEntry(geneName, reg, time.intValue());
					Double var = (mre.getLevel().equals(ModelExpressionEntry.Level.VARIABLE)) ? mre.getVariable() : null;
					ModelExpressionEntry.Level myLev = (mre.getLevel()
							.equals(ModelExpressionEntry.Level.EXPRESSED)) ? ModelExpressionEntry.Level.WEAK_EXPRESSION
							: mre.getLevel();

					int genetoolExpression = mapExpressionLevel(mre);

					if (genetoolExpression != GENETOOL_EXPRESSION_INVALID) {
						geneObject.Expression[region.getNumber()][time.intValue()] = genetoolExpression;
					}
				}
			}
		}
	}

	private class RegionLevelTimeSlice {
		private int startTime_;
		private int endTime_;
		private String region_;
		private String gene_;
		private Source source_;
		private Level level_;

		public RegionLevelTimeSlice(String region, String gene, int start, int end, Source source, Level level) {
			region_ = region;
			gene_ = gene;
			startTime_ = start;
			endTime_ = end;
			source_ = source;
			level_ = level;
		}

		@Override
		public String toString() {
			return("reg = " + region_ + " start = " + startTime_ + " end = " + endTime_ + " gene = " + gene_  + " source = " + source_ + " level= " + level_ );
		}

		public int getEndTime() {
			return endTime_;
		}
		public int getStartTime() {
			return startTime_;
		}

		public String getRegion() {
			return region_;
		}
	}

	/***************************************************************************
	 *
	 * Finds one continuous time slice in the expression data, such that the expression level is the same
	 * in the time slice.
	 *
	 * @param geneName Name of a BioTapestry "expression gene".
	 *
	 * @return Time slice object if one is found, otherwise null.
	 *
	 */
	private RegionLevelTimeSlice findFirstTimeSliceInRegion(String geneName, String region, ModelExpressionEntry.Source source, ModelExpressionEntry.Level level) {
		ArrayList<Integer> timepoints = new ArrayList<Integer>(mSrc_.getExpressionTimes());
		ListIterator<Integer> iterator = timepoints.listIterator();

		Integer first = null;
		while (iterator.hasNext()) {
			Integer timePoint = iterator.next();

			ModelExpressionEntry expressionEntry = mSrc_.getExpressionEntry(geneName, region, timePoint.intValue());
			if (expressionEntry.getSource() == source && expressionEntry.getLevel() == level) {
				first = timePoint;
				break;
			}
		}

		Integer last = null;
		while (iterator.hasNext()) {
			Integer timePoint = iterator.next();
			ModelExpressionEntry expressionEntry = mSrc_.getExpressionEntry(geneName, region, timePoint.intValue());
			if (expressionEntry.getSource() != source || expressionEntry.getLevel() != level) {
				last = timePoint;
				break;
			}
		}

		if (last == null) {
			last = timepoints.get(timepoints.size() - 1);
		}

		if (first == null) {
			return null;
		}

		return new RegionLevelTimeSlice(region, geneName, first, last, source, level);
	}

	/***************************************************************************
	 **
	 ** Build map from node to inbound edges
	 */
	private Map<String, Set<String>> calcInboundEdges(Set<Link> edges) {

		HashMap<String, Set<String>> retval = new HashMap<String, Set<String>>();
		Iterator<Link> li = edges.iterator();

		while (li.hasNext()) {
			Link link = li.next();
			String trg = link.getTrg();
			String src = link.getSrc();
			Set<String> forTrg = retval.get(trg);
			if (forTrg == null) {
				forTrg = new HashSet<String>();
				retval.put(trg, forTrg);
			}
			forTrg.add(src);
		}
		return (retval);
	}

	private interface BinaryNodeFactory {
		public VectorEquationNode getNode(VectorEquationNode node1, VectorEquationNode node2);
	}

	private class AndNodeFactory implements BinaryNodeFactory {
		public VectorEquationNode getNode(VectorEquationNode node1, VectorEquationNode node2) {
			return new AndNode(node1, node2);
		}
	}

	private class OrNodeFactory implements BinaryNodeFactory {
		public VectorEquationNode getNode(VectorEquationNode node1, VectorEquationNode node2) {
			return new OrNode(node1, node2);
		}
	}

	private String getUniqueInternalNameForExpressionGene(String expressionGeneName) {
		String uniqueGeneName = getUniqueGeneName(expressionGeneName);

		if (uniqueGeneName == null) {
			uniqueGeneName = getUniqueNonGeneName(expressionGeneName);
		}

		return uniqueGeneName;
	}

	/***************************************************************************
	 *
	 * Builds "always on" vector equations for root nodes.
     *
     * BUG:
     * Currently, the earliest a manually set input can turn active is at time
     * point 1, as the GeNeTool does not support ">-1" operator for activating
     * a node at time point 0.
	 *
	 */
	private void buildVectorEquationsForInputs() {
		Iterator<ModelLink> lit = mSrc_.getRootModelLinks();

		Map<String, GeneValueNode> gntGeneNodes = new HashMap<String, GeneValueNode>();
		for (String nodeID : nodeIDsToNames_.keySet()) {
			gntGeneNodes.put(nodeID, new GeneValueNode(nodeIDsToNames_.get(nodeID), true, 0));
		}

		Set<String> rootNodes = new HashSet<String>(nodeIDsToNames_.keySet());
		while (lit.hasNext()) {
			ModelLink modelLink = lit.next();
			rootNodes.remove(modelLink.getTrg());
		}

		Set<String> expressionRootNodes = new HashSet<String>();
		Map<String, String> rootNodeIDtoExprGeneNames = new HashMap<String, String>();

		SortedSet<String> exprGeneSet = new TreeSet<String>(mSrc_.getExpressionGenes());
		for (String exprGeneName : exprGeneSet) {
			String uniqueGeneName = getUniqueInternalNameForExpressionGene(exprGeneName);
			String exprGeneID = namesToNodeID_.get(uniqueGeneName);
			if (rootNodes.contains(exprGeneID)) {
				expressionRootNodes.add(exprGeneID);
				rootNodeIDtoExprGeneNames.put(exprGeneID, exprGeneName);
			}
		}

		Map<String, RegionLevelTimeSlice> slicesForRoots = new HashMap<String, RegionLevelTimeSlice>();
		for (ModelRegion mr : mSrc_.getRegions()) {
			String regionName = mr.getRegionName();

			for (String geneNodeID : rootNodes) {
				String exprGeneName = rootNodeIDtoExprGeneNames.get(geneNodeID);
				RegionLevelTimeSlice slice = findFirstTimeSliceInRegion(exprGeneName, regionName, Source.NO_SOURCE_SPECIFIED, Level.EXPRESSED);
				if (slice != null) {
					slicesForRoots.put(geneNodeID, slice);
				}
			}
		}

		for (String rootNodeID : slicesForRoots.keySet()) {
			RegionLevelTimeSlice slice = slicesForRoots.get(rootNodeID);
			VectorEquationNode eq =	new AndNode(
					new InRegionOperator(slice.getRegion()),
					new AndNode(
							new GreaterThanTimeOperator(slice.getStartTime()),
                            // Add one to the end time point, as the operator is "less than", not including equal
                            new LessThanTimeOperator(slice.getEndTime() + 1)));

			addVectorEquationToGene(rootNodeID, eq);
		}
	}

	/***************************************************************************
	 *
	 * Builds vector equations for genes into the GeNeTool model.
	 *
	 * General outline:
	 * - Find back edges in the model
	 * - Build the vector equation for each node
	 *   - Build AND statements
	 *     - For each node, build a set of incoming links
	 *     - Remove feedback edges from the set
	 *     - Construct nested AND statements from the set
	 *   - Build OR statements
	 *     - For each node, build a set of incoming links from the set of feedback links
	 *     - Construct nested OR statements from the set
	 *   - Join both AND and OR vector equation trees using an OR statement
	 *
	 */
	private void buildVectorEquationsForGenes() {
		Set<Link> links = new HashSet<Link>();

		Iterator<ModelLink> lit = mSrc_.getRootModelLinks();

		while (lit.hasNext()) {
			ModelLink modelLink = lit.next();

			String src = modelLink.getSrc();
			String trg = modelLink.getTrg();
			Link link = new Link(src, trg);
			links.add(link);
		}
		
		Set<ModelLink> feedbackLinks = mSrc_.findFeedbackEdges();


		// "AND" vector equations
		Set<Link> andLinks = new HashSet<Link>(links);
		for (ModelLink modelLink : feedbackLinks) {
			andLinks.remove(new Link(modelLink.getSrc(), modelLink.getTrg()));
		}
		Map<String, Set<String>> inboundForAnd = calcInboundEdges(andLinks);
		Map<String, VectorEquationNode> andEquationsPerNode = buildVectorEquationForNode(gntGeneNodes_, inboundForAnd, new AndNodeFactory());


		// "OR" vector equations
		Set<Link> orLinks = new HashSet<Link>();
		for (ModelLink modelLink : feedbackLinks) {
			orLinks.add(new Link(modelLink.getSrc(), modelLink.getTrg()));
		}
		Map<String, Set<String>> inboundForOr = calcInboundEdges(orLinks);
		Map<String, VectorEquationNode> orEquationsPerNode = buildVectorEquationForNode(gntGeneNodes_, inboundForOr, new OrNodeFactory());


		// Concatenate AND and OR equations
		Map<String, VectorEquationNode> result = new HashMap<String, VectorEquationNode>();

		for (String nodeID : nodeIDsToNames_.keySet()) {
			VectorEquationNode andTree = andEquationsPerNode.get(nodeID);
			VectorEquationNode orTree = orEquationsPerNode.get(nodeID);

			if (andTree != null || orTree != null) {
				result.put(nodeID, joinVectorEquationTrees(andTree, orTree, new OrNodeFactory()));
			}
		}

		for (String nodeID : nodeIDsToNames_.keySet()) {
			VectorEquationNode eqNode = result.get(nodeID);
			if (eqNode != null) {
				addVectorEquationToGene(nodeID, eqNode);
			}
		}
	}

	private VectorEquationNode joinVectorEquationTrees(VectorEquationNode node1 , VectorEquationNode node2, BinaryNodeFactory nodeFactory) {
		if (node1 == null && node2 != null) {
			return node2;
		}
		else if (node1 != null && node2 == null) {
			return node1;
		}
		else if (node1 != null && node2 != null) {
			return nodeFactory.getNode(node1, node2);
		}
		else {
			return null;
		}
	}

	/***************************************************************************
	 *
	 * Builds the vector equation for a GeNeTool node.
	 *
	 * Given n genes, creates m = n-1 linked vector equation nodes.
	 *
	 */
	private Map<String, VectorEquationNode> buildVectorEquationForNode(Map<String, VectorEquationNode> gntGeneNodes, Map<String, Set<String>> upstreamGraphNodes, BinaryNodeFactory nodeFactory) {
		Map<String, VectorEquationNode> vecEquationsForNodes = new HashMap<String, VectorEquationNode>();

		for (Iterator<String> targetNodeIter = upstreamGraphNodes.keySet().iterator(); targetNodeIter.hasNext(); ) {
			String targetNodeID = targetNodeIter.next();

			Iterator<String> sourceNodeIter = upstreamGraphNodes.get(targetNodeID).iterator();
			String sourceNodeID = sourceNodeIter.next();
			VectorEquationNode node1 = new TimePointOperator(gntGeneNodes.get(sourceNodeID), GENE_DELAY);

			while (sourceNodeIter.hasNext()) {
				sourceNodeID = sourceNodeIter.next();
				node1 = nodeFactory.getNode(node1, new TimePointOperator(gntGeneNodes.get(sourceNodeID), GENE_DELAY));
			}

			vecEquationsForNodes.put(targetNodeID, node1);
		}

		return vecEquationsForNodes;
	}

	/***************************************************************************
	 * Adds a vector equation to a GeNeTool gene object.
	 *
	 * The vector equation is first converted to string representation,
	 * to be parsed by GeNeTool logic. The gene names in the vector
	 * equation must not contain any white space.
	 */
	private void addVectorEquationToGene(String nodeID, VectorEquationNode eqNode) {
		Gene gene = p.gm.getGene(nodeIDsToNames_.get(nodeID));
		GeNeToolAdapterVisitor gntVisitor = new GeNeToolAdapterVisitor(p);
		eqNode.accept(gntVisitor);

		StringifyVisitor visitor = new StringifyVisitor();
		eqNode.accept(visitor);
		String rule = visitor.getValue();

		// Debug print
		System.out.println("VecEq " + nodeIDsToNames_.get(nodeID) + ": " + rule);

		gene.addLogic("", rule, "1", "0", false, false);
	}

	public class SimulatorRow {
		private String region_;
		private String gene_;
		private int time_;
		private Level value_;

		public SimulatorRow(String region_, String gene_, int time_, Level value_) {
			this.region_ = region_;
			this.gene_ = gene_;
			this.time_ = time_;
			this.value_ = value_;
		}

		public String getGene() {
			return gene_;
		}

		public String getRegion() {
			return region_;
		}

		public Level getExpressionLevel() {
			return value_;
		}

		public int getTime() {
			return time_;
		}
	}

    /*
     * Answer the maximum time point of the expression data from
     * the model source.
     */
    public int maxExpressionTimePoint() {
        int maxTime = -1;
        for (Integer time : mSrc_.getExpressionTimes()) {
            if (time > maxTime) {
                maxTime = time;
            }
        }

        return maxTime;
    }

    public HashMap<String, ArrayList<SimulatorRow>> getSimulatedExpressionTable(int maxTime) {
		ArrayList<Model> models = p.lm.getModelArray();

		HashMap<String, ModelDomain> domainMap = new HashMap<String, ModelDomain>();

		for (Model model : models) {
			for (ModelDomain domain : model.getModelDomains()) {
				domainMap.put(domain.getName(), domain);
			}
		}

		HashMap<String, ArrayList<SimulatorRow>> result = new HashMap<String, ArrayList<SimulatorRow>>();

        int maxSimulatedTimePoint = models.get(0).getStepCount() + 1;

		for (String nodeID : nodeIDsToNames_.keySet()) {
            Gene gene = p.gm.getGene(nodeIDsToNames_.get(nodeID));

			ArrayList<SimulatorRow> geneRows = new ArrayList<SimulatorRow>();

            int time = 1;
			for (; time <= maxSimulatedTimePoint; time++) {
				for (String domainName : domainMap.keySet()) {
					ModelDomain domain = domainMap.get(domainName);
                    Level value = GeNeToolSimulationTranslator.booleanToLevel(domain.getValue(gene, time));
					SimulatorRow row = new SimulatorRow(domain.getName(), nodeIDsToModelSourceNames_.get(nodeID), time - 1, value);
					geneRows.add(row);
				}

                result.put(nodeIDToExpressionGeneMap_.get(nodeID), geneRows);
			}

            for (; time <= maxTime; time++) {
                for (String domainName : domainMap.keySet()) {
                    ModelDomain domain = domainMap.get(domainName);
                    SimulatorRow row = new SimulatorRow(domain.getName(), nodeIDsToModelSourceNames_.get(nodeID), time - 1, Level.NO_DATA);
                    geneRows.add(row);
                }

                result.put(nodeIDToExpressionGeneMap_.get(nodeID), geneRows);
            }
		}

		return result;
	}
}
