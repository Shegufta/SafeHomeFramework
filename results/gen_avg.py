from gen_one_plot import SinglePlot
import argparse
import os
import sys

parser = argparse.ArgumentParser()
parser.add_argument("-d", "--dir_expr", required=False, help="the path of root directory of the experiment folder destination")
parser.add_argument("-min_line", "--min_line", help="Minimal number of data line to generate a plot", default=4)
parser.add_argument("-dpi", "--dpi", help="Figure dpi, default 100 (low)", default=100)
args = parser.parse_args()
folder = args.dir_expr

# For average graphs 
files = [ fi for fi in next(os.walk(folder + '/avg/'))[2] if fi.endswith(".dat") ]
for file in files:
  full_file_path = folder + '/avg/' + file
  figure_folder  = folder + '/figure/overall/'
  p1 = SinglePlot('avg')
  p1.generate_plot(full_file_path, figure_folder)
