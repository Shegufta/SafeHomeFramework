from gen_one_plot import SinglePlot
import pandas as pd
import argparse
import os
import sys

parser = argparse.ArgumentParser()
parser.add_argument("-d", "--dir_expr", required=False, help="the path of root directory of the experiment folder destination", default='varyCommand')
parser.add_argument("-min_line", "--min_line", help="Minimal number of data line to generate a plot", default=4)
parser.add_argument("-dpi", "--dpi", help="Figure dpi, default 100 (low)", default=100)
args = parser.parse_args()
folder = args.dir_expr

def aggregateData(filename, all_paths, var_values, tmp_data_name=None, seperator='\t'):
  df_result = None

  for full_fname, var_value in zip(all_paths, var_values):
    df = pd.read_csv(full_fname, sep=seperator)
    df = df.rename(columns={"EV": var_value})
    df_result = pd.concat([df_result, df], ignore_index=False, axis=1)

  if tmp_data_name is None:
    tmp_data_name = 'varyCommand/aggregate_sr_for_fig15c.dat'
  df_result.to_csv(tmp_data_name, index=False, sep='\t')


  return df_result
    

single_runs = [ fo for fo in next(os.walk(folder))[1] if not fo == 'avg' and not fo == 'figure']
var_values = ['2.0', '4.0', '8.0']
filename = 'STRETCH_RATIO.dat'
tmp_file_path = folder + '/aggregate_sr_for_fig15c.dat'

full_paths = []
for v in var_values:
  for fo in single_runs:
    if fo.endswith(v):
      full_paths.append(folder + '/' + fo + '/' + filename)

xlim_max = {
  'E2E_RTN_TIME.dat' : 4000
}

df_stretch = aggregateData(filename, full_paths, var_values, tmp_file_path)

figure_folder = folder + '/figure/'
if not os.path.exists(figure_folder):
  os.makedirs(figure_folder)

p1 = SinglePlot('cdf')
p1.set_xlim(0, 20)
p1.set_ylim(0.7, 1)
p1.generate_plot(tmp_file_path, figure_folder)

