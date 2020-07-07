package conformance.traceAnalysis.resourceDeviation;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;
import org.deckfour.xes.model.XAttributeMap;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XTrace;
import org.processmining.models.graphbased.directed.petrinet.PetrinetGraph;
import org.processmining.plugins.petrinet.replayresult.PNRepResult;
import org.processmining.plugins.petrinet.replayresult.StepTypes;

import conformance.traceAnalysis.IncrementalTraceAnalyzer;
import conformance.traceAnalysis.TraceEditDistance;
import resourcedeviations.ResourceAssignment;
import ressources.IccParameter;
import ressources.TraceAnalysisResult;
import utils.TraceRepresentations;

public class ResourceDeviationAnalyzer extends IncrementalTraceAnalyzer<Map<String,Map<String, Double>>> {
	ResourceAssignment resAssignment;
	//Map<String, List<StepTypes>> stepTypes;

	public ResourceDeviationAnalyzer(IccParameter parameter, ResourceAssignment resAssignment, PetrinetGraph net) {
		super(parameter, net);
		this.resAssignment=resAssignment;
		//this.stepTypes = new HashMap<String, List<StepTypes>>();
		this.conformanceCalculator = new ResourceDeviationConformanceCalculator();
	}

	
	protected Map<String, Map<String, Double>> retrieveConformanceInformation(XTrace trace, PNRepResult replayResult) {
		List<StepTypes> stepTypes = replayResult.iterator().next().getStepTypes();
		//Map<String, List<StepTypes>> stepTypesMap = new HashMap<String, List<StepTypes>>();
		//stepTypesMap.put(TraceRepresentations.getActivitySequence(trace), stepTypes);
		return determineViolatingResources(trace, stepTypes);
	}

	
	protected List<Map<String, Map<String, Double>>> determineConformanceApproximations(XTrace trace,
			Pair<TraceAnalysisResult<Map<String, Map<String, Double>>>, TraceEditDistance> referenceTraceInformation,
			XAttributeMap logAttributes) {
		return null;
	}
	

	public Map<String, Map<String,Double>> determineViolatingResources(XTrace trace, List<StepTypes> stepTypes) {
		Map<String, Map<String,Double>> result = getResourcesFromSkipSteps(trace, stepTypes);
		Map<String, Map<String,Double>> result2 = getUnauthorizedResources(trace);
		for (String activity : result2.keySet()) {
			if (result.containsKey(activity)) {
				for(String resource : result2.get(activity).keySet()) {
					if(result.get(activity).containsKey(resource)) {
						result.get(activity).put(resource, result.get(activity).get(resource)+result2.get(activity).get(resource));
					}
					else
						result.get(activity).put(resource, result2.get(activity).get(resource));
				}
			} else {
				result.put(activity, result2.get(activity));
			}
		}
		return result;
	}
	
	protected Map<String, Map<String,Double>> getResourcesFromSkipSteps(XTrace trace, List<StepTypes> stepTypes) {
		Map<String, Map<String,Double>> result = new HashMap<>();
		for(StepTypes step : stepTypes) {
			int eventIndex = 0;
			if(step == StepTypes.L) {
				XEvent event = trace.get(eventIndex);
				if (event.getAttributes().get("org:resource") != null) {
					String resource = event.getAttributes().get("org:resource").toString();
					String activity = event.getAttributes().get("concept:name").toString();
					addViolatingResource(result, activity, resource);
				}
			}
			if(step == StepTypes.L  || step == StepTypes.LMGOOD) {
				eventIndex++;
			}
		}
		return result;
	}
	
	protected Map<String, Map<String, Double>> getUnauthorizedResources(XTrace trace) {
		Map<String, Map<String, Double>> result = new HashMap<>();
		for (XEvent event : trace) {
			String activity = event.getAttributes().get("concept:name").toString();
			if (event.getAttributes().get("org:resource") != null) {
				String resource = event.getAttributes().get("org:resource").toString();
				if (!this.resAssignment.isAuthorized(activity, resource)) {
					addViolatingResource(result, activity, resource);
				}
			}
		}
		return result;
	}
	
	protected void addViolatingResource(Map<String, Map<String, Double>> violationMap, String activity, String resource) {
		Map<String, Double> activityViolations;
		activityViolations = violationMap.getOrDefault(activity, new HashMap<String, Double>());
		if(activityViolations.containsKey(resource))
			activityViolations.put(resource, activityViolations.get(resource)+1);
		else
			activityViolations.put(resource, 1.0);
		violationMap.put(activity, activityViolations);
	}


}
