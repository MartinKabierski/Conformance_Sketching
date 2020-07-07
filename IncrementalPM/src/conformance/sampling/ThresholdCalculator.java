package conformance.sampling;

//This class calculates the amount of traces that have to be read in without new information
//
public class ThresholdCalculator {
	
	public static int calculateThreshold(double threshold, double confidencelevel){
		int tracesRequired=0;
		boolean thresholdReached=false;
		BinomialConfidenceCalculator binComputer = new
				BinomialConfidenceCalculator(
				BinomialConfidenceCalculator.MethodType.WILSON_SCORE, confidencelevel, threshold);
		while(!thresholdReached){
			tracesRequired++;
			binComputer.processTrailResult(false);
			thresholdReached=binComputer.getConfidenceIntervals().getUpperBound() < threshold; 
		}
		return tracesRequired;
	}
	
	public static int getThresholdCalculation(double threshold, double confidencelevel){
		String ThresholdCalculation="";
		int tracesRequired=0;
		boolean thresholdReached=false;
		BinomialConfidenceCalculator binComputer = new
				BinomialConfidenceCalculator(
				BinomialConfidenceCalculator.MethodType.WILSON_SCORE, confidencelevel, threshold);
		while(!thresholdReached){
			tracesRequired++;
			binComputer.processTrailResult(false);
			//ThresholdCalculation=ThresholdCalculation+"[ " +
				//	binComputer.getConfidenceIntervals().getLowerBound() + "," +
					//binComputer.getConfidenceIntervals().getUpperBound()
					 //+" ]\n";
			thresholdReached=binComputer.getConfidenceIntervals().getUpperBound() < threshold; 
		}
		ThresholdCalculation=ThresholdCalculation+"Required traces: "+tracesRequired;
		return tracesRequired;
	}
}
