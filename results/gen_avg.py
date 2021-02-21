from gen_one_plot import SinglePlot
import pandas as pd
import argparse
import os
import sys

def aggregateData(filename, all_paths, seperator='\t'):
  df_result = None
  f_isoln = all_paths[0]
  f_order = all_paths[1]
  df_isoln = pd.read_csv(f_isoln, sep=seperator)
  df_order = pd.read_csv(f_order, sep=seperator)
  df_isoln = df_isoln.rename(columns={'EV': 'EV(incong)', 'WV': 'WV(incong)'})
  df_order = df_order.rename(columns={'EV': 'EV(order)'})
  
  xname = df_isoln.columns[0]
  df_result = pd.concat([df_isoln[[xname, 'EV(incong)', 'WV(incong)']], df_order['EV(order)']], axis=1)
  
  ind = f_isoln.rfind('/')
  df_result.to_csv(f_isoln[:ind + 1] + filename + '.dat', index=False, sep='\t')


parser = argparse.ArgumentParser()
parser.add_argument("-d", "--dir_expr", required=False, help="the path of root directory of the experiment folder destination")
parser.add_argument("-min_line", "--min_line", help="Minimal number of data line to generate a plot", default=4)
parser.add_argument("-dpi", "--dpi", help="Figure dpi, default 100 (low)", default=100)
parser.add_argument("-merge", "--merge_incong_order", 
                    help="whether to merge temporary incongruance and order mismatch.", default=False)

args = parser.parse_args()
folder = args.dir_expr
need_merge = args.merge_incong_order

file_names = ["ISVLTN5_RTN_LIFESPAN_COLLISION_PERCENT.dat", "ORDERR_MISMATCH_BUBBLE.dat"]
full_paths = [ folder + '/avg/' + fn for fn in file_names]

if need_merge:
  aggregateData('MERGE_ISVL_ORDER', full_paths)

# For average graphs 
files = [ fi for fi in next(os.walk(folder + '/avg/'))[2] if fi.endswith(".dat") ]
for file in files:
  full_file_path = folder + '/avg/' + file
  figure_folder  = folder + '/figure/overall/'
  p1 = SinglePlot('avg')
  p1.generate_plot(full_file_path, figure_folder)
