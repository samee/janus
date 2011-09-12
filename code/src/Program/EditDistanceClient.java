// Copyright (C) 2010 by Yan Huang <yhuang@virginia.edu>

package Program;

import java.math.*;

import ast.AstGCExecutor;
import ast.AstNode;
import YaoGC.*;
import Utils.*;

public class EditDistanceClient extends ProgClient {
	private BigInteger cdna;
	private BigInteger ssecmask;
	private BigInteger csecmask;
	private int sSecBitLen, cSecBitLen;
	private BigInteger[] sdnalbs, cdnalbs;
	private BigInteger extrabits;   // locally computed bits

	private State outputState;

        private AstGCExecutor.LeafEval leafEval = new AstGCExecutor.LeafEval(){
          public boolean serverSide() { return false; }
          public int eval(AstNode node)
          {
            if(node.getType()==ast.AstValueNode.class)
              return ((ast.AstValueNode)node.getData()).getValue();
            if(node.getType()==ast.AstNequNode.class)
            { ast.AstNequNode data = (ast.AstNequNode)node.getData();
              assert !data.getOperandA().isSymbolic();
              assert data.getOperandB().isSymbolic();
              char a = data.getOperandA().getChar();
              char b = EditDistanceCommon.strCdna.charAt(
                  data.getOperandB().getId());
              return a==b?0:1;
            }
            assert false;
            return 0;
          }
        };

	public EditDistanceClient(BigInteger dna, BigInteger secMask, int length) {
		cdna = dna;
		csecmask = secMask;
		EditDistanceCommon.cdnaLen = length;

		EditDistanceCommon.strCdna = EditDistanceServer.biToString(cdna,
				secMask, EditDistanceCommon.sigma, EditDistanceCommon.cdnaLen);
                cSecBitLen=0;
                for(int i=0;i<EditDistanceCommon.strCdna.length();++i) 
                  if(EditDistanceCommon.strCdna.charAt(i)=='#')
                    cSecBitLen+=EditDistanceCommon.sigma;
	}

	protected void init() throws Exception {
		EditDistanceCommon.oos.writeInt(EditDistanceCommon.cdnaLen);
		EditDistanceCommon.oos.writeObject(EditDistanceCommon.strCdna);
		EditDistanceCommon.oos.writeObject(csecmask);
		EditDistanceCommon.oos.flush();
		EditDistanceCommon.sdnaLen = EditDistanceCommon.ois.readInt();
		EditDistanceCommon.strSdna = (String) EditDistanceCommon.ois.readObject();
		ssecmask= (BigInteger) EditDistanceCommon.ois.readObject();

		extrabits = EditDistanceCommon.initAstExpr(leafEval);
		otNumOfPairs = cSecBitLen+EditDistanceCommon.clientExtraInputs;


		super.init();
	}

	protected void execTransfer() throws Exception {
		int bytelength = (Wire.labelBitLength - 1) / 8 + 1;
		int sBaseLen = EditDistanceCommon.sigma 
			* EditDistanceCommon.sdnaLen;
		int cBaseLen = EditDistanceCommon.sigma 
			* EditDistanceCommon.cdnaLen;

		sdnalbs = new BigInteger[sBaseLen+EditDistanceCommon.serverExtraInputs];
		for (int i = 0; i < sdnalbs.length; i++) {
			if (i>=sBaseLen || ssecmask.testBit(i))
				sdnalbs[i] = Utils.readBigInteger(bytelength,
						EditDistanceCommon.ois);
			else
				sdnalbs[i] = null;
		}
		StopWatch.taskTimeStamp("receiving labels for peer's inputs");

		cdnalbs = new BigInteger[cBaseLen+EditDistanceCommon.clientExtraInputs];
		BigInteger tmpchoice = BigInteger.ZERO;
		int otcount = 0;
		for (int i = 0; i < cdnalbs.length; i++) {
			if (i<cBaseLen && csecmask.testBit(i)) {
				if (cdna.testBit(i))
					tmpchoice = tmpchoice.setBit(otcount);
				otcount++;
			}else if(i>=cBaseLen)
			{	if(extrabits.testBit(i-cBaseLen))
					tmpchoice = tmpchoice.setBit(otcount);
				otcount++;
			}
		}

		System.err.println("Lengths = "+sdnalbs.length+" "+cdnalbs.length+
				" "+otcount);
		System.err.println("Extras = "+EditDistanceCommon.serverExtraInputs
				+" "+EditDistanceCommon.clientExtraInputs);
		System.err.println("BaseLens = "+sBaseLen+" "+cBaseLen);
		rcver.execProtocol(tmpchoice);
		BigInteger[] temp = rcver.getData();
		System.err.println("RecvLen = "+temp.length);
		for (int i = 0, j = 0; i < cdnalbs.length; i++)
			if (i>=cBaseLen || csecmask.testBit(i))
				cdnalbs[i] = temp[j++];

                /*
                System.err.println("Client DNA: ");
                for(int i=0;i<EditDistanceCommon.cdnaLen
                    *EditDistanceCommon.sigma;++i)
                  System.err.print(cdna.testBit(i)?1:0);
                System.err.println();
                System.err.println("Server labels:");
                for(int i=0;i<sdnalbs.length;++i)
                  System.err.println(sdnalbs[i]);
                System.err.println("Oblivious recv:");
                for(int i=0;i<cdnalbs.length;++i)
                  System.err.println(cdnalbs[i]);
                  */

		StopWatch.taskTimeStamp("receiving labels for self's inputs");
	}

	protected void execCircuit() throws Exception {
		outputState = EditDistanceCommon.execCircuit(sdnalbs, cdnalbs);
	}

	protected void interpretResult() throws Exception {
		EditDistanceCommon.oos.writeObject(outputState.toLabels());
		EditDistanceCommon.oos.flush();
	}

	protected void verify_result() throws Exception {
		EditDistanceCommon.oos.writeObject(cdna);
		EditDistanceCommon.oos.flush();
	}
}
