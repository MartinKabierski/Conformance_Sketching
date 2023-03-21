package conformance.traceAnalysis;

import ressources.IncrementalConformanceResult;
/*
 * to keep scalability with threads, have internal state updated be synchronous, but mere reads for quuantification of change be approximative
 * i.e. not synchronous. as update only add instances, this means that quantification is always a bit more pessimistic than whats possible
 * 
 * Also, have updates be synchronous, but return local copies of current state, so that threads can calculate difference locally
 */

public interface IncrementalConformanceCalculator<T>{
	public IncrementalConformanceResult get();
	public double update(T conformanceResult);
	public double quantifyChange(T traceConformanceInformation);
}
