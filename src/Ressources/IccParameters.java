package Ressources;

import qualitychecking.QualityCheckManager;

public class IccParameters {
	public final static String PREFIXSUFFIX = "prefixsuffix";
	public final static String NONALIGNING = "nonaligning";

	private double epsilon;
	private double delta;
	private double alpha;
	private double k;
	private int initialSize;
	private String goal;
	private boolean approximate;
	private String approximationMode;
	private boolean checkInternalQuality = false;
	private boolean checkExternalQuality = false;
	private QualityCheckManager internalQualityCheckManager;
	private QualityCheckManager externalQualityCheckManager;
	
	
	public IccParameters(double delta, double alpha, double epsilon, double k, int initialSize, String goal, boolean approximate) {
		this.setEpsilon(epsilon);
		this.setDelta(delta);
		this.setAlpha(alpha);
		this.setK(k);
		this.setInitialSize(initialSize);
		this.setGoal(goal);
		this.setApproximate(approximate);
		if (approximate) this.setApproximationMode(this.NONALIGNING);
	}
	
	public IccParameters(double delta, double alpha, double epsilon, double k, int initialSize, String goal, boolean approximate, String approximationMode) {
		this.setEpsilon(epsilon);
		this.setDelta(delta);
		this.setAlpha(alpha);
		this.setK(k);
		this.setInitialSize(initialSize);
		this.setGoal(goal);
		this.setApproximate(approximate);
		this.setApproximationMode(approximationMode);
	}
	
	public String toString() {
		return "Delta: "+this.getDelta()+", Alpha: "+this.getAlpha()+", Epsilon: "+this.getEpsilon()+", K: "+this.getK()+", initial Size: "+this.getInitialSize()+
				", goal: "+this.getGoal()+", approximate: "+this.isApproximate() + ", internal quality: " + this.isCheckInternalQuality() +", approximation mode: "+ this.approximationMode;
	}

	public double getEpsilon() {
		return epsilon;
	}

	public void setEpsilon(double epsilon) {
		this.epsilon = epsilon;
	}

	public int getInitialSize() {
		return initialSize;
	}

	public void setInitialSize(int initialSize) {
		this.initialSize = initialSize;
	}

	public String getGoal() {
		return goal;
	}

	public void setGoal(String goal) {
		this.goal = goal;
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
		return checkInternalQuality;
	}

	public void setCheckInternalQuality(boolean checkInternalQuality) {
		this.checkInternalQuality = checkInternalQuality;
	}

	public boolean isCheckExternalQuality() {
		return checkExternalQuality;
	}

	public void setCheckExternalQuality(boolean checkExternalQuality) {
		this.checkExternalQuality = checkExternalQuality;
	}
	
	public QualityCheckManager getInternalQualityCheckContainer() {
		if (internalQualityCheckManager == null) {
			internalQualityCheckManager = new QualityCheckManager(true);
		}
		return internalQualityCheckManager;
	}
	
	public QualityCheckManager getExternalQualityCheckContainer() {
		if (externalQualityCheckManager == null) {
			externalQualityCheckManager = new QualityCheckManager(false);
		}
		return externalQualityCheckManager;
	}

	public String getApproximationMode() {
		return approximationMode;
	}

	public void setApproximationMode(String approximationMode) {
		this.approximationMode = approximationMode;
	}
	
	
}
