"""The genderclass model program converted to pandas"""
import pandas as pd
import numpy as np
import os

# in order to analyse the price column I need to bin up that data
# here are my binning parameters the problem we face is some of the
# fares are very large So we can either have a lot of bins with
# nothing in them or we can just absorb some information and just say
# anythng over 30 is just in the last bin so we add a ceiling
fare_ceiling = 40
fare_bracket_size = 10
number_of_fares = fare_ceiling // fare_bracket_size
number_of_classes = 3 #There were 1st, 2nd and 3rd classes on board

def ReadTitanicCsv(filename):
  """Read the data and add a new column BinFare"""
  frame = (pd.read_csv(filename,skipinitialspace=1,index_col=[0])
           # Drop columns not used (not really necessary)
           .drop(['Ticket','Cabin','Embarked'],axis=1))

  # Add the BinFare column
  frame['BinFare'] = ((frame.Fare//fare_bracket_size)
                      .clip_upper(number_of_fares-1)
                      # Use class as substitute if no fare was given
                      .fillna(3-frame.Pclass)
                      .astype(np.int))
  return frame


# Read data and add the bin column
data = ReadTitanicCsv('train.csv')

# This reference table will show we the proportion of survivors as a
# function of Gender, class and ticket fare. I can now find the stats
# of all the women and men on board

# Create a lookup table of survival
survival_table = (data.groupby(['Sex','Pclass','BinFare'])
                  # Extract survival column and take mean value.
                  ['Survived'].mean()
                  # Survive if more than half the people survived.
                  > 0.5).astype(np.int)

# Read the test file
test = ReadTitanicCsv('test.csv')

# Add survival table based on looking up the survival value of
# Sex,Pclass, and BinFare. Use Series.get(key,default) and supply
# 0 (died) if key was not found.
test['Survived'] = (
  test.apply(lambda s: survival_table.get((s.Sex, s.Pclass, s.BinFare),0),
             axis=1))

# Save the result
test[['Survived']].to_csv('genderclassmodel-pandas.csv')


