import os

import plot_dataset_comparison
import plot_deviations
import plot_parameter_comparison
import plot_benchmark
import plot_heuristics

def main():
    plot_dataset_comparison.main()
    plot_benchmark.main()

    inputs=["BPI_Challenge_2012","Detail_Incident_Activity", "Road_Traffic_Fines_Management_Process", "RTFM_model2"]
    #inputs=["RTFM_model2"]
    for input_name in inputs:
        os.makedirs(os.path.join(".", input_name), exist_ok=True)

        plot_parameter_comparison.main(input_name)
        plot_deviations.main(input_name)
        plot_heuristics.main(input_name)


if __name__ == "__main__":
    main()