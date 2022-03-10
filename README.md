# PEARL
### Practical and Effective Argument Recommendation by Combining Program Analysis and Statistical Language Model

Learning and remembering to use APIs are difficult. Several techniques have been proposed to assist developers in using APIs. Most existing techniques focus on recommending the right API methods to call, very few techniques focus on recommending API arguments. In this paper, we propose a novel automated argument recommendation approach, PEARL, which combines program analysis (PA) and statistical language models (LMs) in the recommendation process. We aim to benefit from both PA and LMs to suggest valid and frequent/natural arguments. In PEARL, the LMs are used to suggest the frequent/natural candidates identified by applying PA. Meanwhile, PA navigates LMs working on the set of valid candidates which satisfy syntax, accessibility, and type-compatibility constraints defined by the programming language in use. Our empirical evaluation on a large dataset of the real-world projects shows that PEARL improves the state-of-the-art approach by 19% and 18% in top-1 precision and recall for recommending arguments of frequently-used libraries. For general argument recommendation task, i.e., recommending arguments for every method call, PEARL outperforms the baseline approaches by up to 31% and 125% top-1 accuracy. Moreover, for newly-encountered projects,PEARL achieves +60% top-3 accuracy when evaluating on a larger dataset. For working/maintaining projects, with a personalized LM to capture developers' coding practice, PEARL can productively rank the expected arguments at the top-1 position in 7/10 requests while preserving the privacy of developers.


### Data.
1. List of 1000 most starred project in the empirical study section [here](https://github.com/ttrangnguyen/PEARL/blob/gh-pages/most_starred_repos.txt)
2. Small corpus: [Eclipse](https://www.eclipse.org/downloads/download.php?file=/eclipse/downloads/drops4/R-4.17-202009021800/eclipse-platform-sources-4.17.tar.xz) and [Netbeans](https://github.com/apache/netbeans/tree/54987ffb73ae9e17b23d4a43a23770142f93206b)
3. [Large corpus](https://github.com/ttrangnguyen/PEARL/blob/gh-pages/large_corpus.txt)

### Source code.
1. [Identify valid candidates](https://github.com/ttrangnguyen/PEARL/tree/gh-pages/PEARL-program-analysis)
2. [Reduce candidates](https://github.com/ttrangnguyen/PEARL/tree/gh-pages/PEARL-local-model)
3. [Rank candidates](https://github.com/ttrangnguyen/PEARL/tree/gh-pages/PEARL-global-model)


### Experimental results
1. [Accuracy comparison](https://github.com/ttrangnguyen/PEARL/blob/gh-pages/Experimental%20results/Accuracy%20comparision.xlsx)
2. [Sensitivity analysis](https://github.com/ttrangnguyen/PEARL/blob/gh-pages/Experimental%20results/Sensitivity%20results.xlsx)
3. [Intrinsic analysis](https://github.com/ttrangnguyen/PEARL/blob/gh-pages/Experimental%20results/Intrinsic%20analysis.xlsx) 


