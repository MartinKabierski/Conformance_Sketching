package conformance.traceAnalysis;

import java.util.ArrayList;
import java.util.List;

import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XTrace;
import org.processmining.plugins.petrinet.replayresult.StepTypes;

import com.google.common.collect.Multiset;
import com.google.common.collect.TreeMultiset;

public class TraceEditDistance {

	private class cell{
		int value;
		boolean left;
		boolean bot;
		boolean botleft;
		public cell(int value, boolean left, boolean top, boolean topleft) {
			this.value = value;
			this.left = left;
			this.bot = bot;
			this.botleft = botleft;
		}
	}
	int 		distance;
	cell[][]	TEDMatrix;
	String[] 	trace1Events;
	String[] 	trace2Events;
	XTrace trace1;
	XTrace trace2;
	
	public TraceEditDistance(XTrace trace1, XTrace trace2) {
		this.trace1=trace1;
		this.trace2=trace2;
		this.distance=calculateTED(trace1, trace2);
	}
	
	public TraceEditDistance(String trace1, String trace2) {
		this.distance=tst(trace1, trace2);
	}
	
	
	
	public int getDistance() {
		return this.distance;
	}
	
	public Multiset <String> getNonAligningActivities(){
		Multiset<String> nonAligningActivities = TreeMultiset.create();
		int i=trace1Events.length-1;
		int j=trace2Events.length-1;
		cell currentCell=this.TEDMatrix[i][j];
		
		//TODO only add the corresponding activity in the case, not both
		while(i>0 || j>0) {
			if (i>0 && TEDMatrix[i-1][j].value +1 == currentCell.value) {
				nonAligningActivities.add(trace1Events[i]);

				currentCell=TEDMatrix[i-1][j];
				i=i-1;
				continue;
			}
			if (j>0 && TEDMatrix[i][j-1].value +1 == currentCell.value) {
				nonAligningActivities.add(trace2Events[j]);
				currentCell=TEDMatrix[i][j-1];
				j=j-1;
				continue;
			}
			if (i>0 && j>0 && TEDMatrix[i-1][j-1].value +2 == currentCell.value) {
				nonAligningActivities.add(trace1Events[i]);
				nonAligningActivities.add(trace2Events[j]);
				currentCell=TEDMatrix[i-1][j-1];
				i=i-1;
				j=j-1;
				continue;
			}	
			if (i>0 && j>0 && TEDMatrix[i-1][j-1].value == currentCell.value) {
				currentCell=TEDMatrix[i-1][j-1];
				i=i-1;
				j=j-1;
			}	
		}
		return nonAligningActivities;
	}
	
	public List<StepTypes> getStepTypes(){
		List<StepTypes> stepTypes=new ArrayList<StepTypes>();
		int i=trace1Events.length-1;
		int j=trace2Events.length-1;
		cell currentCell=this.TEDMatrix[i][j];
		
		//TODO only add the corresponding activity in the case, not both
		while(i>0 || j>0) {
			if (i>0 && TEDMatrix[i-1][j].value +1 == currentCell.value) {
				stepTypes.add(StepTypes.L);
				currentCell=TEDMatrix[i-1][j];
				i=i-1;
				continue;
			}
			if (j>0 && TEDMatrix[i][j-1].value +1 == currentCell.value) {
				stepTypes.add(StepTypes.MREAL);
				currentCell=TEDMatrix[i][j-1];
				j=j-1;
				continue;
			}
			if (i>0 && j>0 && TEDMatrix[i-1][j-1].value +2 == currentCell.value) {
				stepTypes.add(StepTypes.LMNOGOOD);
				currentCell=TEDMatrix[i-1][j-1];
				i=i-1;
				j=j-1;
				continue;
			}	
			if (i>0 && j>0 && TEDMatrix[i-1][j-1].value == currentCell.value) {
				stepTypes.add(StepTypes.LMGOOD);
				currentCell=TEDMatrix[i-1][j-1];
				i=i-1;
				j=j-1;
			}	
		}
		return stepTypes;
	}
	
	
	
	public int calculateTED(XTrace trace1, XTrace trace2) {
		trace1Events=new String[trace1.size()+1];
		trace1Events[0]="#";
		trace2Events=new String[trace2.size()+1];
		trace2Events[0]="#";
		
		//init trace Lists
		for(int i=0;i<trace1.size();i++) {
			XEvent current=trace1.get(i);
			trace1Events[i+1]=current.getAttributes().get("concept:name").toString();
		}
		for(int j=0;j<trace2.size();j++) {
			XEvent current=trace2.get(j);
			trace2Events[j+1]=current.getAttributes().get("concept:name").toString();
		}
		
		//init calculation matrix
		this.TEDMatrix=new cell[trace1Events.length][trace2Events.length];
		for (int i=0;i<trace1Events.length;i++) {
			this.TEDMatrix[i][0]=new cell(i,false,false,false);
		}
		for (int j=0;j<trace2Events.length;j++) {
			this.TEDMatrix[0][j]=new cell(j,false,false,false);
		}
		
		//calculate edit distance
		for(int i=1;i<trace1Events.length;i++) {
			for(int j=1;j<trace2Events.length;j++) {
				int left=this.TEDMatrix[i][j-1].value+1;
				int bot=this.TEDMatrix[i-1][j].value+1;
				int botleft=this.TEDMatrix[i-1][j-1].value;
				if(!trace1Events[i].equals(trace2Events[j])) {
					botleft+=2;
				}
				int min=Math.min(left, bot);
				min=Math.min(min, botleft);
				
				boolean bleft=false;
				boolean bbot=false;
				boolean bbotleft=false;
				if (left <= bot && left <= botleft ) {
					min=left;
					bleft = true;
				}
				if (bot <=left && bot <= botleft) {
					min =bot;
					bbot = true;
				}
				if (botleft <=left && botleft <= bot) {
					min =botleft;
					bbotleft = true;
				}

				TEDMatrix[i][j]=new cell(min,bleft,bbot,bbotleft);
			}
		}
		return TEDMatrix[trace1Events.length-1][trace2Events.length-1].value;
	}
	
	public int tst(String trace1, String trace2) {
		String[] test1=trace1.split(" ");
		String [] test2=trace2.split(" ");
		trace1Events=new String[test1.length+1];
		trace1Events[0]="#";
		trace2Events=new String[test2.length+1];
		trace2Events[0]="#";
		
		//init trace Lists
		for(int i=0;i<test1.length;i++) {
			trace1Events[i+1]=test1[i];
		}
		for(int j=0;j<test2.length;j++) {
			trace2Events[j+1]=test2[j];
		}
		
		//init calculation matrix
		this.TEDMatrix=new cell[trace1Events.length][trace2Events.length];
		for (int i=0;i<trace1Events.length;i++) {
			this.TEDMatrix[i][0]=new cell(i,false,false,false);
		}
		for (int j=0;j<trace2Events.length;j++) {
			this.TEDMatrix[0][j]=new cell(j,false,false,false);
		}
		
		//calculate edit distance
		for(int i=1;i<trace1Events.length;i++) {
			for(int j=1;j<trace2Events.length;j++) {
				int left=this.TEDMatrix[i][j-1].value+1;
				int bot=this.TEDMatrix[i-1][j].value+1;
				int botleft=this.TEDMatrix[i-1][j-1].value;
				if(!trace1Events[i].equals(trace2Events[j])) {
					botleft+=2;
				}
				int min=Math.min(left, bot);
				min=Math.min(min, botleft);
				
				boolean bleft=false;
				boolean bbot=false;
				boolean bbotleft=false;
				if (left <= bot && left <= botleft ) {
					min=left;
					bleft = true;
				}
				if (bot <=left && bot <= botleft) {
					min =bot;
					bbot = true;
				}
				if (botleft <=left && botleft <= bot) {
					min =botleft;
					bbotleft = true;
				}

				TEDMatrix[i][j]=new cell(min,bleft,bbot,bbotleft);
			}
		}
		return TEDMatrix[trace1Events.length-1][trace2Events.length-1].value;
	}
	
	
	
public static void main(String[] args) {
		String a="a a a b c d d e f g h h h h";
		String b="a a b c d f h h";
		TraceEditDistance ted = new TraceEditDistance(a,b);
		System.out.println(ted.getDistance());
		System.out.println(ted.getNonAligningActivities());//.entrySet());
	}
	
}
