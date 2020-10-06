package resourcedeviations;

import java.util.HashSet;
import java.util.Set;

import org.deckfour.xes.model.XAttribute;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;

import com.google.common.collect.Multiset;
import com.google.common.collect.TreeMultiset;

public class ResourceAssignmentComputer {

	double threshold;
	
	public ResourceAssignmentComputer(double threshold) {
		this.threshold = threshold;
	}
	
	
	public ResourceAssignment createResourceAssignment(XLog log) {
		ResourceAssignment assignment = new ResourceAssignment();
		for (String activity : getActivities(log)) {
			Set<String> authorized = determineAuthorizedResources(log, activity);
			if (!authorized.isEmpty()) {
				assignment.addAssignment(activity, authorized);
			}
		}
		
		return assignment;
	}

	
	public Set<String> determineAuthorizedResources(XLog log, String activity) {
		Multiset<String> executionCounts = TreeMultiset.create();
		for (XTrace trace : log) {
			for (XEvent event : trace) {
				if (event.getAttributes().get("concept:name").toString().equals(activity)) {
					XAttribute attr = event.getAttributes().get("org:resource");
					if (attr != null) {
						executionCounts.add(attr.toString());
					}
				}
			}
		}
		if (executionCounts.isEmpty()) {
			return new HashSet<String>();
		} else 	{
			Set<String> authorized = new HashSet<String>();
			double avg = getAverage(executionCounts);
			for (String res : executionCounts.elementSet()) {
				if (executionCounts.count(res) >= threshold * avg) {
					authorized.add(res);
				}
			}
			return authorized;
		}
	}

	private double getAverage(Multiset<String> counts) {
		return (double) counts.size() / counts.elementSet().size();
	}

//	private double getStandardDev(Multiset<String> counts, double avg) {
//		if (counts.elementSet().size() == 1) {
//			return 0;
//		}
//		double sum = 0.0;
//		for (String el : counts.elementSet()) {
//			sum += math.pow(counts.count(el) - avg, 2);
//		}
//		return math.sqrt(sum / counts.elementSet().size() - 1);
//	}
	
	
	private Set<String> getActivities(XLog log) {
		Set<String> result = new HashSet<>();
		for (XTrace trace : log) {
			for (XEvent event : trace) {
				result.add(event.getAttributes().get("concept:name").toString());
			}
		}
		return result;
	}
	
	
}

//// Han: this is just something to print to get stuff for the paper. If you see it, I probably forgot to delete it
//Map<String, Set<String>> resMap = new HashMap<String, Set<String>>();
//Set<String> allRes = new HashSet<>();
//int sum = 0;
//int totMin = Integer.MAX_VALUE;
//int totMax = 0;
//for (String activity : getActivities(log)) {
//	Set<String> executingResources = new HashSet<String>();
//	resMap.put(activity, executingResources);
//	for (XTrace trace : log) {
//		for (XEvent event : trace) {
//			if (event.getAttributes().get("concept:name").toString().equals(activity)) {
//				XAttribute attr = event.getAttributes().get("org:resource");
//				if (attr != null) {
//					executingResources.add(attr.toString());
//					allRes.add(attr.toString());
//				}
//			}
//		}
//	}
//	int n = executingResources.size();
//	sum += n;
//	totMin = Math.min(totMin, n);
//	totMax = Math.max(totMax, n);
//}
//
//int authSum = 0;
//int min = Integer.MAX_VALUE;
//int max = 0;
//for (Set<String> resSet : assignment.assignmentMap.values()) {
//	int n = resSet.size();
//	authSum += n;
//	min = Math.min(min, n);
//	max = Math.max(max, n);
//}
//
//int acts = getActivities(log).size();
//System.out.println("Total number of activities: " + acts);
//System.out.println("Total number of resources: " + allRes.size());
//System.out.println("Total number of activity-resource pairs: " + sum + " min: " + totMin + " max: " + totMax);
//System.out.println("Authorized activity-resource pairs: " + authSum + " min: " + min + " max: " + max);