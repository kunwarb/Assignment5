package edu.unh.cs753853.team1;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexOptions;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.similarities.BasicStats;
import org.apache.lucene.search.similarities.SimilarityBase;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;

import co.nstant.in.cbor.CborException;
import edu.unh.cs.treccartool.Data;
import edu.unh.cs.treccartool.read_data.DeserializeData;
import edu.unh.cs.treccartool.read_data.DeserializeData.RuntimeCborException;

class DocumentResults1 {
    String paraId;
    float score;

    DocumentResults1(String pid, float s)
    {
        paraId = pid;
        score = s;
    }
}

/**
 * Implements the Jelinek-Mercer Language Model for ranking documents.
 *
 * @author amf2015 (Austin Fishbaugh)
 */
class LanguageModel_UJM {
    HashMap<String, HashMap<String, Float>> results;
    ArrayList<String> runfilestrings;
    QueryParser qp;
    IndexSearcher is;
    IndexReader ir;
    int maxResults;

    PriorityQueue<DocumentResults1> docQueue = new PriorityQueue<DocumentResults1>(new Comparator<DocumentResults1>() {
       @Override
       public int compare(DocumentResults1 d1, DocumentResults1 d2)
       {
           if(d1.score < d2.score)
               return 1;
           if(d1.score > d2.score)
               return -1;
           return 0;
       }
    });

    /**
     * Constructor for the ranking system.
     * @param pagelist the list of pages to rank
     */
    LanguageModel_UJM(ArrayList<Data.Page> pagelist, int numResults) throws IOException
    {
        runfilestrings = new ArrayList<>();
        results = new HashMap<>();
        maxResults = numResults;
        qp = new QueryParser("parabody", new StandardAnalyzer());
        is = new IndexSearcher(DirectoryReader.open((FSDirectory.open(new File("index").toPath()))));
        ir = is.getIndexReader();
        float sumTotalTermFreq = ir.getSumTotalTermFreq("parabody");
        SimilarityBase bnn = new SimilarityBase() {
			protected float score(BasicStats stats, float freq, float docLen) {
			    return (float)(0.9*(freq/docLen));
			}
			@Override
			public String toString() {
				return null;
			}
		};
		is.setSimilarity(bnn);

        // For each page in the list
        for(Data.Page page : pagelist) {
            // For every term in the query
            String queryId = page.getPageId();
            if(!results.containsKey(queryId))
            {
                results.put(queryId, new HashMap<String, Float>());
            }
            for(String term : page.getPageName().split(" "))
            {
                Term t = new Term("parabody", term);
                TermQuery tQuery = new TermQuery(t);
                TopDocs topDocs = is.search(tQuery, maxResults);
                float totalTermFreq = ir.totalTermFreq(t);
                ScoreDoc[] scores = topDocs.scoreDocs;
                for(int i = 0; i < topDocs.scoreDocs.length; i++)
                {
                    Document doc = is.doc(scores[i].doc);
                    String paraId = doc.get("paraid");

                    if(paraId.equals("0760e843e1c62c7aeb1c21f994f05992876aa0a1"))
                    {
                        System.out.println("UJM");
                        System.out.println(doc.get("parabody"));
                    }
                    if(!results.get(queryId).containsKey(paraId))
                    {
                        results.get(queryId).put(paraId, 0.0f);
                    }
                    float score = results.get(queryId).get(paraId);
                    score += (float)Math.log10((scores[i].score + (.1*(totalTermFreq/sumTotalTermFreq))));
                    results.get(queryId).put(paraId, score);
                }
            }
        }

        for(Map.Entry<String, HashMap<String, Float>> queryResult: results.entrySet())
        {
            String queryId = queryResult.getKey();
            HashMap<String, Float> paraResults = queryResult.getValue();

            for(Map.Entry<String, Float> paraResult: paraResults.entrySet())
            {
                String paraId = paraResult.getKey();
                float score = (float)Math.pow(10, paraResult.getValue());
                DocumentResults1 docResult = new DocumentResults1(paraId, score);
                docQueue.add(docResult);
            }
            DocumentResults1 docResult;
            int count = 0;
            while((docResult = docQueue.poll()) != null)
            {
                runfilestrings.add(queryId + "  Q0 " + docResult.paraId + " " + count + " " + docResult.score + " team1-LM-UJM");
                count++;
                if(count >= 100)
                    break;
            }
            docQueue.clear();
        }
    }

    ArrayList<String> getResults()
    {
        return runfilestrings;
    }
}
