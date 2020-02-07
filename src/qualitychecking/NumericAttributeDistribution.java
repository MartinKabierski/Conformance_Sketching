package qualitychecking;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.math3.stat.inference.KolmogorovSmirnovTest;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XTrace;
import org.deckfour.xes.model.impl.XAttributeContinuousImpl;

public class NumericAttributeDistribution implements AbstractValueDistribution {

	boolean traceAttribute;
	String eventClass;
	String attributeName;
	boolean incremental = false;
	List<Double> values;
	double sum = 0;
	double max = 0;
	int numberOfValues = 0;
	
	public NumericAttributeDistribution(String traceAttributeName) {
		traceAttribute = true;
		this.attributeName = traceAttributeName;
	}
	
	public NumericAttributeDistribution(String traceAttributeName, boolean incremental) {
		traceAttribute = true;
		this.attributeName = traceAttributeName;
		this.incremental = incremental;
	}
	
	public NumericAttributeDistribution(String eventClass, String eventAttributeName) {
		traceAttribute = false;
		this.attributeName = eventAttributeName;
		this.eventClass = eventClass;
	}
	
	public NumericAttributeDistribution(String eventClass, String eventAttributeName, boolean incremental) {
		traceAttribute = false;
		this.attributeName = eventAttributeName;
		this.eventClass = eventClass;
		this.incremental = incremental;
	}
	
	public void addTrace(XTrace trace) {
		if (traceAttribute) {
			Object attrValue = trace.getAttributes().get(this.attributeName);
			if (attrValue != null) {
				XAttributeContinuousImpl contAttr = (XAttributeContinuousImpl) attrValue;
				addValue(contAttr.getValue());
			}	
		} else {
			for (XEvent event : trace) {
				if (eventClass.equals(event.getAttributes().get("concept:name").toString())) {
					Object attrValue = event.getAttributes().get(this.attributeName); 
					if (attrValue != null) {
						XAttributeContinuousImpl contAttr = (XAttributeContinuousImpl) attrValue;
						addValue(contAttr.getValue());
					}	
				}
			}
		}
	}
	
	@Override
	public double getRelativeFrequency(Object value) {
		return getAverageValue();
	}
	
	protected double getAverageValue() {
		if (incremental && numberOfValues == 0) {
			return 0;
		}
		if (incremental) {
			return sum / numberOfValues;
		}
		if (values.size() == 0) {
			return 0;
		}
		return sum / values.size();
	}
	
	
	public double computeIncrementalDistance(AbstractValueDistribution oldDistribution) {
		NumericAttributeDistribution cast = (NumericAttributeDistribution) oldDistribution;
		double maxVal = Math.max(this.max, cast.max);
		return Math.abs(this.getAverageValue() / maxVal - cast.getAverageValue() / maxVal); 
	}
	
	

	
	public double computeExternalStatistic(AbstractValueDistribution validationDistribution) {
//		kolmogorovSmirnovTest(double[] x, double[] y)
//		Computes the p-value, or observed significance level, of a two-sample Kolmogorov-Smirnov test evaluating the null hypothesis
//		that x and y are samples drawn from the same probability distribution.
		double[] x = values.stream().mapToDouble(d -> d).toArray();
		NumericAttributeDistribution cast = (NumericAttributeDistribution) validationDistribution;
		double[] y = cast.values.stream().mapToDouble(d -> d).toArray();
		KolmogorovSmirnovTest ks = new KolmogorovSmirnovTest();
		return ks.kolmogorovSmirnovTest(x, y);
	}
	
	
	private void addValue(double value) {
		if (incremental) {
			sum = sum + value;
			max = Math.max(this.max, value);
			numberOfValues++;
		} else {
			if (values == null) {
				values = new ArrayList<Double>();
			}
			this.values.add(value);
			sum += value;
		}
	}
	
	public String toString() {
		if (traceAttribute) {
			return "numeric trace attr: " + attributeName + " avg: " + getAverageValue();
		}
		return "numeric eventClass: " + eventClass + " attr: " + attributeName + " avg: " + getAverageValue();
	}
	
	@Override
	public NumericAttributeDistribution fullCopy() {
		NumericAttributeDistribution newDistrib = emptyCopy();
		if (incremental) {
			newDistrib.numberOfValues = this.numberOfValues;
			newDistrib.max = this.max;
			newDistrib.sum = this.sum;
		} else {
			newDistrib.values = new ArrayList<Double>(this.values);
			newDistrib.sum = this.sum;
		}
		return newDistrib;
	}
	
	@Override
	public NumericAttributeDistribution emptyCopy() {
		NumericAttributeDistribution newDistrib;
		if (traceAttribute) {
			newDistrib = new NumericAttributeDistribution(this.attributeName);
		} else {
			newDistrib = new NumericAttributeDistribution(this.eventClass, this.attributeName);
		}
		newDistrib.incremental = this.incremental;
		return newDistrib;
	}

	
}
