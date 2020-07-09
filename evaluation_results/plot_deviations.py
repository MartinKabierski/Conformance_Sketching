import argparse
import sys
import pandas as pd
import matplotlib.pyplot as plt
import numpy as np
import re
from collections import defaultdict
from math import log10
import matplotlib.patches as mpatches
import statistics

def main():
        font = {'font.family' : 'normal',
        #'font.weight' : 'bold',
        'font.size'   : 18}
        plt.rcParams.update(font)

        blue_patch = mpatches.Patch(color='blue', label='Complete Log')
        red_patch = mpatches.Patch(color='black', label='Sampled Log')


        bpi2012_orig=pd.read_csv("./results/results10/BPI_Challenge_2012_baseline.csv", sep=';')
        bpi2012_deviations=pd.read_csv("./results/results10/BPI_Challenge_2012_deviationsTEST.csv", sep=';')
        bpi2012_deviations_approx=pd.read_csv("./results/results10/BPI_Challenge_2012_deviationsApproxTEST.csv", sep=';')

        print("Loaded input files")
        print()

        #set parameters for alignments
        chosen_delta=0.05
        chosen_alpha=0.99
        chosen_epsilon=0.01
        chosen_k=0.2
        chosen_heuristics="NONALIGNING_KNOWN"

        print("###used parameters for Evaluation###")
        print("Delta:    " + str(chosen_delta))
        print("Alpha:    " + str(chosen_alpha))
        print("Epsilon:  " + str(chosen_epsilon))
        print("K:        " + str(chosen_k))
        print()

        bpi2012_deviations=bpi2012_deviations.loc[(bpi2012_deviations["delta"]==chosen_delta) & (bpi2012_deviations["alpha"]==chosen_alpha) & (bpi2012_deviations["epsilon"]==chosen_epsilon)]
        bpi2012_deviations_approx=bpi2012_deviations_approx.loc[(bpi2012_deviations_approx["delta"]==chosen_delta) & (bpi2012_deviations_approx["alpha"]==chosen_alpha) & (bpi2012_deviations_approx["epsilon"]==chosen_epsilon) & (bpi2012_deviations_approx["k"]==chosen_k) & (bpi2012_deviations_approx["approximationMode"]==chosen_heuristics)]

        orig_asynchRelDict=get_orig_asynchMovesDict(bpi2012_orig)
        orig_sorted_descending = sorted(orig_asynchRelDict.items(), key=lambda kv: kv[1], reverse=True)
        #orig_asynchRelDict = sorted(orig_asynchRelDict.items(), key=lambda kv: kv[1], reverse=True)
        #print(orig_sorted_descending)
        asynchMovesDict=get_asynchMovesDict(bpi2012_deviations)
        print(asynchMovesDict)
        asynchMoves_descending=[]
        #order values based on descending ordering in original result
        for key in orig_sorted_descending:
                print(key)
                if key[0] in asynchMovesDict:
                        print("--> "+str(statistics.mean(asynchMovesDict[key[0]])))
                        asynchMoves_descending.append(asynchMovesDict[key[0]	])
                if not key[0] in asynchMovesDict:
                        print("--> 0.0")
                        asynchMoves_descending.append([0])
        plt.boxplot(asynchMoves_descending)
        plt.title("BPI-12", fontsize=18)
        orig_values=[0]+[x[1] for x in orig_sorted_descending]
        plt.plot(orig_values,'b.')
        plt.xlabel("Activities")
        plt.ylabel("Frequency (relative)")
        plt.xticks(np.arange(20), ["TEST"]+[i[0] for i in orig_sorted_descending], fontsize=5)
        #plt.fontsize(14)
        #for label in plt.gca().xaxis.get_ticklabels():
        #    label.set_visible(False)
        #for label in plt.gca().xaxis.get_ticklabels()[0::3]:
        #    label.set_visible(True)
        plt.xticks(rotation=90, fontsize=8)
        plt.legend(handles=[blue_patch, red_patch],loc='upper right', fontsize=16)

        plt.show()
        #plt.savefig("./bpi2012_deviations.pdf", bbox_inches='tight')
        plt.clf()
        
        orig_asynchRelDict=get_orig_asynchMovesDict(bpi2012_orig)
        orig_sorted_descending = sorted(orig_asynchRelDict.items(), key=lambda kv: kv[1], reverse=True)
        asynchMovesDict=get_asynchMovesDict(bpi2012_deviations_approx)
        approx_sorted_descending = sorted(asynchMovesDict.items(), key=lambda kv: statistics.median(kv[1]), reverse=True)


        origMoves_descending=[]
        #print(orig_sorted_descending)
        #order values based on descending ordering in original result
        for key in approx_sorted_descending:
                in_orig=False
                #print(key)
                for orig in orig_sorted_descending:
                        #print(" "+str(orig))
                        if key[0]==orig[0]:
                                origMoves_descending.append(orig[1])
                                in_orig=True
                if not in_orig:
                        origMoves_descending.append(0.0)

        #print(origMoves_descending)
        plot_list=[]
        for list in approx_sorted_descending:
                plot_list.append(list[1])

        plt.boxplot(plot_list)
        plt.title("BPI-12", fontsize=18)
        orig_values=[0]+[x for x in origMoves_descending]
        plt.plot(orig_values,'b.')
        plt.xlabel("Activities")
        plt.ylabel("Frequency (relative)")
        plt.xticks(np.arange(20), ["TEST"]+[i[0] for i in approx_sorted_descending], fontsize=5)
        #for label in plt.gca().xaxis.get_ticklabels():
        #    label.set_visible(False)
        #for label in plt.gca().xaxis.get_ticklabels()[0::3]:
        #    label.set_visible(True)
        plt.xticks(rotation=90, fontsize=8)
        plt.legend(handles=[blue_patch, red_patch],loc='upper right', fontsize=16)

        plt.show()
        #plt.savefig("./bpi2012_deviationsApprox_"+str(chosen_k)+"_"+str(chosen_heuristics)+".pdf", bbox_inches='tight')

def get_orig_asynchMovesDict(df):
        asynchMovesRelDict=defaultdict(list)
        #print(df.columns)
        asynchMovesRel=df["deviations"].values
        for asynchmoves in asynchMovesRel:
                 matchObject = re.search("\{(.+)\}", asynchmoves, flags=0)
                 event= matchObject.group(1).split(",")
                 for keyValuePair in event:
                         key,value=keyValuePair.split("=")
                         key = key.strip()
                         value = value.strip()
                         asynchMovesRelDict[key].append(float(value))
        for key in asynchMovesRelDict.keys():
                asynchMovesRelDict[key]=sum(asynchMovesRelDict[key]) / float(len(asynchMovesRelDict[key]))
        return asynchMovesRelDict

def get_asynchMovesDict(df):
        asynchMovesRelDict=defaultdict(list)
        asynchMovesRel=df["deviations"].values
        for asynchmoves in asynchMovesRel:
                 matchObject = re.search("\{(.+)\}", asynchmoves, flags=0)
                 event= matchObject.group(1).split(",")
                 for keyValuePair in event:
                         key,value=keyValuePair.split("=")
                         key = key.strip()
                         value = value.strip()
                         asynchMovesRelDict[key].append(float(value))
                 #while len(asynchMovesRelDict[key])<10:
                 #        print("HIT")
                 #        asynchMovesRelDict[key].append(0.0)
        return asynchMovesRelDict

if __name__ == "__main__":
    #sys.argv = [sys.argv[0], 'ICC_alignment_approx_results', 'alignment_ORIG_results']
    main()#sys.argv)
