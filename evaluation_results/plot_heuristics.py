import pandas as pd
import matplotlib.pyplot as plt
import matplotlib.patches as mpatches
import os


def main(input_name):
    font = {'font.family': 'normal',
            # 'font.weight' : 'bold',
            'font.size': 18}
    plt.rcParams.update(font)

    blue_patch = mpatches.Patch(color='blue', label='Original')

    # build a dataframe for each result file
    bpi2012_deviations = pd.read_csv(os.path.join(".", "results", str(input_name) + "_deviations.csv"),
                                     sep=';')
    bpi2012_deviationsApprox = pd.read_csv(
        os.path.join(".", "results", str(input_name) + "_deviationsApprox.csv"), sep=';')
    bpi2012_prefixsuffix = pd.read_csv(os.path.join(".", "results", str(input_name) + "_prefixsuffix.csv"),
                                       sep=';')

    # set parameters for original-to-implementation comparison for all three logs
    chosen_delta = 0.01
    chosen_alpha = 0.99
    chosen_epsilon = 0.01

    bpi2012_deviations_mean = bpi2012_deviations.loc[
        (bpi2012_deviations["delta"] == chosen_delta) & (bpi2012_deviations["alpha"] == chosen_alpha) & (
                bpi2012_deviations["epsilon"] == chosen_epsilon)]
    print(bpi2012_deviations_mean["time"].values)
    bpi2012_deviations_mean = bpi2012_deviations_mean["time"].mean()

    bpi2012_nonaligning_all = bpi2012_deviationsApprox.loc[
        (bpi2012_deviationsApprox["delta"] == chosen_delta) & (bpi2012_deviationsApprox["alpha"] == chosen_alpha) & (
                bpi2012_deviationsApprox["epsilon"] == chosen_epsilon) & (
                bpi2012_deviationsApprox["approximationMode"] == "NONALIGNING_ALL")]
    bpi2012_nonaligning_known = bpi2012_deviationsApprox.loc[
        (bpi2012_deviationsApprox["delta"] == chosen_delta) & (bpi2012_deviationsApprox["alpha"] == chosen_alpha) & (
                bpi2012_deviationsApprox["epsilon"] == chosen_epsilon) & (
                bpi2012_deviationsApprox["approximationMode"] == "NONALIGNING_KNOWN")]
    bpi2012_prefixsuffix = bpi2012_prefixsuffix.loc[
        (bpi2012_prefixsuffix["delta"] == chosen_delta) & (bpi2012_prefixsuffix["alpha"] == chosen_alpha) & (
                bpi2012_prefixsuffix["epsilon"] == chosen_epsilon) & (
                bpi2012_prefixsuffix["approximationMode"] == "PREFIXSUFFIX")]

    k_nonaligning_list = [0.1, 0.2, 0.3]
    k_prefixsuffix_list = [0.01, 0.05, 0.1]

    # approximation_time_mean=bpi2012_deviations[bpi2012_deviations["k"]==float(chosen_k)]
    # approximation_time_mean=approximation_time_mean["time"].mean()

    nonaligning_all_list = []
    nonaligning_known_list = []
    prefixsuffix_list = []
    for i in range(0, 3):
        nonaligning_all = bpi2012_nonaligning_all.loc[bpi2012_nonaligning_all["k"] == float(k_nonaligning_list[i])]
        nonaligning_all_list.append(nonaligning_all["time"].values / bpi2012_deviations_mean)
        print("Normal mean: "+str(bpi2012_deviations_mean))
        print("Nonaligning all time: "+str(nonaligning_all["time"].values))
        nonaligning_known = bpi2012_nonaligning_known.loc[
            bpi2012_nonaligning_known["k"] == float(k_nonaligning_list[i])]
        nonaligning_known_list.append(nonaligning_known["time"].values / bpi2012_deviations_mean)

        prefixsuffix = bpi2012_prefixsuffix.loc[bpi2012_prefixsuffix["k"] == float(k_prefixsuffix_list[i])]
        prefixsuffix_list.append(prefixsuffix["time"].values / bpi2012_deviations_mean)

    if input_name=="BPI_Challenge_2012":
        f, (ax1, ax2, ax3,) = plt.subplots(1, 3, sharey=True)
        ax2.set_title('2: all dev', fontsize=18)
        ax2.set_xlabel('k', fontsize=18)
        ax2.boxplot(nonaligning_all_list)
        ax2.set_xticklabels(k_nonaligning_list, rotation='horizontal', fontsize=14)
        ax2.axhline(1, color='b', linestyle='--')

        ax3.set_title('3: known dev', fontsize=18)
        ax3.set_xlabel('k', fontsize=18)
        ax3.boxplot(nonaligning_known_list)
        ax3.set_xticklabels(k_nonaligning_list, rotation='horizontal', fontsize=14)
        ax3.axhline(1, color='b', linestyle='--')

        ax1.set_title('1: pref.-suff.', fontsize=18)
        ax1.set_xlabel('k', fontsize=18)
        ax1.boxplot(prefixsuffix_list)
        ax1.set_xticklabels(k_prefixsuffix_list, rotation='horizontal', fontsize=14)
        ax1.axhline(1, color='b', linestyle='--')
        ax1.set_ylabel('Runtime (rel.)', fontsize=18)

        #plt.show()
        f.savefig(os.path.join(".", input_name, str(input_name)+"_approximation_comparison_time.pdf"), bbox_inches='tight')

        nonaligning_all_list = []
        nonaligning_known_list = []
        prefixsuffix_list = []
        for i in range(0, 3):
            nonaligning_all = bpi2012_nonaligning_all.loc[bpi2012_nonaligning_all["k"] == float(k_nonaligning_list[i])]
            nonaligning_all_list.append(((nonaligning_all["approximated"].values) / nonaligning_all["variants"].values))

            nonaligning_known = bpi2012_nonaligning_known.loc[
                bpi2012_nonaligning_known["k"] == float(k_nonaligning_list[i])]
            nonaligning_known_list.append(
                ((nonaligning_known["approximated"].values) / nonaligning_known["variants"].values))

            prefixsuffix = bpi2012_prefixsuffix.loc[bpi2012_prefixsuffix["k"] == float(k_prefixsuffix_list[i])]
            prefixsuffix_list.append(((prefixsuffix["approximated"].values) / prefixsuffix["variants"].values))

        f, (ax1, ax2, ax3) = plt.subplots(1, 3, sharey=True)
        ax2.set_title('2: all dev', fontsize=18)
        ax2.set_xlabel('k', fontsize=18)
        ax2.boxplot(nonaligning_all_list)
        ax2.set_xticklabels(k_nonaligning_list, rotation='horizontal', fontsize=14)

        ax3.set_title('3: known dev', fontsize=18)
        ax3.set_xlabel('k', fontsize=18)
        ax3.boxplot(nonaligning_known_list)
        ax3.set_xticklabels(k_nonaligning_list, rotation='horizontal', fontsize=14)

        ax1.set_title('1: pref.-suff.', fontsize=18)
        ax1.set_xlabel('k', fontsize=18)
        ax1.boxplot(prefixsuffix_list)
        ax1.set_xticklabels(k_prefixsuffix_list, rotation='horizontal', fontsize=14)
        ax1.set_ylabel('Approximated trace variants (rel.)', fontsize=18)

        #plt.show()
        f.savefig(os.path.join(".", input_name, str(input_name)+"_approximation_comparison_approximated_variants.pdf"), bbox_inches='tight')
    else:
        f, (ax1, ax2) = plt.subplots(1, 2, sharey=True)
        ax1.set_title('2: all dev', fontsize=18)
        ax1.set_xlabel('k', fontsize=18)
        ax1.boxplot(nonaligning_all_list)
        ax1.set_xticklabels(k_nonaligning_list, rotation='horizontal', fontsize=14)
        ax1.axhline(1, color='b', linestyle='--')
        ax1.set_ylabel('Runtime (rel.)', fontsize=18)


        ax2.set_title('3: known dev', fontsize=18)
        ax2.set_xlabel('k', fontsize=18)
        ax2.boxplot(nonaligning_known_list)
        ax2.set_xticklabels(k_nonaligning_list, rotation='horizontal', fontsize=14)
        ax2.axhline(1, color='b', linestyle='--')

        # plt.show()
        f.savefig(os.path.join(".", input_name, str(input_name) + "_approximation_comparison_time.pdf"),
                  bbox_inches='tight')

        nonaligning_all_list = []
        nonaligning_known_list = []
        prefixsuffix_list = []
        for i in range(0, 3):
            nonaligning_all = bpi2012_nonaligning_all.loc[bpi2012_nonaligning_all["k"] == float(k_nonaligning_list[i])]
            nonaligning_all_list.append(((nonaligning_all["approximated"].values) / nonaligning_all["variants"].values))

            nonaligning_known = bpi2012_nonaligning_known.loc[
                bpi2012_nonaligning_known["k"] == float(k_nonaligning_list[i])]
            nonaligning_known_list.append(
                ((nonaligning_known["approximated"].values) / nonaligning_known["variants"].values))

            prefixsuffix = bpi2012_prefixsuffix.loc[bpi2012_prefixsuffix["k"] == float(k_prefixsuffix_list[i])]
            prefixsuffix_list.append(((prefixsuffix["approximated"].values) / prefixsuffix["variants"].values))

        f, (ax1, ax2) = plt.subplots(1, 2, sharey=True)
        ax1.set_title('2: all dev', fontsize=18)
        ax1.set_xlabel('k', fontsize=18)
        ax1.boxplot(nonaligning_all_list)
        ax1.set_xticklabels(k_nonaligning_list, rotation='horizontal', fontsize=14)
        ax1.set_ylabel('Approximated trace variants (rel.)', fontsize=18)


        ax2.set_title('3: known dev', fontsize=18)
        ax2.set_xlabel('k', fontsize=18)
        ax2.boxplot(nonaligning_known_list)
        ax2.set_xticklabels(k_nonaligning_list, rotation='horizontal', fontsize=14)
        # plt.show()
        f.savefig(
            os.path.join(".", input_name, str(input_name) + "_approximation_comparison_approximated_variants.pdf"),
            bbox_inches='tight')

if __name__ == "__main__":
    main("BPI_Challenge_2012")
