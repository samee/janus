package ast;

import java.io.PrintStream;

import ast.apps.AstSWSimilarityNode;

// assumes things like add,min,max will have at least 2 children

public class AstCexprPrinter {
	// Used for passing an int by reference
	private static class MutableInt {
		public int value;

		public MutableInt(int v) {
			value = v;
		}
	}

	/** Only two public method in this class :) */
        // prints the expressions evaluating the tree, and returns
        //   the variable name for the root node
	public static String print(AstNode root, PrintStream out) { 
		// vmap contains labels for visited nodes
		AstVisitedMap<String> vmap = new AstVisitedMap<String>();
		MutableInt vc = new MutableInt(1); // serial number of the next label
		printSubtree(root, out, vmap, vc);
                return nodeVarName(root,vmap);
	}
        // Prints misc codes before and after the actual expression
        //   to make full compileable code. The actual expression is printed
        //   by calling print()
        public static void printFull(AstNode root, PrintStream out)
        {
          out.println("inline int min(int a,int b) { return a<b?a:b; }");
          out.println("inline int max(int a,int b) { return a>b?a:b; }");
          out.println("int myfunc(char A[],char B[]) {");
          String rootVar = print(root,out);
          out.println("return "+rootVar+";\n}");
        }

        private static String nodeVarName (AstNode node,
            AstVisitedMap<String> vmap) {
          return vmap.valueAt(node);
        }

	private static void printSubtree(AstNode node, PrintStream out,
			AstVisitedMap<String> vmap, MutableInt vc) {
          if(vmap.isVisited(node)) return;
          AstNode[] child = node.children();
          for(int i=0;i<child.length;++i) printSubtree(child[i],out,vmap,vc);
          // recurse before visited, assumes DAG
          vmap.visit(node,"n"+vc.value++);
          out.print("int "+nodeVarName(node,vmap)+" = ");
          AstNodeData data = node.getData();
          if (node.getType() == AstValueNode.class)
            out.print(((AstValueNode) data).getValue());
          else if (node.getType() == AstNequNode.class) {
            AstNequNode nequ = (AstNequNode) data;
            AstCharRef a = nequ.getOperandA(), b = nequ.getOperandB();
            String cha = (a.isSymbolic() ? "A[" + a.getId() + "]" : "'"
                + a.getChar() + "'");
            String chb = (b.isSymbolic() ? "B[" + b.getId() + "]" : "'"
                + b.getChar() + "'");
            out.print(cha + "!=" + chb);
          } else if (node.getType() == AstSWSimilarityNode.class) {
            AstSWSimilarityNode sim = (AstSWSimilarityNode) data;
            AstCharRef a = sim.getOperandA(), b = sim.getOperandB();
            String cha = (a.isSymbolic() ? "A[" + a.getId() + "]" : "'"
                + a.getChar() + "'");
            String chb = (b.isSymbolic() ? "B[" + b.getId() + "]" : "'"
                + b.getChar() + "'");
            out.print("SwSimilar(" + cha + ", " + chb + ")");
          } else if(node.getType() == AstAddNode.class) {
            out.print(nodeVarName(child[0],vmap));
            for(int i=1;i<child.length;++i)
              out.print(" + "+nodeVarName(child[i],vmap));
          } else {
            String operation;
            if (node.getType() == AstMinNode.class)
              operation = "min";
            else if (node.getType() == AstMaxNode.class)
              operation = "max";
            else
              operation = "---";
            for(int i=0;i<child.length-1;++i)
              out.print(operation+"("+nodeVarName(child[i],vmap)+", ");
            out.print(nodeVarName(child[child.length-1],vmap));
            for(int i=0;i<child.length-1;++i)
              out.print(')');
          }
          out.println(";");
	}
}
