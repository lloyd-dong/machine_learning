#coding-utf-8
import re
import math
import numpy as np
import matplotlib.pylab as plt

def f_file_open(trace_string):
    f=open(trace_string,'r')
    txt=f.readlines()
    f.close()
    return txt
def f_vector_found(txt):
    '''To do: words single, plure, 原形变化 etc.'''
    word_list=[]
    for line in txt:
        line_clean=line.split()
        for word in line_clean:
            if word not in word_list:
                word_list.append(word)
    return word_list

def f_document_vector(document,word_list):
    vector=[]
    document_clean=document.split()
    for word in word_list:
        a=document_clean.count(word)
        vector.append(a)
    return vector

def f_svd_calculate(document_array):
    U,S,V=np.linalg.svd(document_array)
    return (U,S,V)

def f_process_matric_U(matric_U, Save_N_Singualr_value):
    document_matric_U=[]
    for line in matric_U:
        line_new=line[:Save_N_Singualr_value]
        document_matric_U.append(line_new)
    return document_matric_U

def f_process_matric_S(matric_S,Save_information_value):
    """choose the items with large singular value,根据保留信息需求选择奇异值个数"""
    matricS_new=[]
    S_self=0
    N_count=0
    Threshold=sum(matric_S)*float(Save_information_value)
    for value in matric_S:
        if S_self<=Threshold:
            matricS_new.append(value)
            S_self+=value
            N_count+=1
        else:
            break
    print ("the %d largest singular values keep the %s information " %(N_count,Save_information_value))
    return (N_count,matricS_new)

def f_process_matric_V(matric_V,Save_N_Singular_value):
    """according to the matric V, choose the words as the feature in each document,根据前N个奇异值对U进行切分,选择前N行"""
    document_matric_V=matric_V[:Save_N_Singular_value]
    return document_matric_V

def f_combine_U_S_V(matric_u,matric_s,matirc_v):
    """calculate the new document对奇异值筛选后重新计算文档矩阵"""

    new_document_matric=np.dot(np.dot(matric_u,np.diag(matric_s)),matirc_v)
    return new_document_matric

def f_matric_to_document(document_matric,word_list_self):
    """transform the matric to document,将矩阵转换为文档"""
    new_document=[]
    for line in document_matric:
        count=0
        for word in line:
            if float(word)>=0.9:                                                                                     #转换后文档中词选择的阈值
                new_document.append(word_list_self[count]+" ")
            else:
                pass
            count+=1
        new_document.append("\n")
    return new_document


def f_save_file(trace,document):
    f=open(trace,'a')
    for line in document:
        for word in line:
            f.write(word)

trace_open="/Users/bodong/Documents/machine_learning/svd/resource/train.txt "
trace_save="/Users/bodong/Documents/machine_learning/svd/output/psvd_result1.txt"
txt=f_file_open(trace_open)
word_vector=f_vector_found(txt)
print (len(word_vector))

document=[]
Num_line=0
for line in txt:                                #transform the document set to matric
    Num_line=Num_line+1
    document_vector=f_document_vector(line,word_vector)
    document.append(document_vector)
print (len(document))
U,S,V=f_svd_calculate(document)
print (sum(S))
N_count,document_matric_S=f_process_matric_S(S,0.9)
document_matric_U=f_process_matric_U(U,N_count)
document_matric_V=f_process_matric_V(V,N_count)
print (len(document_matric_U[1]))
print (len(document_matric_V))
new_document_matric=f_combine_U_S_V(document_matric_U,document_matric_S,document_matric_V)
print (sorted(new_document_matric[1],reverse=True))
new_document=f_matric_to_document(new_document_matric,word_vector)
f_save_file(trace_save,new_document)
print ("the new document has been saved in %s"%trace_save)
