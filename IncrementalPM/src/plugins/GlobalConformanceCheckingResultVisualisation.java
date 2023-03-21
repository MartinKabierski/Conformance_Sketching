package plugins;

import java.util.ArrayList;
import java.util.Collections;

import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.processmining.contexts.uitopia.annotations.UITopiaVariant;
import org.processmining.contexts.uitopia.annotations.Visualizer;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.plugin.annotations.Plugin;
import org.processmining.framework.plugin.annotations.PluginVariant;

import com.fluxicon.slickerbox.factory.SlickerFactory;

import conformance.IncrementalConformanceChecker.Goals;
import info.clearthought.layout.TableLayout;
import info.clearthought.layout.TableLayoutConstants;
import ressources.IncrementalConformanceResult;

public class GlobalConformanceCheckingResultVisualisation {

	@Plugin(name = "Incremental Conformance Result Visualisation", returnLabels = {
	"Incremental Conformance Result Visualisation" }, returnTypes = {JComponent.class }, parameterLabels = { "Incremental Sample-based Conformance Result" }, userAccessible = true)
	@Visualizer
	@UITopiaVariant(affiliation = "Humboldt-Universität zu Berlin", author = "Martin Kabierski", email = "martin.kabierski@hu-berlin.de", uiLabel = UITopiaVariant.USEPLUGIN)
	@PluginVariant(variantLabel = "Visualise Incremental Conformance Result", requiredParameterLabels = { 0 })
	public JPanel iccVisualisation(PluginContext context, IncrementalConformanceResult iccResult) {
		JPanel panel = new JPanel();
		double[][] mainPanelSizes = {{TableLayoutConstants.FILL}, {TableLayoutConstants.MINIMUM ,TableLayoutConstants.MINIMUM ,TableLayoutConstants.FILL}};
		panel.setLayout(new TableLayout(mainPanelSizes));
		SlickerFactory factory = SlickerFactory.instance();
		
		JPanel test =  factory.createRoundedPanel();
		double[][] testSizes = {{0.1,0.5}, {30, 20, 20, 20, 10, 20, 20, 10}};
		test.setLayout(new TableLayout(testSizes));
		test.add(factory.createLabel("<html><h1>Stats</h1></html>"),"0,0,1,0");
		test.add(factory.createLabel("Delta:"),"0,1,0,1"); test.add(factory.createLabel(Double.toString(iccResult.getIccParameter().getDelta())),"1,1,1,1");
		test.add(factory.createLabel("Alpha:"),"0,2,0,2"); test.add(factory.createLabel(Double.toString(iccResult.getIccParameter().getDelta())),"1,2,1,2");
		test.add(factory.createLabel("Epsilon:"),"0,3,0,3"); test.add(factory.createLabel(Double.toString(iccResult.getIccParameter().getDelta())),"1,3,1,3");
		
		test.add(factory.createLabel("Sampled Traces:"),"0,5,0,5"); test.add(factory.createLabel(Double.toString(iccResult.getCnt())),"1,5,1,5");
		test.add(factory.createLabel("Sampled Trace Variants:"),"0,6,0,6"); test.add(factory.createLabel(Double.toString(iccResult.getTotalVariants())),"1,6,1,6");
		
		panel.add(test, "0,0");
	
		JPanel test2 =  factory.createRoundedPanel();
		

		if(iccResult.getIccParameter().isApproximate()) {
			double[][] test2Sizes = {{0.1,0.5}, {30, 20, 20, 20, 10, 20, 20, 10}};
			test2.setLayout(new TableLayout(test2Sizes));
			test2.add(factory.createLabel("<html><h1>Approximation</html></h1>)"),"0,0,1,0");
			test2.add(factory.createLabel("Approximative Conformance was utilized"),"0,1,1,1");
			test2.add(factory.createLabel("Approximation Heuristic:"),"0,2,0,2"); test2.add(factory.createLabel(iccResult.getIccParameter().getApproximationHeuristic().toString()),"1,2,1,2");
			test2.add(factory.createLabel("k:"),"0,3,0,3"); test2.add(factory.createLabel(Double.toString((iccResult.getIccParameter().getK()))),"1,3,1,3");
			
			test2.add(factory.createLabel("Approximated Trace Variants:"),"0,5,0,5"); test2.add(factory.createLabel(Double.toString((iccResult.getApproximatedVariants()))),"1,5,1,5");
			test2.add(factory.createLabel("Approxiamted then calculated.:"),"0,6,0,6"); test2.add(factory.createLabel(Double.toString((iccResult.GetApproxThencalc()))),"1,6,1,6");
		}
		else {
			double[][] test2Sizes = {{0.1,0.5}, {30, 20, 10}}; 
			test2.setLayout(new TableLayout(test2Sizes));
			test2.add(factory.createLabel("<html><h1>Approximation</html></h1>)"),"0,0,1,0");
			test2.add(factory.createLabel("Approximative Conformance was not utlized"),"0,1,1,1");
		}

		panel.add(test2, "0,1");
		
		
		JPanel dp =  factory.createRoundedPanel();
		if(iccResult.getIccParameter().getGoal().equals(Goals.FITNESS)) {
			double panelSize[][] = {{0.1,0.5},{30,20}};
			dp.setLayout(new TableLayout(panelSize));
			
			dp.add(factory.createLabel("<html><h1>Goal: Fitness</h1></html>"),"0,0,1,0");
			dp.add(factory.createLabel("Fitness:"),"0,1,0,1"); dp.add(factory.createLabel(Double.toString((iccResult.getFitness()))),"1,1,1,1");
		}
		
		if(iccResult.getIccParameter().getGoal().equals(Goals.DEVIATIONS)) {
			double[] dims = new double[iccResult.getDeviations().keySet().size()+3];
			
			dims[0]= 30;
			dims[1]= 30;
			dims[2]= 30;
			for(int i=2;i<dims.length;i++) {
				dims[i]=20;
			}
		
			double panelSize[][] = {{0.05,0.2,0.1, 0.1},dims};
			dp.setLayout(new TableLayout(panelSize));
			
			dp.add(factory.createLabel("<html><h1>Goal: Deviations</h1></html>"),"0,0,2,0");
			dp.add(factory.createLabel("Total Number Deviations: "+ Integer.toString(iccResult.getNoDeviations())),"0,1,2,1");
			dp.add(factory.createLabel("<html><h3>Deviating Activities</h3></html>"),"1,2,1,2"); dp.add(factory.createLabel("<html><h3>Relative Counts</h3></html>"),"2,2,2,2"); dp.add(factory.createLabel("<html><h3>Absolute Counts</h3></html>"),"3,2,3,2");
			
			int c =3;
			for(String key : iccResult.getDeviations().keySet()) {
				dp.add(factory.createLabel(key),"1,"+c+",1,"+c+",l,b");
				dp.add(factory.createLabel(iccResult.getDeviations().get(key).toString()),"2,"+c+",2,"+c+",l,b");
				dp.add(factory.createLabel((iccResult.getDeviationsAbsolute().get(key)).toString()),"3,"+c+",3,"+c+",l,b");

				c++;
			}
		}
		panel.add(dp, "0,2");
		
		return panel;
		
		
	}
}
