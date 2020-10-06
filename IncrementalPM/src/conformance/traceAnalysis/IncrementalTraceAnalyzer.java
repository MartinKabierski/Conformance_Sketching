package conformance.traceAnalysis;

import java.util.List;
import java.util.concurrent.ExecutionException;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.deckfour.xes.classification.XEventClassifier;
import org.deckfour.xes.model.XAttributeMap;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.deckfour.xes.model.impl.XLogImpl;
import org.processmining.models.graphbased.directed.petrinet.PetrinetGraph;
import org.processmining.models.graphbased.directed.petrinet.elements.Place;
import org.processmining.models.semantics.petrinet.Marking;
import org.processmining.plugins.connectionfactories.logpetrinet.TransEvClassMapping;
import org.processmining.plugins.petrinet.replayresult.PNRepResult;

import conformance.replay.ReplayerFactory;
import nl.tue.alignment.Progress;
import nl.tue.alignment.Replayer;
import ressources.GlobalConformanceResult;
import ressources.IccParameter;
import ressources.TraceAnalysisResult;
import ressources.TraceAnalysisResultMap;

/**
 * Template class for incremental conformance checking
 * @author Martin Bauer
 *
 */
public abstract class IncrementalTraceAnalyzer<T>{

	protected TraceAnalysisResultMap<T> results;
	protected IccParameter parameter;
	//protected Replayer replayer;
	protected IncrementalConformanceCalculator<T> conformanceCalculator;
	protected int cnt;
	protected int totalVariants;
	protected int approximatedVariants;
	protected int approximatedThenCalculated;
	protected Marking initialMarking;
	protected Marking finalMarking;
	
	protected PetrinetGraph net;
	protected XEventClassifier classifier;
	protected TransEvClassMapping mapping;
	
	
	public IncrementalTraceAnalyzer(IccParameter parameter, PetrinetGraph net, TransEvClassMapping mapping, XEventClassifier classifier) {
		//this.replayer=replayer;
		this.parameter=parameter;
		this.results=new TraceAnalysisResultMap<T>();
		this.cnt=0;
		this.totalVariants=0;
		this.approximatedVariants=0;
		this.approximatedThenCalculated=0;

		//helper functions for replayer creation
		initialMarking = getInitialMarking(net);
		finalMarking = getFinalMarking(net);
		
		this.net = net;
		this.classifier = classifier;
		this.mapping = mapping;
	}
	
	
	
	/**
	 * top level entry function
	 * @param trace
	 * @param net
	 * @param logAttributes
	 * @return whether trace induces significant change on global conformance
	 */
	public boolean analyzeTrace(XTrace trace, XAttributeMap logAttributes) {
		XLog traceAsLog=new XLogImpl(logAttributes);
		traceAsLog.add(trace);
		increaseThreadCnt();
		
		if(this.results.contains(trace)) {
			//System.out.println("Trace already known: "+TraceRepresentations.getActivitySequence(trace));
			T conformanceInformation = results.get(trace).getResult();
			//System.out.println("Using Information: "+conformanceInformation);
			//System.out.println();
			//T  = this.retrieveConformanceInformation(trace, replayResult);
			TraceAnalysisResult<T> result = new TraceAnalysisResult<T>(trace, false, conformanceInformation, results.get(trace).getReplayResult());
			double distance = this.updateConformance(result);
			//System.out.println(distance);
			return distance>this.parameter.getEpsilon();
		}
		this.totalVariants++;
		Replayer replayer = ReplayerFactory.createReplayer(this.net, traceAsLog, this.mapping, this.classifier, true, this.initialMarking, this.finalMarking);
		if(this.parameter.isApproximate())
			return approximateConformance(trace, net, logAttributes, replayer);
		else {
			PNRepResult replayResult = this.replayTraceOnLog(traceAsLog, logAttributes, replayer);
			T conformanceResult = this.retrieveConformanceInformation(trace, replayResult);
			TraceAnalysisResult<T> result = new TraceAnalysisResult<T>(trace, false, conformanceResult, replayResult);
			double distance = this.updateConformance(result);
			return distance>this.parameter.getEpsilon();		
		}
	}
	
	
	
	private synchronized void increaseThreadCnt() {
		this.cnt++;		
	}



	/**
	 * template algorithm for approximation-based incremental conformance checking
	 * @param trace
	 * @param net
	 * @param logAttributes
	 * @return
	 */
	protected boolean approximateConformance(XTrace trace, PetrinetGraph net, XAttributeMap logAttributes, Replayer replayer) {
		XLog traceAsLog=new XLogImpl(logAttributes);
		traceAsLog.add(trace);
		Pair<TraceAnalysisResult<T>, TraceEditDistance> referenceTraceInformation = mostSimilarTrace(trace);
		if(kSimilar(trace, referenceTraceInformation)) {
			List<T> potentialResults = determineConformanceApproximations(trace, referenceTraceInformation, logAttributes);
			for (T candidate : potentialResults) {
				if (this.conformanceCalculator.quantifyChange(candidate)>this.parameter.getEpsilon()) {
					this.approximatedThenCalculated++;
					PNRepResult replayResult = this.replayTraceOnLog(traceAsLog, logAttributes, replayer);
					T conformanceResult = this.retrieveConformanceInformation(trace, replayResult);
					//System.out.println("Approximation->Calculation: "+conformanceResult.toString());
					double distance = this.updateConformance(new TraceAnalysisResult<T>(trace, false, conformanceResult, replayResult));
					return distance>this.parameter.getEpsilon();
					//this.updateConformanceTEST(new TraceAnalysisResult<T>(trace, false, conformanceResult, replayResult));
					//return true;
				}
			}
			this.approximatedVariants++;
			//System.out.println("Approximation: "+potentialResults.get(0));
			this.updateConformanceTEST(new TraceAnalysisResult<T>(trace, true, potentialResults.get(0), referenceTraceInformation.getLeft().getReplayResult()));
			return false;
		}
		else {
			PNRepResult replayResult = this.replayTraceOnLog(traceAsLog, logAttributes, replayer);
			T conformanceResult = this.retrieveConformanceInformation(trace, replayResult);
			//System.out.println("Calculation: "+conformanceResult.toString());
			double distance = this.updateConformance(new TraceAnalysisResult<T>(trace, false, conformanceResult, replayResult));
			return distance>this.parameter.getEpsilon();
		}
	}
	
	
	
	/**
	 * update global conformance result
	 * @param traceAnalysisResult
	 * @return change in global conformance result after update
	 */
	protected double updateConformance(TraceAnalysisResult<T> traceAnalysisResult) {
		this.results.put(traceAnalysisResult);
		double distance = this.conformanceCalculator.update(traceAnalysisResult.getResult());
		return distance;
	}
	
	/**
	 * update global conformance result
	 * @param traceAnalysisResult
	 * @return change in global conformance result after update
	 */
	protected void updateConformanceTEST(TraceAnalysisResult<T> traceAnalysisResult) {
		this.results.put(traceAnalysisResult);
		this.conformanceCalculator.update(traceAnalysisResult.getResult());
	}	
	
	
	
	/**
	 * determine if traces are kSimilar
	 * @param trace
	 * @param referenceTraceInformation
	 * @return
	 */
	protected boolean kSimilar(XTrace trace, Pair<TraceAnalysisResult<T>, TraceEditDistance> referenceTraceInformation) {
		return referenceTraceInformation.getLeft()!=null &&
				referenceTraceInformation.getRight().getDistance()/(double)(trace.size()+referenceTraceInformation.getLeft().getTrace().size())<this.parameter.getK();
	}
	
	
	
	/**
	 * find reference trace, that is most similar to given trace (in terms of edit distance)
	 * @param trace
	 * @return closest trace
	 */
	private Pair<TraceAnalysisResult<T>, TraceEditDistance> mostSimilarTrace(XTrace trace) {
		TraceEditDistance minDist=new TraceEditDistance("","");
		minDist.distance=Integer.MAX_VALUE;
		TraceAnalysisResult<T> referenceTrace = null;
		for (TraceAnalysisResult<T> alignmentInfo : results.values()) {
			//TODO check if pruning is correct
			if (alignmentInfo.approximated||Math.abs((trace.size()-alignmentInfo.getTrace().size())/(trace.size()+alignmentInfo.getTrace().size()))>this.parameter.getK())
				continue;
			//get trace edit distance
			TraceEditDistance distance=new TraceEditDistance(trace,alignmentInfo.getTrace());
			if(distance.getDistance()<minDist.distance) {
				minDist=distance;
				referenceTrace = alignmentInfo;
			}
		}
		return new ImmutablePair<TraceAnalysisResult<T>, TraceEditDistance>(referenceTrace, minDist);
	}
	
	

	public GlobalConformanceResult getAnalysisResult() {
		GlobalConformanceResult result = this.conformanceCalculator.get();
		result.setCnt(this.cnt);
		result.setApproxCalc(approximatedThenCalculated);
		result.setTotalVariants(this.totalVariants);
		result.setApproximatedVariants(this.approximatedVariants);
		return result;
	}
	
	
	protected PNRepResult replayTraceOnLog(XLog log, XAttributeMap logAttributes, Replayer replayer){
		//XLog testlog=new XLogImpl(logAttributes);
		//testlog.add(trace);
		try {
			PNRepResult result = replayer.computePNRepResult(Progress.INVISIBLE, log);
			return result;
		} catch (InterruptedException | ExecutionException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	
	/**
	 * calculate proper conformance result using replayer
	 * @param replayResult
	 * @param trace 
	 * @return the calculated conformance result
	 */
	protected abstract T retrieveConformanceInformation(XTrace trace, PNRepResult replayResult);
	
	
	
	/**
	 * calculate a set of potential conformance approximations
	 * @param trace
	 * @param referenceTraceInformation
	 */
	protected abstract List<T> determineConformanceApproximations(XTrace trace, Pair<TraceAnalysisResult<T>, TraceEditDistance> referenceTraceInformation, XAttributeMap logAttributes);



	/**
	 * retrieve final marking from net
	 * @param net
	 * @return
	 */
	private static Marking getFinalMarking(PetrinetGraph net) {
		Marking finalMarking = new Marking();
	
		for (Place p : net.getPlaces()) {
			if (net.getOutEdges(p).isEmpty())
				finalMarking.add(p);
		}
	
		return finalMarking;
	}
	
	/**
	 * retrieve initial marking from net
	 * @param net
	 * @return
	 */
	private static Marking getInitialMarking(PetrinetGraph net) {
		Marking initMarking = new Marking();
	
		for (Place p : net.getPlaces()) {
			if (net.getInEdges(p).isEmpty())
				initMarking.add(p);
		}
	
		return initMarking;
	}
}
