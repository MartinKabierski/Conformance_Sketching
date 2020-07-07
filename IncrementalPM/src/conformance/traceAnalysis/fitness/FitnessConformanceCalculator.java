package conformance.traceAnalysis.fitness;
import conformance.traceAnalysis.IncrementalConformanceCalculator;
import ressources.GlobalConformanceResult;

public class FitnessConformanceCalculator implements IncrementalConformanceCalculator<Double>{
	protected double fitness;
	protected int cnt;
	
	public FitnessConformanceCalculator() {
		this.cnt=0;
		this.fitness=0;
	}
	
	public synchronized double update(Double conformanceResult) {
		double oldFitness=this.fitness;
		this.fitness=(this.fitness*(cnt)+conformanceResult)/(++this.cnt);
		return Math.abs(oldFitness-fitness);
		
	}
	
	public double quantifyChange(Double traceConformanceInformation) {
		double oldFitness=this.fitness;
		double localcnt=this.cnt;
		double newFitness=(oldFitness*(localcnt)+traceConformanceInformation)/(localcnt+1);	
		return Math.abs(oldFitness-newFitness);
	}

	public synchronized GlobalConformanceResult get() {
		GlobalConformanceResult result = new GlobalConformanceResult();
		result.setFitness(fitness);
		return result;
	}

}
