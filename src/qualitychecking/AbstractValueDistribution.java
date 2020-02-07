package qualitychecking;

import org.deckfour.xes.model.XTrace;

public interface AbstractValueDistribution {
	
	public void addTrace(XTrace trace);
	
	public double computeIncrementalDistance(AbstractValueDistribution oldDistribution);
	
	public double getRelativeFrequency(Object value);
	
	public double computeExternalStatistic(AbstractValueDistribution validationDistribution); 
	
	public AbstractValueDistribution fullCopy();
	
	public AbstractValueDistribution emptyCopy();
}
