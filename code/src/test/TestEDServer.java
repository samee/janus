// Copyright (C) 2010 by Yan Huang <yhuang@virginia.edu>

package test;

import java.util.*;
import java.math.*;

import jargs.gnu.CmdLineParser;

import Utils.*;
import Program.*;
import YaoGC.Wire;

class TestEDServer {
	static BigInteger dna;
	static BigInteger secMask;
	static boolean autogen;
        static double prob;
	static int n;

	static Random rnd = new Random(0);

	private static void printUsage() {
		System.out
				.println("Usage: java TestEDServer [{-d, --dna} dna] [{-L, --EDBitLength} L] [{-a, --autogen}] [{-n, --DNALength} length]");
	}

	private static void process_cmdline_args(String[] args) {
          Wire.labelBitLength = 32;
                Wire.R = new java.math.BigInteger(Wire.labelBitLength-1,Wire.rnd);
		CmdLineParser parser = new CmdLineParser();
		CmdLineParser.Option optionAuto = parser.addBooleanOption('a',
				"autogen");
		CmdLineParser.Option optionDNALength = parser.addIntegerOption('n',
				"DNALength");
		CmdLineParser.Option optionSigma = parser
				.addIntegerOption('g', "sigma");
                CmdLineParser.Option optionSeed 
                  = parser.addLongOption('q',"seed");
                CmdLineParser.Option optionSecretProb
                  = parser.addDoubleOption('p',"probability");

		try {
			parser.parse(args);
		} catch (CmdLineParser.OptionException e) {
			System.err.println(e.getMessage());
			printUsage();
			System.exit(2);
		}

                prob = (Double) parser.getOptionValue(optionSecretProb,0.5);
		autogen = (Boolean) parser.getOptionValue(optionAuto, false);
		n = ((Integer) parser.getOptionValue(optionDNALength, new Integer(100)))
				.intValue();
		EditDistanceCommon.sigma = ((Integer) parser.getOptionValue(
				optionSigma, new Integer(2))).intValue();
                Long seed = (Long ) parser.getOptionValue(optionSeed);
                if(seed!=null) rnd = new Random(seed);
	}

	private static void generateData() throws Exception {
		dna = new BigInteger(EditDistanceCommon.sigma * n, rnd);
		
                /*
		BigInteger temp = new BigInteger(n, rnd);
		secMask = BigInteger.ZERO;
		BigInteger ones = BigInteger.valueOf((1 << EditDistanceCommon.sigma) - 1);
		for (int i = 0; i < n; i++) {
			secMask = secMask.shiftLeft(EditDistanceCommon.sigma);
			if (temp.testBit(i))
				secMask = secMask.or(ones);
		}
                */
                secMask = BigInteger.ZERO;
                BigInteger ones = BigInteger.valueOf((1 << EditDistanceCommon.sigma) - 1);
                for (int i = 0; i < n; i++) {
                  secMask = secMask.shiftLeft(EditDistanceCommon.sigma);
                  if(rnd.nextFloat()<prob)
                    secMask = secMask.or(ones);
                }
		// dna = EditDistanceServer.getDNAString(r, n);
	}

	public static void main(String[] args) throws Exception {
		StopWatch.pointTimeStamp("Starting program");
		process_cmdline_args(args);

		if (autogen)
			generateData();

		EditDistanceServer edserver = new EditDistanceServer(dna, secMask, n);
		edserver.run();
	}
}
