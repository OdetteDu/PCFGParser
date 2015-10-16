package cs224n.assignment;

import cs224n.assignment.Grammar.BinaryRule;
import cs224n.assignment.Grammar.UnaryRule;
import cs224n.ling.Tree;

import java.util.*;

import cs224n.util.Triple;

/**
 * The CKY PCFG Parser you will implement.
 */
public class PCFGParser implements Parser {
	private Grammar grammar;
	private Lexicon lexicon;
	public static final int UNARY_INDEX = -1;
	private Set<String> preTerms;
	private List<String> nonTermsList;
	private Map<String, Integer> nonTermsListIndexMap;
	private List<String> sentence;

	public void train(List<Tree<String>> trainTrees) {
		List<Tree<String>> binarizedTrees = new ArrayList<Tree<String>>();
		for (Tree<String> tree : trainTrees)
		{
			binarizedTrees.add(TreeAnnotations.annotateTree(tree));
		}
		lexicon = new Lexicon(binarizedTrees);
		grammar = new Grammar(binarizedTrees);

		preTerms = lexicon.getAllTags();
		nonTermsList = new ArrayList<String>();
		nonTermsList.addAll(grammar.nonTerms);
		nonTermsListIndexMap = new HashMap<String, Integer>();
		for (int i=0; i<nonTermsList.size(); i++)
		{
			nonTermsListIndexMap.put(nonTermsList.get(i), i);
		}
	}

	public Tree<String> getBestParse(List<String> sentence) {

		long startTime = System.nanoTime();
		this.sentence = sentence;
		int numWords = sentence.size();

		double[][][] score = new double[numWords + 1][numWords + 1][nonTermsList.size()];
		Triple<Integer, Integer, Integer>[][][] back = new Triple[numWords + 1][numWords + 1][nonTermsList.size()];
		for (int i=0; i<numWords; i++)
		{
			for (String tag : preTerms)
			{
				score[i][i+1][nonTermsListIndexMap.get(tag)] = lexicon.scoreTagging(sentence.get(i), tag);
			}

			//handle unaries
			boolean added = true;
			while (added)
			{
				added = false;
				for (int b=0; b<nonTermsList.size(); b++)
				{
					if (score[i][i+1][b] > 0)
					{
						List<UnaryRule> unaryRules = grammar.getUnaryRulesByChild(nonTermsList.get(b));
						for (UnaryRule rule : unaryRules)
						{
							double prob = rule.getScore() * score[i][i+1][b];
							int a = nonTermsListIndexMap.get(rule.getParent());
							if (prob > score[i][i+1][a])
							{
								score[i][i+1][a] = prob;
								back[i][i+1][a] = new Triple<Integer, Integer, Integer>(UNARY_INDEX, b, UNARY_INDEX);
								added = true;
							}
						}
					}
				}

			}
		}

		for (int span = 2; span <= numWords; span++)
		{
			for (int begin = 0; begin <= numWords - span; begin++)
			{
				int end = begin + span;
				for (int split = begin + 1; split <= end - 1; split ++)
				{
					for (int b = 0; b < nonTermsList.size(); b++)
					{
						List<BinaryRule> binaryRules = grammar.getBinaryRulesByLeftChild(nonTermsList.get(b));
						for (BinaryRule rule : binaryRules)
						{
							int a = nonTermsListIndexMap.get(rule.getParent());
							int c = nonTermsListIndexMap.get(rule.getRightChild());
							double prob = score[begin][split][b] * score[split][end][c] *rule.getScore();
							if (prob > score[begin][end][a])
							{
								score[begin][end][a] = prob;
								back[begin][end][a] = new Triple<Integer, Integer, Integer>(split, b, c);
							}
						}
					}
				}

				//handle unaries
				boolean added = true;
				while (added)
				{
					added = false;
					for (int b=0; b<nonTermsList.size(); b++)
					{
						List<UnaryRule> unaryRules = grammar.getUnaryRulesByChild(nonTermsList.get(b));
						for (UnaryRule rule : unaryRules)
						{
							double prob = rule.getScore() * score[begin][end][b];
							int a = nonTermsListIndexMap.get(rule.getParent());
							if (prob > score[begin][end][a])
							{
								score[begin][end][a] = prob;
								back[begin][end][a] = new Triple<Integer, Integer, Integer>(UNARY_INDEX, b, UNARY_INDEX);
								added = true;
							}
						}
					}

				}
			}
		}

		Tree<String> tree =  buildTree(score, back, "ROOT", 0, score[0].length - 1);
		long endTime = System.nanoTime();
		double timeCost = (endTime - startTime) * 1.0 / 1000000000;
		System.out.println("Time spent: " + timeCost);
		return TreeAnnotations.unAnnotateTree(tree);
	}

	private Tree<String> buildTree(double[][][] score, Triple<Integer, Integer, Integer>[][][] back, String parent, int indexI, int indexJ)
	{
		int correctScoreIndex = this.nonTermsListIndexMap.get(parent);
		
		List<Tree<String>> children = new ArrayList<Tree<String>>();
		if (this.lexicon.getAllTags().contains(parent))
		{
			children.add(new Tree<String>(sentence.get(indexI)));
			return new Tree<String>(parent, children);
		}

		Triple<Integer, Integer, Integer> currentBack = back[indexI][indexJ][correctScoreIndex];
		if (currentBack.getFirst() == -1)
		{
			//handle unary
			String child = nonTermsList.get(currentBack.getSecond());
			//add the only child
			children.add(buildTree(score, back, child, indexI, indexJ));
		}
		else
		{
			//handle binary
			String leftChild = nonTermsList.get(currentBack.getSecond());
			String rightChild = nonTermsList.get(currentBack.getThird());
			int split = currentBack.getFirst();
			//Add left child
			children.add(buildTree(score, back, leftChild, indexI, split));
			//Add right child
			children.add(buildTree(score, back, rightChild, split, indexJ));
		}
		return new Tree<String>(parent, children);
	}
}
