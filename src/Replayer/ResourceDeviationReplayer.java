package Replayer;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import org.deckfour.xes.classification.XEventClasses;
import org.deckfour.xes.classification.XEventClassifier;
import org.deckfour.xes.info.XLogInfo;
import org.deckfour.xes.info.XLogInfoFactory;
import org.deckfour.xes.info.impl.XLogInfoImpl;
import org.deckfour.xes.model.XAttributeMap;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.deckfour.xes.model.impl.XLogImpl;
import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.models.graphbased.directed.petrinet.PetrinetGraph;
import org.processmining.models.graphbased.directed.petrinet.elements.Place;
import org.processmining.models.semantics.petrinet.Marking;
import org.processmining.plugins.connectionfactories.logpetrinet.TransEvClassMapping;
import org.processmining.plugins.petrinet.replayresult.PNRepResult;
import org.processmining.plugins.petrinet.replayresult.StepTypes;

import Ressources.IccParameters;
import Ressources.ReplayResultsContainer;
import Ressources.TraceReplayResult;
import nl.tue.alignment.Progress;
import nl.tue.alignment.Replayer;
import nl.tue.alignment.ReplayerParameters;
import nl.tue.alignment.algorithms.ReplayAlgorithm.Debug;
import resourcedeviations.ResourceAssignment;

public class ResourceDeviationReplayer implements IncrementalReplayer {

	ReplayResultsContainer resultHistory;
	IccParameters iccparameters;
	ResourceAssignment resAssignment;
	Set<String> activities;
	
	public ResourceDeviationReplayer(IccParameters parameters, ResourceAssignment resAssignment, XLog log) {
		this.resultHistory=new ReplayResultsContainer();
		this.iccparameters=parameters;
		this.resAssignment = resAssignment;
		this.activities = getActivities(log);
	}
	
	public void setResourceAssignment(ResourceAssignment resAssignment) {
		this.resAssignment = resAssignment;
	}
	
	public boolean TraceVariantKnown(XTrace trace) {
		if(this.resultHistory.contains(this.resultHistory.convertToString(trace))) {
			return true;
		}
		return false;
	}

	public boolean abstractAndCheckPredicate(XTrace trace, Object[] additionalInformation) {
		PetrinetGraph net=(PetrinetGraph) additionalInformation[0];
		XAttributeMap logAttributes=(XAttributeMap) additionalInformation[1];
		TransEvClassMapping mapping =(TransEvClassMapping) additionalInformation[2];
		XLog log=new XLogImpl(logAttributes);
		log.add(trace);

		
		int nThreads = 2;
		int costUpperBound = Integer.MAX_VALUE;
		
		XEventClassifier eventClassifier=XLogInfoImpl.STANDARD_CLASSIFIER;
		XLogInfo summary = XLogInfoFactory.createLogInfo(log, eventClassifier);
		XEventClasses classes = summary.getEventClasses();
		
		Marking initialMarking = getInitialMarking(net);
		Marking finalMarking = getFinalMarking(net);
		
		ReplayerParameters parameters = new ReplayerParameters.Default(nThreads, costUpperBound, Debug.NONE);
		Replayer replayer = new Replayer(parameters, (Petrinet) net, initialMarking, finalMarking, classes, mapping, false);
		PNRepResult pnrresult = null;
		try {
			pnrresult = replayer.computePNRepResult(Progress.INVISIBLE, log);
		} catch (InterruptedException | ExecutionException e) {
			e.printStackTrace();
		}
		
		TraceReplayResult result = new TraceReplayResult(this.resultHistory.convertToString(trace),trace,trace.size(),false,false,false,0.0,0.0,null);
		List<StepTypes> stepTypes = pnrresult.iterator().next().getStepTypes();
		result.setStepTypes(stepTypes);
		result.setViolatingResources(determineViolatingResources(trace, stepTypes));
		
		// check new information predicate on conformance result
		Map<String, Set<String>> oldResults = resultHistory.getViolatingResources();
		resultHistory.addTraceResult(trace, result);
		Map<String, Set<String>> newResults = resultHistory.getViolatingResources();
		
		// check if new information in conformance result
		return computeDistance(oldResults, newResults) > iccparameters.getEpsilon();
	}
	
	public double computeDistance(Map<String, Set<String>> oldResults, Map<String, Set<String>> newResults) {
		double sum = 0.0;
		for (String activity : newResults.keySet()) {
			int oldCount = 0;
			if (oldResults.containsKey(activity)) {
				oldCount = oldResults.get(activity).size();
			} 
			int newCount = newResults.get(activity).size();
			sum += (double) (newCount - oldCount) / newCount;
		}
		return sum / this.activities.size();
	}
	

	public boolean incrementAndCheckPredicate(XTrace trace) {
		String traceString = resultHistory.convertToString(trace);
		TraceReplayResult result = new TraceReplayResult(this.resultHistory.convertToString(trace),trace,trace.size(),false,false,false,0.0,0.0,null);
		List<StepTypes> stepTypes = resultHistory.get(traceString).getStepTypes();
		result.setStepTypes(stepTypes);
		result.setViolatingResources(determineViolatingResources(trace, stepTypes));
		
		// check new information predicate on conformance result
		Map<String, Set<String>> oldResults = resultHistory.getViolatingResources();
		resultHistory.addTraceResult(trace, result);
		Map<String, Set<String>> newResults = resultHistory.getViolatingResources();

		// check if new information in conformance result
		return computeDistance(oldResults, newResults) > iccparameters.getEpsilon();
	}
	
	private Map<String, Set<String>> determineViolatingResources(XTrace trace, List<StepTypes> stepTypes) {
		Map<String, Set<String>> result = getResourcesFromSkipSteps(trace, stepTypes);
		Map<String, Set<String>> result2 = getUnauthorizedResources(trace);
		
		for (String activity : result2.keySet()) {
			if (result.containsKey(activity)) {
				result.get(activity).addAll(result2.get(activity));
			} else {
				result.put(activity, result2.get(activity));
			}
		}
		return result;
	}
	
	private Map<String, Set<String>> getResourcesFromSkipSteps(XTrace trace, List<StepTypes> stepTypes) {
		Map<String, Set<String>> result = new HashMap<>();
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
	
	private Map<String, Set<String>> getUnauthorizedResources(XTrace trace) {
		Map<String, Set<String>> result = new HashMap<>();
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
	
	private void addViolatingResource(Map<String, Set<String>> violationMap, String activity, String resource) {
		Set<String> activityViolations;
		if (violationMap.containsKey(activity)) {
			activityViolations = violationMap.get(activity);
		} else {
			activityViolations = new HashSet<>();
		}
		activityViolations.add(resource);
		violationMap.put(activity, activityViolations);
	}	

	public ReplayResultsContainer getResult() {
		return resultHistory;
	}
	
	
	private static Marking getFinalMarking(PetrinetGraph net) {
		Marking finalMarking = new Marking();

		for (Place p : net.getPlaces()) {
			if (net.getOutEdges(p).isEmpty())
				finalMarking.add(p);
		}

		return finalMarking;
	}

	private static Marking getInitialMarking(PetrinetGraph net) {
		Marking initMarking = new Marking();

		for (Place p : net.getPlaces()) {
			if (net.getInEdges(p).isEmpty())
				initMarking.add(p);
		}

		return initMarking;
	}
	
	private Set<String> getActivities(XLog log) {
		Set<String> result = new HashSet<>();
		for (XTrace trace : log) {
			for (XEvent event : trace) {
				result.add(event.getAttributes().get("concept:name").toString());
			}
		}
		return result;
	}

	
	
}
