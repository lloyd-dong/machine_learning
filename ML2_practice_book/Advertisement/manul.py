import pandas as pd
import matplotlib.pyplot as plt
import numpy as np
from sklearn.cross_validation import train_test_split

def regression(data, alpha, lamda):
    n= len(data[0])-1
    theta = np.zeros(n)
    for times in range(100):
        for d in data:
            x= d[:-1]
            y=d[-1]
            g = np.dot(theta,x) - y
            theta = theta-alpha *g*x + lamda* theta
        print times, theta
    return theta

if __name__ == '__main__':
    data = pd.read_csv("Advertising.csv")
    x = data[['TV', 'Radio']]
    y = data['Sales'] - np.average(data['Sales'])
    x_train, x_test, y_train, y_test = train_test_split(x, y, random_state=1)

    data_input = np.hstack((x_train,y_train.reshape(-1,1)))
    regression(data_input, 1, 0)
