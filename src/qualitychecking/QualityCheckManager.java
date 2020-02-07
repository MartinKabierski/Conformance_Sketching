package qualitychecking;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.deckfour.xes.model.XTrace;

public class QualityCheckManager {

	private List<AbstractValueDistribution> valueDistributions;
	private List<AbstractValueDistribution> valueDistributionsOld;
	boolean directlyFollowsChecking;
	List<String> attributes;
	boolean isInternal;
	int times_triggered = 0;
	
	public QualityCheckManager(boolean isInternal) {
		this.valueDistributions = new ArrayList<>();
		this.valueDistributionsOld = new ArrayList<>();
		this.directlyFollowsChecking = false;
		this.attributes = new ArrayList<>();
		this.isInternal = isInternal;
	}
	
	public void addDirectlyFollowsChecking() {
		this.valueDistributions.add(new DirectlyFollowsDistribution());
		this.directlyFollowsChecking = true;
	}
	
	public void addNominalTraceAttribute(String attributeName) {
		this.valueDistributions.add(new NominalAttributeDistribution(attributeName));
		attributes.add(attributeName);
	}
	
	public void addNominalEventAttribute(String eventClass, String attributeName) {
		this.valueDistributions.add(new NominalAttributeDistribution(eventClass, attributeName));
		attributes.add(eventClass + "-"  + attributeName);
	}
	
	public void addNumericTraceAttribute(String attributeName) {
		this.valueDistributions.add(new NumericAttributeDistribution(attributeName, isInternal));
		attributes.add(attributeName);
	}
	
	public void addNumericEventAttribute(String eventClass, String attributeName) {
		this.valueDistributions.add(new NumericAttributeDistribution(eventClass, attributeName, isInternal));
		attributes.add(eventClass + "-"  + attributeName);
	}
	
	public void addLogSample(Collection<XTrace> sample) {
		for (XTrace trace : sample) {
			addTraceToDistributions(trace);
		}
	}
	
	public void addTraceToDistributions(XTrace trace) {
		for (AbstractValueDistribution distrib : valueDistributions) {
			distrib.addTrace(trace);
		}
	}
	
	
	public void addTraceToDistributionsAndStoreOld(XTrace trace) {
		storeAllCurrentDistributions();
		addTraceToDistributions(trace);
	}
	
	private void storeAllCurrentDistributions() {
		// stores copies of old current distributions before updating the new ones
		// TODO: probably there is a more efficient way to do this..
		valueDistributionsOld.clear();
		for (AbstractValueDistribution distrib : valueDistributions) {
			valueDistributionsOld.add(distrib.fullCopy());
		}
	}
	
	public double maxIncrementalDistributionDistance() {
		//TODO: change to a boolean check against epsilon
		double max = 0;
		for (int i = 0; i < valueDistributions.size(); i++) {
			AbstractValueDistribution currentDistrib = valueDistributions.get(i);
			AbstractValueDistribution oldDistrib = valueDistributionsOld.get(i);
			max = Math.max(max, currentDistrib.computeIncrementalDistance(oldDistrib));
		}
		return max;
	}
	
	public boolean hasSignificantDifference(Collection<XTrace> validationSample, double alpha) {
		for (AbstractValueDistribution currentDistrib : valueDistributions) {
			// compute value distributions of validation sample
			AbstractValueDistribution validationDistrib = currentDistrib.emptyCopy();
			for (XTrace trace : validationSample) {
				validationDistrib.addTrace(trace);
			}
			// compare distributions
			double significanceLevel = currentDistrib.computeExternalStatistic(validationDistrib);
			if (significanceLevel > alpha) {
				return true;
			}
		}
		return false;
	}

	public boolean checkDirectlyFollows() {
		return this.directlyFollowsChecking;
	}
	
	public String getCheckedAttributes() {
		return attributes.stream().map(Object::toString).collect(Collectors.joining(","));
	}

	public void wasUsed() {
		this.times_triggered++;
	}
	
	public int timesTriggered() {
		return this.times_triggered;
	}
	
	public void resetAll() {
		this.times_triggered = 0;
		List<AbstractValueDistribution> empty = new ArrayList<>();
		for (AbstractValueDistribution distrib : valueDistributions) {
			empty.add(distrib.emptyCopy());
		}
		this.valueDistributions = empty;
	}
	
	
}
