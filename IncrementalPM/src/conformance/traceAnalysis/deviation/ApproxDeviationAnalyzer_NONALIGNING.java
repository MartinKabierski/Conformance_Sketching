package conformance.traceAnalysis.deviation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;
import org.deckfour.xes.classification.XEventClassifier;
import org.deckfour.xes.model.XAttributeMap;
import org.deckfour.xes.model.XTrace;
import org.processmining.models.graphbased.directed.petrinet.PetrinetGraph;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;
import org.processmining.plugins.connectionfactories.logpetrinet.TransEvClassMapping;

import com.google.common.collect.Multiset;
import com.google.common.collect.Multiset.Entry;
import com.google.common.collect.TreeMultiset;

import conformance.IncrementalConformanceChecker.Heuristics;
import conformance.traceAnalysis.TraceEditDistance;
import ressources.IccParameter;
import ressources.TraceAnalysisResult;
/**
 * 
 * @author Martin Bauer
 *
 */
public class ApproxDeviationAnalyzer_NONALIGNING extends DeviationAnalyzer {	
	
	Set<String> knownTrueDeviations;
	HashMap <String, String> eventActivityMapping;
	
	public ApproxDeviationAnalyzer_NONALIGNING(IccParameter parameter, TransEvClassMapping mapping, PetrinetGraph net, XEventClassifier classifier) {
		super(parameter, mapping, net, classifier);
		this.knownTrueDeviations = new HashSet<String>();
		this.eventActivityMapping = new HashMap<String, String>();

		//allows for conversion of string-based activities in constant time, 
		//TODO check if this can be done with mapping instead (then non-aligning activities need to be transitions somehow)
		//System.out.println(mapping.toString());
		for (Transition t : mapping.keySet()) {
			
			eventActivityMapping.put(t.toString(),mapping.get(t).toString());
		}
		//System.out.println(eventActivityMapping);
	}
	
	//TODO get rid of multisets
	protected List<Map<String, Double>> determineConformanceApproximations(XTrace trace,
			Pair<TraceAnalysisResult<Map<String, Double>>, TraceEditDistance> referenceTraceInformation,
			XAttributeMap logAttributes) {
		Multiset<String> nonAligningActivities=TreeMultiset.create();
		nonAligningActivities=referenceTraceInformation.getRight().getNonAligningActivities();
	    Map<String, Double> deviations = new HashMap<String, Double>();
	    for(Entry<String> entry : nonAligningActivities.entrySet()) {
	    	if(eventActivityMapping.containsKey(entry.getElement().toString())){
				deviations.put(eventActivityMapping.get(entry.getElement()).toString(),deviations.getOrDefault(eventActivityMapping.get(entry.getElement()).toString(), 0.0)+entry.getCount());
			}
			else {
				deviations.put(entry.getElement().toString(), deviations.getOrDefault(entry.getElement().toString(), 0.0)+entry.getCount());
			}
	    }
			
		if(this.parameter.getApproximationHeuristic()== Heuristics.NONALIGNING_KNOWN) {
			//System.out.println("Current Known: "+this.knownTrueDeviations);
			//System.out.println("Approximated Deviations: "+deviations.keySet());
			deviations.keySet().retainAll(this.knownTrueDeviations);
			//System.out.println("Approximated after retain: "+deviations.keySet()+"\n");
		}
	    Map<String, Double> referenceDeviations = referenceTraceInformation.getLeft().getResult();
	    referenceDeviations.entrySet().stream().forEach(x->deviations.put(x.getKey(), deviations.getOrDefault(x.getKey(), 0.0)+x.getValue()));
	    List<Map<String, Double>> approximations = new ArrayList<Map<String, Double>>();
	    approximations.add(deviations);
		//System.out.println("Approximated after adding reference: "+deviations.keySet()+"\n");

	    return approximations;
	}
	
	protected double updateConformance(TraceAnalysisResult<Map<String, Double>> traceAnalysisResult) {
		this.results.put(traceAnalysisResult);
		if(this.parameter.getApproximationHeuristic()== Heuristics.NONALIGNING_KNOWN && !traceAnalysisResult.approximated) {
			//int size = this.knownTrueDeviations.size();
			this.knownTrueDeviations.addAll(traceAnalysisResult.getResult().keySet());
			//int newSize = this.knownTrueDeviations.size();
			//if (size!=newSize)
				//System.out.println("Known Update: "+this.knownTrueDeviations+"\n");
			//System.out.println(this.knownTrueDeviations);
		}
		double distance = this.conformanceCalculator.update(traceAnalysisResult.getResult());
		//System.out.println(this.conformanceCalculator.get().toString());
		return distance;
	}
}
