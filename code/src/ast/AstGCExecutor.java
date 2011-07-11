package ast;

import Program.EditDistance;
import YaoGC.State;

public class AstGCExecutor {
	// Change this class to hold any info you need
	// e.g. References to garbled circuit objects
	public static class NodeData {
		public State state;

		public void calculateFromChildren(AstNode current, NodeData childLeft,
				NodeData childRight) {
			// this.nodeCount = childLeft.nodeCount+childRight.nodeCount+1;
			// calculate any other value you need here
			/*
			 * You can check for the current node type like this:
			 * if(current.getType()==AstAddNode.class) ... else
			 * if(current.getType()==AstMinNode.class) ...
			 */
		}

		public void calculateFromLeaf(AstNode leaf) {
			// if(leaf.getType()==AstValueNode.class)
			// this.nodeCount = 1; // leaf representing constant integer value
			// else if(leaf.getType()==AstNequNode.class)
			// this.nodeCount = 3; // leaf representing comparison between chars
			// else
			// this.nodeCount = 1; // I don't really know any other leaf right
			// now
		}
	}

	// ---------- You shouldn't have to modify anything beyond this -------
	/** The only public method in this class :) . */
	public static NodeData execute(AstNode root) {
		AstVisitedMap<NodeData[]> visited = new AstVisitedMap<NodeData[]>();
		return executeSubtree(root, visited);
	}

	private static NodeData executeSubtree(AstNode node,
			AstVisitedMap<NodeData[]> visited) {
		// If I have already visited this node, return reference to
		// previously computed object
		if (visited.isVisited(node))
			return visited.valueAt(node)[0];

		AstNode[] children = node.children();
		NodeData data[];
		assert children.length != 1 : "Add new NodeData method for one child nodes";

		if (children.length == 0) {
			data = new NodeData[1];
			data[0] = new NodeData();
			data[0].calculateFromLeaf(node);
			visited.visit(node, data);
			return data[0];
		}

		data = new NodeData[children.length - 1];
		visited.visit(node, data);

		int i = data.length;
		NodeData last = executeSubtree(children[i], visited);
		for (--i; i >= 0; --i) {
			data[i] = new NodeData();
			data[i].calculateFromChildren(node, executeSubtree(children[i],
					visited), last);
			last = data[i];
		}

		return last;
	}

	// for testing
	public static void main(String[] args) {
		EditDistance ed = new EditDistance("ab#cd", "a#bcf");
		AstPrinter.print(ed.getRoot(), System.out);
		State s = AstGCExecutor.execute(ed.getRoot()).state;
		// System.out.println("\nnodeCount = " +
		// AstGCExecutor.execute(ed.getRoot()).nodeCount);
	}
}