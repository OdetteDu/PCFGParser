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
		Set<String> preTerms = lexicon.getAllTags();
		Set<String> nonterms = grammar.nonTerms;
		List<String> nonTermsList = new ArrayList<String>();
		nonTermsList.addAll(nonterms);

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
				for (int a=0; a<nonTermsList.size(); a++)
				{
					for (int b=0; b<nonTermsList.size(); b++)
					{
						if (score[i][i+1][b] > 0)
						{
							List<UnaryRule> unaryRules = grammar.getUnaryRulesByChild(nonTermsList.get(b));
							UnaryRule unaryRule = null;
							for (UnaryRule rule : unaryRules)
							{
								if (rule.getParent().equals(nonTermsList.get(a)))
								{
									unaryRule = rule;
									break;
								}
							}
							if (unaryRule != null)
							{
								double prob = unaryRule.getScore() * score[i][i+1][b];
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
					for (int a=0; a<nonTermsList.size(); a++)
					{
						for (int b=0; b<nonTermsList.size(); b++)
						{
							List<UnaryRule> unaryRules = grammar.getUnaryRulesByChild(nonTermsList.get(b));
							UnaryRule unaryRule = null;
							for (UnaryRule rule : unaryRules)
							{
								if (rule.getParent().equals(nonTermsList.get(a)))
								{
									unaryRule = rule;
									break;
								}
							}
							if (unaryRule != null)
							{
								double prob = unaryRule.getScore() * score[begin][end][b];
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
		}
		//TODO buildTree(score, back);
		return null;
	}
}
