from gen_one_plot import SinglePlot
import matplotlib.pyplot as plt
import pandas as pd
import argparse
import os
import sys

def aggregateData(filename, all_paths, seperator='\t', save_tmp_data=False, tmp_data_name=None):
  df_result = None

  for full_fname in all_paths:
    info = full_fname.split('/')
    strategy = info[-4]
    cc_level = info[-3]
    df = pd.read_csv(full_fname, sep=seperator)
    df = df.dropna(how='all', axis=1)
    x_axis = df.columns[1:]
    str_cols = [strategy] * len(df.index)
    cc_cols  = [cc_level] * len(df.index)
    df['strategy'] = str_cols
    df['num_cc_routines'] = cc_cols

    if df_result is None:
      df_result = df.copy()
    else:
      df_result = pd.concat([df_result, df]).reset_index(drop=True)
    
  # Save aggregated data to tmp file
  if save_tmp_data:
    if tmp_data_name is None:
      tmp_data_name = 'fig15ab/aggreagted_' + filename
    df_result.to_csv(tmp_data_name, index=False)

  return df_result

def drawBarPlot(df, figpath, strategies, var2, var1_name):
  var1 = sorted(df[var1_name].unique(), reverse=True)
  row_groups = {}
    
  for v1 in var1:
    for v2 in var2:
      row = []
      for strategy in strategies:
        value = df.loc[(df['num_cc_routines'] == v2) & (df[var1_name] == v1) & (df['strategy'] == strategy)]
        row.extend(value['EV'].to_list())

      data_name = v2 + ', ' + var1_name + '='+ str(v1)
      row_groups[data_name] = row

  df_plot = pd.DataFrame(row_groups, index=strategies)
  ax = df_plot.plot.bar(rot=0)
  ax.yaxis.grid(True)
  plt.savefig(figpath)
  plt.clf()
  plt.close()
  

parser = argparse.ArgumentParser()
parser.add_argument("-d", "--dir_expr", required=False, help="the path of root directory of the experiment folder destination", default='fig15ab/')
parser.add_argument("-min_line", "--min_line", help="Minimal number of data line to generate a plot", default=4)
parser.add_argument("-dpi", "--dpi", help="Figure dpi, default 100 (low)", default=100)
args = parser.parse_args()
root_folder = args.dir_expr

source_dirs = ['fig15ab/preoff/con2/', 'fig15ab/preoff/con4/', 'fig15ab/bothoff/con2/', 'fig15ab/bothoff/con4/', 'fig15ab/bothon/con2/', 'fig15ab/bothon/con4/', 'fig15ab/postoff/con2/', 'fig15ab/postoff/con4/']
strategies = ['bothoff', 'postoff', 'preoff', 'bothon']
var2 = None
figure_folder = 'fig15ab/figure/'
if not os.path.exists(figure_folder):
  os.makedirs(figure_folder)


# Get paths of all data files.
incong_data_source = []
latency_data_source = []

if strategies is None:
    strategies = next(os.walk(root_folder))[1]

if var2 is None:
  sttg_dir = root_folder + strategies[0]
  var2 = sorted(next(os.walk(sttg_dir))[1], reverse=True)

if source_dirs is None:
  source_dir = []
  for strategy in strategies:
    sttg_dir = root_folder + strategy
    var_dirs = next(os.walk(sttg_dir))[1]
    for var_dir in var_dirs:
      bar_file_dir = sttg_dir + '/' + var_dir + '/avg/'
      source_dir.append(sttg_dir + '/' + var_dir + '/')
      incong_data_source.append(bar_file_dir + 'ISVLTN5_RTN_LIFESPAN_COLLISION_PERCENT.dat') 
      latency_data_source.append(bar_file_dir + 'E2E_RTN_TIME.dat')
else:
  for source_dir in source_dirs:
    incong_data_source.append(source_dir + 'avg/ISVLTN5_RTN_LIFESPAN_COLLISION_PERCENT.dat')
    latency_data_source.append(source_dir + 'avg/E2E_RTN_TIME.dat')
    
# Aggregate data
df_incong = aggregateData('ISVLTN5_RTN_LIFESPAN_COLLISION_PERCENT.dat', incong_data_source, save_tmp_data=True)
df_latency = aggregateData('E2E_RTN_TIME.dat', latency_data_source, save_tmp_data=True)

drawBarPlot(df_incong, figure_folder + 'ISVLTN5_RTN_LIFESPAN_COLLISION_PERCENT.png', strategies, var2, 'minCmdCntPerRtn')
drawBarPlot(df_latency, figure_folder + 'E2E_RTN_TIME.png', strategies, var2, 'minCmdCntPerRtn')
