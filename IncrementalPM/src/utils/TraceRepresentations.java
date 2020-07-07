package utils;

import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XTrace;

public class TraceRepresentations {

	public static String getActivitySequence(XTrace trace) {
		String sequence="";
		for(XEvent event : trace) {
			sequence=sequence+event.getAttributes().get("concept:name").toString()+">>";
		}
		return sequence.substring(0, sequence.length()-2);
	}
}
