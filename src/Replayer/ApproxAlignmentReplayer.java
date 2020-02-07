package Replayer;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import org.deckfour.xes.classification.XEventClasses;
import org.deckfour.xes.classification.XEventClassifier;
import org.deckfour.xes.info.XLogInfo;
import org.deckfour.xes.info.XLogInfoFactory;
import org.deckfour.xes.info.impl.XLogInfoImpl;
import org.deckfour.xes.model.XAttributeMap;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.deckfour.xes.model.impl.XLogImpl;
import org.processmining.contexts.uitopia.UIPluginContext;
import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.models.graphbased.directed.petrinet.PetrinetGraph;
import org.processmining.models.graphbased.directed.petrinet.elements.Place;
import org.processmining.models.semantics.petrinet.Marking;
import org.processmining.plugins.connectionfactories.logpetrinet.TransEvClassMapping;
import org.processmining.plugins.petrinet.replayresult.PNRepResult;
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
/**
 * 
 * @author Martin Bauer
 *
 */
public class ApproxAlignmentReplayer implements IncrementalReplayer {

	ReplayResultsContainer traceAlignmentHistory;
	Replayer replayer;
	IccParameters iccParameters;
	boolean initialized;
	
	public ApproxAlignmentReplayer(IccParameters iccParameters) {
		this.iccParameters = iccParameters;
		this.traceAlignmentHistory=new ReplayResultsContainer();
		this.initialized=false;
	}
	
	public boolean TraceVariantKnown(XTrace trace) {
		if(this.traceAlignmentHistory.contains(this.traceAlignmentHistory.convertToString(trace))) {
			return true;
		}
		return false;
	}
	
	
	
	public boolean abstractAndCheckPredicate(XTrace trace, Object[] additionalInformation) {
		//TODO work on XEvents instead of string labels
		//initial trace is replayed
		if(!initialized) {
			initialized=true;
			TraceReplayResult result=replayTraceOnNet(trace, additionalInformation);
			this.traceAlignmentHistory.put(result.getActivities(), result);
			return true;
		}
		
		else {
			//for each consec approximation scheme is used
			TraceEditDistance ted = new TraceEditDistance("","");
			
			double minimalDistance=-1.0;
			TraceReplayResult closestTraceReplayResult=null;
			for (TraceReplayResult alignmentInfo : traceAlignmentHistory.values()) {
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
		    if (finalSetSize>trace.size() || minimalDistance>Math.ceil(trace.size()*this.iccParameters.getK())) {
		    	TraceReplayResult result=replayTraceOnNet(trace, additionalInformation);
				Multiset<String> oldAsynchMoves = traceAlignmentHistory.getAsynchMoves();
				traceAlignmentHistory.put(result.getActivities(), result);
				Multiset<String> newAsynchMoves = traceAlignmentHistory.getAsynchMoves();
				double difference=0.0;
				for(String activity : newAsynchMoves.elementSet()) {
					double relativeFrequencyInNew = (double)newAsynchMoves.count(activity)/(double)newAsynchMoves.size();
					double relativeFrequencyInOld;
					if (!oldAsynchMoves.contains(activity)) {
						relativeFrequencyInOld=0.0;
					}
					else relativeFrequencyInOld = (double)oldAsynchMoves.count(activity)/(double)oldAsynchMoves.size();
					//double dif=Math.abs(relativeFrequencyInOld-relativeFrequencyInNew);

					double dif=Math.pow(relativeFrequencyInOld-relativeFrequencyInNew,2);
					
					difference+=dif;
				}
				difference=Math.sqrt(difference);				
				//System.out.println(difference);
				////difference=difference/newAsynchMoves.elementSet().size();
				if(difference>this.iccParameters.getEpsilon()) {
					return true;
				}
				else
					return false;
		    }

		    Multiset<String> candidates=TreeMultiset.create();
			if(this.iccParameters.getApproximationMode().equals(IccParameters.PREFIXSUFFIX)) {
			    //build candidate sets for asynchronous moves based on TED and suffix-/affix equality
				//TODO work on XEvents directly instead of activity labels
				//prefix check
				String referenceActivities=this.traceAlignmentHistory.convertToString(closestTraceReplayResult.getTrace());
				String currentActivities=this.traceAlignmentHistory.convertToString(trace);
			    String prefix = "";
			    int minLength = Math.min(referenceActivities.length(), currentActivities.length());
			    for (int i = 0; i < minLength; i++) {
			        if (referenceActivities.charAt(i) != currentActivities.charAt(i)) {
			            prefix = referenceActivities.substring(0, i);
			            break;
			        }
			    }
			    
			    //suffix check
			    referenceActivities=new StringBuilder(referenceActivities.substring(Math.max(prefix.length(),0))).reverse().toString();
			    currentActivities=new StringBuilder(currentActivities.substring(Math.max(prefix.length(),0))).reverse().toString();
			    prefix = "";
			    minLength = Math.min(referenceActivities.length(), currentActivities.length());
			    for (int i = 0; i < minLength; i++) {
			        if (referenceActivities.charAt(i) != currentActivities.charAt(i)) {
			            prefix = referenceActivities.substring(0, i);
			            break;
			        }
			    }
			    //TODO work on xevent instances
			    //hack that keeps lifecycle information in candidate set
			    prefix=prefix.substring(0, Math.max(0,prefix.length()-9));
			    referenceActivities=new StringBuilder(referenceActivities.substring(Math.max(prefix.length(),0))).reverse().toString();
			    currentActivities=new StringBuilder(currentActivities.substring(Math.max(prefix.length(),0))).reverse().toString();
			    
			    //build the final sets
	
			    Multiset<String> referenceAsynch = TreeMultiset.create(Arrays.asList(referenceActivities.split(">")));
			    Multiset<String> currentAsynch= TreeMultiset.create(Arrays.asList(currentActivities.split(">")));
			    
			    candidates.addAll(referenceAsynch);
			    candidates.addAll(currentAsynch);
			}
		    
		    //uncomment next two lines of only exclusive events to each trace should be considered
		    //referenceAsynch.retainAll(currentAsynch);
		    //candidates.removeAll(referenceAsynch);
		   
		   /* for(String candidate : referenceAsynch) {
		    	candidates.add(candidate);
		    }
		    for(String candidate : currentAsynch) {
		    	if(candidates.contains(candidate)) {
		    		if(! toRemove.contains(candidate)) {
		    			toRemove.add(candidate);
		    		}
		    	}
		    	
		    }
		    candidates.removeAll(toRemove);*/
		    
		    /*
		    for(String candidate : referenceAsynch)
		    
		    for(String candidate : currentAsynch) {
		    	boolean inBoth=false;
		    	for(String compare : referenceAsynch) {
		    		if(compare.equals(candidate)){
		    			inBoth=true;
		    		}
		    	}
		    	if(inBoth==false) {
			    	candidates.add(candidate);
		    	}
		    }
		    for(String candidate : referenceAsynch) {
		    	boolean inBoth=false;
		    	for(String compare : currentAsynch) {
		    		if(compare.equals(candidate)){
		    			inBoth=true;
		    		}
		    	}
		    	if(inBoth==false) {
			    	candidates.add(candidate);
		    	}
		    }*/

		    
		    //get approximated size of async move set, if too large stop approximation and use replayer instead
		    if(this.iccParameters.getApproximationMode().equals(IccParameters.NONALIGNING)) {
		    	candidates=ted.getNonAligningActivities();
		    }
		    
		    //else get all possible worlds of candidate sets
		    Multiset<String> singleElementSet=TreeMultiset.create();
		    Set<Multiset<String>> finalCandidateSet=new HashSet<Multiset<String>>();
		    
		    //List<String> candidateList = new ArrayList<String>();
		    //candidates.iterator().forEachRemaining(candidateList::add);
		    
		    for (String candidate : candidates) {
		    	Multiset<String> currSet=TreeMultiset.create();
		    	currSet.add(candidate);
		    	singleElementSet.add(candidate);
			    finalCandidateSet.add(currSet);
		    }
		    for(int i=1;i<minimalDistance;i++) {
			    Set<Multiset<String>> temp=new HashSet<Multiset<String>>();
			    temp.addAll(finalCandidateSet);
			    finalCandidateSet=new HashSet<Multiset<String>>();
			    for(Multiset<String> cur: temp) {
			    	for(String curSingle : singleElementSet) {
			    		Multiset<String> newForFinal=TreeMultiset.create();
			    		newForFinal.addAll(cur);
			    		//if(!newForFinal.contains(curSingle)) {
			    			newForFinal.add(curSingle);
			    			finalCandidateSet.add(newForFinal);
			    		//}
			    	}
			    }
		    }
		    //for each world consisting of differing activities and original deviations check if a large enough deviation could occur in it
	    	Multiset<String> orig = TreeMultiset.create();
	    	orig.addAll(traceAlignmentHistory.getAsynchMoves());
	    	for(Multiset<String> world: finalCandidateSet) {
				Multiset<String> newA = TreeMultiset.create();
				newA.addAll(traceAlignmentHistory.getAsynchMoves());
				newA.addAll(world);
				newA.addAll(closestTraceReplayResult.getAsynchMoves());
				
				double difference=0.0;
				for(String activity : newA.elementSet()) {
					double relativeFrequencyInNew = (double)newA.count(activity)/(double)newA.size();
					double relativeFrequencyInOld;
					if (!orig.contains(activity)) {
						relativeFrequencyInOld=0.0;
						//TraceReplayResult result=replayTraceOnNet(trace, additionalInformation);
						//traceAlignmentHistory.put(result.getActivities(), result);
						//return true;						
					}
					else relativeFrequencyInOld = (double)orig.count(activity)/(double) orig.size();
					//double dif=Math.abs(relativeFrequencyInOld-relativeFrequencyInNew);
					double dif=Math.pow(relativeFrequencyInOld-relativeFrequencyInNew,2);
					
					difference+=dif;
				}
				difference=Math.sqrt(difference);
				//System.out.println(difference);
				//if significant change replay and return true, else keep going
				////difference=difference/newA.elementSet().size();

				if(difference>this.iccParameters.getEpsilon()) {
					TraceReplayResult result=replayTraceOnNet(trace, additionalInformation);
					traceAlignmentHistory.put(result.getActivities(), result);
					return true;
				}
		    }
		    //add insignificant events
		    TraceReplayResult result = new TraceReplayResult(this.traceAlignmentHistory.convertToString(trace),trace,0, false, false, true, -1, -1,finalCandidateSet.iterator().next());
			traceAlignmentHistory.put(result.getActivities(), result);
		    return false;
		}
	}
	
	
	
	public boolean incrementAndCheckPredicate(XTrace trace) {
		Multiset<String> oldAsynchMoves = traceAlignmentHistory.getAsynchMoves();
		traceAlignmentHistory.incrementMultiplicity(this.traceAlignmentHistory.convertToString(trace));
		Multiset<String> newAsynchMoves = traceAlignmentHistory.getAsynchMoves();
		
		double difference=0.0;
		for(String activity : newAsynchMoves.elementSet()) {
			double relativeFrequencyInNew = (double)newAsynchMoves.count(activity)/(double)newAsynchMoves.size();
			double relativeFrequencyInOld;
			if (!oldAsynchMoves.contains(activity)) {
				relativeFrequencyInOld=0.0;
			}
			else relativeFrequencyInOld = (double)oldAsynchMoves.count(activity)/(double)oldAsynchMoves.size();

			//double dif=Math.abs(relativeFrequencyInOld-relativeFrequencyInNew);
			double dif=Math.pow(relativeFrequencyInOld-relativeFrequencyInNew,2);
			
			difference+=dif;
		}
		difference=Math.sqrt(difference);		
		
		//difference=difference/newAsynchMoves.size();
		////difference=difference/newAsynchMoves.elementSet().size();
		if(difference>this.iccParameters.getEpsilon()) {
			return true;
		}
		else return false;
	}

	
	
	public ReplayResultsContainer getResult() {
		return traceAlignmentHistory;
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

	public void init(UIPluginContext context, PetrinetGraph net, XLog log) {
		// TODO Auto-generated method stub
		
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
		return new TraceReplayResult(this.traceAlignmentHistory.convertToString(trace),trace, trace.size(), false, false, false,-1, -1, asynchronousMoveBag);
	}

}
