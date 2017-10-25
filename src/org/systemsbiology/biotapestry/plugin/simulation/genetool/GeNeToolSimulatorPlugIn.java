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

import processing.core.PApplet;
import grnboolmodel.GRNBoolModel;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JFrame;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.*;

import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.border.EmptyBorder;

import org.systemsbiology.biotapestry.plugin.SimulatorPlugIn;
import org.systemsbiology.biotapestry.simulation.ModelExpressionEntry;
import org.systemsbiology.biotapestry.simulation.ModelLink;
import org.systemsbiology.biotapestry.simulation.ModelNode;
import org.systemsbiology.biotapestry.simulation.ModelRegion;
import org.systemsbiology.biotapestry.simulation.ModelRegionTopologyForTime;
import org.systemsbiology.biotapestry.simulation.ModelSource;
import org.systemsbiology.biotapestry.util.UiUtil;

public class GeNeToolSimulatorPlugIn extends JFrame implements SimulatorPlugIn {
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE CONSTANTS
  //
  ////////////////////////////////////////////////////////////////////////////
	
  private static String menuName_ = "GeNeTool Simulator";                  
	
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////  

  private ModelSource mSrc_;
  private JEditorPane descriptionPane_;
  private PApplet embed_;
  private GeNeToolModelAdapter modelAdapter_;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** Constructor 
  */ 
  
  public GeNeToolSimulatorPlugIn()  {
    super("GeNeTool Simulation PlugIn");
    Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
    setSize(1024, 1024);    

    Dimension frameSize = getSize();
    int x = (screenSize.width - frameSize.width) / 2;
    int y = (screenSize.height - frameSize.height) / 2;
    setLocation(x, y);
    JPanel cp = (JPanel)getContentPane();
    cp.setBorder(new EmptyBorder(20, 20, 20, 20));
    cp.setLayout(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints();    

    //
    // Build the username panel:
    //
    embed_ = new GRNBoolModel();
    
    int rowNum = 0;
    UiUtil.gbcSet(gbc, 0, rowNum, 1, 6, UiUtil.BO, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 1.0);
    rowNum += 6;
    cp.add(embed_, gbc);
       
    String redirect = "Simulator Stub!"; 
    descriptionPane_ = new JEditorPane("text/plain", "");
    JScrollPane jsp = new JScrollPane(descriptionPane_);
    descriptionPane_.setEditable(false);
    descriptionPane_.setText(redirect);
    UiUtil.gbcSet(gbc, 0, rowNum, 1, 2, UiUtil.BO, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 0.3);
    rowNum += 2;
    cp.add(jsp, gbc);

    //
    // Build the button panel:
    //

    JButton buttonConvert = new JButton("Convert model");
    buttonConvert.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ev) {
        try {
          setupGeNeTool();

        } catch (Exception ex) {
          System.err.println("Caught exception");
          ex.printStackTrace();
        }
      }
    });

    UiUtil.gbcSet(gbc, 0, rowNum++, 1, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 0.0, 0.0);
    cp.add(buttonConvert, gbc);

    JButton buttonReadSimResults = new JButton("Print simulation result");
    buttonReadSimResults.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ev) {
        try {
          readSimulatorResults();
        } catch (Exception ex) {
          System.err.println("Caught exception");
          ex.printStackTrace();
        }
      }
    });

    UiUtil.gbcSet(gbc, 0, rowNum++, 1, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 0.0, 0.0);
    cp.add(buttonReadSimResults, gbc);

    /*
    JButton buttonFeedbackLinks = new JButton("Highlight feedback links");
    buttonFeedbackLinks.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ev) {
        try {
          highlightFeedbackLinks();
        } catch (Exception ex) {
          System.err.println("Caught exception");
          ex.printStackTrace();
        }
      }
    });

    UiUtil.gbcSet(gbc, 0, rowNum++, 1, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 0.0, 0.0);
    cp.add(buttonFeedbackLinks, gbc);
    */

    JButton buttonO = new JButton("Close this Window");
    buttonO.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ev) {
        try {
          GeNeToolSimulatorPlugIn.this.setVisible(false);
          GeNeToolSimulatorPlugIn.this.dispose();
        } catch (Exception ex) {
          System.err.println("Caught exception");
        }
      }
    });     

    UiUtil.gbcSet(gbc, 0, rowNum++, 1, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 0.0, 0.0);
    cp.add(buttonO, gbc);  
    
    addWindowListener(new WindowAdapter() {
      @Override
      public void windowClosing(WindowEvent e) {
	  	embed_.stop();
        dispose();
      }
    });
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Launch the engine
  */
  
  public void launch() {
    // important to call this whenever embedding a PApplet.
    // It ensures that the animation thread is started and
    // that other internal variables are properly set.
       
  	embed_.init();
	setVisible(true);
  }
  
  /***************************************************************************
  **
  ** Set the model source
  */  
   
  public void setModelSource(ModelSource mSrc) {
    mSrc_ = mSrc;
    return;
  }
  
  /***************************************************************************
   *
   * Provide results
   *
   * Gets the simulated data from GeNeTool.
   *
   * The returned list always contains as many time points as the expression
   * data originally provided to the plugin contained. If the GeNeTool
   * simulation contains less time points, the result will be padded with
   * NO_DATA entries.
   *
  */
  public Map<String, List<ModelExpressionEntry>> provideResults() {
      GeNeToolModelAdapter adapter = getAdapterInstance();
      int maxTime = adapter.maxExpressionTimePoint();
      return new GeNeToolSimulationTranslator(adapter.getSimulatedExpressionTable(maxTime)).getModelExpressionEntryList();
  }

  /***************************************************************************
  **
  ** Returns the menu name
  */  
  public String getMenuName() {
	  return menuName_;
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** List the nodes
  */  
   
  private void listNodes() {
    StringBuffer buf = new StringBuffer();
    Iterator<ModelNode> nit = mSrc_.getRootModelNodes();
    while (nit.hasNext()) {
      ModelNode node = nit.next();
      String name = node.getName();
      String id = node.getUniqueInternalID();
      ModelNode.Type type = node.getType();
      buf.append("\"");
      buf.append(name);
      buf.append("\" ");
      buf.append(id);
      buf.append(" ");
      buf.append(type);
      buf.append("\n");
    }
    descriptionPane_.setText(buf.toString());
    return;
  }
  
  /***************************************************************************
  **
  ** List the nodes
  */  
   
  private void listLinks() {
    StringBuffer buf = new StringBuffer();
    Iterator<ModelLink> lit = mSrc_.getRootModelLinks();
    while (lit.hasNext()) {
      ModelLink link = lit.next();
      String src = link.getSrc();
      String trg = link.getTrg();
      String id = link.getUniqueInternalID();
      ModelLink.Sign sign = link.getSign();
      buf.append("\"");
      buf.append(mSrc_.getNode(src).getName());
      buf.append("\" \"");
      buf.append(mSrc_.getNode(trg).getName());
      buf.append("\" ");
      buf.append(id);
      buf.append(" ");
      buf.append(sign);
      buf.append("\n");
    }
    descriptionPane_.setText(buf.toString());
    return;
  }
  
  /***************************************************************************
  **
  ** List the nodes
  */  
   
  private void listTopology() {
    StringBuffer buf = new StringBuffer();
    Iterator<ModelRegionTopologyForTime> lit = mSrc_.getRegionTopology();
    while (lit.hasNext()) {
      ModelRegionTopologyForTime topo = lit.next();
      int minTime = topo.getMinTime();
      int maxTime = topo.getMaxTime();
      buf.append("--------------------------TIME: min = ");
      buf.append(minTime);
      buf.append(" max = ");
      buf.append(maxTime);
      buf.append("\n"); 
      buf.append("REGIONS:\n"); 
      Iterator<String> rit = topo.getRegions();
      while (rit.hasNext()) {
        buf.append("Region: ");
        buf.append(rit.next());
        buf.append("\n"); 
      }
      buf.append("LINKS:\n"); 
      Iterator<ModelRegionTopologyForTime.TopoLink> tlit = topo.getLinks();
      while (tlit.hasNext()) {
        ModelRegionTopologyForTime.TopoLink tlink = tlit.next();
        buf.append("Link: ");
        buf.append(tlink.getRegion1());
        buf.append(" to: ");
        buf.append(tlink.getRegion2());
        buf.append("\n"); 
      }
    }
    descriptionPane_.setText(buf.toString());
    return;
  }
  
  /***************************************************************************
  **
  ** List the nodes
  */  
   
  private void listLineage() {
    StringBuffer buf = new StringBuffer(); 
    List<ModelRegion> mrl = mSrc_.getRegions();
    for (ModelRegion mr : mrl) {
      dumpAReg(mr, buf);
    }
    descriptionPane_.setText(buf.toString());
    return;
  }
  
  /***************************************************************************
  **
  ** List the nodes
  */  	
   
  private void dumpAReg(ModelRegion mr, StringBuffer buf) {
    buf.append(mr.getRegionName());
    buf.append(": minTime = ");
    buf.append(mr.getRegionStart());
    buf.append(" maxTime = ");
    buf.append(mr.getRegionEnd());
    buf.append(" lineage:");
    List<String> lin = mr.getLineage();
    for (String lr : lin) {
      buf.append(" \"");
      buf.append(lr);
      buf.append("\"");
    }
    buf.append("\n");
    return;
  }

  /***************************************************************************
  **
  ** List Expression. 
  ** Note:
  ** GeNeTool codes: 0 = No data, 1 = No expression, 2 = Weak Expression , 3 = Expression, 4 = Maternal
  */  
   
  private void listExpression() {
    StringBuffer buf = new StringBuffer(); 
    SortedSet<String> mrl = new TreeSet<String>(mSrc_.getExpressionGenes());    
    SortedSet<Integer> times = mSrc_.getExpressionTimes();
    List<ModelRegion> mrs = mSrc_.getRegions();
    ArrayList<String> mrn = new ArrayList<String>();
    for (ModelRegion mr : mrs) {
      mrn.add(mr.getRegionName());
    }
    for (String gene : mrl) {
      buf.append(gene);
      buf.append(":\n");
      for (String reg : mrn) {
        buf.append("  ");
        buf.append(reg);
        buf.append(":");
        for (Integer time : times) {
          ModelExpressionEntry mre = mSrc_.getExpressionEntry(gene, reg, time.intValue());
          if (mre != null) {
            buf.append(" ");
            buf.append(mre.getTime());
            buf.append(":");
            buf.append(mre.getLevel());
          }
        }
        buf.append("\n");
      }
    }
    descriptionPane_.setText(buf.toString());
    return;
  }
  
	public void convertModel() {
		getAdapterInstance().convert();
	}

	void setupGeNeTool() {
		convertModel();

		// Enable the expression data display
		GRNBoolModel pApp = (GRNBoolModel) embed_;

		pApp.getMenuManager().active("data", 1);

        /*
          The Gene-panel cannot be enabled by default, because
          the GRNBoolModel will crash when adding the vector
          equations to the genes in GeNeToolModelAdapter.

          The genes and vector equations have to be created first,
          then the Gene-panel can be expanded by the user.

          Therefore, do not uncomment this line:

          pApp.getMenuManager().active("genes", 1);
        */
	}

  void highlightFeedbackLinks() {
    Set<ModelLink> result = mSrc_.findFeedbackEdges();

    Set<String> linkIDs = new HashSet<String>();
    for (ModelLink modelLink : result) {
      linkIDs.add(modelLink.getUniqueInternalID());
    }

    mSrc_.goToModelAndSelect("bioTapA", new HashSet<String>(), linkIDs);

    System.out.println("Possible feedback links:");
    for (ModelLink link : result) {
      System.out.println("   " + link.getSrc() + " -> " + link.getTrg());
    }
  }

  private GeNeToolModelAdapter getAdapterInstance() {
    if (this.modelAdapter_ == null) {
      modelAdapter_ = new GeNeToolModelAdapter(mSrc_, (GRNBoolModel)embed_);
    }

    return modelAdapter_;
  }

  void readSimulatorResults() {
      GeNeToolModelAdapter adapter = getAdapterInstance();
      int maxTime = adapter.maxExpressionTimePoint();

      String xmlString = new GeNeToolSimulationTranslator(adapter.getSimulatedExpressionTable(maxTime)).asXMLString();
      System.out.println(xmlString);
  }
}