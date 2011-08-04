package ast;

import ast.circuit.PartialCircuit;
import java.math.BigInteger;
import Program.EditDistance;
import YaoGC.*;

public class AstGCExecutor {

    private BigInteger[] sdnalbs, cdnalbs;
    private AstVisitedMap<NodeData[]> visited;
    private static final int MAX_WIDTH = 40;
    private ADD_2L_Lplus1[] addCircuit;
    private MIN_2L_L[] minCircuit;
    private MAX_2L_L[] maxCircuit;
    private NEQUAL_2L_1[] nequCircuit;

    // Should return the number of bits required to represent 
    //   any given node
    public static interface BitSizeCalculator
    {
        public int bitsFor(AstNode node);
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

        public State resizeState(State s,int width)
        {
            if(s.getWidth()==width) return s; // nop
            else if(s.getWidth()>width)
              return State.extractState(s,0,width);
            else return State.concatenate(
                    new State(BigInteger.ZERO,width-s.getWidth()),s);
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
                MAX_2L_L cir = maxCircuit[w];
                state = //PartialCircuit.create(cir,
                      cir.startExecuting(
                        State.concatenate(
                            resizeState(childLeft.state,w),
                            resizeState(childRight.state,w)))
;//                    .startExecuting();
            }
        }

        public void calculateFromLeaf(AstNode leaf) {
            if(leaf.getType()==AstValueNode.class)
                buildValueState(leaf);
            else if(leaf.getType()==AstNequNode.class)
                executeNEQU(leaf);
            else
                assert false : "Unknown type of leaf nodes";
        }

        private void executeNEQU(AstNode node) {
            AstNequNode ann = (AstNequNode)(node.getData());
            int sigma = 2;     // FIXME sigma
            NEQUAL_2L_1 cir = nequCircuit[sigma];
            State sa,sb;

            AstCharRef a = ann.getOperandA(), b = ann.getOperandB();
            if(a.isSymbolic()) 
            {   int ind = a.getId()*sigma;
                assert sdnalbs[ind]!=null && sdnalbs[ind+1]!=null;
                sa = State.fromLabels(sdnalbs,ind,ind+sigma);
            }
            else sa = new State(BigInteger.valueOf((int)a.getChar()-'A'),sigma);
            if(b.isSymbolic()) 
            {   int ind = b.getId()*sigma;
                assert cdnalbs[ind]!=null && cdnalbs[ind+1]!=null;
                sb = State.fromLabels(cdnalbs,ind,ind+sigma);
            }
            else sb = new State(BigInteger.valueOf((int)b.getChar()-'A'),sigma);

            build(cir);
            state = cir.startExecuting(State.concatenate(sa,sb));
            //state = PartialCircuit.create(cir,State.concatenate(sa,sb))
            //    .startExecuting();
            // FIXME zero extend the results from this, not sign extend
        }

        private void buildValueState(AstNode node) {
            AstValueNode vnode = (AstValueNode)(node.getData());
            state = new State(BigInteger.valueOf(vnode.getValue()),
                    bitSize.bitsFor(node));
        }

    }

    /** The only public method in this class :) . */
    public static State execute(BigInteger[] sdnalbs, 
            BigInteger[] cdnalbs, AstNode root,BitSizeCalculator bsc) 
    {
        //AstPrinter.print(root,System.err); System.err.println();
        AstGCExecutor exec = new AstGCExecutor(sdnalbs,cdnalbs,bsc);
        return exec.executeSubtree(root).state;
        //return exec.executeSubtree(root.children()[0].children()[0].children()[0].children()[2].children()[1]).state;
//        return exec.executeSubtree(root.children()[0].children()[0].children()[0].children()[2].children()[1]).state;
    }

    private AstGCExecutor(BigInteger[] sdnalbs,BigInteger[] cdnalbs,
            BitSizeCalculator bitSize)
    { this.sdnalbs = sdnalbs;
      this.cdnalbs = cdnalbs;
      this.bitSize = bitSize;
      this.visited = new AstVisitedMap<NodeData[]>();
      addCircuit = new ADD_2L_Lplus1[MAX_WIDTH];
      minCircuit = new MIN_2L_L[MAX_WIDTH];
      maxCircuit = new MAX_2L_L[MAX_WIDTH];
      nequCircuit = new NEQUAL_2L_1[MAX_WIDTH];
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
        }
      }
    }

    private NodeData executeSubtree(AstNode node)
    {
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
