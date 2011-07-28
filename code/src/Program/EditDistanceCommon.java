// Copyright (C) 2010 by Yan Huang <yhuang@virginia.edu>

package Program;

import java.math.*;
import java.util.Map;

import ast.AstCharRef;
import ast.circuit.AstCircuit;
import ast.circuit.AstDefaultCharTraits;
import YaoGC.State;

public class EditDistanceCommon extends ProgCommon {
	static int sdnaLen;
	static int cdnaLen;
	static String strSdna, strCdna; 
	public static int sigma;
        private static AstCircuit cir;

	static int bitLength(int x) {
		return BigInteger.valueOf(x).bitLength();
	}

	public static void initCircuits() {
          System.err.println("Server input: "+strSdna);
          System.err.println("Client input: "+strCdna);
          EditDistance ed = new EditDistance(strSdna,strCdna);
          AstCircuit.Generator gen = new AstCircuit.Generator();
          gen.charTraits = new AstDefaultCharTraits(sigma);
          /*
          ast.AstPrinter.print(ed.getRoot(),System.err);
          System.err.println();
          */
          cir = gen.generate(ed.getRoot());
	}

        public static int inputBits() { return cir.inputWires.length; }

	public static State execCircuit(BigInteger[] sdnalbs, BigInteger[] cdnalbs)
			throws Exception {

          // make State object and call cir.startExecuting(state)
          BigInteger[] inputLabels = new BigInteger[inputBits()];
          //   but first, convert string index to circuit input index
          for(Map.Entry<AstCharRef,Integer> mentry : cir.getInputs().entrySet())
          {
            int i = mentry.getValue(),j;
            AstCharRef chref = mentry.getKey();
            assert chref.isSymbolic();
            int stringInd = chref.getId();  // id was the same as index
            // intentionally comparing reference, not .equals()
            if(chref.getStringRef()==strSdna)  
              for(j=0;j<sigma;++j) inputLabels[i+j]=sdnalbs[sigma*stringInd+j];
            else for(j=0;j<sigma;++j) 
              inputLabels[i+j]=cdnalbs[sigma*stringInd+j];
          }
          for(int i=0;i<inputLabels.length;++i)
            assert inputLabels[i]!=null:i+"";
          return cir.startExecuting(State.fromLabels(inputLabels));
	}
}
