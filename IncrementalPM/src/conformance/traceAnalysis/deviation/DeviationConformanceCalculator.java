package conformance.traceAnalysis.deviation;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import conformance.traceAnalysis.IncrementalConformanceCalculator;
import ressources.GlobalConformanceResult;

public class DeviationConformanceCalculator implements IncrementalConformanceCalculator<Map<String, Double>>{
	protected double cnt;
	protected Map<String,Double> deviationsAbsolute;
	
	public DeviationConformanceCalculator() {
		this.cnt=0;
		this.deviationsAbsolute = new ConcurrentHashMap<String, Double>();
	}
	
	public double update(Map<String, Double> conformanceResult) {
		//update internal state and get local copy
		Map<String, Double> localDeviationsAbsolute= new HashMap<String, Double>();
		//Map<String, Double> localDeviationsRelative= new HashMap<String, Double>();
		Double localCnt=new Double(0);
		Double conformanceResultSize = new Double(0);
		updateAndGetState(localDeviationsAbsolute, localCnt, conformanceResult, conformanceResultSize);
		
		//derive old state from local copy of current state
		Map<String, Double> newDeviationsRelative = new HashMap<String, Double>();
		Map<String, Double> oldDeviationsRelative = new HashMap<String, Double>();
		for (Entry<String, Double> activity : localDeviationsAbsolute.entrySet()) {
			newDeviationsRelative.put(activity.getKey(), (activity.getValue())/(localCnt));
			oldDeviationsRelative.put(activity.getKey(), (activity.getValue()-conformanceResult.getOrDefault(activity.getKey(), 0.0))/(localCnt-conformanceResultSize));
		}
		//return change in states
		return calculateDistance(oldDeviationsRelative, newDeviationsRelative);
	}

	/*public void updateTest(Map<String, Double> conformanceResult) {		//copy old original
		//get local copy of everything
		Map<String, Double> localDeviationsAbsolute= new HashMap<String, Double>();
		Double localCnt=new Double(0);
		Double conformanceResultSize = new Double(0);
		updateAndGetState(localDeviationsAbsolute,localCnt, conformanceResult, conformanceResultSize);
	}*/

	private synchronized void updateAndGetState(Map<String, Double> localDeviationsAbsolute, Double localCnt, Map<String, Double> conformanceResult, Double conformanceSize) {
		//System.out.println("Updating internal state");
		for (Entry<String, Double> entry : conformanceResult.entrySet()) {
			this.cnt = this.cnt + entry.getValue();
			conformanceSize=conformanceSize+entry.getValue();
			this.deviationsAbsolute.put(entry.getKey(), entry.getValue()+this.deviationsAbsolute.getOrDefault(entry.getKey(), 0.0));
			localDeviationsAbsolute.put(entry.getKey(), entry.getValue()+this.deviationsAbsolute.getOrDefault(entry.getKey(), 0.0));
		}
		//for (Entry<String, Double> entry :this.deviationsAbsolute.entrySet()) {
		//	this.deviationsRelative.put(entry.getKey(), entry.getValue()/this.cnt);
		//	localDeviationsRelative.put(entry.getKey(), entry.getValue()/this.cnt);
		//}
		localCnt = this.cnt;
	}
	
	public double quantifyChange(Map<String,Double> traceConformanceInformation) {
		//iterate through internal state without synchronization, result may yield larger impact, than possible, thus not problematic
		Map<String, Double> oldDeviations = new HashMap<String, Double>();
		Map<String, Double> newDeviations = new HashMap<String, Double>();
		
		double conformanceSize =0;
		double localcnt = this.cnt;
		
		for (Entry <String, Double> activity : this.deviationsAbsolute.entrySet()) {
			oldDeviations.put(activity.getKey(), activity.getValue()/localcnt);
			newDeviations.put(activity.getKey(), activity.getValue());
		}
		for (Entry <String, Double> activity : traceConformanceInformation.entrySet()) {
			conformanceSize=conformanceSize+activity.getValue();
			newDeviations.put(activity.getKey(), activity.getValue()+newDeviations.getOrDefault(activity.getKey(),0.0));
		}
		for (Entry <String, Double> activity : newDeviations.entrySet()) {
			newDeviations.put(activity.getKey(), activity.getValue()/(localcnt+conformanceSize));
		}
		return calculateDistance(oldDeviations, newDeviations);
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
	
	
	public synchronized GlobalConformanceResult get() {
		GlobalConformanceResult result = new GlobalConformanceResult();
		Map<String, Double> deviations = new HashMap<String, Double>();
		for (Entry<String, Double> entry : this.deviationsAbsolute.entrySet()) {
			deviations.put(entry.getKey(), entry.getValue()/this.cnt);
		}
		result.setDeviations(deviations);
		return result;
	}
}
