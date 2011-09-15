package ast;

import java.util.ArrayList;
import java.util.Stack;

import ast.apps.AstSWSimilarityNode;
import ast.apps.AstSWSimilarityReducer;

public class AstReducer {
	// We still need to propagate limits for circuit generator
	//   even if reductions is disabled (for performance evaluations only)
	public static final boolean REDUCE_DISABLED = false;
	public static final boolean LOCALITY_ENABLED = true
      && AstNode.LOCAL_EVAL_ENABLED;
	AstValueReducer valueHelper = new AstValueReducer();
	AstMinReducer minHelper = new AstMinReducer(this);
	AstMaxReducer maxHelper = new AstMaxReducer(this);
	AstAddReducer addHelper = new AstAddReducer(this);
	AstNequReducer nequHelper = new AstNequReducer();
	AstSWSimilarityReducer swSimilarityHelper = new AstSWSimilarityReducer();

	public void printStats(java.io.PrintStream printStream) {
		float avg = minHelper.branchtotal * 1f / minHelper.branchcount;
		printStream.println("Min branch avg = " + avg + ", max = "
				+ minHelper.branchmax);
	}

        public static final int absorb_threshold = 1;
        // TODO refactor upper/lower limits directly into AstNode
	// node-specific extra info used by reduce
	public static class ReduceInfo {
		public int upperLim, lowerLim;
		public boolean hasConst; // used only for non-leaves
	}

	private AstVisitedMap<ReduceInfo> visited;

	public AstReducer() {
		visited = new AstVisitedMap<ReduceInfo>();
	}

	// Do some extra processing not normally done on every node
	// Does min() --> reduce(add(add(min(),+1),-1))
	// Helps activate the add(min(add())) rules even when min() is root
	public void reduceRoot(AstNode root) {
		if(REDUCE_DISABLED) return;
		AstLocalAbsorb absorber = new AstLocalAbsorb(root);
		if(root.needsGarbled()||!LOCALITY_ENABLED)
		{	AstNode addchild1[] = { root, AstValueNode.create(+1) };
			AstNode add1 = AstAddNode.create(addchild1);
			AstNode addchild2[] = { add1, AstValueNode.create(-1) };
			AstNode add2 = AstAddNode.create(addchild2);
			if(!REDUCE_DISABLED) reduce(add2);	// if not really needed
			root.setData(add2.getData());
		}
		removeDuplicates(root);
		System.err.println("Absorbs: "+absorber.statA+" "+absorber.statB
				+" "+AstNodeCounter.count(root));
	}

	private void removeDuplicates(AstNode node) {
		Stack<AstNode> stk = new Stack<AstNode>();
		StructureMap smap = new StructureMap();
		stk.push(node);
		int itercount = 0;
		while (!stk.empty()) {
			node = stk.pop();
			AstNode child[] = node.children();
			for (int i = 0; i < child.length; ++i) {
				AstNode p = smap.getDuplicate(child[i]);
				if(p==child[i]) continue;		// already pushed this in
				if(p!=null)  					// found a duplicate
					child[i]=p;					//   hoping it's safe to change
				else stk.push(child[i]);
			}
			itercount++;
			if(itercount %50000==0)
				System.err.println(itercount+" "+visited.size());
		}
		// System.err.println(node.getType());
	}

	public int childCount = 0;
	public long minCount = 0;
	public boolean loggingTime = false;

	public ReduceInfo reduce(AstNode node) {
		if (visited.isVisited(node))
			return visited.valueAt(node); // already reduced
		ReduceInfo nodeinfo = new ReduceInfo();
		visited.visit(node, nodeinfo);

		boolean repeat;

		do {
			repeat = false;
			if (node.getType() == AstValueNode.class)
				repeat = valueHelper.reduce(node, nodeinfo);

			else if (node.getType() == AstMinNode.class)
				repeat = minHelper.reduce(node, nodeinfo);
			else if (node.getType() == AstMaxNode.class)
				repeat = maxHelper.reduce(node, nodeinfo);
			else if (node.getType() == AstAddNode.class)
				repeat = addHelper.reduce(node, nodeinfo);
			else if (node.getType() == AstNequNode.class)
				repeat = nequHelper.reduce(node, nodeinfo);
			else if (node.getType() == AstSWSimilarityNode.class)
				repeat = swSimilarityHelper.reduce(node, nodeinfo);

			if (loggingTime) {
				AstPrinter.print(node, System.err);
				System.err.println();
			}
		} while (repeat);

		return nodeinfo;
	}

	private static int flatsize(AstNode root) {
		int rv = 0;
		AstNode mychildren[] = root.children();
		for (int i = 0; i < mychildren.length; ++i)
			if (mychildren[i].getType() == root.getType())
				rv += flatsize(mychildren[i]);
			else
				rv++;
		return rv;
	}

	public static ArrayList<AstNode> flattenedChildList(AstNode root) {
		ArrayList<AstNode> rv = new ArrayList<AstNode>();
		AstNode mychildren[] = root.children();
		if (mychildren.length >= 1000)
			System.err.println("Flattening to breadth " + flatsize(root));
		for (int i = 0; i < mychildren.length; ++i)
			if (mychildren[i].getType() == root.getType() && 
                    (!LOCALITY_ENABLED||mychildren[i].needsGarbled()))
				rv.addAll(flattenedChildList(mychildren[i]));
			else
				rv.add(mychildren[i]);
		return rv;
	}

        public static class UnknownNodeException extends RuntimeException
        {
          public UnknownNodeException(String s) { super(s); }
          public UnknownNodeException(AstNode n) { super(""+n.getType()); }
        }

        // Methods for post-reduce metadata
        public int nodeValueUpperLim(AstNode node)
        {
          if(node.getType()==AstValueNode.class)
            return (((AstValueNode)node.getData()).getValue());
          if(!visited.isVisited(node)) 
		  {	 AstPrinter.print(node,System.err);
			  System.err.println();
			  throw new UnknownNodeException(node);
		  }
          return visited.valueAt(node).upperLim;
        }
        public int nodeValueLowerLim(AstNode node)
        {
          if(node.getType()==AstValueNode.class)
            return (((AstValueNode)node.getData()).getValue());
          if(!visited.isVisited(node)) 
			  throw new UnknownNodeException(node);
          return visited.valueAt(node).lowerLim;
        }

        public static 
          boolean absorbConstsLocally(AstNode node)
        {
          if(!node.needsGarbled()) return false;
          assert node.getType()==AstMinNode.class
            || node.getType()==AstMaxNode.class
            || node.getType()==AstAddNode.class
            : "This method only works on associative and commutative operators";
          AstNode[] child = node.children();
          int dumpind=-1,cc=0,i;
          for(i=0;i<child.length;++i)
          { boolean ad = child[i].dependsOnA(), bd = child[i].dependsOnB();
            if(!ad && !bd) cc++;
            if(dumpind<0)
            { if(ad && !bd) dumpind=i;
              if(!ad && bd) dumpind=i;
            }
          }
          if(cc==0 || dumpind==-1) return false;
          AstNode[] cnode = new AstNode[cc+1];
          AstNode[] rest = new AstNode[child.length-cc];
          cnode[cc]=child[dumpind];
          int ci=0,ri=0,newdump=-1;
          for(i=0;i<child.length;++i)
          {
            if(!child[i].dependsOnA() && !child[i].dependsOnB())
              cnode[ci++]=child[i];
            else if(i!=dumpind) rest[ri++]=child[i];
            else newdump=ri++;
          }

          rest[newdump] = newSameType(node,cnode);
          node.setData(newSameType(node,rest).getData());
          return true;
        }
		// better if you call absorbConstsLocally before this
        public static 
          boolean groupLocalChildren(AstNode node)
        {
          if(!node.needsGarbled()) return false;
          assert node.getType()==AstMinNode.class
            || node.getType()==AstMaxNode.class
            || node.getType()==AstAddNode.class
            : "This method only works on associative and commutative operators";
          int aonly = 0, bonly = 0, consts = 0, i, j;
          AstNode[] child = node.children();
          for(i=0;i<child.length;++i) if(!child[i].needsGarbled())
          { if(child[i].dependsOnA()) aonly++;  // group consts with A
            else if(child[i].dependsOnB()) bonly++;
            else consts++;
          }
          if(aonly<=1 && bonly<=1 && consts<=1) return false;
          AstNode[] agroup = new AstNode[aonly];
          AstNode[] bgroup = new AstNode[bonly];
          AstNode[] cgroup = new AstNode[consts];
          AstNode[] ggroup = new AstNode[child.length-aonly-bonly-consts];
          int aind=0,bind=0,cind=0,gind=0;
          for(i=0;i<child.length;++i)
          { if(child[i].needsGarbled()) ggroup[gind++]=child[i];
            else if(child[i].dependsOnA()) agroup[aind++]=child[i];
            else if(child[i].dependsOnB()) bgroup[bind++]=child[i];
            else cgroup[cind++]=child[i];
          }
          AstNode[] stem = new AstNode[4];
          int rootc=0;
          if(aonly==1) stem[0] = agroup[0];
          else if(aonly>1) stem[0] = newSameType(node,agroup);
          if(bonly==1) stem[1] = bgroup[0];
          else if(bonly>1) stem[1] = newSameType(node,bgroup);
          if(consts==1) stem[2] = cgroup[0];
          else if(consts>1) stem[2] = newSameType(node,cgroup);
          if(ggroup.length==1) stem[3] = ggroup[0];
          else if(ggroup.length>1) stem[3] = newSameType(node,ggroup);
          for(i=0;i<4;++i) if(stem[i]!=null) rootc++;
          AstNode[] rv = new AstNode[rootc];
          for(i=0,j=0;i<4;++i) if(stem[i]!=null) rv[j++]=stem[i];
          node.setData(newSameType(node,rv).getData());
          return true;
        }
        public static AstNode newSameType(AstNode node,AstNode[] children)
        {
		  assert children!=null;
		  for(int i=0;i<children.length;++i) assert children[i]!=null;
          if(node.getType()==AstAddNode.class) 
            return AstAddNode.create(children);
          if(node.getType()==AstMinNode.class) 
            return AstMinNode.create(children);
          if(node.getType()==AstMaxNode.class) 
            return AstMaxNode.create(children);
          return null;
        }

}
