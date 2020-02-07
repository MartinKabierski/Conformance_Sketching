package qualitychecking;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.math3.stat.inference.ChiSquareTest;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XTrace;
import org.processmining.framework.util.Pair;

public class DirectlyFollowsDistribution implements AbstractValueDistribution {

	Map<Pair<String, String>, Integer> directlyFollowsCounts; 
	int total;
	
	public DirectlyFollowsDistribution() {
		directlyFollowsCounts = new HashMap<>();
		total = 0;
	}
	
	public void addTrace(XTrace trace) {
		for (int i = 0; i < trace.size() - 1; i++) {
			addEventClassPair(trace.get(i), trace.get(i + 1));
		}
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public double getRelativeFrequency(Object value) {
		return getRelativeFrequency((Pair<String, String>) value );
	}
	
	private double getRelativeFrequency(Pair<String, String> eventClassPair) {
		if (!directlyFollowsCounts.containsKey(eventClassPair)) {
			return 0;
		}
		return (double) directlyFollowsCounts.get(eventClassPair) / total;
	}
	
	public double computeIncrementalDistance(AbstractValueDistribution oldDistribution) {
		double totalDiff = 0.0;
		for (Pair<String, String> pair : this.directlyFollowsCounts.keySet()) {
				totalDiff += Math.pow(oldDistribution.getRelativeFrequency(pair) - this.getRelativeFrequency(pair), 2);  
		}	
		return Math.sqrt(totalDiff);
	}
	
	public double computeExternalStatistic(AbstractValueDistribution validationDistribution) {
//		chiSquareTestDataSetsComparison(long[] observed1, long[] observed2)
//	Returns the observed significance level, or p-value, associated with a Chi-Square two sample test comparing bin frequency 
//	counts in observed1 and observed2.
	Set<Pair<String, String>> uniqueValues = new HashSet<>(this.directlyFollowsCounts.keySet());
	DirectlyFollowsDistribution validationDistributionCast = (DirectlyFollowsDistribution) validationDistribution;
	uniqueValues.addAll(validationDistributionCast.directlyFollowsCounts.keySet());
	
	long[] observed1 = new long[uniqueValues.size()];
	long[] observed2 = new long[uniqueValues.size()];
	
	int i = 0;
	for (Pair<String, String> value : uniqueValues) {
		if (this.directlyFollowsCounts.containsKey(value)) {
			observed1[i] = this.directlyFollowsCounts.get(value);  
		}
		if (validationDistributionCast.directlyFollowsCounts.containsKey(value)) { 
			observed2[i] = validationDistributionCast.directlyFollowsCounts.get(value);
		}
		i++;
	}
	ChiSquareTest cst = new ChiSquareTest(); 
	return cst.chiSquareTestDataSetsComparison(observed1, observed2);
	}
	
	public Pair<String, String> getEventClassPair(XEvent event1, XEvent event2) {
		return new Pair<String, String>(getEventClass(event1), getEventClass(event2));
	}
	
	private void addEventClassPair(XEvent event1, XEvent event2) {
		Pair<String, String> pair = getEventClassPair(event1, event2);
		int count = 0;
		if (directlyFollowsCounts.containsKey(pair)) {
			count = directlyFollowsCounts.get(pair);
		}
		directlyFollowsCounts.put(pair, count + 1);
		total = total + 1;
	}
	

	public Set<Pair<String, String>> keySet() {
		return directlyFollowsCounts.keySet();
	}
	
	private String getEventClass(XEvent event) {
		return event.getAttributes().get("concept:name").toString()+"+"+event.getAttributes().get("lifecycle:transition").toString();
	}
	
	public int getTotal() {
		return total;
	}
	
	public String toString() {
		return directlyFollowsCounts.toString();
	}
	
	public DirectlyFollowsDistribution fullCopy() {
		DirectlyFollowsDistribution newDFD = new DirectlyFollowsDistribution();
		newDFD.directlyFollowsCounts.putAll(this.directlyFollowsCounts);
		newDFD.total = this.getTotal();
		return newDFD;
	}
	
	public DirectlyFollowsDistribution emptyCopy() {
		DirectlyFollowsDistribution newDFD = new DirectlyFollowsDistribution();
		return newDFD;
	}



	
	
	
}
