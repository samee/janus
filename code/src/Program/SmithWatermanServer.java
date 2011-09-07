// Copyright (C) 2010 by Yan Huang <yhuang@virginia.edu>

package Program;

import java.math.*;

import YaoGC.*;
import Utils.*;

public class SmithWatermanServer extends ProgServer {
	private BigInteger sdna;
	private BigInteger ssecmask;
	private BigInteger csecmask;
	private int cSecBitLen;
	private BigInteger[][] sdnalps, cdnalps;

	private State outputState;

	public SmithWatermanServer(BigInteger dna, BigInteger secMask, int length) {
		sdna = dna;
		ssecmask = secMask;
                cSecBitLen = 0;

		// length of dna sequence. Effective bit length of variable dna is sigma
		// times longer
		SmithWatermanCommon.sdnaLen = length;

		SmithWatermanCommon.strSdna = SmithWatermanServer.biToString(sdna,
				secMask, SmithWatermanCommon.sigma, SmithWatermanCommon.sdnaLen);
                assert SmithWatermanCommon.sdnaLen 
                  == SmithWatermanCommon.strSdna.length();
	}

	protected void init() throws Exception {
		SmithWatermanCommon.cdnaLen = SmithWatermanCommon.ois.readInt();
		SmithWatermanCommon.strCdna = (String) SmithWatermanCommon.ois.readObject();
		csecmask= (BigInteger) SmithWatermanCommon.ois.readObject();
		SmithWatermanCommon.oos.writeInt(SmithWatermanCommon.sdnaLen);
		SmithWatermanCommon.oos.writeObject(SmithWatermanCommon.strSdna);
		SmithWatermanCommon.oos.writeObject(ssecmask);
		SmithWatermanCommon.oos.flush();

		super.init();

		generateLabelPairsForDNAs();


	}

	private void generateLabelPairsForDNAs() {
		sdnalps = new BigInteger[SmithWatermanCommon.sigma
				* SmithWatermanCommon.sdnaLen][2];
		cdnalps = new BigInteger[SmithWatermanCommon.sigma
				* SmithWatermanCommon.cdnaLen][2];

		for (int i = 0; i < sdnalps.length; i++) {
			if (ssecmask.testBit(i))
					sdnalps[i] = Wire
							.newLabelPair();
			else
					sdnalps[i] = null;
		}

		for (int i = 0; i < cdnalps.length; i++) {
			if (csecmask.testBit(i))
                        { 
					cdnalps[i] = Wire
							.newLabelPair();
                                        cSecBitLen++;
                        }
			else
					cdnalps[i] = null;
		}
	}

	protected void execTransfer() throws Exception {
		int bytelength = (Wire.labelBitLength - 1) / 8 + 1;

		for (int i = 0; i < sdnalps.length; i++) {
			if (ssecmask.testBit(i)) {
				int idx = sdna.testBit(i) ? 1 : 0;
				Utils.writeBigInteger(sdnalps[i][idx], bytelength,
						SmithWatermanCommon.oos);
			}
		}

		SmithWatermanCommon.oos.flush();
		StopWatch.taskTimeStamp("sending labels for selfs inputs");

                /*
                System.err.println("Server DNA: ");
                for(int i=0;i<SmithWatermanCommon.sdnaLen*
                    SmithWatermanCommon.sigma;++i)
                  System.err.print(sdna.testBit(i)?1:0);
                System.err.println();
                */

		BigInteger[][] temp 
                  = new BigInteger[cSecBitLen][2];
		for (int i = 0, j = 0; i < cdnalps.length; i++)
			if (csecmask.testBit(i))
				temp[j++] = cdnalps[i];
		snder.execProtocol(temp);
		StopWatch.taskTimeStamp("sending labels for peers inputs");

                /*
                System.err.println("Server labels:");
                for(int i=0;i<sdnalps.length;++i)
                  if (ssecmask.testBit(i))
                    System.err.println(sdnalps[i][sdna.testBit(i)?1:0]);
                  else System.err.println("null");
                System.err.println("Oblivious send:");
                for(int i=0;i<cdnalps.length;++i)
                  if(csecmask.testBit(i))
                    System.err.println(cdnalps[i][0]+" "+cdnalps[i][1]);
                  else System.err.println("null");
                  */
	}

	protected void execCircuit() throws Exception {
		BigInteger[] sdnalbs = new BigInteger[SmithWatermanCommon.sigma
				* SmithWatermanCommon.sdnaLen];
		BigInteger[] cdnalbs = new BigInteger[SmithWatermanCommon.sigma
				* SmithWatermanCommon.cdnaLen];

		for (int i = 0; i < sdnalps.length; i++)
                  if(sdnalps[i]!=null)
			sdnalbs[i] = sdnalps[i][0];

		for (int i = 0; i < cdnalps.length; i++)
                  if(cdnalps[i]!=null)
			cdnalbs[i] = cdnalps[i][0];

		outputState = SmithWatermanCommon.execCircuit(sdnalbs, cdnalbs);
	}

	protected void interpretResult() throws Exception {
		BigInteger[] outLabels = (BigInteger[]) SmithWatermanCommon.ois
				.readObject();

		BigInteger output = BigInteger.ZERO;
		for (int i = 0; i < outLabels.length; i++) {
			if (outputState.wires[i].value != Wire.UNKNOWN_SIG) {
				if (outputState.wires[i].value == 1)
					output = output.setBit(i);
				continue;
			} else if (outLabels[i]
					.equals(outputState.wires[i].invd ? outputState.wires[i].lbl
							: outputState.wires[i].lbl.xor(Wire.R.shiftLeft(1)
									.setBit(0)))) {
				output = output.setBit(i);
			} else if (!outLabels[i]
					.equals(outputState.wires[i].invd ? outputState.wires[i].lbl
							.xor(Wire.R.shiftLeft(1).setBit(0))
							: outputState.wires[i].lbl))
				throw new Exception("Bad label encountered: i = "
						+ i
						+ "\t"
						+ outLabels[i]
						+ " != ("
						+ outputState.wires[i].lbl
						+ ", "
						+ outputState.wires[i].lbl.xor(Wire.R.shiftLeft(1)
								.setBit(0)) + ")");
		}

		System.out.println("output (pp): " + output + " ("
                    +outLabels.length+" bits)");
		StopWatch.taskTimeStamp("output labels received and interpreted");
	}

	static String biToString(BigInteger encoding, int sigma, int n) {
		StringBuilder res = new StringBuilder("");
		BigInteger mask = BigInteger.ONE.shiftLeft(sigma).subtract(
				BigInteger.ONE);

		for (int i = 0; i < n; i++) {
			res.append((char) (encoding.shiftRight(i * sigma).and(mask)
					.intValue() +'A')); // offset by 36 because '#' is used as a
										// special character that stands for a
										// symbolic value
		}
		return res.toString();
	}

	static String biToString(BigInteger encoding, BigInteger secMask,
			int sigma, int n) {
		StringBuilder res = new StringBuilder("");
		BigInteger mask = BigInteger.ONE.shiftLeft(sigma).subtract(
				BigInteger.ONE);

		for (int i = 0; i < n; i++) {
			if (secMask.testBit(sigma * i)) // '1' implies secret
				res.append('#');
			else
				res.append((char) (encoding.shiftRight(i * sigma).and(mask)
						.intValue() + 'A')); // offset by 36 because '#' is used
			// as a special character that
			// stands for a symbolic value
		}
		return res.toString();
	}

	protected void verify_result() throws Exception {
		BigInteger cdna = (BigInteger) SmithWatermanCommon.ois.readObject();

		String sdnaStr = biToString(sdna, SmithWatermanCommon.sigma,
				SmithWatermanCommon.sdnaLen);
		String cdnaStr = biToString(cdna, SmithWatermanCommon.sigma,
				SmithWatermanCommon.cdnaLen);
                System.err.println("Full data:");
                System.err.println("  Server: "+sdnaStr);
                System.err.println("  Client: "+cdnaStr);

		int[][] D = new int[SmithWatermanCommon.sdnaLen + 1][SmithWatermanCommon.cdnaLen + 1];

		for (int i = 0; i < SmithWatermanCommon.sdnaLen + 1; i++)
			D[i][0] = 0;

		for (int j = 0; j < SmithWatermanCommon.cdnaLen + 1; j++)
			D[0][j] = 0;

		for (int i = 1; i < SmithWatermanCommon.sdnaLen + 1; i++)
			for (int j = 1; j < SmithWatermanCommon.cdnaLen + 1; j++) {
                          int m = sdnaStr.charAt(i-1)==cdnaStr.charAt(j-1)?1:0;
                          m+=D[i-1][j-1];
                          if(m<0) m=0;
                          for(int k=0;k<i;++k)
                          { int t=D[k][j]-(i-k);
                            if(m<t) m=t;
                          }
                          for(int k=0;k<j;++k)
                          { int t=D[i][k]-(j-k);
                            if(m<t) m=t;
                          }

                          D[i][j]=m;
			}

		System.out.println("output (verify): "
				+ D[SmithWatermanCommon.sdnaLen][SmithWatermanCommon.cdnaLen]);
                /*
                // Print full matrix
                for(int i=1;i<D.length;++i)
                { for(int j=1;j<D[i].length;++j)
                    System.err.print(D[i][j]+" ");
                  System.err.println();
                }
                */
	}
}
