package conformance.traceAnalysis.deviation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;
import org.deckfour.xes.classification.XEventClassifier;
import org.deckfour.xes.model.XAttributeMap;
import org.deckfour.xes.model.XTrace;
import org.processmining.models.graphbased.directed.petrinet.PetrinetGraph;
import org.processmining.plugins.connectionfactories.logpetrinet.TransEvClassMapping;

import com.google.common.collect.Multiset;
import com.google.common.collect.TreeMultiset;

import conformance.traceAnalysis.TraceEditDistance;
import ressources.IccParameter;
import ressources.TraceAnalysisResult;
import utils.TraceRepresentations;
/**
 * 
 * @author Martin Bauer
 *
 */
public class ApproxDeviationAnalyzer_PREFIXSUFFIX extends DeviationAnalyzer {

	public ApproxDeviationAnalyzer_PREFIXSUFFIX(IccParameter parameter, TransEvClassMapping mapping, PetrinetGraph net, XEventClassifier classifier) {
		super(parameter, mapping, net, classifier);
	}

	protected boolean kSimilar(XTrace trace, Pair<TraceAnalysisResult<Map<String, Double>>, TraceEditDistance> referenceTraceInformation) {
		int cnt=0;
		if (referenceTraceInformation.getLeft() != null) {
			for (Entry<String, Double> activity : referenceTraceInformation.getLeft().getResult().entrySet()) {
				cnt+=activity.getValue();
			}
		}
		return referenceTraceInformation.getLeft() != null &&
				(referenceTraceInformation.getRight().getDistance()+cnt)<trace.size()&&
				//referenceTraceInformation.getRight().getDistance()/(trace.size()+referenceTraceInformation.getLeft().getTrace().size())<this.parameter.getK();
				referenceTraceInformation.getRight().getDistance()<trace.size()*this.parameter.getK();
	}
	
	

	protected List<Map<String, Double>> determineConformanceApproximations(XTrace trace,
			Pair<TraceAnalysisResult<Map<String, Double>>, TraceEditDistance> referenceTraceInformation,
			XAttributeMap logAttributes) {
		
		Multiset<String> candidates=TreeMultiset.create();
	    //build candidate sets for asynchronous moves based on TED and suffix-/affix equality
		//prefix check
		String referenceActivities=TraceRepresentations.getActivitySequence(referenceTraceInformation.getLeft().getTrace());
		String currentActivities=TraceRepresentations.getActivitySequence(trace);
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
	    //hack that keeps lifecycle information in candidate set
	    prefix=prefix.substring(0, Math.max(0,prefix.length()-9));
	    referenceActivities=new StringBuilder(referenceActivities.substring(Math.max(prefix.length(),0))).reverse().toString();
	    currentActivities=new StringBuilder(currentActivities.substring(Math.max(prefix.length(),0))).reverse().toString();
	    
	    //build the final sets
	    Multiset<String> referenceAsynch = TreeMultiset.create(Arrays.asList(referenceActivities.split(">>")));
	    Multiset<String> currentAsynch= TreeMultiset.create(Arrays.asList(currentActivities.split(">>")));
	    candidates.addAll(referenceAsynch);
	    candidates.addAll(currentAsynch);

	    Multiset<String> singleElementSet=TreeMultiset.create();
	    Set<Multiset<String>> finalCandidateSet=new HashSet<Multiset<String>>();

	    //TODO make it iterate over elements as often as they appear in set
    	for (String candidate : candidates) {
	    	Multiset<String> currSet=TreeMultiset.create();
	    	currSet.add(candidate);
	    	singleElementSet.add(candidate);
		    finalCandidateSet.add(currSet);
	    }
    	
	    for(int i=1;i<referenceTraceInformation.getRight().getDistance();i++) {
		    Set<Multiset<String>> temp=new HashSet<Multiset<String>>();
		    temp.addAll(finalCandidateSet);
		    finalCandidateSet=new HashSet<Multiset<String>>();
		    for(Multiset<String> cur: temp) {
		    	for(String curSingle : singleElementSet) {
		    		Multiset<String> newForFinal=TreeMultiset.create();
		    		newForFinal.addAll(cur);
	    			newForFinal.add(curSingle);
	    			finalCandidateSet.add(newForFinal);
		    	}
		    }
	    }
	    List<Map<String, Double>> approximations = new ArrayList<Map<String, Double>>();
	    //TODO revise this again
	    finalCandidateSet.stream()
	    .forEach(x->
	    	{Map<String, Double> current = new HashMap<String, Double>(); 
	    	x.entrySet().stream()
	    	.forEach(y->current.put(y.getElement(), current.getOrDefault(y.getElement(), 0.0)+y.getCount()));
	    	approximations.add(current);
	    	}
	    );
	    return approximations;
	}
}
	
			//only consider calculated values, disregard those that cannot be k-similar due to size differences
			//if(alignmentInfo.approximated || Math.abs((trace.size()-alignmentInfo.getTrace().size())/(trace.size()+alignmentInfo.getTrace().size()))>this.parameter.getK())

