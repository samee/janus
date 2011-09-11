// Copyright (C) 2010 by Yan Huang <yhuang@virginia.edu>

package Program;

import java.io.PrintStream;
import java.math.*;
import java.util.Map;

import ast.AstCharRef;
import ast.AstGCExecutor;
import ast.AstNode;
import ast.apps.AstSWSimilarityNode;
import ast.circuit.AstCircuit;
import ast.circuit.AstDefaultCharTraits;
import YaoGC.State;
import Utils.StopWatch;

public class SmithWatermanCommon extends ProgCommon {
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
          int clen = strSdna.length()>strCdna.length()?
            strSdna.length():strCdna.length();
          int cost[] = new int[clen];
          for (int i=0;i<cost.length;++i) cost[i]=i+1;
          final SmithWaterman sw = new SmithWaterman(strSdna,strCdna,cost);
          StopWatch.taskTimeStamp("Expression reductions done");
          /*
          ast.AstPrinter.print(sw.getRoot(),System.err);
          System.err.println();
          */
          test.SplitOpsTest.test(sw.getRoot());
          // print these only on one side
          //if(YaoGC.Circuit.isForGarbling) compareWithGcc(sw.getRoot());

          // TODO refactor this out into a DefaultBitSizeCalculator class
          AstGCExecutor.BitSizeCalculator bsc 
            = new AstGCExecutor.BitSizeCalculator() {
              public int bitCount(int value) {
                int rv=0;
                assert value>=0;
                while(value!=0) { value>>=1; rv++; }
                return rv==0?1:rv;
              }
              // without sign bit provision
              public int bitsForOneNode(AstNode node) {
                int u = bitCount(abs(sw.nodeUpperLim(node)));
                int l = bitCount(abs(sw.nodeLowerLim(node)));
                int rv = u>l?u:l;
                return rv;
              }
              public boolean needsNeg(AstNode node)
              { if(nodeNeedsNeg(node)) return true;
                ast.AstNode[] child = node.children();
                for(int i=0;i<child.length;++i)
                  if(nodeNeedsNeg(child[i])) return true;
                return false;
              }
              public boolean nodeNeedsNeg(AstNode node)
                { return sw.nodeLowerLim(node)<0; }
              public int abs(int x) { return x<0?-x:x; }

              public int bitsFor(ast.AstNode node) { 
                int rv = bitsForOneNode(node);
                boolean oneMore = nodeNeedsNeg(node);
                ast.AstNode[] child = node.children();
                for(int i=0;i<child.length;++i) 
                { int t = bitsForOneNode(child[i]);
                  if(t>rv) rv=t;
                  oneMore = oneMore || nodeNeedsNeg(child[i]);
                }
                return oneMore?rv+1:rv;
              }
            };
          // TODO timestamp here

//          return AstGCExecutor.execute(sdnalbs,cdnalbs,sw.getRoot(),bsc);
          return AstGCExecutor.execute(sdnalbs,cdnalbs,sw.getRoot(),bsc);
	}
}
