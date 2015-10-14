package cs224n.assignment;

import cs224n.ling.Tree;

import java.util.*;

import cs224n.util.Pair;

/**
 * The CKY PCFG Parser you will implement.
 */
public class PCFGParser implements Parser {
    private Grammar grammar;
    private Lexicon lexicon;

    public void train(List<Tree<String>> trainTrees) {
    	List<Tree<String>> binarizedTrees = new ArrayList<Tree<String>>();
    	for (Tree<String> tree : trainTrees)
    	{
    		binarizedTrees.add(TreeAnnotations.annotateTree(tree));
    	}
        lexicon = new Lexicon(binarizedTrees);
        grammar = new Grammar(binarizedTrees);
    }

	public Tree<String> getBestParse(List<String> sentence) {
    	int numWords = sentence.size();
    	Set<String> nonterms = lexicon.getAllTags();
    	
        @SuppressWarnings("unchecked")
		Entry<String>[][] score = new Entry[numWords + 1][numWords + 1];
        @SuppressWarnings("unchecked")
		Entry<Pair<String, String>>[][] back = new Entry[numWords + 1][numWords + 1];
        for (int i=0; i<numWords; i++)
        {
        	for (String tag : nonterms)
        	{
        		if(score[i][i+1] == null)
        		{
        			score[i][i+1] = new Entry<String>();
        		}
        		score[i][i+1].add(tag, lexicon.scoreTagging(sentence.get(i), tag));
        	}
        }
        return null;
    }
}
