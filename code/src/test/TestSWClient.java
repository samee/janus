// Copyright (C) 2010 by Yan Huang <yhuang@virginia.edu>

package test;

import java.util.*;
import java.math.*;

import jargs.gnu.CmdLineParser;

import Utils.*;
import Program.*;
import YaoGC.Wire;

class TestSWClient {
	static BigInteger dna;
	static BigInteger secMask;
	static boolean autogen;
	static int n;

	static Random rnd = new Random();

	private static void printUsage() {
		System.out
				.println("Usage: java TestSWClient   [{-a, --autogen}] [{-n, --DNALength} length] [{-r, --iteration} iterCount] [{-s, --server} servername] [{-q, --seed} seedvalue] [{-g, --sigma} sigmaValue]");
	}

	private static void process_cmdline_args(String[] args) {
          Wire.labelBitLength = 32;
                Wire.R = new java.math.BigInteger(Wire.labelBitLength-1,Wire.rnd);
		CmdLineParser parser = new CmdLineParser();
		CmdLineParser.Option optionServerIPname = parser.addStringOption('s',
				"server");
		CmdLineParser.Option optionAuto = parser.addBooleanOption('a',
				"autogen");
		CmdLineParser.Option optionDNALength = parser.addIntegerOption('n',
				"DNALength");
		CmdLineParser.Option optionIterCount = parser.addIntegerOption('r',
				"iteration");
		CmdLineParser.Option optionSigma = parser
				.addIntegerOption('g', "sigma");
                CmdLineParser.Option optionSeed 
                  = parser.addLongOption('q',"seed");

		try {
			parser.parse(args);
		} catch (CmdLineParser.OptionException e) {
			System.err.println(e.getMessage());
			printUsage();
			System.exit(2);
		}

		autogen = (Boolean) parser.getOptionValue(optionAuto, false);
		n = ((Integer) parser.getOptionValue(optionDNALength, new Integer(100)))
				.intValue();
		ProgClient.serverIPname = (String) parser.getOptionValue(
				optionServerIPname, new String("localhost"));
		Program.iterCount = ((Integer) parser.getOptionValue(optionIterCount,
				new Integer(10))).intValue();
		SmithWatermanCommon.sigma = ((Integer) parser.getOptionValue(
				optionSigma, new Integer(2))).intValue();
                Long seed = (Long ) parser.getOptionValue(optionSeed);
                if(seed!=null) rnd = new Random(seed);
	}

	private static void generateData() throws Exception {
		dna = new BigInteger(SmithWatermanCommon.sigma * n, rnd);

                secMask = BigInteger.ZERO;
                BigInteger ones = BigInteger.valueOf((1 << SmithWatermanCommon.sigma) - 1);
                for (int i = 0; i < n; i++) {
                  secMask = secMask.shiftLeft(SmithWatermanCommon.sigma);
                  if(rnd.nextFloat()<1.2)
                    secMask = secMask.or(ones);
                }
	}

	public static void main(String[] args) throws Exception {
		StopWatch.pointTimeStamp("Starting program");
		process_cmdline_args(args);

		if (autogen)
			generateData();

		SmithWatermanClient swclient = new SmithWatermanClient(dna, secMask, n);
		swclient.run();
	}
}
