/**
 * Implementation of Unigram Language Model for Laplace smoothing for ranking documents.
 *
 * @author (Bindu Kumari)
 */
package edu.unh.cs753853.team1;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.similarities.BasicStats;
import org.apache.lucene.search.similarities.SimilarityBase;
import org.apache.lucene.store.FSDirectory;

import edu.unh.cs.treccartool.Data;

class UnigramLanguageModel {
	QueryParser qp;
	IndexSearcher is;
	IndexReader ir;
	int maxResults;
	HashMap<String, HashMap<String, Float>> results;
	ArrayList<String> runfilestrings;

	/***** To compare the document **/

	PriorityQueue<DocumentResults> docQueue = new PriorityQueue<DocumentResults>(new Comparator<DocumentResults>() {
		@Override
		public int compare(DocumentResults d1, DocumentResults d2) {
			if (d1.score < d2.score)
				return 1;
			if (d1.score > d2.score)
				return -1;
			return 0;
		}
	});

	/*** Get the size of the vocabulary */
	private static int getSizeOfVocabulary(ArrayList<String> unigramList) {
		ArrayList<String> list = new ArrayList<String>();
		Set<String> hs = new HashSet<>();

		hs.addAll(unigramList);
		list.addAll(hs);
		return list.size();
	}

	/*** Unigram Analyzer */

	private static ArrayList<String> analyzeByUnigram(String inputStr) throws IOException {
		Reader reader = new StringReader(inputStr);

		ArrayList<String> strList = new ArrayList<String>();
		Analyzer analyzer = new UnigramAnalyzer();
		TokenStream tokenizer = analyzer.tokenStream("content", inputStr);

		CharTermAttribute charTermAttribute = tokenizer.addAttribute(CharTermAttribute.class);
		tokenizer.reset();
		while (tokenizer.incrementToken()) {
			String token = charTermAttribute.toString();
			strList.add(token);

		}
		tokenizer.end();
		tokenizer.close();
		return strList;
	}

	class DocumentResults {
		String paraId;
		float score;

		DocumentResults(String pid, float s) {
			paraId = pid;
			score = s;
		}
	}

	/**
	 * Constructor for the ranking system.
	 * 
	 * @param pagelist
	 *            the list of pages to rank
	 */
	UnigramLanguageModel(ArrayList<Data.Page> pagelist, int numResults) throws IOException {
		runfilestrings = new ArrayList<>();
		results = new HashMap<>();
		maxResults = numResults;
		qp = new QueryParser("parabody", new StandardAnalyzer());
		is = new IndexSearcher(
				DirectoryReader.open((FSDirectory.open(new File(QueryParagraphs.INDEX_DIRECTORY).toPath()))));
		ir = is.getIndexReader();
		float sumTotalTermFreq = ir.getSumTotalTermFreq("parabody");
		SimilarityBase custom = new SimilarityBase() {
			protected float score(BasicStats stats, float freq, float docLen) {

				return (float) ((freq + 1 / docLen));
			}

			@Override
			public String toString() {
				return null;
			}
		};
		is.setSimilarity(custom);

		// For each page in the list
		for (Data.Page page : pagelist) {
			// For every term in the query

			String queryId = page.getPageId();
			if (!results.containsKey(queryId)) {
				results.put(queryId, new HashMap<String, Float>());
			}
			for (String term : page.getPageName().split(" ")) {
				Term t = new Term("parabody", term);
				TermQuery tQuery = new TermQuery(t);

				TopDocs topDocs = is.search(tQuery, maxResults);
				float totalTermFreq = ir.totalTermFreq(t);
				ScoreDoc[] scores = topDocs.scoreDocs;
				for (int i = 0; i < topDocs.scoreDocs.length; i++) {

					Document doc = is.doc(scores[i].doc);
					String paraId = doc.get("paraid");
					String docBody = doc.get("parabody");
					ArrayList<String> unigram_list = analyzeByUnigram(docBody);
					int size_of_voc = getSizeOfVocabulary(unigram_list);
					int size_of_doc = unigram_list.size();

					if(paraId.equals("0760e843e1c62c7aeb1c21f994f05992876aa0a1"))
					{
						System.out.println("UL");
						System.out.println(docBody);
					}

					if (!results.get(queryId).containsKey(paraId)) {
						results.get(queryId).put(paraId, 0.0f);
					}
					float score = results.get(queryId).get(paraId);
					score += (float) ((scores[i].score / (size_of_doc + size_of_voc)));
					results.get(queryId).put(paraId, score);
				}
			}
		}

		for (Map.Entry<String, HashMap<String, Float>> queryResult : results.entrySet()) {
			String queryId = queryResult.getKey();
			HashMap<String, Float> paraResults = queryResult.getValue();

			for (Map.Entry<String, Float> paraResult : paraResults.entrySet()) {
				String paraId = paraResult.getKey();
				float score = paraResult.getValue();
				DocumentResults docResult = new DocumentResults(paraId, score);
				docQueue.add(docResult);
			}
			DocumentResults docResult;
			int count = 0;
			while ((docResult = docQueue.poll()) != null) {
				runfilestrings.add(
						queryId + "  Q0 " + docResult.paraId + " " + count + " " + docResult.score + " team1-UL-L");
				count++;
				if (count >= 100)
					break;
			}
			docQueue.clear();
		}
	}

	ArrayList<String> getResults() {
		return runfilestrings;
	}
}