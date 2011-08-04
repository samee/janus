// Copyright (C) 2010 by Yan Huang <yhuang@virginia.edu>

package Program;

import java.math.*;
import java.util.Map;

import ast.AstCharRef;
import ast.AstGCExecutor;
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


	public static State execCircuit(BigInteger[] sdnalbs, BigInteger[] cdnalbs)
			throws Exception {

          System.err.println("Server input: "+strSdna);
          System.err.println("Client input: "+strCdna);
          final EditDistance ed = new EditDistance(strSdna,strCdna);
          StopWatch.taskTimeStamp("Expression reductions done");
          AstGCExecutor.BitSizeCalculator bsc 
            = new AstGCExecutor.BitSizeCalculator() {
              public int bitCount(int value) {
                int rv=0;
                while(value!=0) { value>>=1; rv++; }
                return rv==0?1:rv;
              }
              public int bitsFor(ast.AstNode node) { 
                int rv = bitCount(ed.nodeUpperLim(node));
                ast.AstNode[] child = node.children();
                for(int i=0;i<child.length;++i) 
                { int t = bitCount(ed.nodeUpperLim(child[i]));
                  if(t>rv) rv=t;
                }
                return rv;
              }
            };
          // TODO timestamp here

          return AstGCExecutor.execute(sdnalbs,cdnalbs,ed.getRoot(),bsc);
	}
}
