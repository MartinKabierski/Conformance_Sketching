package qualitychecking;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.deckfour.xes.model.XTrace;
import org.processmining.framework.util.Pair;

public class QualityCheckManager {

	private List<AbstractValueDistribution> valueDistributions;
	List<String> attributes;
	boolean isInternal;
	int times_triggered = 0;
	double alpha;
	
	public QualityCheckManager(boolean isInternal) {
		this.valueDistributions = new ArrayList<>();
		this.attributes = new ArrayList<>();
		this.isInternal = isInternal;
		this.alpha = 0.05;
	}
	
	public QualityCheckManager(boolean isInternal, double alpha) {
		this.valueDistributions = new ArrayList<>();
		this.attributes = new ArrayList<>();
		this.isInternal = isInternal;
		this.alpha = alpha;
	}
	//TODO split into external and internal class
	
	
	public void addDirectlyFollowsChecking() {
		this.valueDistributions.add(new DirectlyFollowsDistribution());
	}
	
	public void addDependencyMeasureChecking() {
		this.valueDistributions.add(new DependencyMeasureDistribution());
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
	
	public boolean addTraceToDistributionsAndCheckPredicate(XTrace trace, double epsilon) {
		boolean significantChange=false;
		for (int i = 0; i < valueDistributions.size(); i++) {
			AbstractValueDistribution currentDistrib = valueDistributions.get(i);
			//Han: this has to remain separated in order to ensure that all distributions stay up to date
			boolean currentDistribSignificantChange = currentDistrib.addTraceAndCheckPredicate(trace, epsilon); 
			significantChange = significantChange || currentDistribSignificantChange; 
		}
		return significantChange;
	}
	
	
	public boolean hasSignificantDifference(Collection<XTrace> validationSample) {
		for (AbstractValueDistribution currentDistrib : valueDistributions) {
			// compute value distributions of validation sample
			AbstractValueDistribution validationDistrib = currentDistrib.emptyCopy();
			for (XTrace trace : validationSample) {
				validationDistrib.addTrace(trace);
			}
			// compare distributions
			double p = currentDistrib.computeExternalStatistic(validationDistrib);
			System.out.println(p+", "+(this.alpha)+","+(p<this.alpha?" BIASED":" NOT BIASED"));
			if (p < this.alpha) {
				return true;
			}
		}
		return false;
	}
	
	
	public String getCheckedAttributes() {
		return attributes.stream().map(Object::toString).collect(Collectors.joining(","));
	}
	
	public List<AbstractValueDistribution> getDistributions() {
		return valueDistributions;
	}

	public void wasUsed() {
		this.times_triggered++;
	}
	
	public int timesTriggered() {
		return this.times_triggered;
	}
	
	public double getAlpha() {
		return this.alpha;
	}
	
	public boolean hasDirectlyFollowsChecker() {
		for (AbstractValueDistribution currentDistrib : valueDistributions) {
			if (currentDistrib instanceof DirectlyFollowsDistribution) {
				return true;
			}
		}
		return false;
	}
	
	public boolean hasDependencyMeasureChecker() {
		for (AbstractValueDistribution currentDistrib : valueDistributions) {
			if (currentDistrib instanceof DependencyMeasureDistribution) {
				return true;
			}
		}
		return false;
	}
	
	public boolean hasAttributeChecker() {
		for (AbstractValueDistribution currentDistrib : valueDistributions) {
			if (currentDistrib instanceof NominalAttributeDistribution || currentDistrib instanceof NumericAttributeDistribution) {
				return true;
			}
		}
		return false;
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
