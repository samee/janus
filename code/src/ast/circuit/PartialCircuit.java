package ast.circuit;

import YaoGC.Circuit;
import YaoGC.CompositeCircuit;
import YaoGC.State;

// Implements a circuit with some inputs publicly disclosed at runtime
//   by a given State object
public class PartialCircuit extends CompositeCircuit
{
  private Circuit cir;
  private State state;

  public static PartialCircuit create(Circuit cir,State state) {
    int indeg=0;
    for(int i=0;i<cir.inputWires.length;++i)
      if(state.wires[i].lbl!=null) indeg++;
    return new PartialCircuit(cir,state,indeg);
  }

  private PartialCircuit(Circuit cir,State state,int indeg) {
    super(indeg,cir.outputWires.length,0,"PartialCircuit");
    this.cir = cir;
    this.state = state;
    try { cir.build(); build(); } 
    catch(Exception ex) { ex.printStackTrace();  System.exit(1); }
  }

  protected void createSubCircuits() {}
  protected void connectWires() {
    int i,j=0;
    for(i=0;i<cir.inputWires.length;++i)
      if(state.wires[i].lbl!=null) inputWires[j++].connectTo(cir.inputWires,i);
  }
  protected void defineOutputWires() {
    for(int i=0;i<outDegree;++i)
      outputWires[i]=cir.outputWires[i];
  }
  protected void fixInternalWires() {
    for(int i=0;i<cir.inputWires.length;++i)
      if(state.wires[i].lbl==null)
      {
        assert state.wires[i].value!=YaoGC.Wire.UNKNOWN_SIG;
        cir.inputWires[i].fixWire(state.wires[i].value);
      }
    if(inDegree==0) System.err.println("Here");
  }

  // NOT an override
  public State startExecuting() {
    if(inDegree==0) 
    {
      for(int i=0;i<outputWires.length;++i) 
        assert outputWires[i].value != YaoGC.Wire.UNKNOWN_SIG;

      return State.fromWires(outputWires);
    }
    State[] start = new State[inDegree];
    int i,j=0;
    for(i=0;i<cir.inputWires.length;++i)
      if(state.wires[i].lbl!=null) start[j++]=State.extractState(state,i,i+1);
    return super.startExecuting(concatAll(start));
  }
  public static State concatAll(State[] s)
  {
    State rv=s[0];
    for(int i=1;i<s.length;++i)
      rv = State.fromConcatenation(rv,s[i]);
    return rv;
  }
}
