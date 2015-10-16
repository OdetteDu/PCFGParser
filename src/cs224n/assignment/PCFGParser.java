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

		this.sentence = sentence;
		int numWords = sentence.size();

		double[][][] score = new double[numWords + 1][numWords + 1][nonTermsList.size()];
		Triple<Integer, Integer, Integer>[][][] back = new Triple[numWords + 1][numWords + 1][nonTermsList.size()];
		for (int i=0; i<numWords; i++)
		{
			for (int j=0; j<nonTermsList.size(); j++)
			{
				String tag = nonTermsList.get(j);
				if (preTerms.contains(tag))
				{
					score[i][i+1][j] = lexicon.scoreTagging(sentence.get(i), tag);
				}
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
					for (int a = 0; a < nonTermsList.size(); a++)
					{
						for (int b = 0; b < nonTermsList.size(); b++)
						{
							for (int c = 0; c < nonTermsList.size(); c++)
							{
								List<BinaryRule> binaryRules = grammar.getBinaryRulesByLeftChild(nonTermsList.get(b));
								BinaryRule binaryRule = null;
								String A = nonTermsList.get(a);
								String B = nonTermsList.get(b);
								String C = nonTermsList.get(c);
								for (BinaryRule rule : binaryRules)
								{
									if (rule.getParent().equals(A) && rule.getRightChild().equals(C))
									{
										binaryRule = rule;
										break;
									}
								}
								if (binaryRule != null)
								{
									double prob = score[begin][split][b] * score[split][end][c] *binaryRule.getScore();
									if (prob > score[begin][end][a])
									{
										score[begin][end][a] = prob;
										back[begin][end][a] = new Triple<Integer, Integer, Integer>(split, b, c);
									}
								}
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
		return TreeAnnotations.unAnnotateTree(tree);
	}

	private Tree<String> buildTree(double[][][] score, Triple<Integer, Integer, Integer>[][][] back, String parentTag, int indexI, int indexJ)
	{
		double[] currentScore = score[indexI][indexJ];
		int correctScoreIndex = -1;
		for (int i=0; i<currentScore.length; i++)
		{
			if (nonTermsList.get(i).equals(parentTag))
			{
				correctScoreIndex = i;
				break;
			}
		}

		String parent = nonTermsList.get(correctScoreIndex);
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
