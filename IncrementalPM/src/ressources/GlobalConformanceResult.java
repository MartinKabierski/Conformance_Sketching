package ressources;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class GlobalConformanceResult {
	private double fitness;
	private Map<String,Double> deviations;
	private Map<String,Map<String,Double>>resourceDeviations;
	private int cnt;
	private int totalVariants;
	private int approximatedVariants;
	private int approximatedThenCalculated;

	public GlobalConformanceResult() {
		fitness=-1.0;
		deviations=new HashMap<>();
		resourceDeviations=new HashMap<>();
		this.cnt = 0;
		this.totalVariants=0;
		this.approximatedVariants=0;
		this.approximatedThenCalculated=0;
	}
	
	public void setApproxCalc(int value) {
		this.approximatedThenCalculated=value;
	}
	
	public int GetApproxThencalc(){
		return this.approximatedThenCalculated;
	}

	public double getFitness() {
		return fitness;
	}

	public void setFitness(double fitness) {
		this.fitness = fitness;
	}

	public Map<String,Double> getDeviations() {
		return deviations;
	}

	public void setDeviations(Map<String,Double> deviations) {
		Map<String, Double> sortedDeviations =  deviations
				.entrySet()
				.stream()
				.sorted(Map.Entry.<String, Double>comparingByValue().reversed())
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
				(e1, e2) -> e1, LinkedHashMap::new));
		this.deviations = sortedDeviations;
	}

	public Map<String,Map<String,Double>> getResourceDeviations() {
		return resourceDeviations;
	}

	public void setResourceDeviations(Map<String,Map<String,Double>> resourceDeviations) {
		this.resourceDeviations = resourceDeviations;
	}
	
	public int getCnt() {
		return this.cnt;
	}
	
	public void setCnt(int cnt) {
		this.cnt = cnt;
	}
	
	public String toString() {

		return  "Fitness:     				"+fitness+
				"\nDeviations:  			"+deviations+
				"\nResources:   			"+resourceDeviations+
				"\nSampled Traces:			"+this.cnt+
				"\nTrace Variants:			"+this.totalVariants+
				"\nApproximated variants:	"+this.approximatedVariants;	
	}

	public void setTotalVariants(int totalVariants) {
		this.totalVariants=totalVariants;
	}
	
	public int getTotalVariants() {
		return this.totalVariants;
	}

	public void setApproximatedVariants(int approximatedVariants) {
		this.approximatedVariants=approximatedVariants;		
	}
	
	public int getApproximatedVariants() {
		return this.approximatedVariants;
	}
}
