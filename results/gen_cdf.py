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

single_runs = [ fo for fo in next(os.walk(folder))[1] if not fo == 'avg' and not fo == 'figure']
cdf_files = ['WAIT_TIME.dat', 'ISVLTN5_RTN_LIFESPAN_COLLISION_PERCENT.dat', 'ORDERR_MISMATCH_BUBBLE.dat', 'PARALLEL_DELTA.dat', 'E2E_RTN_TIME.dat', 'STRETCH_RATIO.dat']

fig_names = {
  'WAIT_TIME.dat' : 'wait_time.png', 
  'ISVLTN5_RTN_LIFESPAN_COLLISION_PERCENT.dat' : 'isolation.png',
  'ORDERR_MISMATCH_BUBBLE.dat' : 'order.png',
  'PARALLEL_DELTA.dat' : 'parallel.png',
  'E2E_RTN_TIME.dat' : 'latency.png',
  'STRETCH_RATIO.dat' : 'stretch.png'}

xlim_max = {
  'E2E_RTN_TIME.dat' : 4000
}

for sub_folder in single_runs:
  figure_folder = folder + '/figure/' + sub_folder
  sub_folder_fp = folder + '/' + sub_folder
  if not os.path.exists(figure_folder):
    os.makedirs(figure_folder)

  if cdf_files is None:
    cdf_files = [ fi for fi in next(os.walk(sub_folder_fp))[2] if fi.endswith(".dat") ]

  for dat_file in cdf_files:
    dat_file_fp = sub_folder_fp + '/' + dat_file
    p1 = SinglePlot('cdf')
    if dat_file in fig_names:
      p1.set_figname(fig_names[dat_file])
    if dat_file in xlim_max:
      p1.set_xlim(0, xlim_max[dat_file])
    p1.generate_plot(dat_file_fp, figure_folder)

