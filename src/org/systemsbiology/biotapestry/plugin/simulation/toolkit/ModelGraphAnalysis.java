/*
**    Copyright (C) 2003-2016 Institute for Systems Biology
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

package org.systemsbiology.biotapestry.plugin.simulation.toolkit;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.systemsbiology.biotapestry.simulation.ModelLink;
import org.systemsbiology.biotapestry.simulation.ModelNode;
import org.systemsbiology.biotapestry.simulation.ModelSource;

public class ModelGraphAnalysis {
  private ModelSource mSrc_;

  public ModelGraphAnalysis(ModelSource mSrc) {
    mSrc_ = mSrc;
  }

  /***************************************************************************
   **
   ** Finds feedback links
   */

  public Set<ModelLink> findFeedbackEdges() {
    Set<ModelLink> result = new HashSet<ModelLink>();

    Set<String> nodes = new HashSet<String>();
    for (Iterator<ModelNode> nodeIter = mSrc_.getRootModelNodes(); nodeIter.hasNext();) {
      ModelNode node = nodeIter.next();
      String id = node.getUniqueInternalID();
      nodes.add(id);
    }

    Set<Link> links = new HashSet<Link>();
    Map<Link, ModelLink> linkMap = new HashMap<Link, ModelLink>();

    for (Iterator<ModelLink> linkIter = mSrc_.getRootModelLinks(); linkIter.hasNext();) {
      ModelLink modelLink = linkIter.next();

      String src = modelLink.getSrc();
      String trg = modelLink.getTrg();
      Link link = new Link(src, trg);
      links.add(link);

      linkMap.put(link, modelLink);
    }

    Set<String> rootNodeIDs = findRootModelRootNodeIDs();

    GraphFeedbackFinder feedbackFinder = new GraphFeedbackFinder(nodes, links);
    Set<Link> possibleFeedback = feedbackFinder.run(new ArrayList<String>(rootNodeIDs));

    for (Link link : possibleFeedback) {
      result.add(linkMap.get(link));
    }

    return result;
  }

  /***************************************************************************
   **
   ** Finds root nodes
   */
  public Set<String> findRootModelRootNodeIDs() {
    HashSet<String> retval = new HashSet<String>();

    for (Iterator<ModelNode> nodeIter = mSrc_.getRootModelNodes(); nodeIter.hasNext();) {
      ModelNode node = nodeIter.next();
      String id = node.getUniqueInternalID();
      retval.add(id);
    }

    for (Iterator<ModelLink> linkIter = mSrc_.getRootModelLinks(); linkIter.hasNext();) {
      ModelLink modelLink = linkIter.next();
      retval.remove(modelLink.getTrg());
    }

    return retval;
  }
}
