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
import org.processmining.plugins.replayer.replayresult.SyncReplayResult;

import com.google.common.collect.Multiset;
import com.google.common.collect.TreeMultiset;

import Ressources.IccParameters;
import Ressources.ReplayResultsContainer;
import Ressources.TraceReplayResult;
import Utils.TraceEditDistance;
import nl.tue.alignment.Progress;
import nl.tue.alignment.Replayer;
import nl.tue.alignment.ReplayerParameters;
import nl.tue.alignment.algorithms.ReplayAlgorithm.Debug;
import resourcedeviations.ResourceAssignment;

public class ApproxResourceDeviationReplayer implements IncrementalReplayer {

	ReplayResultsContainer resultHistory;
	IccParameters iccparameters;
	boolean initialized;
	ResourceAssignment resAssignment;
	Set<String> activities;
	
	
	public ApproxResourceDeviationReplayer(IccParameters parameters, ResourceAssignment resAssignment, XLog log) {
		this.resultHistory=new ReplayResultsContainer();
		this.iccparameters=parameters;
		this.resAssignment = resAssignment;
		this.activities = getActivities(log);
		this.initialized=false;
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
		if(!initialized) {
			initialized=true;
			TraceReplayResult result=replayTraceOnNet(trace, additionalInformation);
			this.resultHistory.addTraceResult(trace, result);
			return true;
		}

		Map<String, Set<String>> possibleNewViolations = possibleNewViolations(trace);

		// check if worst case scenario could lead to a significant difference
		double worstCase = computeDistanceWorstCase(resultHistory.getViolatingResources(), possibleNewViolations);
		if (worstCase <= iccparameters.getEpsilon()) {
			return false;
		}
		
		Map<String, Set<String>> authorizationViolations = getUnauthorizedResources(trace);
		//for each consec approximation scheme is used
		TraceEditDistance ted = new TraceEditDistance("","");

		double minimalDistance=-1.0;
		TraceReplayResult closestTraceReplayResult=null;
		for (TraceReplayResult alignmentInfo : resultHistory.values()) {
			TraceEditDistance current =new TraceEditDistance(trace,alignmentInfo.getTrace());
			double distance=current.getDistance();
			if (distance<minimalDistance || minimalDistance==-1.0) {
				minimalDistance=distance;
				closestTraceReplayResult=alignmentInfo;
				ted = current;
			}
		}

		//k-similarity check - depends on size of final set - if too large, distance is too large or exponential blowup in sets
		//TODO recheck if finalSetSize test is really needed?
		int finalSetSize=(int) (minimalDistance+closestTraceReplayResult.getAsynchMoves().size());
		if (finalSetSize > trace.size() || minimalDistance>Math.ceil(trace.size()*this.iccparameters.getK())) {
			TraceReplayResult result=replayTraceOnNet(trace, additionalInformation);

			// check new information predicate on conformance result
			Map<String, Set<String>> oldResults = resultHistory.getViolatingResources();
			resultHistory.addTraceResult(trace, result);
			Map<String, Set<String>> newResults = resultHistory.getViolatingResources();

			// check if new information in conformance result
			return computeDistance(oldResults, newResults) > iccparameters.getEpsilon();
		}
		
		Multiset<String> candidates=ted.getNonAligningActivities();

		//else get all possible worlds of candidate sets
		Multiset<String> singleElementSet=TreeMultiset.create();
		Set<Multiset<String>> finalCandidateSet=new HashSet<Multiset<String>>();

		//List<String> candidateList = new ArrayList<String>();
		//candidates.iterator().forEachRemaining(candidateList::add);
		Map<String, Set<String>> oldResults = this.resultHistory.getViolatingResources();
		
		for (String candidate : candidates) {
			Multiset<String> currSet=TreeMultiset.create();
			currSet.add(candidate);
			singleElementSet.add(candidate);
			finalCandidateSet.add(currSet);
			double possibleDistance = computeDistancePossibleWorld(oldResults, possibleNewViolations, authorizationViolations, currSet);
			if (possibleDistance > iccparameters.getEpsilon()) {
				TraceReplayResult result=replayTraceOnNet(trace, additionalInformation);

				// check new information predicate on conformance result
				oldResults = resultHistory.getViolatingResources();
				resultHistory.addTraceResult(trace, result);
				Map<String, Set<String>> newResults = resultHistory.getViolatingResources();

				// check if new information in conformance result
				return computeDistance(oldResults, newResults) > iccparameters.getEpsilon();
			}
		}

		for(int i=1;i<minimalDistance;i++) {
			Set<Multiset<String>> temp=new HashSet<Multiset<String>>();
			temp.addAll(finalCandidateSet);
			finalCandidateSet=new HashSet<Multiset<String>>();
			for(Multiset<String> cur: temp) {
				for(String curSingle : singleElementSet) {
					Multiset<String> newForFinal=TreeMultiset.create();
					newForFinal.addAll(cur);
					newForFinal.add(curSingle);
					finalCandidateSet.add(newForFinal);
					double possibleDistance = computeDistancePossibleWorld(oldResults, possibleNewViolations, authorizationViolations, newForFinal);
					if (possibleDistance > iccparameters.getEpsilon()) {
						TraceReplayResult result=replayTraceOnNet(trace, additionalInformation);

						// check new information predicate on conformance result
						oldResults = resultHistory.getViolatingResources();
						resultHistory.addTraceResult(trace, result);
						Map<String, Set<String>> newResults = resultHistory.getViolatingResources();

						// check if new information in conformance result
						return computeDistance(oldResults, newResults) > iccparameters.getEpsilon();
					}
				}
			}
		}
		return false;
	}

	private  Map<String, Set<String>> possibleNewViolations(XTrace trace) {
		Map<String, Set<String>> knownViolations = this.resultHistory.getViolatingResources();
		Map<String, Set<String>> result = new HashMap<>();
		for (XEvent event : trace) {
			String activity = event.getAttributes().get("concept:name").toString();
			if (event.getAttributes().get("org:resource") != null) {
				String resource = event.getAttributes().get("org:resource").toString();
				if (!knownViolations.containsKey(activity) || !knownViolations.get(activity).contains(resource)) {
					if (!result.containsKey(activity)) {
						result.put(activity, new HashSet<String>());
					}
					result.get(activity).add(resource);
				}
			}
		}
		return result;
	}
	
	private double computeDistanceWorstCase(Map<String, Set<String>> oldResults, Map<String, Set<String>> possibleNewViolations) {
		double sum = 0.0;
		for (String activity : possibleNewViolations.keySet()) {
			int possibleNew = possibleNewViolations.get(activity).size();
			if (possibleNew > 0) {
				int oldCount = 0;
				if (oldResults.containsKey(activity)) {
					oldCount = oldResults.get(activity).size();
				}
				sum += (double) possibleNew / (possibleNew + oldCount);
			}
		}
		return sum / this.activities.size();
	}
	
	private double computeDistancePossibleWorld(Map<String, Set<String>> oldResults, Map<String, Set<String>> possibleNewViolations, 
			Map<String, Set<String>> unauthorizedResources, Multiset<String> possibleWorld) {
		double sum = 0.0;
		for (String activity : possibleWorld.elementSet()) {
			if (possibleNewViolations.containsKey(activity)) {
				Set<String> newResources = new HashSet<String>(possibleNewViolations.get(activity));
				int possibleNew = Math.max(newResources.size(), possibleWorld.count(activity));
				if (possibleNew > 0) {
					int oldCount = 0;
					if (oldResults.containsKey(activity)) {
						oldCount = oldResults.get(activity).size();
					}
					sum += (double) (possibleNew - oldCount) / possibleNew;
				}
			}
		}
		return sum / this.activities.size();
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
		TraceReplayResult reference = resultHistory.get(traceString);
		TraceReplayResult result = new TraceReplayResult(this.resultHistory.convertToString(trace),trace,trace.size(),false,false,false,0.0,0.0,null);
		List<StepTypes> stepTypes = reference.getStepTypes();
		result.setAsynchMoves(reference.getAsynchMoves());
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

	public TraceReplayResult replayTraceOnNet(XTrace trace, Object[] additionalInformation) {
		PetrinetGraph net=(PetrinetGraph) additionalInformation[0];
		XAttributeMap logAttributes=(XAttributeMap) additionalInformation[1];
		TransEvClassMapping mapping =(TransEvClassMapping) additionalInformation[2];
		XLog log=new XLogImpl(logAttributes);
		log.add(trace);
		Multiset<String> asynchronousMoveBag=TreeMultiset.create();
		
		int nThreads = 2;
		int costUpperBound = Integer.MAX_VALUE;
		
		XEventClassifier eventClassifier=XLogInfoImpl.STANDARD_CLASSIFIER;
		XLogInfo summary = XLogInfoFactory.createLogInfo(log, eventClassifier);
		XEventClasses classes = summary.getEventClasses();
		
		Marking initialMarking = getInitialMarking(net);
		Marking finalMarking = getFinalMarking(net);
		
		ReplayerParameters parameters = new ReplayerParameters.Default(nThreads, costUpperBound, Debug.NONE);
		Replayer replayer = new Replayer(parameters, (Petrinet) net, initialMarking, finalMarking, classes, mapping, false);

		
		PNRepResult pnresult=null;
		try {
			pnresult = replayer.computePNRepResult(Progress.INVISIBLE, log);
			
		} catch (InterruptedException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (ExecutionException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		for(SyncReplayResult replayResult : pnresult) {
			for (int j=0;j<replayResult.getStepTypes().size();j++) {
				if(replayResult.getStepTypes().get(j).toString().equals("Log move") || replayResult.getStepTypes().get(j).toString().equals("Model move")) {
					//System.out.println(replayResult.getNodeInstance().get(j).toString());
					if(replayResult.getStepTypes().get(j).toString().equals("Model move")) {
						if(mapping.containsKey(replayResult.getNodeInstance().get(j))) {
							asynchronousMoveBag.add(mapping.get(replayResult.getNodeInstance().get(j)).toString());
						}
						else {
							asynchronousMoveBag.add((replayResult.getNodeInstance().get(j)).toString());
						}
					}
					if(replayResult.getStepTypes().get(j).toString().equals("Log move")) {
						asynchronousMoveBag.add((replayResult.getNodeInstance().get(j)).toString());
					}
				}
			}
		}
		TraceReplayResult traceResult = new TraceReplayResult(this.resultHistory.convertToString(trace),trace, trace.size(), false, false, false,-1, -1, asynchronousMoveBag);
		List<StepTypes> stepTypes = pnresult.iterator().next().getStepTypes();
		traceResult.setStepTypes(stepTypes);
		traceResult.setViolatingResources(determineViolatingResources(trace, stepTypes));
		return traceResult; 
	}

	
}
