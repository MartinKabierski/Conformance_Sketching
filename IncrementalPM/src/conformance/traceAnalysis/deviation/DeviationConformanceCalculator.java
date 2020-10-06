package conformance.traceAnalysis.deviation;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang3.mutable.MutableDouble;

import conformance.traceAnalysis.IncrementalConformanceCalculator;
import ressources.GlobalConformanceResult;

public class DeviationConformanceCalculator implements IncrementalConformanceCalculator<Map<String, Double>>{
	protected double cnt;
	protected Map<String,Double> deviationsAbsolute;
	protected double noTraces;
	protected Map<String, Double> occurenceInTraces;
	
	public DeviationConformanceCalculator() {
		this.cnt=0;
		this.deviationsAbsolute = new ConcurrentHashMap<String, Double>();
		this.occurenceInTraces = new ConcurrentHashMap<String, Double>();
	}
	
	public double update(Map<String, Double> conformanceResult) {
		//update internal state and get local copy
		//System.out.println("Updating using:" +conformanceResult);
		Map<String, Double> localDeviationsAbsolute= new HashMap<String, Double>();
		//Map<String, Double> localDeviationsRelative= new HashMap<String, Double>();
		MutableDouble localCnt=new MutableDouble(0);
		MutableDouble conformanceResultSize = new MutableDouble(0);
		updateAndGetState(localDeviationsAbsolute, localCnt, conformanceResult, conformanceResultSize);
		
		//pruning for faster distance calculation
		/*
		double totalNew =conformanceResult.values().stream().reduce(0.0, Double::sum);
		if (conformanceResult.size()<=0) return 0.0;
		Double lowest = localDeviationsAbsolute.values().stream().min(Double::compare).get();
		Double denominator = localCnt.getValue();
		double x = 0.0;
		for (int i=0; i<totalNew; i++) {
			double worstCase = Math.pow((lowest/denominator-(lowest+1/denominator+1))*2,2);
			x += worstCase;
			double y = x + worstCase*(totalNew-i-1);
			if(Math.sqrt(y)<epsilon)return 0.0;
			if (Math.sqrt(x)>epsilon) break;
			lowest++;
			denominator++;
		}
		*/
		
		//derive old state from local copy of current state
		Map<String, Double> newDeviationsRelative = new HashMap<String, Double>();
		Map<String, Double> oldDeviationsRelative = new HashMap<String, Double>();
		//System.out.println(localCnt.doubleValue()+" "+localCnt);
		for (Entry<String, Double> activity : localDeviationsAbsolute.entrySet()) {
			newDeviationsRelative.put(activity.getKey(), (activity.getValue())/(localCnt.getValue()));
			oldDeviationsRelative.put(activity.getKey(), (activity.getValue()-conformanceResult.getOrDefault(activity.getKey(), 0.0))/(localCnt.getValue()-conformanceResultSize.getValue()));
		}
		//System.out.println(newDeviationsRelative);
		//System.out.println(oldDeviationsRelative);
		//System.out.println("Updated total Deviations:" + this.deviationsAbsolute);
		//System.out.println();
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

	private synchronized void updateAndGetState(Map<String, Double> localDeviationsAbsolute, MutableDouble localCnt, Map<String, Double> conformanceResult, MutableDouble conformanceSize) {
		//System.out.println("Updating internal state");
		noTraces++;
		//System.out.println("Incorporating Deviations: "+conformanceResult);
		for (Entry<String, Double> entry : conformanceResult.entrySet()) {
			this.cnt = this.cnt + entry.getValue();
			this.occurenceInTraces.put(entry.getKey(), this.occurenceInTraces.getOrDefault(entry.getKey(), 0.0)+1.0);

			conformanceSize.setValue(conformanceSize.getValue()+entry.getValue());
			this.deviationsAbsolute.put(entry.getKey(), entry.getValue()+this.deviationsAbsolute.getOrDefault(entry.getKey(), 0.0));
			localDeviationsAbsolute.put(entry.getKey(), entry.getValue()+this.deviationsAbsolute.getOrDefault(entry.getKey(), 0.0));
		}
		//for (Entry<String, Double> entry :this.deviationsAbsolute.entrySet()) {
		//	this.deviationsRelative.put(entry.getKey(), entry.getValue()/this.cnt);
		//	localDeviationsRelative.put(entry.getKey(), entry.getValue()/this.cnt);
		//}
		localCnt.setValue(this.cnt);
		//System.out.println(localCnt+" "+this.cnt);

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
			//System.out.println(relativeFrequencyInNew+" "+relativeFrequencyInOld);
			difference+=Math.pow(relativeFrequencyInOld-relativeFrequencyInNew,2);
			//difference+=Math.abs(relativeFrequencyInOld-relativeFrequencyInNew);
		}
		//System.out.println(difference);
		//return difference;
		//System.out.println(Math.sqrt(difference)>0.01?"HIT":"");
		//if(Math.sqrt(difference)>0.01)System.out.println(Math.sqrt(difference));
		return Math.sqrt(difference);
		//return 0.0;
	}
	
	protected double getCnt() {
		return this.cnt;
	}
	
	
	public synchronized GlobalConformanceResult get() {
		GlobalConformanceResult result = new GlobalConformanceResult();
		Map<String, Double> deviationsRelative = new HashMap<String, Double>();
		Map<String, Double> deviationsAbsolute = new HashMap<String, Double>();
		for (Entry<String, Double> entry : this.deviationsAbsolute.entrySet()) {
			deviationsAbsolute.put(entry.getKey(), entry.getValue());
			deviationsRelative.put(entry.getKey(), entry.getValue()/this.cnt);
		}
		result.setNoDeviations((int)this.cnt);
		result.setDeviationsRelative(deviationsRelative);
		result.setDeviationsAbsolute(deviationsAbsolute);
		Map<String, Double> occurenceInTraces = new HashMap<String, Double>();
		for (Entry<String, Double> entry : this.occurenceInTraces.entrySet())
			occurenceInTraces.put(entry.getKey(), entry.getValue());
		result.setOccurencePerTrace(occurenceInTraces);
		return result;
	}
}
