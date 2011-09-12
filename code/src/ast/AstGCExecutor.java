package ast;

import ast.apps.AstSWSimilarityNode;
import ast.circuit.PartialCircuit;
import java.math.BigInteger;
import java.util.BitSet;
import Program.EditDistance;
import YaoGC.*;

public class AstGCExecutor {

    private BigInteger[] sdnalbs, cdnalbs;
    private AstVisitedMap<NodeData[]> visited;
    private AstVisitedMap<Integer> labelIndex;
	private AstVisitedMap<Integer> localValue;
	private BitSet bitsBeingAdded = null;
	private int clientExtraLabels = 0, serverExtraLabels = 0;
	private int bitsAdded = 0;
	private boolean alreadyExecuted = false;
    private static final int MAX_WIDTH = 40;
    private ADD_2L_Lplus1[] addCircuit;
    private MIN_2L_L[] minCircuit;
    private MAX_2L_L[] maxCircuit;
    private SMAX_2L_L[] smaxCircuit;
    private NEQUAL_2L_1[] nequCircuit;
    private NOT notCircuit;
	private static final int sigma = 2;     // FIXME sigma

    // Should return the number of bits required to represent 
    //   any given node
    public static interface BitSizeCalculator
    {
        public int bitsFor(AstNode node);
        public boolean needsNeg(AstNode node);
    }

	public static interface LeafEval
	{
		public boolean serverSide();
		public int eval(AstNode leafNode);
	}

    private BitSizeCalculator bitSize;

    private static void build(Circuit cir) {
        try {
            cir.build(); 
        }catch(Exception ex) { ex.printStackTrace(); System.exit(1); }
    }
    // Change this class to hold any info you need
    // e.g. References to garbled circuit objects
    public class NodeData {
        public State state;
        public boolean isSigned = false;

        // should be renamed to unsignedResize, that doesn't take in extra 's'
        public State resizeState(State s,int width)
        {
            if(s.getWidth()==width) return s; // nop
            else if(s.getWidth()>width)
              return State.extractState(s,0,width);
            else return State.concatenate(
                    new State(BigInteger.ZERO,width-s.getWidth()),s);
        }
        public State signedResize(int width)
        {
          if(!isSigned) return resizeState(state,width);
          if(state.getWidth()==width) return state; // nop
          else if(state.getWidth()>width)
            return State.extractState(state,0,width);
          else return State.signExtend(state,width);
        }
        // There's a reason I hate some of the circuits here! This is it!
        public State riffleStates(State s1,State s2)
        {
            assert s1.getWidth()==s2.getWidth()
              : s1.getWidth()+" != "+s2.getWidth();
            State s = new State(BigInteger.ZERO,s1.getWidth()+s2.getWidth());
            for(int i=0;i<s1.getWidth();++i)
            {
                s.wires[2*i] = s1.wires[i];
                s.wires[2*i+1] = s2.wires[i];
            }
            s.plainValue = null;
            return s;
            /*
               State[] s = new State[s1.getWidth()*2];
               for(int i=0;i<s1.getWidth();++i)
               {
               s[2*i] = State.extractState(s1,i,i+1);
               s[2*i+1] = State.extractState(s2,i,i+1);
               }
               return ast.circuit.PartialCircuit.concatAll(s);
             */
        }
        public void calculateFromChildren(AstNode current, NodeData childLeft,
                NodeData childRight) {

            if (current.getType()==AstAddNode.class)
            {  
                int w = bitSize.bitsFor(current);
                ADD_2L_Lplus1 cir = addCircuit[w];
                if(bitSize.needsNeg(current)) 
                { isSigned = true;
                  state = cir.startExecuting(
                      riffleStates(
                          childLeft.signedResize(w),
                          childRight.signedResize(w)));
                }
                else
                  state = //PartialCircuit.create(cir,
                          cir.startExecuting(
                          riffleStates(
                              resizeState(childLeft.state,w),
                              resizeState(childRight.state,w)))
                      ;//.startExecuting();
                state = State.extractState(state,0,w);
            }
            else if(current.getType()==AstMinNode.class) 
            {
                int w = bitSize.bitsFor(current);
                MIN_2L_L cir = minCircuit[w];
                state = //PartialCircuit.create(cir,
                        cir.startExecuting(
                        State.concatenate(
                          resizeState(childLeft.state,w),
                          resizeState(childRight.state,w)))
            ;//        .startExecuting();
            }
            else if (current.getType()==AstMaxNode.class) 
            {
                int w = bitSize.bitsFor(current);
                // FIXME using one single width for the whole max node
                if(bitSize.needsNeg(current)) 
                { SMAX_2L_L cir = smaxCircuit[w];
                  isSigned = true;
                  state = cir.startExecuting(
                      State.concatenate(
                        childLeft.signedResize(w),
                        childRight.signedResize(w)));
                }
                else
                { MAX_2L_L cir = maxCircuit[w];
                  state = //PartialCircuit.create(cir,
                      cir.startExecuting(
                        State.concatenate(
                            resizeState(childLeft.state,w),
                            resizeState(childRight.state,w)))
;//                    .startExecuting();
                }
            }
        }

        public void calculateFromLeaf(AstNode leaf) {
            if(leaf.getType()==AstValueNode.class)
                buildValueState(leaf);
            else if(leaf.getType()==AstNequNode.class)
                executeNEQU(leaf);
            else if(leaf.getType()==AstSWSimilarityNode.class)
                executeSW(leaf);
            else
                assert false : "Unknown type of leaf nodes";
        }

        private void executeNEQU(AstNode node) {
            AstNequNode ann = (AstNequNode)(node.getData());
            NEQUAL_2L_1 cir = nequCircuit[sigma];
            State sa,sb;

            AstCharRef a = ann.getOperandA(), b = ann.getOperandB();
            if(a.isSymbolic()) sa = labels2State(a,true);
            else sa = new State(BigInteger.valueOf((int)a.getChar()-'A'),sigma);
            if(b.isSymbolic()) sb = labels2State(b,false);
            else sb = new State(BigInteger.valueOf((int)b.getChar()-'A'),sigma);

            //build(cir); ??
            state = cir.startExecuting(State.concatenate(sa,sb));
            //state = PartialCircuit.create(cir,State.concatenate(sa,sb))
            //    .startExecuting();
            // FIXME zero extend the results from this, not sign extend
        }

        private void executeSW(AstNode node) {
            AstSWSimilarityNode ann = (AstSWSimilarityNode)(node.getData());
            NEQUAL_2L_1 cir = nequCircuit[sigma];
            State sa,sb;

            AstCharRef a = ann.getOperandA(), b = ann.getOperandB();
            if(a.isSymbolic()) sa = labels2State(a,true);
            else sa = new State(BigInteger.valueOf((int)a.getChar()-'A'),sigma);
            if(b.isSymbolic()) sb = labels2State(b,false);
            else sb = new State(BigInteger.valueOf((int)b.getChar()-'A'),sigma);

            //build(cir);
            state = cir.startExecuting(State.concatenate(sa,sb));
			state = notCircuit.startExecuting(state);
            //state = PartialCircuit.create(cir,State.concatenate(sa,sb))
            //    .startExecuting();
            // FIXME zero extend the results from this, not sign extend
        }

        private void buildValueState(AstNode node) {
            AstValueNode vnode = (AstValueNode)(node.getData());
            state = new State(BigInteger.valueOf(vnode.getValue()),
                    bitSize.bitsFor(node));
            if(vnode.getValue()<0) isSigned=true;
        }

    }

    public static State execute(BigInteger[] sdnalbs, 
            BigInteger[] cdnalbs, AstNode root,BitSizeCalculator bsc) 
    {
        //AstPrinter.print(root,System.err); System.err.println();
        AstGCExecutor exec = new AstGCExecutor(bsc);
        return exec.execute(root,sdnalbs,cdnalbs);
        //return exec.executeSubtree(root.children()[0].children()[0].children()[0].children()[2].children()[1]).state;
//        return exec.executeSubtree(root.children()[0].children()[0].children()[0].children()[2].children()[1]).state;
    }

    public State execute(AstNode root, 
        BigInteger[] sdnalbs, BigInteger[] cdnalbs) 
    { 
		assert sdnalbs.length>=serverExtraLabels;
		assert cdnalbs.length>=clientExtraLabels;
		assert !alreadyExecuted;
		this.sdnalbs = sdnalbs;
		this.cdnalbs = cdnalbs;
		alreadyExecuted = true;
		return executeSubtree(root).state; 
    }

    public BigInteger localEval(AstNode root, LeafEval leafEval)
    {
		if(!AstNode.LOCAL_EVAL_ENABLED) return BigInteger.ZERO;
		// enable local eval mods if needed
		if(labelIndex==null)
		{
			labelIndex = new AstVisitedMap<Integer>();
			localValue = new AstVisitedMap<Integer>();
			serverExtraLabels = clientExtraLabels = 0;
		}
		// explore tree for index and sizes
		bitsBeingAdded = new BitSet();
		bitsAdded=0;
		localEvalRecur(root,leafEval,true);
		BitSet bs = bitsBeingAdded;
		bitsBeingAdded=null;
		return bitSet2BigInteger(bs);
    }
	public int getBitsAdded() { return bitsAdded; }
	private static BigInteger bitSet2BigInteger(BitSet bs)
	{
		byte[] res = new byte[bs.length()/8+1];
		for(int i=0;i<bs.length();++i)
			if(bs.get(i)) res[res.length-1-i/8]|=(1<<(i%8));
		return new BigInteger(res);
	}

	private void reserveGarbledLabels(AstNode node)
	{
		assert !node.needsGarbled();
		if(node.dependsOnA())
		{	int w=bitSize.bitsFor(node);
			labelIndex.visit(node,serverExtraLabels);
			serverExtraLabels+=w;
		}else if(node.dependsOnB())
		{	int w=bitSize.bitsFor(node);
			labelIndex.visit(node,clientExtraLabels);
			clientExtraLabels+=w;
		}
	}
	private int localEvalRecur(AstNode node, LeafEval leafEval, 
			boolean localRoot)
	{
		// silent fix if value doesn't make sense
		if(node.needsGarbled()) localRoot=false;
		if(labelIndex.isVisited(node))
		{
			if((node.dependsOnA() ^ node.dependsOnB()) &&
					localRoot && labelIndex.valueAt(node)==null)
				// no labels were reserved the last time I visited
				reserveGarbledLabels(node);
			if(hasLocalValue(node,leafEval)) return localValue.valueAt(node);
			else return 0;	// something garbage that should not be used by
							//   caller
		}
		// reserve garbled label slots if needed
		if(node.needsGarbled()) labelIndex.visit(node,null);
		else if(!localRoot)     labelIndex.visit(node,null);
		else if(node.dependsOnA() || node.dependsOnB())
			reserveGarbledLabels(node);

		int nodevalue = 0;
		AstNode[] child = node.children();
		if(child.length==0 && hasLocalValue(node,leafEval)) 
			nodevalue=leafEval.eval(node);
		for(int i=0;i<child.length;++i)
		{	int res=localEvalRecur(child[i],leafEval,node.needsGarbled());
			if(i==0) nodevalue=res;
			else if(node.getType()==AstAddNode.class) nodevalue+=res;
			else if(node.getType()==AstMinNode.class) 
				nodevalue=Math.min(nodevalue,res);
			else if(node.getType()==AstMaxNode.class)
				nodevalue=Math.max(nodevalue,res);
			else throw new AstReducer.UnknownNodeException(child[i]);
		}
		if(hasLocalValue(node,leafEval)) 
		{	int w = bitSize.bitsFor(node);
			for(int i=0;i<w;++i)
				bitsBeingAdded.set(bitsAdded+i,(nodevalue&(i<<1))!=0);
			bitsAdded+=w;
			localValue.visit(node,nodevalue);
		}
		return nodevalue;
	}
	private boolean hasLocalValue(AstNode node, LeafEval leafEval)
	{
		if(node.needsGarbled()) return false;
		if(node.dependsOnA()) return leafEval.serverSide();
		if(node.dependsOnB()) return !leafEval.serverSide();
		return true;
	}

    public int localServerBitCount() { return serverExtraLabels; }
    public int localClientBitCount() { return clientExtraLabels; }

	/*
	   Returns a State object if they can be composed from
	   the BigInteger label arrays (sdnalbs or cdnalbs). Otherwise returns null.
	   Right now, it returns non-null only if it represents value that has
	   been locally evaluated by one of the parties
	*/
	private State labels2State(AstNode node)
	{
		if(labelIndex==null) return null;	// check if local eval enabled
		// null if nobody can locally evaluate it
		if(node.needsGarbled()) return null;
		if(!labelIndex.isVisited(node)) return null;
		int ind=labelIndex.valueAt(node);
		int w=bitSize.bitsFor(node);
		if(node.dependsOnA()) 
		{	ind+=sdnalbs.length-serverExtraLabels;
			return State.fromLabels(sdnalbs,ind,ind+w);
		}
		if(node.dependsOnB()) 
		{	ind+=cdnalbs.length-clientExtraLabels;
			return State.fromLabels(cdnalbs,ind,ind+w);
		}
		return null;
	}
	/*
	   Returns a State object if they can be composed from
	   the BigInteger label arrays (sdnalbs or cdnalbs). Otherwise returns null.
	   Returns non-null if chRef.isSymbolic() is true
	*/
	private State labels2State(AstCharRef chRef, boolean server)
	{ 	if(!chRef.isSymbolic()) return null;
		return State.fromLabels(server?sdnalbs:cdnalbs,
				chRef.getId()*sigma,chRef.getId()*sigma+sigma);
	}

    public AstGCExecutor(BitSizeCalculator bitSize)
    {
      this.bitSize = bitSize;
      this.visited = new AstVisitedMap<NodeData[]>();
      addCircuit = new ADD_2L_Lplus1[MAX_WIDTH];
      minCircuit = new MIN_2L_L[MAX_WIDTH];
      maxCircuit = new MAX_2L_L[MAX_WIDTH];
      smaxCircuit = new SMAX_2L_L[MAX_WIDTH+1];
      nequCircuit = new NEQUAL_2L_1[MAX_WIDTH];
	  notCircuit = new NOT();
	  build(notCircuit);
      for(int i=1;i<MAX_WIDTH;++i)
      { addCircuit[i] = new ADD_2L_Lplus1(i);
        build(addCircuit[i]);
        minCircuit[i] = new MIN_2L_L(i);
        build(minCircuit[i]);
        maxCircuit[i] = new MAX_2L_L(i);
        build(maxCircuit[i]);
        if(i>1)
        { nequCircuit[i] = new NEQUAL_2L_1(i);
          build(nequCircuit[i]);
          smaxCircuit[i] = new SMAX_2L_L(i);
          build(smaxCircuit[i]);
        }
      }
    }

    private NodeData executeSubtree(AstNode node)
    {
        // If I have already visited this node, return reference to
        // previously computed object
        if (visited.isVisited(node))
            return visited.valueAt(node)[0];

		State temp = labels2State(node);
		if(temp!=null)
		{	// we do not need to go further
			NodeData rv = new NodeData();
			rv.state = temp;
			rv.isSigned = bitSize.needsNeg(node);
			return rv;
		}

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
        NodeData last = executeSubtree(children[i]);
        for (--i; i >= 0; --i) {
            data[i] = new NodeData();
            data[i].calculateFromChildren(node, executeSubtree(children[i]), 
                    last);
            last = data[i];
        }

        return last;
    }

}
