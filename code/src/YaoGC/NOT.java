// Copyright (C) 2010 by Yan Huang <yhuang@virginia.edu>

package YaoGC;

public class NOT extends CompositeCircuit {
    public NOT() {
	super(1, 1, 1, "NOT");
    }

    protected void createSubCircuits() throws Exception {
	subCircuits[0] = new XOR_2_1();

	super.createSubCircuits();
    }

    protected void connectWires() {
	inputWires[0].connectTo(subCircuits[0].inputWires, 0);
    }

    protected void defineOutputWires() {
	outputWires[0] = subCircuits[0].outputWires[0];
    }

    protected void fixInternalWires() {
    	Wire internalWire = subCircuits[0].inputWires[1];
    	internalWire.fixWire(1);
    }
}
