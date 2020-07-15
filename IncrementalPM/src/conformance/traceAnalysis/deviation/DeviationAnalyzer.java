package conformance.traceAnalysis.deviation;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;
import org.deckfour.xes.model.XAttributeMap;
import org.deckfour.xes.model.XTrace;
import org.processmining.models.graphbased.directed.petrinet.PetrinetGraph;
import org.processmining.plugins.connectionfactories.logpetrinet.TransEvClassMapping;
import org.processmining.plugins.petrinet.replayresult.PNRepResult;
import org.processmining.plugins.petrinet.replayresult.StepTypes;
import org.processmining.plugins.replayer.replayresult.SyncReplayResult;

import conformance.traceAnalysis.IncrementalTraceAnalyzer;
import conformance.traceAnalysis.TraceEditDistance;
import ressources.IccParameter;
import ressources.TraceAnalysisResult;
import utils.TraceRepresentations;
/**
 * 
 * @author Martin Bauer
 *
 */
public class DeviationAnalyzer extends IncrementalTraceAnalyzer<Map<String, Double>> {
	TransEvClassMapping mapping;
	
	public DeviationAnalyzer(IccParameter parameter, TransEvClassMapping mapping, PetrinetGraph net) {
		super(parameter, net);
		this.mapping = mapping;
		this.conformanceCalculator = new DeviationConformanceCalculator();
	}
	
	protected Map<String, Double> retrieveConformanceInformation(XTrace trace, PNRepResult replayResult) {
		//System.out.println("New Trace Variant: "+ TraceRepresentations.getActivitySequence(trace));
		Map<String, Double> deviations = new HashMap<String, Double>();
		for(SyncReplayResult replaySteps : replayResult) {
			//System.out.println(replaySteps.getStepTypes().toString());
			for (int j=0;j<replaySteps.getStepTypes().size();j++) {
				if(replaySteps.getStepTypes().get(j)== StepTypes.L||replaySteps.getStepTypes().get(j)==StepTypes.MREAL) {
					//deviations.put(replaySteps.getNodeInstance().get(j).toString(),deviations.getOrDefault(replaySteps.getNodeInstance().get(j).toString(), 0.0)+1.0);
					//System.out.println(mapping.toString());
					//System.out.println(replaySteps.getStepTypes().get(j).toString()+" , "+replaySteps.getNodeInstance().get(j).toString());
					if(replaySteps.getStepTypes().get(j)==StepTypes.MREAL) {
						if(mapping.containsKey(replaySteps.getNodeInstance().get(j))){
							deviations.put(mapping.get(replaySteps.getNodeInstance().get(j)).toString(),deviations.getOrDefault(mapping.get(replaySteps.getNodeInstance().get(j)).toString(), 0.0)+1.0);
						}
						else {
							deviations.put(replaySteps.getNodeInstance().get(j).toString(),deviations.getOrDefault(replaySteps.getNodeInstance().get(j).toString(), 0.0)+1.0);
						}
					}
					else if(replaySteps.getStepTypes().get(j)== StepTypes.L) {
						deviations.put(replaySteps.getNodeInstance().get(j).toString(),deviations.getOrDefault(replaySteps.getNodeInstance().get(j).toString(), 0.0)+1.0);
					}
				}
			}
		}
		//System.out.println("Retrieved Deviations: "+deviations);
		//System.out.println();
		return deviations;
	}

	protected List<Map<String, Double>> determineConformanceApproximations(XTrace trace,
			Pair<TraceAnalysisResult<Map<String, Double>>, TraceEditDistance> referenceTraceInformation,
			XAttributeMap logAttributes) {
		return null;
	}
}
