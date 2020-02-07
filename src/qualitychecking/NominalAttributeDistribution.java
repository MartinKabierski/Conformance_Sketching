package qualitychecking;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.math3.stat.inference.ChiSquareTest;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XTrace;

public class NominalAttributeDistribution implements AbstractValueDistribution {

	boolean traceAttribute;
	String eventClass;
	String attributeName;
	Map<String, Integer> valueCounts = new HashMap<>();;
	int total = 0; 
	
	public NominalAttributeDistribution(String traceAttributeName) {
		traceAttribute = true;
		this.attributeName = traceAttributeName;
	}
	
	public NominalAttributeDistribution(String eventClass, String eventAttributeName) {
		traceAttribute = false;
		this.attributeName = eventAttributeName;
		this.eventClass = eventClass;
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
	}
	
	@Override
	public double getRelativeFrequency(Object value) {
		return getRelativeFrequency((String) value);
	}
	
	private double getRelativeFrequency(String value) {
		if (!valueCounts.containsKey(value)) {
			return 0;
		}
		return (double) valueCounts.get(value) / total;
	}
	
	public double computeIncrementalDistance(AbstractValueDistribution oldDistribution) {
		double totalDiff = 0.0;
		for (String value : this.valueCounts.keySet()) {
				totalDiff += Math.pow(oldDistribution.getRelativeFrequency(value) - this.getRelativeFrequency(value), 2);  
		}	
		return Math.sqrt(totalDiff);
	}
	
	public double computeExternalStatistic(AbstractValueDistribution validationDistribution) {
//			chiSquareTestDataSetsComparison(long[] observed1, long[] observed2)
//		Returns the observed significance level, or p-value, associated with a Chi-Square two sample test comparing bin frequency 
//		counts in observed1 and observed2.
		Set<String> uniqueValues = new HashSet<>(this.valueCounts.keySet());
		NominalAttributeDistribution validationDistributionCast = (NominalAttributeDistribution) validationDistribution;
		uniqueValues.addAll(validationDistributionCast.valueCounts.keySet());
		
		long[] observed1 = new long[uniqueValues.size()];
		long[] observed2 = new long[uniqueValues.size()];
		
		int i = 0;
		for (String value : uniqueValues) {
			if (this.valueCounts.containsKey(value)) {
				observed1[i] = this.valueCounts.get(value);
			}
			if (validationDistributionCast.valueCounts.containsKey(value)) {
				observed2[i] = validationDistributionCast.valueCounts.get(value);
			}
			i++;
		}
		ChiSquareTest cst = new ChiSquareTest();
		return cst.chiSquareTestDataSetsComparison(observed1, observed2);
	}
	
	private void incrementCount(String value) {
		int count = 0;
		if (valueCounts.containsKey(value)) {
			count = valueCounts.get(value);
		}
		valueCounts.put(value, count + 1);
		total++;
	}
	
	public String toString() {
		if (traceAttribute) {
			return "trace attr: " + attributeName + valueCounts.toString();
		}
		return "eventClass: " + eventClass + " attr: " + attributeName + " values: " + valueCounts.toString();
	}
	
	@Override
	public NominalAttributeDistribution fullCopy() {
		NominalAttributeDistribution newDistrib;
		if (traceAttribute) {
			newDistrib = new NominalAttributeDistribution(this.attributeName);
		} else {
			newDistrib = new NominalAttributeDistribution(this.eventClass, this.attributeName);
		}
		newDistrib.valueCounts.putAll(this.valueCounts);
		newDistrib.total = this.total;
		return newDistrib;
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
