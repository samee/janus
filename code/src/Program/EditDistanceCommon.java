// Copyright (C) 2010 by Yan Huang <yhuang@virginia.edu>

package Program;

import java.io.PrintStream;
import java.math.*;
import java.util.Map;

import ast.AstCharRef;
import ast.AstGCExecutor;
import ast.AstNode;
import ast.circuit.AstCircuit;
import ast.circuit.AstDefaultCharTraits;
import YaoGC.State;
import Utils.StopWatch;

public class EditDistanceCommon extends ProgCommon {
	static int sdnaLen;
	static int cdnaLen;
	static String strSdna, strCdna; 
	public static int sigma;

	static int bitLength(int x) {
		return BigInteger.valueOf(x).bitLength();
	}

	public static void initCircuits() {
	}


        private static void compareWithGcc(AstNode root)
        {
          try
          {
            PrintStream cfile = new PrintStream("cfile.c");
            cfile.println("// "+ast.AstNodeCounter.count(root));
            ast.AstCexprPrinter.printFull(root,cfile);
            cfile.close();
          }
          catch(java.io.IOException ex)
          {
            ex.printStackTrace();
            System.exit(1);
          }
          StopWatch.taskTimeStamp("Done printing to cfile.c");
        }

	public static State execCircuit(BigInteger[] sdnalbs, BigInteger[] cdnalbs)
			throws Exception {

          System.err.println("Server input: "+strSdna);
          System.err.println("Client input: "+strCdna);
          final EditDistance ed = new EditDistance(strSdna,strCdna);
          StopWatch.taskTimeStamp("Expression reductions done");
          test.SplitOpsTest.test(ed.getRoot());
          /*
          ast.AstPrinter.print(ed.getRoot(),System.err);
          System.err.println();
          */
          // print these only on one side
          //if(YaoGC.Circuit.isForGarbling) compareWithGcc(ed.getRoot());

          AstGCExecutor.BitSizeCalculator bsc 
            = new AstGCExecutor.BitSizeCalculator() {
              public int bitCount(int value) {
                int rv=0;
                assert value>=0;
                while(value!=0) { value>>=1; rv++; }
                return rv==0?1:rv;
              }
              public boolean needsNeg(AstNode node)
                { return ed.nodeLowerLim(node)<0; }
              public int abs(int x) { return x<0?-x:x; }

              public int bitsFor(ast.AstNode node) { 
                int rv = bitCount(ed.nodeUpperLim(node));
                boolean oneMore = needsNeg(node);
                ast.AstNode[] child = node.children();
                for(int i=0;i<child.length;++i) 
                { int t = bitCount(ed.nodeUpperLim(child[i]));
                  if(t>rv) rv=t;
                  oneMore = oneMore || needsNeg(child[i]);
                }
                return oneMore?rv+1:rv;
              }
            };
          // TODO timestamp here

          return AstGCExecutor.execute(sdnalbs,cdnalbs,ed.getRoot(),bsc);
	}
}
