import json
import random
import itertools
import argparse
from statistics import stdev, mean

TIME = 0
STATE = 1

class Command:
  dev = "dump"
  state = 0
  duration = 0
  start_time = 0

  def __init__(self, dev, state, duration):
    self.dev = dev
    self.state = state
    self.duration = duration

  def set_start(self, t):
    self.start_time = t

  def print(self):
    print("dev:", self.dev, " state:", self.state, " dur:", self.duration)

  def __str__(self):
    return "dev: " + self.dev + "  state: " + str(self.state) + "  dur: " + str(self.duration)


class Routine:
  rid = 0
  commands = []

  def __init__(self, id, cmds):
    self.rid = id
    self.commands = cmds

  def __str__(self):
    res =  "ID: " + str(self.rid) + "  Commands:\n"
    for cmd in self.commands:
      res += "    " + str(cmd) + "\n"
    return res 

# Python function to print permutations of a given list 
def permutation(lst): 
    if len(lst) == 0: 
        return [] 
    if len(lst) == 1: 
        return [lst] 
  
    l = [] 
    for i in range(len(lst)): 
       m = lst[i] 
       remLst = lst[:i] + lst[i+1:] 
       for p in permutation(remLst): 
           l.append([m] + p) 
    return l 

# check whether the item in first map is all in second map with same value
def isValid(map1, map2):
  for key in map1:
    if key not in map2:
      print("Unexisting key:", key)
      return False
    if map2[key] is not map1[key]:
      return False
  return True

def invalidPerc(map1, map2):
  count = 0
  for key in map1:
    if map2[key] is not map1[key]:
      count += 1
  return count * 1.0 / len(map1)

def getWVState(start_t, routines):
  wv_end_state = {}  # format: <dev: (end_time, state)>

  for i, rtn in enumerate(routines):
    t = start_t[i] # start time of that routine
    for cmd in rtn.commands:
      if cmd.dev in wv_end_state :
        if wv_end_state[cmd.dev][TIME] < t + cmd.duration:
          wv_end_state[cmd.dev] = (t + cmd.duration, i)
      else:
        wv_end_state[cmd.dev] = (t + cmd.duration, i)
      t += cmd.duration

  wv_state = {} # maintain end state only (no time record)
  for dev in wv_end_state:
    wv_state[dev] = wv_end_state[dev][STATE]

  return wv_state

def getMinInvalidLv(wv_state, routines):
  any_valid_perm = False
  min_invalid_level = 1.0
  all_permutations = itertools.permutations(routines)
  for i, perm in enumerate(all_permutations):
    gsv_state = {}
    valid_perm = True
    for i, rtn in reversed(list(enumerate(perm))):     # check from the last routine
      for cmd in rtn.commands:
        if cmd.dev not in gsv_state:
          gsv_state[cmd.dev] = i
    
    min_invalid_level = min(min_invalid_level, invalidPerc(gsv_state, wv_state))
  # print(min_invalid_level)
  return min_invalid_level

def anyValid(wv_state, routines):
  # check whether there is a permutation that is valid.
  any_valid_perm = False
  all_permutations = itertools.permutations(routines)
  for i, perm in enumerate(all_permutations):
    gsv_state = {}
    valid_perm = True
    for i, rtn in reversed(list(enumerate(perm))):     # check from the last routine
      for cmd in rtn.commands:
        if cmd.dev not in gsv_state:
          gsv_state[cmd.dev] = i
     
      if not isValid(gsv_state, wv_state):  # The result of GSV could not be the same with WV
        valid_perm = False
        break
    if valid_perm:
      any_valid_perm = True
      break
  # print(any_valid_perm)
  return any_valid_perm

def endStateIncons(routines, para):
  # Start simulating
  fst_rtn_fixed = para[0]
  lst_rtn_fixed = para[1]
  fst_lst_start_interval = para[2]

  start_t = random.sample(range(fst_lst_start_interval)[1:-1], len(routines))
  if fst_rtn_fixed:
    start_t[0] = 0
  if lst_rtn_fixed: # move the largest time to the end
    max_ind = start_t.index(max(start_t))
    start_t[-1], start_t[max_ind] = start_t[max_ind], start_t[-1]

  return getMinInvalidLv(getWVState(start_t, routines), routines)

def readRtnFromJson(scn):
  fin = open(scn + '.json')
  routines = []
  for rid, rtn in enumerate(json.load(fin)):
    cmds = []
    for cmd in rtn['Routine']:
      dev = cmd.split(':')[0]
      if len(cmd.split(':')) > 2:
        cmds.append(Command(dev, rid, int(cmd.split(':')[2])))
      else:
        cmds.append(Command(dev, rid, random.randint(1, 5)))
    routines.append(Routine(rid, cmds))
  return routines

def compareFactory(num_stage, num_item_per_stage, num_run):
  routines = []
  start_t = []
  for stage in range(num_stage):
    reg_time = random.randint(0, 5)
    for item in range(num_item_per_stage):
      cmds = []
      rid = stage * num_item_per_stage + item
      start_t.append(reg_time)
    
      if stage > 0:
        for i in range(4): # for left common devices
          if random.random() < 0.3:
            duration = random.randint(1, 30)
            reg_time += duration
            cmds.append(Command('com' + str(stage - 1) + str(stage) + str(i), rid, duration))
      
      for i in range(4): # for local devices
        if random.random() < 0.6:
          duration = random.randint(1, 30)
          reg_time += duration  
          cmds.append(Command('loc' + str(stage) + str(i), rid, duration))

      if stage < num_stage - 1:
        for i in range(4): # for common devices
          if random.random() < 0.3:
            duration = random.randint(1, 30)
            reg_time += duration
            cmds.append(Command('com' + str(stage) + str(stage + 1) + str(i), rid, duration))

      for i in range(5): # for global devices
        if random.random() < 0.1:
          duration = random.randint(1, 30)
          reg_time += duration  
          cmds.append(Command('glo' + str(i), rid, duration))

      routines.append(Routine(rid, cmds))

  incons_level = []
  for i in range(num_run):
    print("fac" + str(num_stage) + str(num_item_per_stage) + "run", i)
    incons_level.append(endStateIncons(routines, para[scn_ind]))
  return incons_level
  
parser = argparse.ArgumentParser()
parser.add_argument("-nrun", "--num_run", default=20)
parser.add_argument("-output", "--output", help="output file path", default='finalIncong.txt')
args = parser.parse_args()
num_run = int(args.num_run)
out_file = args.output

scns = ['morning', 'party']
para = [[True, True, 750], [True, False, 7200]]
fout = open(out_file, 'w+')
fout.write("scenario,mean,stdev\n")

for scn_ind, scn in enumerate(scns):
  routines = readRtnFromJson(scn)
  
  incons_level = []
  for i in range(num_run):
    print(scn + "run", i)
    incons_level.append(endStateIncons(routines, para[scn_ind]))
  fout.write(scn + "," + str(mean(incons_level)) + "," + str(stdev(incons_level)) + "\n")

incons_level = compareFactory(3, 3, num_run) # num_stage, num_item_per_stage
fout.write("factory," + str(mean(incons_level)) + "," + str(stdev(incons_level)) + "\n")

incons_level = compareFactory(9, 1, num_run) # num_stage, num_item_per_stage
fout.write("factory91," + str(mean(incons_level)) + "," + str(stdev(incons_level)) + "\n")
