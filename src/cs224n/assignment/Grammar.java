package cs224n.assignment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cs224n.ling.Tree;
import cs224n.util.CollectionUtils;
import cs224n.util.Counter;

// Grammar ====================================================================

/**
 * Simple implementation of a PCFG grammar, offering the ability to
 * look up rules by their child symbols.  Rule probability estimates
 * are just relative frequency estimates off of training trees.
 */
public class Grammar {

	// BinaryRule =================================================================

	/* A binary grammar rule with score representing its probability. */
	public static class BinaryRule {

		String parent;
		String leftChild;
		String rightChild;
		double score;

		public String getParent() {
			return parent;
		}

		public String getLeftChild() {
			return leftChild;
		}

		public String getRightChild() {
			return rightChild;
		}

		public double getScore() {
			return score;
		}

		public void setScore(double score) {
			this.score = score;
		}

		public boolean equals(Object o) {
			if (this == o) return true;
			if (!(o instanceof BinaryRule)) return false;

			final BinaryRule binaryRule = (BinaryRule) o;

			if (leftChild != null ? !leftChild.equals(binaryRule.leftChild) : binaryRule.leftChild != null) 
				return false;
			if (parent != null ? !parent.equals(binaryRule.parent) : binaryRule.parent != null) 
				return false;
			if (rightChild != null ? !rightChild.equals(binaryRule.rightChild) : binaryRule.rightChild != null) 
				return false;

			return true;
		}

		public int hashCode() {
			int result;
			result = (parent != null ? parent.hashCode() : 0);
			result = 29 * result + (leftChild != null ? leftChild.hashCode() : 0);
			result = 29 * result + (rightChild != null ? rightChild.hashCode() : 0);
			return result;
		}

		public String toString() {
			return parent + " -> " + leftChild + " " + rightChild + " %% "+score;
		}

		public BinaryRule(String parent, String leftChild, String rightChild) {
			this.parent = parent;
			this.leftChild = leftChild;
			this.rightChild = rightChild;
		}
	}


	// UnaryRule ==================================================================

	/** A unary grammar rule with score representing its probability. */
	public static class UnaryRule {

		String parent;
		String child;
		double score;

		public String getParent() {
			return parent;
		}

		public String getChild() {
			return child;
		}

		public double getScore() {
			return score;
		}

		public void setScore(double score) {
			this.score = score;
		}

		public boolean equals(Object o) {
			if (this == o) return true;
			if (!(o instanceof UnaryRule)) return false;

			final UnaryRule unaryRule = (UnaryRule) o;

			if (child != null ? !child.equals(unaryRule.child) : unaryRule.child != null) return false;
			if (parent != null ? !parent.equals(unaryRule.parent) : unaryRule.parent != null) return false;

			return true;
		}

		public int hashCode() {
			int result;
			result = (parent != null ? parent.hashCode() : 0);
			result = 29 * result + (child != null ? child.hashCode() : 0);
			return result;
		}

		public String toString() {
			return parent + " -> " + child + " %% "+score;
		}

		public UnaryRule(String parent, String child) {
			this.parent = parent;
			this.child = child;
		}
	}	
	
	Map<String, List<BinaryRule>> binaryRulesByLeftChild = 
			new HashMap<String, List<BinaryRule>>();
	Map<String, List<BinaryRule>> binaryRulesByRightChild = 
			new HashMap<String, List<BinaryRule>>();
	Map<String, List<UnaryRule>> unaryRulesByChild = 
			new HashMap<String, List<UnaryRule>>();
	Set<String> nonTerms = new HashSet<String>();

	/* Rules in grammar are indexed by child for easy access when
	 * doing bottom up parsing. */
	public List<BinaryRule> getBinaryRulesByLeftChild(String leftChild) {
		return CollectionUtils.getValueList(binaryRulesByLeftChild, leftChild);
	}

	public List<BinaryRule> getBinaryRulesByRightChild(String rightChild) {
		return CollectionUtils.getValueList(binaryRulesByRightChild, rightChild);
	}

	public List<UnaryRule> getUnaryRulesByChild(String child) {
		return CollectionUtils.getValueList(unaryRulesByChild, child);
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		List<String> ruleStrings = new ArrayList<String>();
		for (String leftChild : binaryRulesByLeftChild.keySet()) {
			for (BinaryRule binaryRule : getBinaryRulesByLeftChild(leftChild)) {
				ruleStrings.add(binaryRule.toString());
			}
		}
		for (String child : unaryRulesByChild.keySet()) {
			for (UnaryRule unaryRule : getUnaryRulesByChild(child)) {
				ruleStrings.add(unaryRule.toString());
			}
		}
		for (String ruleString : CollectionUtils.sort(ruleStrings)) {
			sb.append(ruleString);
			sb.append("\n");
		}
		return sb.toString();
	}

	private void addBinary(BinaryRule binaryRule) {
		CollectionUtils.addToValueList(binaryRulesByLeftChild, 
				binaryRule.getLeftChild(), binaryRule);
		CollectionUtils.addToValueList(binaryRulesByRightChild, 
				binaryRule.getRightChild(), binaryRule);
	}

	private void addUnary(UnaryRule unaryRule) {
		CollectionUtils.addToValueList(unaryRulesByChild, 
				unaryRule.getChild(), unaryRule);
	}

	/* A builds PCFG using the observed counts of binary and unary
	 * productions in the training trees to estimate the probabilities
	 * for those rules.  */ 
	public Grammar(List<Tree<String>> trainTrees) {
		Counter<UnaryRule> unaryRuleCounter = new Counter<UnaryRule>();
		Counter<BinaryRule> binaryRuleCounter = new Counter<BinaryRule>();
		Counter<String> symbolCounter = new Counter<String>();
		for (Tree<String> trainTree : trainTrees) {
			tallyTree(trainTree, symbolCounter, unaryRuleCounter, binaryRuleCounter);
		}
		for (UnaryRule unaryRule : unaryRuleCounter.keySet()) {
			double unaryProbability = 
					unaryRuleCounter.getCount(unaryRule) / 
					symbolCounter.getCount(unaryRule.getParent());
			unaryRule.setScore(unaryProbability);
			addUnary(unaryRule);
		}
		for (BinaryRule binaryRule : binaryRuleCounter.keySet()) {
			double binaryProbability = 
					binaryRuleCounter.getCount(binaryRule) / 
					symbolCounter.getCount(binaryRule.getParent());
			binaryRule.setScore(binaryProbability);
			addBinary(binaryRule);
		}
	}

	private void tallyTree(Tree<String> tree, Counter<String> symbolCounter,
			Counter<UnaryRule> unaryRuleCounter, 
			Counter<BinaryRule> binaryRuleCounter) {
		if (tree.isLeaf()) return;
		if (tree.isPreTerminal()) return;
		if (tree.getChildren().size() == 1) {
			UnaryRule unaryRule = makeUnaryRule(tree);
			symbolCounter.incrementCount(tree.getLabel(), 1.0);
			unaryRuleCounter.incrementCount(unaryRule, 1.0);
		}
		if (tree.getChildren().size() == 2) {
			BinaryRule binaryRule = makeBinaryRule(tree);
			symbolCounter.incrementCount(tree.getLabel(), 1.0);
			binaryRuleCounter.incrementCount(binaryRule, 1.0);
		}
		if (tree.getChildren().size() < 1 || tree.getChildren().size() > 2) {
			throw new RuntimeException("Attempted to construct a Grammar with an illegal tree: "+tree);
		}
		for (Tree<String> child : tree.getChildren()) {
			tallyTree(child, symbolCounter, unaryRuleCounter,  binaryRuleCounter);
		}
	}

	private UnaryRule makeUnaryRule(Tree<String> tree) {
		String parent = tree.getLabel();
		String child = tree.getChildren().get(0).getLabel();
		nonTerms.add(parent);
		nonTerms.add(child);
		return new UnaryRule(parent, child);
	}

	private BinaryRule makeBinaryRule(Tree<String> tree) {
		String parent = tree.getLabel();
		String leftChild = tree.getChildren().get(0).getLabel();
		String rightChild = tree.getChildren().get(1).getLabel();
		nonTerms.add(parent);
		nonTerms.add(leftChild);
		nonTerms.add(rightChild);
		return new BinaryRule(parent, leftChild, rightChild);
	}
}
