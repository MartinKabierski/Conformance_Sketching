package ressources;

import org.deckfour.xes.model.XTrace;
import org.processmining.plugins.petrinet.replayresult.PNRepResult;

public class TraceAnalysisResult<T> {
	private XTrace trace;
	public boolean approximated;
	public PNRepResult replayResult;
	
	private T result;

	//TODO replace null pointer with Optional
	public TraceAnalysisResult(XTrace trace, boolean approximated, T absoluteResult, PNRepResult replayResult) {
		this.setTrace(trace);
		this.approximated=approximated;
		this.setResult(absoluteResult);
		this.replayResult=replayResult;
	}

	public XTrace getTrace() {
		return trace;
	}

	public void setTrace(XTrace trace) {
		this.trace = trace;
	}

	public T getResult() {
		return result;
	}

	public void setResult(T result) {
		this.result = result;
	}
	
	public PNRepResult getReplayResult() {
		return replayResult;
	}
}