package qualitychecking;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XTrace;
import org.processmining.framework.util.Pair;

public class DependencyMeasureDistribution implements AbstractValueDistribution {

	Map<Pair<String, String>, Integer> directlyFollowsCounts;
	Map<Pair<String, String>, Double> dependencyMeasures;
//	double total;
	
	public DependencyMeasureDistribution() {
		directlyFollowsCounts = new ConcurrentHashMap<>();
		dependencyMeasures = new ConcurrentHashMap<>();
//		total = 0.0;
	}
	
	public void addTrace(XTrace trace) {
		// update directly follows counts
		for (int i = 0; i < trace.size() - 1; i++) {
			addEventClassPair(trace.get(i), trace.get(i + 1));
		}
		//update dependency measures
		for (Pair<String,String> key : this.directlyFollowsCounts.keySet()) {
			this.dependencyMeasures.put(key, computeDependencyMeasure(key.getFirst(), key.getSecond()));
		}
	}


	
	@Override
	public synchronized boolean addTraceAndCheckPredicate(XTrace trace, Double epsilon) {
		Map<Pair<String, String>, Double> oldMeasures = new HashMap<>(dependencyMeasures);
		addTrace(trace);
		Map<Pair<String, String>, Double> newMeasures = dependencyMeasures;
		
		double totalDiff = 0.0;
		for(Pair<String, String> pair : newMeasures.keySet()) {
			double newValue = newMeasures.get(pair);
			double oldValue = 0.0;;
			if(oldMeasures.containsKey(pair)) {
				oldValue = oldMeasures.get(pair);
			}
			totalDiff += Math.pow(oldValue - newValue, 2) ;  
		}
		double val = Math.sqrt(totalDiff) / newMeasures.keySet().size();
		return val > epsilon;
	}	
	
	
	public double computeExternalStatistic(AbstractValueDistribution validationDistribution) {
		// WARNING: the dependency measure is not suitable for statistical checking
		return 0.0;
	}
	
	public double computeTotalDistance(AbstractValueDistribution validationDistribution) {
		DependencyMeasureDistribution validationDistributionCast = (DependencyMeasureDistribution) validationDistribution;
		Set<Pair<String, String>> allKeys = new HashSet<>(dependencyMeasures.keySet());
		allKeys.addAll(validationDistributionCast.dependencyMeasures.keySet());
		double totalDiff = 0.0;
		for(Pair<String, String> key : allKeys) {
			double sampleValue = 0.0;
			double validationValue = 0.0;
			if (dependencyMeasures.containsKey(key)) {
				sampleValue = dependencyMeasures.get(key); 
			}
			if (validationDistributionCast.dependencyMeasures.containsKey(key)) {
				validationValue = validationDistributionCast.dependencyMeasures.get(key);
			}
			
			totalDiff += Math.pow(sampleValue - validationValue, 2) ;  
		}
		return totalDiff / allKeys.size();
	}
	

	
	private Pair<String, String> getEventClassPair(XEvent event1, XEvent event2) {
		return new Pair<String, String>(getEventClass(event1), getEventClass(event2));
	}
	
	private double computeDependencyMeasure(String activity1, String activity2) {
		if (activity1.equals(activity2)) {
			int count12 = getCount(activity1, activity2);
			return count12 * 1.0 / (count12 + 1);
		} 
		int count12 = getCount(activity1, activity2);
		int count21 = getCount(activity2, activity1);
		double val  =(count12 - count21) * 1.0 / (count12 + count21 + 1); 
		return val;
	}

	private int getCount(String activity1, String activity2) {
		Pair<String, String> pair = new Pair<String, String>(activity1, activity2);
		if (directlyFollowsCounts.containsKey(pair)) {
			return directlyFollowsCounts.get(pair);
		}	
		return 0;
	}
	
	private void addEventClassPair(XEvent event1, XEvent event2) {
		Pair<String, String> pair = getEventClassPair(event1, event2);
		directlyFollowsCounts.put(pair, directlyFollowsCounts.getOrDefault(pair,0) + 1);
	}
	
	private String getEventClass(XEvent event) {
		return event.getAttributes().get("concept:name").toString()+"+"+event.getAttributes().get("lifecycle:transition").toString();
	}
	
	public String toString() {
		return dependencyMeasures.toString();
	}
	
	
	public DependencyMeasureDistribution emptyCopy() {
		DependencyMeasureDistribution newDFD = new DependencyMeasureDistribution();
		return newDFD;
	}

	
}
