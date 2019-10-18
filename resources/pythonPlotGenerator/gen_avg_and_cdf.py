from gen_one_plot import SinglePlot
import argparse
import os
import sys

parser = argparse.ArgumentParser()
parser.add_argument("-d", "--dir_expr", required=False, help="the path of root directory of the experiment folder destination")
parser.add_argument("-min_line", "--min_line", help="Minimal number of data line to generate a plot", default=4)
parser.add_argument("-dpi", "--dpi", help="Figure dpi, default 100 (low)", default=100)
args = parser.parse_args()
# folder = 'new_format/1570039943920_VARY_minLngRnCmdTimSpn_R_100_C_2.0-5.0'
folder = args.dir_expr

# For average graphs 
files = [ fi for fi in next(os.walk(folder + '/avg/'))[2] if fi.endswith(".dat") ]
# files = ['ISVLTN5_RTN_LIFESPAN_COLLISION_PERCENT.dat']
for file in files:
  full_file_path = folder + '/avg/' + file
  figure_folder  = folder + '/figure/overall/'
  p1 = SinglePlot('avg')
  p1.generate_plot(full_file_path, figure_folder)

single_runs = [ fo for fo in next(os.walk(folder))[1] if not fo == 'avg' and not fo == 'figure']
cdf_files = None
# cdf_files = ['WAIT_TIME.dat', 'ISVLTN5_RTN_LIFESPAN_COLLISION_PERCENT.dat', 'ORDERR_MISMATCH_BUBBLE.dat', 'PARALLEL.dat', 'LATENCY_OVERHEAD.dat']
# fig_names = {
#   'WAIT_TIME.dat' : 'wait_time.png', 
#   'ISVLTN5_RTN_LIFESPAN_COLLISION_PERCENT.dat' : 'isolation.png',
#   'ORDERR_MISMATCH_BUBBLE.dat' : 'order.png',
#   'PARALLEL.dat' : 'parallel.png',
#   'LATENCY_OVERHEAD.dat' : 'latency.png'}

# cdf_files = ['ORDERR_MISMATCH_BUBBLE.dat']
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
    # if dat_file in fig_names:
    #   p1.set_figname(fig_names[dat_file])
    p1.generate_plot(dat_file_fp, figure_folder)



  