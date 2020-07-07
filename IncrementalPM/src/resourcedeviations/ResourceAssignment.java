package resourcedeviations;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class ResourceAssignment {

	Map<String, Set<String>> assignmentMap;
	
	public ResourceAssignment() {
		this.assignmentMap = new HashMap<>();
	}
	
	public void addAssignment(String eventClass, Set<String> resources) {
		assignmentMap.put(eventClass, resources);
	}
	
	public boolean isAuthorized(String eventClass, String resource) {
		// if no assignment is defined for an eventClass, then no violation possible
		if (!assignmentMap.containsKey(eventClass)) {
			return true;
		}
		return assignmentMap.get(eventClass).contains(resource);
	}
	
}
