import os
import pandas as pd
import matplotlib.pyplot as plt
import numpy as np
import re
from collections import defaultdict
import matplotlib.patches as mpatches
import statistics

def main(input_name):
    font = {'font.family': 'normal',
            # 'font.weight' : 'bold',
            'font.size': 18}
    plt.rcParams.update(font)
    blue_patch = mpatches.Patch(color='blue', label='Complete Log')
    red_patch = mpatches.Patch(color='black', label='Sampled Log')


    chosen_delta = 0.01
    chosen_alpha = 0.99
    chosen_epsilon = 0.01
    k = [0.1, 0.2, 0.3]
    heuristics = ["NONALIGNING_ALL", "NONALIGNING_KNOWN"]

    for chosen_k in k:
        for chosen_heuristics in heuristics:
            bpi2012_orig = pd.read_csv("./src/" + input_name + "_baseline.csv", sep=';')
            bpi2012_deviations = pd.read_csv("./src/" + input_name + "_deviations.csv", sep=';')
            bpi2012_deviations_approx = pd.read_csv("./src/" + input_name + "_deviationsApprox.csv",
                                                    sep=';')
            bpi2012_deviations = bpi2012_deviations.loc[
                (bpi2012_deviations["delta"] == chosen_delta) &
                (bpi2012_deviations["alpha"] == chosen_alpha) &
                (bpi2012_deviations["epsilon"] == chosen_epsilon)]

            bpi2012_deviations_approx = bpi2012_deviations_approx.loc[
                (bpi2012_deviations_approx["delta"] == chosen_delta) &
                (bpi2012_deviations_approx["alpha"] == chosen_alpha) &
                (bpi2012_deviations_approx["epsilon"] == chosen_epsilon) &
                (bpi2012_deviations_approx["k"] == chosen_k) &
                (bpi2012_deviations_approx["approximationMode"] == chosen_heuristics)]

            orig_asynchRelDict = get_orig_asynchMovesDict(bpi2012_orig)
            orig_sorted_descending = sorted(orig_asynchRelDict.items(), key=lambda kv: kv[1], reverse=True)

            asynchMovesDict = get_asynchMovesDict(bpi2012_deviations)
            asynchMoves_descending = []
            # order values based on descending ordering in original result
            for key in orig_sorted_descending:
                print(key)
                if key[0] in asynchMovesDict:
                    print("--> " + str(statistics.mean(asynchMovesDict[key[0]])))
                    asynchMoves_descending.append(asynchMovesDict[key[0]])
                if not key[0] in asynchMovesDict:
                    print("--> 0.0")
                    asynchMoves_descending.append([0.0])
            plt.boxplot(asynchMoves_descending)
            plt.title(derive_plot_name(input_name), fontsize=18)
            orig_values = [x[1] for x in orig_sorted_descending]
            plt.plot([i for i in range(1, len(orig_values) + 1)], orig_values, 'b.')
            plt.xlabel("Activities")
            plt.ylabel("Frequency (relative)")
            plt.xticks(np.arange(1, len(orig_values) + 1), [] + [i for i in range(1, len(orig_values) + 1)], fontsize=5)
            # plt.fontsize(14)
            # for label in plt.gca().xaxis.get_ticklabels():
            #    label.set_visible(False)
            # for label in plt.gca().xaxis.get_ticklabels()[0::3]:
            #    label.set_visible(True)

            # plt.xticks(rotation=90, fontsize=8)
            plt.xticks(fontsize=8)
            plt.legend(handles=[blue_patch, red_patch], loc='upper right', fontsize=16)

            # plt.show()
            plt.savefig(os.path.join(".", str(input_name), str(input_name) + "_deviations.pdf"), bbox_inches='tight')
            plt.clf()

            orig_asynchRelDict = get_orig_asynchMovesDict(bpi2012_orig)
            orig_sorted_descending = sorted(orig_asynchRelDict.items(), key=lambda kv: kv[1], reverse=True)
            asynchMovesDict = get_asynchMovesDict(bpi2012_deviations_approx)
            approx_sorted_descending = sorted(asynchMovesDict.items(), key=lambda kv: statistics.median(kv[1]), reverse=True)

            origMoves_descending = []
            plot_list = []
            for key in approx_sorted_descending:
                plot_list.append(key[1])
                print(key)
                in_orig = False
                for orig in orig_sorted_descending:
                    if key[0] == orig[0]:
                        origMoves_descending.append(orig[1])
                        in_orig = True
                        print("--> " + str(statistics.mean(asynchMovesDict[key[0]])))
                if not in_orig:
                    origMoves_descending.append(0.0)
                    print("--> 0.0")
            for original_deviation in orig_sorted_descending:
                in_sample = False
                for approx_deviation in approx_sorted_descending:
                    if original_deviation[0] == approx_deviation[0]:
                        in_sample = True
                if not in_sample:
                    print(original_deviation)
                    approx_sorted_descending.append([original_deviation[0], 0.0])
                    origMoves_descending.append(original_deviation[1])

            plt.boxplot(plot_list)
            plt.title(derive_plot_name_approximation(input_name, chosen_k, chosen_heuristics), fontsize=18)
            orig_values = [x[1] for x in orig_sorted_descending]
            plt.plot([i for i in range(1, len(origMoves_descending) + 1)], origMoves_descending, 'b.')
            plt.xlabel("Activities")
            plt.ylabel("Frequency (relative)")
            plt.xticks(np.arange(1, len(origMoves_descending) + 1), [] + [i for i in range(1, len(origMoves_descending) + 1)],
                       fontsize=5)
            # for label in plt.gca().xaxis.get_ticklabels():
            #    label.set_visible(False)
            # for label in plt.gca().xaxis.get_ticklabels()[0::3]:
            #    label.set_visible(True)
            # plt.xticks(rotation=90, fontsize=8)
            plt.xticks(fontsize=8)

            plt.legend(handles=[blue_patch, red_patch], loc='upper right', fontsize=16)

            #plt.show()
            plt.savefig(os.path.join(".", str(input_name), str(input_name) + "_deviationsApprox_" + str(chosen_k) + "_" + str(chosen_heuristics) + ".pdf"), bbox_inches='tight')
            #plt.savefig("./bpi2012_deviationsApprox_" + str(chosen_k) + "_" + str(chosen_heuristics) + ".pdf",
                        #bbox_inches='tight')
            plt.clf()

def get_orig_asynchMovesDict(df):
    asynchMovesRelDict = defaultdict(list)
    # print(df.columns)
    asynchMovesRel = df["deviations"].values
    for asynchmoves in asynchMovesRel:
        matchObject = re.search("\{(.+)\}", asynchmoves, flags=0)
        event = matchObject.group(1).split(",")
        for keyValuePair in event:
            key, value = keyValuePair.split("=")
            key = key.strip()
            value = value.strip()
            asynchMovesRelDict[key].append(float(value))
    for key in asynchMovesRelDict.keys():
        asynchMovesRelDict[key] = sum(asynchMovesRelDict[key]) / float(len(asynchMovesRelDict[key]))
    return asynchMovesRelDict


def get_asynchMovesDict(df):
    asynchMovesRelDict = defaultdict(list)
    asynchMovesRel = df["deviations"].values
    for asynchmoves in asynchMovesRel:
        matchObject = re.search("\{(.+)\}", asynchmoves, flags=0)
        if matchObject:
            event = matchObject.group(1).split(",")
            for keyValuePair in event:
                key, value = keyValuePair.split("=")
                key = key.strip()
                value = value.strip()
                asynchMovesRelDict[key].append(float(value))
    return asynchMovesRelDict


def derive_plot_name(input_name):
    if input_name == "BPI_Challenge_2012":
        return "BPI-12"
    if input_name == "Detail_Incident_Activity":
        return "BPI-14"
    if input_name == "Road_Traffic_Fines_Management_Process":
        return "RTF"
    if input_name == "RTFM_model2":
        return "RTFr"


def derive_plot_name_approximation(input_name, k, h):
    if h=="NONALIGNING_ALL":
        h="all dev"
    elif h=="NONALIGNING_KNOWN":
        h="known dev"
    if input_name == "BPI_Challenge_2012":
        return "BPI-12: k=" + str(k) + ", " + str(h)
    if input_name == "Detail_Incident_Activity":
        return "BPI-14: k=" + str(k) + ", " + str(h)
    if input_name == "Road_Traffic_Fines_Management_Process":
        return "RTF: k=" + str(k) + ", " + str(h)
    if input_name == "RTFM_model2":
        return "RTFr: k=" + str(k) + ", " + str(h)


if __name__ == "__main__":
    main("BPI_Challenge_2012")
