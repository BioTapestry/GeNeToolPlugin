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

import org.systemsbiology.biotapestry.simulation.ModelExpressionEntry;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GeNeToolSimulationTranslator {
    private HashMap<String, ArrayList<GeNeToolModelAdapter.SimulatorRow>> data_;

    public GeNeToolSimulationTranslator(HashMap<String, ArrayList<GeNeToolModelAdapter.SimulatorRow>> data) {
        data_ = data;
    }

    public String transformLevel(ModelExpressionEntry.Level level) {
        if (level == ModelExpressionEntry.Level.EXPRESSED) {
            return "yes";
        }
        else {
            return "no";
        }
    }

    public String asXMLString() {
        String xmlString = "";
        try {
            Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();

            Element rootElement = doc.createElement("TimeCourseData");
            doc.appendChild(rootElement);

            for (String geneName : data_.keySet()) {
                Element timeCourse = doc.createElement("timeCourse");
                timeCourse.setAttribute("gene", geneName);
                timeCourse.setAttribute("baseConfidence", "normal");
                timeCourse.setAttribute("timeCourse", "no");

                rootElement.appendChild(timeCourse);

                for (GeNeToolModelAdapter.SimulatorRow row : data_.get(geneName)) {
                    Element dataEl = doc.createElement("data");
                    timeCourse.appendChild(dataEl);

                    dataEl.setAttribute("region", row.getRegion());
                    dataEl.setAttribute("time", new Integer(row.getTime()).toString());
                    dataEl.setAttribute("expr", transformLevel(row.getExpressionLevel()));
                }
            }

            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            DOMSource source = new DOMSource(doc);

            StringWriter writer = new StringWriter();
            StreamResult result = new StreamResult(writer);

            transformer.transform(source, result);

            xmlString = writer.toString();

        } catch (TransformerConfigurationException tce) {
            tce.printStackTrace();
        } catch (TransformerException te) {
            te.printStackTrace();
        } catch (ParserConfigurationException pce) {
            pce.printStackTrace();
        }

        return xmlString;
    }

    public static ModelExpressionEntry.Level booleanToLevel(boolean value) {
        if (value) {
            return ModelExpressionEntry.Level.EXPRESSED;
        }
        else {
            return ModelExpressionEntry.Level.NOT_EXPRESSED;
        }
    }

    public Map<String, List<ModelExpressionEntry>> getModelExpressionEntryList() {
        Map<String, List<ModelExpressionEntry>> result = new HashMap<String, List<ModelExpressionEntry>>();

        for (String geneName : data_.keySet()) {
            List<ModelExpressionEntry> meeList = new ArrayList<ModelExpressionEntry>();
            result.put(geneName, meeList);
            for (GeNeToolModelAdapter.SimulatorRow row : data_.get(geneName)) {
                meeList.add(new ModelExpressionEntry(row.getRegion(),
                        row.getTime(),
                        row.getExpressionLevel(),
                        ModelExpressionEntry.Source.NO_SOURCE_SPECIFIED, 0.0));
            }
        }

        return result;
    }
}
