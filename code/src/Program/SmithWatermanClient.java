// Copyright (C) 2010 by Yan Huang <yhuang@virginia.edu>

package Program;

import java.math.*;

import YaoGC.*;
import Utils.*;

public class SmithWatermanClient extends ProgClient {
	private BigInteger cdna;
	private BigInteger ssecmask;
	private BigInteger csecmask;
	private int sSecBitLen, cSecBitLen;
	private BigInteger[] sdnalbs, cdnalbs;

	private State outputState;

	public SmithWatermanClient(BigInteger dna, BigInteger secMask, int length) {
		cdna = dna;
		csecmask = secMask;
		SmithWatermanCommon.cdnaLen = length;

		SmithWatermanCommon.strCdna = SmithWatermanServer.biToString(cdna,
				secMask, SmithWatermanCommon.sigma, SmithWatermanCommon.cdnaLen);
                cSecBitLen=0;
                for(int i=0;i<SmithWatermanCommon.strCdna.length();++i) 
                  if(SmithWatermanCommon.strCdna.charAt(i)=='#')
                    cSecBitLen+=SmithWatermanCommon.sigma;
	}

	protected void init() throws Exception {
		SmithWatermanCommon.oos.writeInt(SmithWatermanCommon.cdnaLen);
		SmithWatermanCommon.oos.writeObject(SmithWatermanCommon.strCdna);
		SmithWatermanCommon.oos.writeObject(csecmask);
		SmithWatermanCommon.oos.flush();
		SmithWatermanCommon.sdnaLen = SmithWatermanCommon.ois.readInt();
		SmithWatermanCommon.strSdna = (String) SmithWatermanCommon.ois.readObject();
		ssecmask= (BigInteger) SmithWatermanCommon.ois.readObject();

		otNumOfPairs = cSecBitLen;


		super.init();
	}

	protected void execTransfer() throws Exception {
		int bytelength = (Wire.labelBitLength - 1) / 8 + 1;

		sdnalbs = new BigInteger[SmithWatermanCommon.sigma
				* SmithWatermanCommon.sdnaLen];
		for (int i = 0; i < sdnalbs.length; i++) {
			if (ssecmask.testBit(i))
				sdnalbs[i] = Utils.readBigInteger(bytelength,
						SmithWatermanCommon.ois);
			else
				sdnalbs[i] = null;
		}
		StopWatch.taskTimeStamp("receiving labels for peer's inputs");

		cdnalbs = new BigInteger[SmithWatermanCommon.sigma
				* SmithWatermanCommon.cdnaLen];
		BigInteger tmpchoice = BigInteger.ZERO;
		for (int i = 0, j = 0; i < cdnalbs.length; i++)
			if (csecmask.testBit(i)) {
				if (cdna.testBit(i))
					tmpchoice = tmpchoice.setBit(j);
				j++;
			}

		rcver.execProtocol(tmpchoice);
		BigInteger[] temp = rcver.getData();
		for (int i = 0, j = 0; i < cdnalbs.length; i++)
			if (csecmask.testBit(i))
				cdnalbs[i] = temp[j++];

                /*
                System.err.println("Client DNA: ");
                for(int i=0;i<SmithWatermanCommon.cdnaLen
                    *SmithWatermanCommon.sigma;++i)
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
		SmithWatermanCommon.initCircuits();
		outputState = SmithWatermanCommon.execCircuit(sdnalbs, cdnalbs);
	}

	protected void interpretResult() throws Exception {
		SmithWatermanCommon.oos.writeObject(outputState.toLabels());
		SmithWatermanCommon.oos.flush();
	}

	protected void verify_result() throws Exception {
		SmithWatermanCommon.oos.writeObject(cdna);
		SmithWatermanCommon.oos.flush();
	}
}
