# PEARL
### PEARL: An Effective Argument Recommendation Approach

Learning and remembering to use APIs are difficult. Several techniques have been proposed to assist developers in using APIs. Most existing techniques focus on recommending the right API methods to call, but very few techniques focus on recommending API arguments. In this paper, we propose PEARL, a novel automated argument recommendation approach which suggests arguments by predicting developers’ expectations when they define and use API methods. To implement this idea in the recommendation process, PEARL combines program analysis (PA), language models (LMs), and several features specialized for the recommendation task which consider the functionality of formal parameters and the positional information of code elements (e.g., variables or method calls) in the given context. In PEARL, the LMs and the recommending features are used to suggest the promising candidates identified by PA. Meanwhile, PA navigates the LMs and the features working on the set of valid candidates which satisfy syntax, accessibility, and type-compatibility constraints defined by the programming language in use. Our empirical evaluation on a large dataset of real-world projects shows that PEARL improves the state-of-the-art approach by 19% and 18% in top-1 precision and recall for recommending arguments of frequently-used libraries. For general argument recommendation task, i.e., recommending arguments for every method call, PEARL outperforms the baseline approaches by up to 125% top-1 accuracy. Moreover, for newly-encountered projects, PEARL achieves more than 60% top-3 accuracy when evaluating on a larger dataset. For working/maintaining projects, with a personalized LM to capture developers’ coding practice, PEARL can productively rank the expected arguments at the top-1 position in 7/10 requests while still preserving the privacy of developers.


### Data.
1. List of 1000 most starred project in the empirical study section [here](https://github.com/ttrangnguyen/PEARL/blob/gh-pages/most_starred_repos.txt)
2. Small corpus: [Eclipse](https://www.eclipse.org/downloads/download.php?file=/eclipse/downloads/drops4/R-4.17-202009021800/eclipse-platform-sources-4.17.tar.xz) and [Netbeans](https://github.com/apache/netbeans/tree/54987ffb73ae9e17b23d4a43a23770142f93206b)
3. [Large corpus](https://github.com/ttrangnguyen/PEARL/blob/gh-pages/large_corpus.txt)

### Source code.
1. [Identify valid candidates](https://github.com/ttrangnguyen/PEARL/tree/gh-pages/PEARL-program-analysis)
2. [Reduce candidates](https://github.com/ttrangnguyen/PEARL/tree/gh-pages/PEARL-local-model)
3. [Rank candidates](https://github.com/ttrangnguyen/PEARL/tree/gh-pages/PEARL-global-model)


### Experimental results
1. Statistics of the dataset

|              | Small corpus       | Large corpus |
|--------------|-------------------:|-------------:|
| #Projects    | Eclipse & Netbeans |        9,271 |
| #Files       |             53,787 |      961,493 |
| #LOCs        |          7,218,637 |   84,236,829 |
| #AR requests |            700,696 |      913,175 |

2. Accuracy Comparison (RQ1) 

2.1 Performance of the AR approaches for the methods in the frequently-used libraries


<table class="tg">
<thead>
  <tr>
    <th class="tg-c3ow" colspan="2" rowspan="2">Project</th>
    <th class="tg-c3ow" colspan="2">PEARL</th>
    <th class="tg-c3ow" colspan="2">PARC</th>
    <th class="tg-c3ow" colspan="2">GPT-2</th>
    <th class="tg-c3ow" colspan="2">SLP</th>
  </tr>
  <tr>
    <th class="tg-dvpl">Precision</th>
    <th class="tg-dvpl">Recall</th>
    <th class="tg-dvpl">Precision</th>
    <th class="tg-dvpl">Recall</th>
    <th class="tg-dvpl">Precision</th>
    <th class="tg-dvpl">Recall</th>
    <th class="tg-dvpl">Precision</th>
    <th class="tg-dvpl">Recall</th>
  </tr>
</thead>
<tbody>
  <tr>
    <td class="tg-0pky" rowspan="3">Netbeans</td>
    <td class="tg-0pky">Top-1</td>
    <td class="tg-dvpl">52.92%</td>
    <td class="tg-dvpl">51.67%</td>
    <td class="tg-dvpl">46.46%</td>
    <td class="tg-dvpl">44.86%</td>
    <td class="tg-dvpl">47.72%</td>
    <td class="tg-dvpl">46.63%</td>
    <td class="tg-dvpl">36.04%</td>
    <td class="tg-dvpl">36.04%</td>
  </tr>
  <tr>
    <td class="tg-0pky">Top-3</td>
    <td class="tg-dvpl">70.18%</td>
    <td class="tg-dvpl">68.28%</td>
    <td class="tg-dvpl">66.20%</td>
    <td class="tg-dvpl">66.75%</td>
    <td class="tg-dvpl">55.15%</td>
    <td class="tg-dvpl">53.90%</td>
    <td class="tg-dvpl">49.52%</td>
    <td class="tg-dvpl">49.52%</td>
  </tr>
  <tr>
    <td class="tg-0pky">Top-10</td>
    <td class="tg-dvpl">78.36%</td>
    <td class="tg-dvpl">76.15%</td>
    <td class="tg-dvpl">72.06%</td>
    <td class="tg-dvpl">69.57%</td>
    <td class="tg-dvpl">55.94%</td>
    <td class="tg-dvpl">54.67%</td>
    <td class="tg-dvpl">64.52%</td>
    <td class="tg-dvpl">64.52%</td>
  </tr>
  <tr>
    <td class="tg-0pky" rowspan="3">Eclipse</td>
    <td class="tg-0pky">Top-1</td>
    <td class="tg-dvpl">56.66%</td>
    <td class="tg-dvpl">55.04%</td>
    <td class="tg-dvpl">47.65%</td>
    <td class="tg-dvpl">46.65%</td>
    <td class="tg-dvpl">61.37%</td>
    <td class="tg-dvpl">58.87%</td>
    <td class="tg-dvpl">26.24%</td>
    <td class="tg-dvpl">26.24%</td>
  </tr>
  <tr>
    <td class="tg-0pky">Top-3</td>
    <td class="tg-dvpl">67.88%</td>
    <td class="tg-dvpl">65.63%</td>
    <td class="tg-dvpl">65.05%</td>
    <td class="tg-dvpl">63.68%</td>
    <td class="tg-dvpl">68.85%</td>
    <td class="tg-dvpl">66.03%</td>
    <td class="tg-dvpl">37.00%</td>
    <td class="tg-dvpl">37.00%</td>
  </tr>
  <tr>
    <td class="tg-0pky">Top-10</td>
    <td class="tg-dvpl">73.14%</td>
    <td class="tg-dvpl">70.76%</td>
    <td class="tg-dvpl">72.26%</td>
    <td class="tg-dvpl">70.73%</td>
    <td class="tg-dvpl">69.75%</td>
    <td class="tg-dvpl">66.85%</td>
    <td class="tg-dvpl">54.39%</td>
    <td class="tg-dvpl">54.39%</td>
  </tr>
</tbody>
</table>


2.2. Comparison in general AR task

<table>
<thead>
  <tr>
    <th>Project</th>
    <th></th>
    <th>PEARL</th>
    <th>GPT-2</th>
    <th>CodeT5</th>
    <th>SLP</th>
  </tr>
</thead>
<tbody>
  <tr>
    <td rowspan="5">Netbeans</td>
    <td>Top-1</td>
    <td>65.15%</td>
    <td>52.63%</td>
    <td>59.97%</td>
    <td>34.91%</td>
  </tr>
  <tr>
    <td>Top-3</td>
    <td>78.16%</td>
    <td>57.69%</td>
    <td>67.16%</td>
    <td>48.10%</td>
  </tr>
  <tr>
    <td>Top-5</td>
    <td>81.10%</td>
    <td>57.87%</td>
    <td>67.57%</td>
    <td>55.02%</td>
  </tr>
  <tr>
    <td>Top-10</td>
    <td>83.53%</td>
    <td>57.88%</td>
    <td>67.60%</td>
    <td>67.20%</td>
  </tr>
  <tr>
    <td>MRR</td>
    <td>0.72</td>
    <td>0.55</td>
    <td>0.63</td>
    <td>0.44</td>
  </tr>
  <tr>
    <td rowspan="5">Eclipse</td>
    <td>Top-1</td>
    <td>64.19%</td>
    <td>56.53%</td>
    <td>61.20%</td>
    <td>28.52%</td>
  </tr>
  <tr>
    <td>Top-3</td>
    <td>76.29%</td>
    <td>61.89%</td>
    <td>67.21%</td>
    <td>41.60%</td>
  </tr>
  <tr>
    <td>Top-5</td>
    <td>79.23%</td>
    <td>62.09%</td>
    <td>67.53%</td>
    <td>49.46%</td>
  </tr>
  <tr>
    <td>Top-10</td>
    <td>81.65%</td>
    <td>62.10%</td>
    <td>67.54%</td>
    <td>62.67%</td>
  </tr>
  <tr>
    <td>MRR</td>
    <td>0.70</td>
    <td>0.59</td>
    <td>0.64</td>
    <td>0.38</td>
  </tr>
</tbody>
</table>

3. Sensitivity analysis

3.1. Top-𝑘 accuracy of PEARL in different scenarios

<table>
<thead>
  <tr>
    <th></th>
    <th>New project</th>
    <th>Working project</th>
    <th>Maintain project</th>
  </tr>
</thead>
<tbody>
  <tr>
    <td>Top-1</td>
    <td>53.42%</td>
    <td>69.96%</td>
    <td>74.49%</td>
  </tr>
  <tr>
    <td>Top-3</td>
    <td>61.50%</td>
    <td>81.14%</td>
    <td>83.23%</td>
  </tr>
  <tr>
    <td>Top-5</td>
    <td>64.21%</td>
    <td>83.74%</td>
    <td>85.38%</td>
  </tr>
  <tr>
    <td>Top-10</td>
    <td>67.96%</td>
    <td>85.88%</td>
    <td>87.38%</td>
  </tr>
  <tr>
    <td>MRR</td>
    <td>0.58</td>
    <td>0.76</td>
    <td>0.79</td>
  </tr>
</tbody>
</table>

3.2. PEARL’s performance by the expression types of expected arguments

<table>
<thead>
  <tr>
    <th>Expression type</th>
    <th>Distribution (%)</th>
    <th>Top-1 (%)</th>
  </tr>
</thead>
<tbody>
  <tr>
    <td>Simple Name</td>
    <td>48.14</td>
    <td>83.66</td>
  </tr>
  <tr>
    <td>Method Invocation</td>
    <td>15.19</td>
    <td>45.51</td>
  </tr>
  <tr>
    <td>Field Access</td>
    <td>6.09</td>
    <td>31.01</td>
  </tr>
  <tr>
    <td>Array Access</td>
    <td>0.74</td>
    <td>53.26</td>
  </tr>
  <tr>
    <td>Cast Expr</td>
    <td>0.99</td>
    <td>18.46</td>
  </tr>
  <tr>
    <td>String Literal</td>
    <td>10.03</td>
    <td>98.14</td>
  </tr>
  <tr>
    <td>Number Literal</td>
    <td>5.06</td>
    <td>95.66</td>
  </tr>
  <tr>
    <td>Character Literal</td>
    <td>0.47</td>
    <td>87.93</td>
  </tr>
  <tr>
    <td>Type Literal</td>
    <td>0.90</td>
    <td>81.92</td>
  </tr>
  <tr>
    <td>Bool Literal</td>
    <td>1.50</td>
    <td>78.43</td>
  </tr>
  <tr>
    <td>Null Literal</td>
    <td>0.79</td>
    <td>84.45</td>
  </tr>
  <tr>
    <td>Object Creation</td>
    <td>2.09</td>
    <td>51.96</td>
  </tr>
  <tr>
    <td>Array Creation</td>
    <td>0.29</td>
    <td>43.14</td>
  </tr>
  <tr>
    <td>This Expr</td>
    <td>1.06</td>
    <td>91.05</td>
  </tr>
  <tr>
    <td>Super Expr</td>
    <td>0.00</td>
    <td>0.00</td>
  </tr>
  <tr>
    <td>Compound Expr</td>
    <td>5.65</td>
    <td>3.69</td>
  </tr>
  <tr>
    <td>Lamda Expr</td>
    <td>0.73</td>
    <td>78.83</td>
  </tr>
  <tr>
    <td>Method Reference</td>
    <td>0.28</td>
    <td>0.56</td>
  </tr>
  <tr>
    <td>Total</td>
    <td>100.00</td>
    <td>69.96</td>
  </tr>
</tbody>
</table>


3.3.  Impact of Context Length on Performance

<table>
<thead>
  <tr>
    <th></th>
    <th>l1</th>
    <th>l2</th>
    <th>l3</th>
    <th>l4</th>
    <th>l5</th>
  </tr>
</thead>
<tbody>
  <tr>
    <td>Top-1 (%)</td>
    <td>62.05</td>
    <td>65.86</td>
    <td>66.14</td>
    <td>67.00</td>
    <td>67.83</td>
  </tr>
  <tr>
    <td>MRR</td>
    <td>0.70</td>
    <td>0.72</td>
    <td>0.72</td>
    <td>0.73</td>
    <td>0.74</td>
  </tr>
  <tr>
    <td>Run. time (s)</td>
    <td>0.33</td>
    <td>0.39</td>
    <td>0.42</td>
    <td>0.51</td>
    <td>0.56</td>
  </tr>
</tbody>
</table>


4. Intrinsic Evaluation Results
 
4.1. Impact of Valid Candidate Identification


<table>
<thead>
  <tr>
    <th></th>
    <th>Top-1 (%)</th>
    <th>MRR</th>
    <th>Run. time (s)</th>
  </tr>
</thead>
<tbody>
  <tr>
    <td>ON</td>
    <td>69.96</td>
    <td>0.76</td>
    <td>0.444</td>
  </tr>
  <tr>
    <td>OFF</td>
    <td>47.50</td>
    <td>0.51</td>
    <td>0.809</td>
  </tr>
</tbody>
</table>

4.2. Impact of Candidate Reduction
<table>
<thead>
  <tr>
    <th></th>
    <th>Top-1 (%)</th>
    <th>MRR</th>
    <th>Run. time (s)</th>
  </tr>
</thead>
<tbody>
  <tr>
    <td>ON</td>
    <td>69.96</td>
    <td>0.76</td>
    <td>0.444</td>
  </tr>
  <tr>
    <td>OFF</td>
    <td>61.98</td>
    <td>0.69</td>
    <td>2.424</td>
  </tr>
</tbody>
</table>

4.3. Impact of reducing threshold, 𝑅𝑇

<table>
<thead>
  <tr>
    <th>RT</th>
    <th>10</th>
    <th>20</th>
    <th>30</th>
    <th>40</th>
    <th>50</th>
  </tr>
</thead>
<tbody>
  <tr>
    <td>Top-1 (%)</td>
    <td>63.77</td>
    <td>64.67</td>
    <td>65.10</td>
    <td>65.34</td>
    <td>65.49</td>
  </tr>
  <tr>
    <td>Run. time (s)</td>
    <td>0.342</td>
    <td>0.406</td>
    <td>0.418</td>
    <td>0.464</td>
    <td>0.508</td>
  </tr>
</tbody>
</table>

4.4. Impact of heavy-ranking stage

<table>
<thead>
  <tr>
    <th>P_ℎ𝑟</th>
    <th>Top-1 (%)</th>
    <th>MRR</th>
    <th>Run. time (s)</th>
  </tr>
</thead>
<tbody>
  <tr>
    <td>OFF</td>
    <td>65.37</td>
    <td>0.72</td>
    <td>0.125</td>
  </tr>
  <tr>
    <td>GPT-2</td>
    <td>70.71</td>
    <td>0.76</td>
    <td>0.732</td>
  </tr>
  <tr>
    <td>CodeT5</td>
    <td>68.59</td>
    <td>0.74</td>
    <td>0.186</td>
  </tr>
  <tr>
    <td>LSTM</td>
    <td>49.26</td>
    <td>0.61</td>
    <td>0.198</td>
  </tr>
  <tr>
    <td>n-gram</td>
    <td>36.89</td>
    <td>0.51</td>
    <td>0.137</td>
  </tr>
</tbody>
</table>
