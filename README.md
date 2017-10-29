1 Produce Features for Learning to Rank
Implement your own code that, given a list of retrieval functions F1;F2; : : :FF and a query Q,
1. produces a list of rankings R1;R2; : : :RF ; all for the same query|(one ranking Ri for each
retrieval function Fi);
2. for each document D, produces a feature vector~f for the query-document pair, where each
vector entry fi is reciprocal of the rank rD;i at which the document D can be found in the
i'th ranking Ri;
fi =
(
1
rD;i
if D 2 Ri
0 otherwise
3. represents the feature vector~f for document D and query Q as one line in RankLib format1;
for details see: https://sourceforge.net/p/lemur/wiki/RankLib%20File%20Format/.
Note that relevant documents are indicated by label 1 and non-relevant with label 0.
1actually, this format is called SVMlight format
2
Test your code with this example: For a single query, where only D2;D3;D5 are relevant, consider
the following four document rankings (each ranking corresponding to one ranking function):
R1 R2 R3 R4
D1 D2 D1 D1
D2 D5 D2 D2
D3 D6 D5 D8
D4 D7 D10
D5 D8 D12
D6 D9
D10
D11
1. Produce the corresponding feature data in RankLib le format.
2. Use the feature data to train a RankLib model with Coordinate Ascent optimizing for
MAP { you must use the --qrels option (!)2
3. Measure training performance by applying the learned model to the feature data to produce
a ranking, then measure the MAP of the ranking.
In your report:
 Print the lines in the feature data corresponding to the documents D1 and D2.
 What training performance in MAP does the model achieve?
2 Combining Dierent Ranking Functions
Use code from previous assignments as well as the code above to generate features for the following
retrieval functions F1;F2; : : :FF only considering the top 10 documents in each ranking.3
 F1 = TF-IDF lnc.ltn
 F2 = TF-IDF bnn.bnn
 F3 = LM-U, Unigram language model with Laplace smoothing ( = 1)
 F4 = U-JM, Unigram language model with Jelinek-Mercer smoothing ( = 0:9)
 F5 = U-DS, Unigram language model with Dirichlet smoothing ( = 1000)
(TF-IDF variants given in SMART notation, ddd.qqq)
Please use the Lucene's default stemmer/tokenizer.
Take queries from page id/page name from the test200 benchmark contains a le called
train.test200.cbor.outlines.
1. Create one feature data le across all queries (that is just a single le)
2The option is not documented in the web page, but you nd out about it in the program's usage help.
3documents that are not contained in any top 10 ranking for a query, do not need to be included in the feature
data le.
3
2. Use the feature data le to train and test the RankLib model 5-fold cross validation using
RankLib's option. For training, use Coordinate Ascent optimizing for MAP { you must
use the --qrels option (!)
Answer in your report:
 Which MAP performance does the method achieve?
 How does this compare to the training MAP performance?
 How does this performance compare to retrieval models in previous assignments?
 For the query Brush%20rabbit, display the contents of the top-ranked paragraph.
3 Graduate Students: Other Features
Answer the following questions in your report:
1. If instead of using reciprocal ranks 1
rD;i
as features, you would use the logarithmic query
likelihood score
log score(D;Q) =
X
t2Q
log p(: : :
what value should you assign to the corresponding feature fi when the document is not
contained in the ranking? (Hint: all scores will be negative, your default should be less
than those.)
2. When you use Jelinek-Mercer (interpolated) smoothing, how can you use learning-to-rank
to set the smoothing parameter ?
4
