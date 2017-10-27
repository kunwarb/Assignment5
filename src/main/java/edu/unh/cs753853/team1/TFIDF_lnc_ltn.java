package edu.unh.cs753853.team1;

import edu.unh.cs.treccartool.Data;
import edu.unh.cs.treccartool.read_data.DeserializeData;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.search.similarities.BasicStats;
import org.apache.lucene.search.similarities.SimilarityBase;
import org.apache.lucene.store.FSDirectory;

import java.io.*;
import java.util.*;

class ResultComparator implements Comparator<DocumentResults>
{
    public int compare(DocumentResults d2, DocumentResults d1)
    {
        if(d1.getScore() < d2.getScore())
            return -1;
        if(d1.getScore() == d2.getScore())
            return 0;
        return 1;
    }
}

public class TFIDF_lnc_ltn {
    // Lucene tools
    private IndexSearcher searcher;
    private QueryParser parser;

    // List of pages to query
    private ArrayList<Data.Page> pageList;

    // Number of documents to return
    private int numDocs;

    // Map of queries to map of Documents to scores for that query
    private HashMap<Query, ArrayList<DocumentResults>> queryResults;


    TFIDF_lnc_ltn(ArrayList<Data.Page> pl, int n) throws ParseException, IOException
    {

        numDocs = n; // Get the (max) number of documents to return
        pageList = pl; // Each page title will be used as a query

        // Parse the parabody field using StandardAnalyzer
        parser = new QueryParser("parabody", new StandardAnalyzer());

        // Create an index searcher
        String INDEX_DIRECTORY = "index";
        searcher = new IndexSearcher(DirectoryReader.open(FSDirectory.open((new File(INDEX_DIRECTORY).toPath()))));

        // Set our own similarity class which computes tf[t,d]
        SimilarityBase lnc_ltn = new SimilarityBase() {
            protected float score(BasicStats stats, float freq, float docLen) {
                return (float)(1 + Math.log10(freq));
            }

            @Override
            public String toString() {
                return null;
            }
        };
        searcher.setSimilarity(lnc_ltn);
    }

    /**
     *
     * @param runfile   The name of the runfile to output to
     * @throws IOException
     * @throws ParseException
     */
    public void dumpScoresTo(String runfile) throws IOException, ParseException
    {
        queryResults = new HashMap<>(); // Maps query to map of Documents with TF-IDF score

        for(Data.Page page:pageList)
        {   // For every page in .cbor.outline
            // We need...
            HashMap<Document, Float> scores = new HashMap<>();          // Mapping of each Document to its score
            HashMap<Document, DocumentResults> docMap = new HashMap<>();
            PriorityQueue<DocumentResults> docQueue = new PriorityQueue<>(new ResultComparator());
            ArrayList<DocumentResults> docResults = new ArrayList<>();
            HashMap<TermQuery, Float> queryweights = new HashMap<>();   // Mapping of each term to its query tf
            ArrayList<TermQuery> terms = new ArrayList<>();             // List of every term in the query
            Query q = parser.parse(page.getPageName());                 // The full query containing all terms
            String qid = page.getPageId();

            for(String term: page.getPageName().split(" "))
            {   // For every word in page name...
                // Take word as query term for parabody
                TermQuery tq = new TermQuery(new Term("parabody", term));
                terms.add(tq);

                // Add one to our term weighting every time it appears in the query
                queryweights.put(tq, queryweights.getOrDefault(tq, 0.0f)+1.0f);
            }
            for(TermQuery query: terms)
            {   // For every Term

                // Get our Index Reader for helpful statistics
                IndexReader reader = searcher.getIndexReader();

                // If document frequency is zero, set DF to 1; else, set DF to document frequency
                float DF = (reader.docFreq(query.getTerm()) == 0) ? 1 : reader.docFreq(query.getTerm());

                // Calculate TF-IDF for the query vector
                float qTF = (float)(1 + Math.log10(queryweights.get(query)));   // Logarithmic term frequency
                float qIDF = (float)(Math.log10(reader.numDocs()/DF));          // Logarithmic inverse document frequency
                float qWeight = qTF * qIDF;                                     // Final calculation

                // Store query weight for later calculations
                queryweights.put(query, qWeight);

                // Get the top 100 documents that match our query
                TopDocs tpd = searcher.search(query, numDocs);
                for(int i = 0; i < tpd.scoreDocs.length; i++)
                {   // For every returned document...
                    Document doc = searcher.doc(tpd.scoreDocs[i].doc);                  // Get the document
                    double score = tpd.scoreDocs[i].score * queryweights.get(query);    // Calculate TF-IDF for document

                    DocumentResults dResults = docMap.get(doc);
                    if(dResults == null)
                    {
                        dResults = new DocumentResults(doc);
                    }
                    float prevScore = dResults.getScore();
                    dResults.score((float)(prevScore+score));
                    dResults.queryId(qid);
                    dResults.paragraphId(doc.getField("paraid").stringValue());
                    dResults.teamName("team1");
                    dResults.methodName("tf.idf_lnc_ltn");
                    docMap.put(doc, dResults);

                    // Store score for later use
                    scores.put(doc, (float)(prevScore+score));
                }
            }

            // Get cosine Length
            float cosineLength = 0.0f;
            for(Map.Entry<Document, Float> entry: scores.entrySet())
            {
                Document doc = entry.getKey();
                Float score = entry.getValue();

                cosineLength = (float)(cosineLength + Math.pow(score, 2));
            }
            cosineLength = (float)(Math.sqrt(cosineLength));

            // Normalization of scores
            for(Map.Entry<Document, Float> entry: scores.entrySet())
            {   // For every document and its corresponding score...
                Document doc = entry.getKey();
                Float score = entry.getValue();

                // Normalize the score
                scores.put(doc, score/scores.size());
                DocumentResults dResults = docMap.get(doc);
                dResults.score(dResults.getScore()/cosineLength);

                docQueue.add(dResults);
            }

            int rankCount = 0;
            DocumentResults current;
            while((current = docQueue.poll()) != null)
            {
                current.rank(rankCount);
                docResults.add(current);
                rankCount++;
            }

            // Map our Documents and scores to the corresponding query
            queryResults.put(q, docResults);
        }


        System.out.println("TFIDF_lnc_ltn writing results to: \t\t" + runfile);
        FileWriter runfileWriter = new FileWriter(new File(runfile));
        for(Map.Entry<Query, ArrayList<DocumentResults>> results: queryResults.entrySet())
        {
            ArrayList<DocumentResults> list = results.getValue();
            for(int i = 0; i < list.size(); i++)
            {
                DocumentResults dr = list.get(i);
                runfileWriter.write(dr.getRunfileString());
            }
        }
        runfileWriter.close();


    }



}
