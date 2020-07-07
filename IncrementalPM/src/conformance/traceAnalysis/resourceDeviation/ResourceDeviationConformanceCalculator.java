package conformance.traceAnalysis.resourceDeviation;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import conformance.traceAnalysis.IncrementalConformanceCalculator;
import ressources.GlobalConformanceResult;

public class ResourceDeviationConformanceCalculator implements IncrementalConformanceCalculator<Map<String,Map<String,Double>>> {

	
	protected Map<String, Double> cnts;
	protected Map<String, Map<String, Double>> violatingResourcesAbsolute;
	//protected Map<String, Map<String, Double>> violatingResourcesRelative;
	
	public ResourceDeviationConformanceCalculator() {
		violatingResourcesAbsolute = new ConcurrentHashMap<String, Map<String, Double>>();
		//violatingResourcesRelative = new HashMap<String, Map<String, Double>>();
		cnts = new HashMap<String, Double>();
	}

/**
 * update the current state and return whether it yields significant change
 * update is synchronous, while significant change test is done locally on local copies of the state
 * local copies gained after updateAndGetLocalcopy are of absolute values, these are convertred to relative frequencies locally to reduce time in synchronicity
 */
	public double update(Map<String, Map<String, Double>> deviations) {
		Map<String, Map<String, Double>> newDeviations=new HashMap<String, Map<String, Double>>();
		Map<String, Double> newCnts=new HashMap<String, Double>();
		Map<String, Double> deviationCnts =new HashMap<String, Double>();
		
		updateAndGetLocalCopy(newDeviations, newCnts, deviationCnts, deviations);
		//from the curretn state, recreate the old state and compare for significant change
		Map<String, Map<String, Double>> oldDeviationsRelative=new HashMap<String, Map<String, Double>>();
		Map<String, Map<String, Double>> newDeviationsRelative=new HashMap<String, Map<String, Double>>();
		for (String activity : newDeviations.keySet()) {
			newDeviationsRelative.put(activity, new HashMap<String, Double>());
			oldDeviationsRelative.put(activity, new HashMap<String, Double>());
			for(Entry<String, Double> resources : newDeviations.get(activity).entrySet()) {
				double deviation=0.0;
				if(deviations.containsKey(activity)) {
					deviation=deviations.get(activity).getOrDefault(resources.getKey(), 0.0);
				}
				newDeviationsRelative.get(activity).put(resources.getKey(), resources.getValue()/newCnts.get(activity));
				oldDeviationsRelative.get(activity).put(resources.getKey(), (resources.getValue()-deviation)/(newCnts.get(activity)-deviationCnts.getOrDefault(activity, 0.0)));
			}
		}
		return calculateDistance(oldDeviationsRelative, newDeviationsRelative);
	}
	
	/**
	 * updates the current state synchronously
	 * also returns local copies after update in parameters
	 * returns absolute values, conversion to relative frequency is done locally to save time
	 */
	public synchronized void updateAndGetLocalCopy(Map<String, Map<String, Double>> newDeviations, Map<String, Double> newCnts, Map<String, Double> deviationCnts, Map<String, Map<String, Double>> deviations) {
		//first, get all resources deviations that did not change and add to copy
		for (String activity :this.violatingResourcesAbsolute.keySet()) {
			if(!deviations.containsKey(activity)) {
				newDeviations.put(activity, new HashMap<String, Double>());
				for (Entry<String,Double> resource : this.violatingResourcesAbsolute.get(activity).entrySet()) {
					newDeviations.get(activity).put(resource.getKey(),resource.getValue());
					newCnts.put(activity, cnts.get(activity));
				}
			}
		}
		
		for(String activity : deviations.keySet()) {
			//update counter for each affected activity
			double size = 0.0;
			for (double value : deviations.get(activity).values()) {
				size =size +value;
			}
			this.cnts.put(activity, this.cnts.getOrDefault(activity,0.0)+size);
			newCnts.put(activity, this.cnts.getOrDefault(activity,0.0)+size);
			deviationCnts.put(activity, size);
			
			//if activity is new and was not deviating before, just add all resource from deviations
			if(!this.violatingResourcesAbsolute.containsKey(activity)) {
				this.violatingResourcesAbsolute.put(activity, new HashMap<String, Double>());
				newDeviations.put(activity, new HashMap<String,Double>());
				for (Entry<String,Double> resource : deviations.get(activity).entrySet()) {
					this.violatingResourcesAbsolute.get(activity).put(resource.getKey(),resource.getValue());
					newDeviations.get(activity).put(resource.getKey(),resource.getValue());
				}
			}
			//for all remaining activities, merge deviation counts of resources
			else {
				newDeviations.put(activity, new HashMap<String, Double>());
				Set<String> unionKeySet = new HashSet<String>();
				for(String key : violatingResourcesAbsolute.get(activity).keySet())
					unionKeySet.add(key);
				for(String key : deviations.get(activity).keySet())
					unionKeySet.add(key);
				
				 //= violatingResourcesAbsolute.get(activity).keySet();
				//unionKeySet.addAll(deviations.get(activity).keySet());
				for(String key : unionKeySet) {
					double violatingResourceCount=violatingResourcesAbsolute.get(activity).getOrDefault(key,0.0);
					double deviationResourceCount=deviations.get(activity).getOrDefault(key,0.0);
					this.violatingResourcesAbsolute.get(activity).put(key, violatingResourceCount+deviationResourceCount);
					newDeviations.get(activity).put(key, (deviationResourceCount+violatingResourceCount));
				}
			}
		}
	}
	
	/**
	 * locally create potential future state induced by deviations and compare for significant change, convert to relative frequency as soon as possible
	 */
	public double quantifyChange(Map<String, Map<String, Double>> deviations) {
		Map<String, Map<String, Double>> newDeviations=new HashMap<String, Map<String, Double>>();
		Map<String, Map<String, Double>> oldDeviations=new HashMap<String, Map<String, Double>>();
		Map<String,Double> cnts=new HashMap<String, Double>();
		
		//first, get all resources deviations that did not change and add to copy
		for(String activity : this.violatingResourcesAbsolute.keySet()) {
			oldDeviations.put(activity,new HashMap<String, Double>());
			cnts.put(activity,this.cnts.get(activity));
			newDeviations.put(activity,new HashMap<String, Double>());

			
			for(Entry<String, Double> resource : this.violatingResourcesAbsolute.get(activity).entrySet()) {
				oldDeviations.get(activity).put(resource.getKey(),resource.getValue()/cnts.get(activity));
				if(!deviations.containsKey(activity)) {
					newDeviations.get(activity).put(resource.getKey(),resource.getValue()/cnts.get(activity));
				}
			}
		}
		
		for(String activity : deviations.keySet()) {
			if(!newDeviations.containsKey(activity)) {
				newDeviations.put(activity, new HashMap<String, Double>());
			}
		
			double size = 0.0;
			for (double value : deviations.get(activity).values()) {
				size=size+value;
			}
			//if activity is new and was not deviating before, just add all resource from deviations
			if(!this.violatingResourcesAbsolute.containsKey(activity)) {
				for(Entry<String, Double> resource : deviations.get(activity).entrySet()) {
					newDeviations.get(activity).put(resource.getKey(), resource.getValue()/size);
				}
			}
			
			//for all remaining activities, merge deviation counts of resources
			else {
				Set<String> unionKeySet = new HashSet<String>();
				for(String key : violatingResourcesAbsolute.get(activity).keySet())
					unionKeySet.add(key);
				for(String key : deviations.get(activity).keySet())
					unionKeySet.add(key);
				for(String key : unionKeySet) {
					double violatingResourceCount=violatingResourcesAbsolute.get(activity).getOrDefault(key,0.0);
					double deviationResourceCount=deviations.get(activity).getOrDefault(key,0.0);

					newDeviations.get(activity).put(key, (deviationResourceCount+violatingResourceCount)/(size+cnts.getOrDefault(activity,0.0)));
				}
			}
		}
		return calculateDistance(oldDeviations, newDeviations);
	}
	
	private double calculateDistance(Map<String, Map<String, Double>> oldDeviations, Map<String, Map<String, Double>> newDeviations) {
		double sum = 0.0;
		for (String activity : newDeviations.keySet()) {
			if(!oldDeviations.containsKey(activity)) 
				sum=sum+1.0;
			else {
				double dif=0.0;
				for (String resource : newDeviations.get(activity).keySet()) {
					double frequencyInOld=oldDeviations.get(activity).getOrDefault(resource, 0.0);
					double frequencyInNew = newDeviations.get(activity).get(resource);
					dif = dif + Math.pow(frequencyInOld-frequencyInNew,2);
				}
				sum=sum+Math.sqrt(dif);
			}
		}
		return sum /newDeviations.keySet().size();
	}
	
	
	public GlobalConformanceResult get() {
		GlobalConformanceResult result = new GlobalConformanceResult();
		Map<String, Map<String, Double>> deviationsRelative=new HashMap<String, Map<String, Double>>();
		for(String activity : this.violatingResourcesAbsolute.keySet()) {
			deviationsRelative.put(activity, new HashMap<String,Double>());
			for(Entry<String, Double> resource : this.violatingResourcesAbsolute.get(activity).entrySet()) {
				deviationsRelative.get(activity).put(resource.getKey(), resource.getValue()/this.cnts.get(activity));
			}
		}
		result.setResourceDeviations(deviationsRelative);
		return result;
	}
}
