package ressources;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

public class IncrementalConformanceResult {
	private double fitness;
	private Map<String,Double> deviations;
	private Map<String,Double> absoluteDeviations;
	private Map<String,Map<String,Double>>resourceDeviations;
	private Map<String, Double>occurencePerTrace;
	private int cnt;
	private int noDeviations;
	private int totalVariants;
	private int approximatedVariants;
	private int approximatedThenCalculated;
	public double fitnessAtFirst;
	
	public IccParameter iccParameter;

	public IncrementalConformanceResult() {
		fitness=-1.0;
		deviations=new HashMap<>();
		absoluteDeviations=new HashMap<>();
		resourceDeviations=new HashMap<>();
		occurencePerTrace=new HashMap<>();
		this.cnt = 0;
		this.noDeviations = 0;
		this.totalVariants=0;
		this.approximatedVariants=0;
		this.approximatedThenCalculated=0;
		this.fitnessAtFirst=0;
		
		this.iccParameter = null;
	}
	
	public void setIccParameter(IccParameter iccParameter) {
		this.iccParameter = iccParameter;
	}
	
	public IccParameter getIccParameter() {
		return this.iccParameter;
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

	public void setDeviationsRelative(Map<String,Double> deviations) {
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
	
	public int getNoDeviations() {
		return this.noDeviations;
	}
	
	public void setNoDeviations(int noDeviations) {
		this.noDeviations = noDeviations;
	}
	
	public String toString() {
		for (Entry<String, Double> x : this.getDeviationsAbsolute().entrySet()) {
			if (x.getValue()<this.getOccurencesPerTrace().get(x.getKey()))
				System.out.println("More Occurences than Costs "+x.toString()+" - "+this.getOccurencesPerTrace().get(x.getKey()));
			if (!this.getOccurencesPerTrace().containsKey(x.getKey()))
				System.out.println("Occurence not added "+x.getKey());
			}
		return  "Fitness:     			"+fitness+
				"\nNo Deviations:			"+this.noDeviations+
				"\nDeviations (relative):		"+this.deviations+
				"\nDeviations (absolute):		"+this.getDeviationsAbsolute()+
				"\nNo Traces with Deviation: 	"+this.getOccurencesPerTrace()+
				"\nResources:   			"+resourceDeviations+
				"\nSampled Traces:			"+this.cnt+
				"\nTrace Variants:			"+this.totalVariants+
				"\nApproximated variants:	"+this.approximatedVariants+
				"\nApproximated then calc:	"+this.approximatedThenCalculated;

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
	
	public void setOccurencePerTrace(Map<String, Double> occurences) {
		this.occurencePerTrace=occurences;
	}
	
	public Map<String, Double> getOccurencesPerTrace() {
		Map<String, Double> sortedOccurences =  this.occurencePerTrace
				.entrySet()
				.stream()
				.sorted(Map.Entry.<String, Double>comparingByValue().reversed())
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
				(e1, e2) -> e1, LinkedHashMap::new));
		return sortedOccurences;
	}
	
	public void setDeviationsAbsolute(Map<String, Double> absoluteDeviations) {
		this.absoluteDeviations=absoluteDeviations;
	}
	
	public Map<String, Double> getDeviationsAbsolute() {
		Map<String, Double> sortedOccurences =  this.absoluteDeviations
				.entrySet()
				.stream()
				.sorted(Map.Entry.<String, Double>comparingByValue().reversed())
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
				(e1, e2) -> e1, LinkedHashMap::new));
		return sortedOccurences;
	}
}
