package conformance.traceAnalysis.resourceDeviation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang3.tuple.Pair;
import org.deckfour.xes.model.XAttributeMap;
import org.deckfour.xes.model.XTrace;
import org.processmining.models.graphbased.directed.petrinet.PetrinetGraph;
import org.processmining.plugins.petrinet.replayresult.StepTypes;

import conformance.IncrementalConformanceChecker.Heuristics;
import conformance.traceAnalysis.TraceEditDistance;
import resourcedeviations.ResourceAssignment;
import ressources.IccParameter;
import ressources.TraceAnalysisResult;

public class ApproxResourceDeviationAnalyzer_NONALIGNING extends ResourceDeviationAnalyzer {
	Map<String,Set<String>> knownViolations;
	
	
	public ApproxResourceDeviationAnalyzer_NONALIGNING(IccParameter parameter, ResourceAssignment resAssignment, PetrinetGraph net) {
		super(parameter, resAssignment, net);
		this.knownViolations = new ConcurrentHashMap<String,Set<String>>();
	}

	protected List<Map<String,Map<String, Double>>> determineConformanceApproximations(XTrace trace, Pair<TraceAnalysisResult<Map<String,Map<String, Double>>>, TraceEditDistance> referenceTraceInformation, XAttributeMap logAttributes) {
		//filter out unknown deviations and merge approximated + referenceTrace 
		Map<String,Map<String,Double>> original = referenceTraceInformation.getLeft().getResult();
		List<StepTypes> approximatedStepTypes=referenceTraceInformation.getRight().getStepTypes();
		Map<String,Map<String,Double>> approximatedViolations = determineViolatingResources(trace, approximatedStepTypes);
		
		if(this.parameter.getApproximationHeuristic()==Heuristics.NONALIGNING_KNOWN) {
			approximatedViolations.keySet().retainAll(knownViolations.keySet());
			for(String activity : approximatedViolations.keySet())
				approximatedViolations.get(activity).keySet().retainAll(knownViolations.get(activity));
			/*Map<String,Map<String,Double>> knownApproximatedViolations = new HashMap<String,Map<String,Double>>();
			for (String activity : approximatedViolations.keySet()) {
				if(this.knownViolations.containsKey(activity)) {
					knownApproximatedViolations.put(activity, new HashMap<String,Double>());
					for(String resource : approximatedViolations.get(activity).keySet()) {
						if(this.knownViolations.get(activity).contains(resource))
							knownApproximatedViolations.get(activity).put(resource,approximatedViolations.get(activity).get(resource));
					}
				}
			}*/
			//approximatedViolations = knownApproximatedViolations;
		}
		for(String activity : original.keySet()) {
			if(!approximatedViolations.containsKey(activity)) {
				approximatedViolations.put(activity, new HashMap<String, Double>());
			}
			for(String resource : original.get(activity).keySet()) {
				approximatedViolations.get(activity).put(resource, approximatedViolations.get(activity).getOrDefault(resource, 0.0)+original.get(activity).get(resource));
			}
		}
		List<Map<String,Map<String, Double>>> approximations = new ArrayList<Map<String,Map<String, Double>>>();
		approximations.add(approximatedViolations);
		return approximations;
	}
	
	protected double updateConformance(TraceAnalysisResult<Map<String,Map<String, Double>>> traceAnalysisResult) {
		this.results.put(traceAnalysisResult);
		if(this.parameter.getApproximationHeuristic()==Heuristics.NONALIGNING_KNOWN && !traceAnalysisResult.approximated)
			for(String activity : traceAnalysisResult.getResult().keySet()) {
				if(!this.knownViolations.containsKey(activity))
					this.knownViolations.put(activity, new HashSet<String>());
				for(String resource : traceAnalysisResult.getResult().get(activity).keySet()) {
					this.knownViolations.get(activity).add(resource);
				}
			}
		double distance = this.conformanceCalculator.update(traceAnalysisResult.getResult());
		return distance;
	}
}
