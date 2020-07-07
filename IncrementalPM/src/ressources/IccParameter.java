package ressources;

import conformance.IncrementalConformanceChecker;
import conformance.IncrementalConformanceChecker.Goals;
import conformance.IncrementalConformanceChecker.Heuristics;
import qualitychecking.QualityCheckManager;

public class IccParameter {

	private double epsilon;
	private double delta;
	private double alpha;
	private double k;
	private Goals goal;
	
	private boolean approximate;
	private Heuristics approximationHeuristic;
	
	private boolean storeSampledTraces;
	private boolean checkInternalQuality;
	private boolean checkExternalQuality;
	private QualityCheckManager internalQualityCheckManager;
	private QualityCheckManager externalQualityCheckManager;
	
	private int noThreads;
	int NO_THREADS=10;
	//int NO_THREADS= Runtime.getRuntime().availableProcessors() / 4;
	
	//TODO reduce number of constructors as needed
	public IccParameter(Goals goal, boolean approximate) {
		this.setEpsilon(0.01);
		this.setDelta(0.05);
		this.setAlpha(0.99);
		this.setK(0.2);
		this.setGoal(goal);
		this.setApproximate(approximate);
		this.approximationHeuristic = IncrementalConformanceChecker.Heuristics.NONALIGNING_KNOWN;
		this.checkInternalQuality = false;
		this.checkExternalQuality = false;
		this.noThreads = NO_THREADS;
		//System.out.println(this.noThreads);
	}

	
	public IccParameter(double delta, double alpha, double epsilon, double k, Goals goal, boolean approximate, boolean checkInternalQuality, boolean checkExternalQuality) {
		this.setEpsilon(epsilon);
		this.setDelta(delta);
		this.setAlpha(alpha);
		this.setK(k);
		this.setGoal(goal);
		this.setApproximate(approximate);
		this.checkInternalQuality = checkInternalQuality;
		this.checkExternalQuality = checkExternalQuality;
		this.approximationHeuristic = IncrementalConformanceChecker.Heuristics.NONALIGNING_KNOWN;
		this.noThreads = NO_THREADS;
		//System.out.println(this.noThreads);}
	}
	
	public IccParameter(double delta, double alpha, double epsilon, double k, Goals goal, boolean approximate, Heuristics heuristic, boolean checkInternalQuality, boolean checkExternalQuality) {
		this.setEpsilon(epsilon);
		this.setDelta(delta);
		this.setAlpha(alpha);
		this.setK(k);
		this.setGoal(goal);
		this.setApproximate(approximate);
		this.approximationHeuristic = heuristic;
		this.checkInternalQuality = checkInternalQuality;
		this.checkExternalQuality = checkExternalQuality;
		this.noThreads = NO_THREADS;
		//System.out.println(this.noThreads);	
	}
	
	public IccParameter(double delta, double alpha, double epsilon, double k, Goals goal, boolean approximate) {
		this.setEpsilon(epsilon);
		this.setDelta(delta);
		this.setAlpha(alpha);
		this.setK(k);
		this.setGoal(goal);
		this.setApproximate(approximate);
		this.approximationHeuristic = IncrementalConformanceChecker.Heuristics.NONALIGNING_KNOWN;
		this.checkInternalQuality = false;
		this.checkExternalQuality = false;
		this.noThreads = NO_THREADS;
		//System.out.println(this.noThreads);	
	}
	
	public String toString() {
		return "Delta: "+this.getDelta()+", Alpha: "+this.getAlpha()+", Epsilon: "+this.getEpsilon()+", K: "+this.getK()+
				", goal: "+this.getGoal()+", approximate: "+this.isApproximate() + ", int. quality: " + this.isCheckInternalQuality() + ", ext. quality: " + this.isCheckExternalQuality() + ", approximation mode: "+ this.approximationHeuristic;
	}

	public double getEpsilon() {
		return epsilon;
	}

	public void setEpsilon(double epsilon) {
		this.epsilon = epsilon;
	}

	public void setGoal(Goals goal) {
		this.goal = goal;
	}
	
	public Goals getGoal() {
		return goal;
	}

	public boolean storeSampledTraces() {
		return storeSampledTraces;
	}
	
	public void setStoreSampledTraces(boolean storeSampledTraces) {
		this.storeSampledTraces = storeSampledTraces;
	}

	public boolean isApproximate() {
		return approximate;
	}

	public void setApproximate(boolean approximate) {
		this.approximate = approximate;
	}

	public double getDelta() {
		return delta;
	}

	public void setDelta(double delta) {
		this.delta = delta;
	}

	public double getAlpha() {
		return alpha;
	}

	public void setAlpha(double alpha) {
		this.alpha = alpha;
	}

	public double getK() {
		return k;
	}

	public void setK(double k) {
		this.k = k;
	}

	public boolean isCheckInternalQuality() {
		return checkInternalQuality && internalQualityCheckManager != null;
	}

	public void setCheckInternalQuality(boolean checkInternalQuality) {
		this.checkInternalQuality = checkInternalQuality;
	}

	public boolean isCheckExternalQuality() {
		return checkExternalQuality && externalQualityCheckManager != null;
	}

	public void setCheckExternalQuality(boolean checkExternalQuality) {
		this.checkExternalQuality = checkExternalQuality;
	}

	public Heuristics getApproximationHeuristic() {
		return approximationHeuristic;
	}

	public void setApproximationHeuristic(Heuristics approximationHeuristic) {
		this.approximationHeuristic = approximationHeuristic;
	}

	public QualityCheckManager getExternalQualityCheckManager() {
		return externalQualityCheckManager;
	}

	public void setExternalQualityCheckManager(QualityCheckManager externalQualityCheckManager) {
		this.externalQualityCheckManager = externalQualityCheckManager;
	}

	public QualityCheckManager getInternalQualityCheckManager() {
		return internalQualityCheckManager;
	}

	public void setInternalQualityCheckManager(QualityCheckManager internalQualityCheckManager) {
		this.internalQualityCheckManager = internalQualityCheckManager;
	}

	public int getNoThreads() {
		return this.noThreads;
	}
	
	
}
