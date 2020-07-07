package qualitychecking;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.collections.list.SynchronizedList;
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
		values = Collections.synchronizedList(new ArrayList());
	}
	
	public NumericAttributeDistribution(String traceAttributeName, boolean incremental) {
		traceAttribute = true;
		this.attributeName = traceAttributeName;
		this.incremental = incremental;
		values = Collections.synchronizedList(new ArrayList());

	}
	
	public NumericAttributeDistribution(String eventClass, String eventAttributeName) {
		traceAttribute = false;
		this.attributeName = eventAttributeName;
		this.eventClass = eventClass;
		values = Collections.synchronizedList(new ArrayList());

	}
	
	public NumericAttributeDistribution(String eventClass, String eventAttributeName, boolean incremental) {
		traceAttribute = false;
		this.attributeName = eventAttributeName;
		this.eventClass = eventClass;
		this.incremental = incremental;
		values = Collections.synchronizedList(new ArrayList());

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
	
	
	public synchronized boolean addTraceAndCheckPredicate(XTrace trace, Double epsilon) {
		double oldValue = this.getAverageValue();
		addTrace(trace);
		double newValue = this.getAverageValue();
		return Math.abs(oldValue-newValue)>epsilon;
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
	

	public double computeExternalStatistic(AbstractValueDistribution validationDistribution) {
		double[] x = values.stream().mapToDouble(d -> d).toArray();
		NumericAttributeDistribution cast = (NumericAttributeDistribution) validationDistribution;
		double[] y = cast.values.stream().mapToDouble(d -> d).toArray();
		KolmogorovSmirnovTest ks = new KolmogorovSmirnovTest();
		return ks.kolmogorovSmirnovTest(x, y);
	}
	
	@Override
	public double computeTotalDistance(AbstractValueDistribution validationDistribution) {
		NumericAttributeDistribution cast = (NumericAttributeDistribution) validationDistribution;
		return Math.abs(cast.getAverageValue() - this.getAverageValue());
	}
	
	
	private void addValue(double value) {
		max = Math.max(this.max, value);
		sum += value;
		if (incremental) {
			numberOfValues++;
		} else {
			if (values == null) {
				values = new ArrayList<Double>();
			}
			this.values.add(value);
		}
	}
	
	public String toString() {
		if (traceAttribute) {
			return "numeric trace attr: " + attributeName + " avg: " + getAverageValue();
		}
		return "numeric eventClass: " + eventClass + " attr: " + attributeName + " avg: " + getAverageValue();
	}
	
	
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
