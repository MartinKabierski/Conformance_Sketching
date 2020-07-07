package conformance.traceAnalysis.deviation;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import conformance.traceAnalysis.IncrementalConformanceCalculator;
import ressources.GlobalConformanceResult;

public class DeviationConformanceCalculatorTEST implements IncrementalConformanceCalculator<Map<String, Double>>{
	protected double cnt;
	protected ConcurrentMap<String,Double> deviationsAbsolute;
	protected ConcurrentMap<String,Double> deviationsRelative;
	
	public DeviationConformanceCalculatorTEST() {
		this.cnt=0;
		this.deviationsAbsolute = new ConcurrentHashMap<String, Double>();
		this.deviationsRelative = new ConcurrentHashMap<String, Double>();
	}
	
	public double update(Map<String, Double> conformanceResult) {
		//copy old original
		Double localCnt = new Double(0);
		Map<String, Double> localDeviationsAbsolute = new HashMap<String, Double>();
		Map<String, Double> localDeviationsRelative = new HashMap<String, Double>();

		updateAndGetLocalCopy(localCnt, localDeviationsAbsolute, localDeviationsRelative, conformanceResult);
		
		Map<String, Double> oldDeviations = new HashMap<String, Double>();
		for (Entry<String, Double> activity : localDeviationsRelative.entrySet())
			oldDeviations.put(activity.getKey(), activity.getValue());
		
		return calculateDistance(oldDeviations, localDeviationsRelative);
	}
	
	private synchronized void updateAndGetLocalCopy(Double localCnt, Map<String, Double> localDeviationsAbsolute, Map<String, Double> localDeviationsRelative,  Map<String, Double> conformanceResult) {
		//add changes to absolute frequencies
		for(Entry<String, Double> activity : conformanceResult.entrySet()) {
			this.cnt=this.cnt+activity.getValue();
			this.deviationsAbsolute.put(activity.getKey(), activity.getValue()+this.deviationsAbsolute.getOrDefault(activity.getKey(),0.0));
		}
		//update relative frequencies
		for (Map.Entry<String, Double> deviatingActivity : deviationsAbsolute.entrySet()) {
			this.deviationsRelative.put(deviatingActivity.getKey(), deviatingActivity.getValue()/this.cnt);
			localDeviationsRelative.put(deviatingActivity.getKey(), deviatingActivity.getValue()/this.cnt);
			localDeviationsAbsolute.put(deviatingActivity.getKey(),deviatingActivity.getValue());
			localCnt=this.cnt;
		}
	}

	public double quantifyChange(Map<String,Double> traceConformanceInformation) {
		Double localCnt = new Double(0);
		Map<String, Double> localDeviationsAbsolute = new HashMap<String, Double>();
		Map<String, Double> localDeviationsRelative = new HashMap<String, Double>();

		updateAndGetLocalCopy(localCnt, localDeviationsAbsolute, localDeviationsRelative, new HashMap<String,Double>());
		
		for(Entry<String,Double> entry: traceConformanceInformation.entrySet()) {
			localDeviationsAbsolute.put(entry.getKey(), entry.getValue()+localDeviationsAbsolute.getOrDefault(entry.getKey(), 0.0));
			localCnt=localCnt+entry.getValue();
		}
		Map<String, Double  > newDeviationsRelative = new HashMap<String, Double >();
		for(Entry<String,Double> entry: localDeviationsAbsolute.entrySet()) {
			newDeviationsRelative.put(entry.getKey(), entry.getValue()/localCnt);
		}
		return calculateDistance(localDeviationsRelative, newDeviationsRelative);
	}
	
	private double calculateDistance(Map<String,Double> oldDeviations, Map<String,Double> newDeviations) {
		double difference=0.0;
		for(Entry<String, Double> activity : newDeviations.entrySet()) {
			double relativeFrequencyInNew = activity.getValue();
			double relativeFrequencyInOld = oldDeviations.getOrDefault(activity.getKey(),0.0);
			difference+=Math.pow(relativeFrequencyInOld-relativeFrequencyInNew,2);
		}
		return Math.sqrt(difference);
	}
	
	protected double getCnt() {
		return this.cnt;
	}
	
	
	public GlobalConformanceResult get() {
		GlobalConformanceResult result = new GlobalConformanceResult();
		result.setDeviations(this.deviationsRelative);
		return result;
	}
}
