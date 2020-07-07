package ressources;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.deckfour.xes.model.XTrace;

import utils.TraceRepresentations;

public class TraceAnalysisResultMap<T> extends ConcurrentHashMap<String, TraceAnalysisResult<T>>{
	private static final long serialVersionUID = 1L;

	//useful statistics
	AtomicInteger approximatedTraceVariantCnt;
	
	public TraceAnalysisResultMap() {
		approximatedTraceVariantCnt=new AtomicInteger(0);
	}

	public int getTraceVariantCount() {
		return this.size();
	}
	
	public int getApproximatedTraceVariantCnt() {
		return this.approximatedTraceVariantCnt.get();
	}
	
	public void put(TraceAnalysisResult<T> result) {
		if(result.approximated)
			this.approximatedTraceVariantCnt.getAndIncrement();
		this.put(TraceRepresentations.getActivitySequence(result.getTrace()), result);
	}

	public TraceAnalysisResult<T> get(XTrace trace) {
		return this.get(TraceRepresentations.getActivitySequence(trace));
	}
	
	public boolean contains(XTrace trace) {
		return this.containsKey(TraceRepresentations.getActivitySequence(trace));
	}
	
	public synchronized Map<String, TraceAnalysisResult<T>> getCopy(){
		Map copyMap=new HashMap<String, TraceAnalysisResult<T>>();
		for (Entry entry : this.entrySet()) {
			copyMap.put(entry.getKey(), entry.getValue());
		}
		return copyMap;
	}
}
