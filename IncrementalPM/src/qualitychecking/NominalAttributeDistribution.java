package qualitychecking;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.math3.stat.inference.ChiSquareTest;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XTrace;
import org.processmining.framework.util.Pair;

public class NominalAttributeDistribution implements AbstractValueDistribution {

	boolean traceAttribute;
	String eventClass;
	String attributeName;
	Map<String, Integer> valueCountsAbsolute;
	Map<String, Double> valueCountsRelative;

	double total = 0.0; 
	
	public NominalAttributeDistribution(String traceAttributeName) {
		traceAttribute = true;
		this.attributeName = traceAttributeName;
		valueCountsAbsolute = new ConcurrentHashMap();
		valueCountsRelative = new ConcurrentHashMap();
	}
	
	public NominalAttributeDistribution(String eventClass, String eventAttributeName) {
		traceAttribute = false;
		this.attributeName = eventAttributeName;
		this.eventClass = eventClass;
		valueCountsAbsolute = new ConcurrentHashMap();
		valueCountsRelative = new ConcurrentHashMap();
	}
	
	public void addTrace(XTrace trace) {
		if (traceAttribute) {
			Object attrValue = trace.getAttributes().get(this.attributeName);
			if (attrValue != null) {
				incrementCount(attrValue.toString());
			}	
		} else {
			for (XEvent event : trace) {
				if (eventClass.equals(event.getAttributes().get("concept:name").toString())) {
					Object attrValue = event.getAttributes().get(this.attributeName);
					if (attrValue != null) {
						incrementCount(attrValue.toString());
					}	
				}
			}
		}
		//update relativeCounts
		for (String key : this.valueCountsAbsolute.keySet()) {
			valueCountsRelative.put(key, this.valueCountsAbsolute.get(key)/total);
		}
	}
	
	public synchronized boolean addTraceAndCheckPredicate(XTrace trace, Double epsilon) {
		Map<String, Double> oldValueCounts = new HashMap<>(this.valueCountsRelative);
		addTrace(trace);

		double totalDiff = 0.0;
		for (String key : this.valueCountsRelative.keySet()) {
				double newValue = this.valueCountsRelative.get(key);
				double oldValue;
				if(!oldValueCounts.containsKey(key))
					oldValue=0.0;
				else
					oldValue=oldValueCounts.get(key);
				totalDiff += Math.pow(oldValue - newValue, 2);  
		}	
		return Math.sqrt(totalDiff)>epsilon;
	}
	
	
	public double computeExternalStatistic(AbstractValueDistribution validationDistribution) {
		NominalAttributeDistribution validationDistributionCast = (NominalAttributeDistribution) validationDistribution;
		Set<String> allKeys = new HashSet<>(this.valueCountsAbsolute.keySet());
		allKeys.addAll(validationDistributionCast.valueCountsAbsolute.keySet());
		
		long[] observed1 = new long[allKeys.size()];
		long[] observed2 = new long[allKeys.size()];

		int i = 0;
		for (String key : allKeys) {
			if (this.valueCountsAbsolute.containsKey(key)) {
				observed1[i] = this.valueCountsAbsolute.get(key).longValue();
			}
			if (validationDistributionCast.valueCountsAbsolute.containsKey(key)) {
				observed2[i] = validationDistributionCast.valueCountsAbsolute.get(key).longValue();
			}
			i++;
		}
		ChiSquareTest cst = new ChiSquareTest();
		return cst.chiSquareTestDataSetsComparison(observed1, observed2);
	}
	
	
	@Override
	public double computeTotalDistance(AbstractValueDistribution validationDistribution) {
		NominalAttributeDistribution validationDistributionCast = (NominalAttributeDistribution) validationDistribution;
		Set<String> allKeys = new HashSet<>(this.valueCountsRelative.keySet());
		allKeys.addAll(validationDistributionCast.valueCountsRelative.keySet());
		
		double totalDiff = 0.0;
		for(String key : allKeys) {
			double sampleValue = 0.0;
			double validationValue = 0.0;
			if (valueCountsRelative.containsKey(key)) {
				sampleValue = valueCountsRelative.get(key); 
			}
			if (validationDistributionCast.valueCountsRelative.containsKey(key)) {
				validationValue = validationDistributionCast.valueCountsRelative.get(key);
			}
			
			totalDiff += Math.pow(sampleValue - validationValue, 2);  
		}
		return totalDiff;
	}
	
	private void incrementCount(String value) {
		int count = 0;
		if (valueCountsAbsolute.containsKey(value)) {
			count = valueCountsAbsolute.get(value);
		}
		valueCountsAbsolute.put(value, count + 1);
		total++;
	}
	
	public String toString() {
		if (traceAttribute) {
			return "trace attr: " + attributeName + valueCountsRelative.toString();
		}
		return "eventClass: " + eventClass + " attr: " + attributeName + " values: " + valueCountsRelative.toString();
	}
	
	
	@Override
	public NominalAttributeDistribution emptyCopy() {
		NominalAttributeDistribution newDistrib;
		if (traceAttribute) {
			newDistrib = new NominalAttributeDistribution(this.attributeName);
		} else {
			newDistrib = new NominalAttributeDistribution(this.eventClass, this.attributeName);
		}
		return newDistrib;
	}


}
