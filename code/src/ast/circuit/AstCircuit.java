package ast.circuit;

import java.util.Hashtable;
import java.util.Map;
import ast.*;
import YaoGC.*;

/*
Usage:
  AstNode root = ... ;
  AstCircuit.Generator gen = new AstCircuit.Generator();
  gen.charTraits = ...;
  gen.astNodeWidth = ...;
  AstCircuit cir = gen.generate(root);

Circuit is created directly from expression, without any knowledge
of which private value belongs to whom, or what their values are. 
It only provides a mapping from AstCharRef to input wire index,
using the method .getInputs().

The one actually executing the circuit should be able to provide
the actual private values for each of those AstCharRef.
   */

public class AstCircuit extends CompositeCircuit
{
  private AstCharTraits charTraits;
  private AstNodeWidth astNodeWidth;
  private AstProperties props;
  private int outputComponent;

  private AstCircuit(AstNode root, AstProperties props, 
      AstCharTraits charTraits, AstNodeWidth astNodeWidth, String name)
  {
    super(props.inputBits,astNodeWidth.bitsFor(root),
        props.subCircuitCount,name);
    this.charTraits = charTraits;
    this.astNodeWidth = astNodeWidth;
    this.props = props;
    outputComponent = props.circuitIndex.get(root);

    try { build(); } catch (Exception e) { e.printStackTrace(); System.exit(1);}
  }

  protected void createSubCircuits() throws Exception
  {
    for(Map.Entry<AstNode,Integer> mentry : props.circuitIndex.entrySet())
    { 
      AstNode node = mentry.getKey();
      int i=mentry.getValue(),j;
      Class t = node.getType();
      int bits = astNodeWidth.bitsFor(node);
      int cc = node.children().length;
      if(t==AstValueNode.class) subCircuits[i]=new XOR_2L_L(bits);
      else if(t==AstNequNode.class) 
        subCircuits[i] = new NEQUAL_2L_1(charTraits.bitsPerChar());
      else if(t==AstMinNode.class) 
      { int[] bitsArr = astNodeWidth.bitsForComponents(node);
        for(j=i;j<i+cc-1;++j) subCircuits[j]=new MIN_2L_L(bitsArr[j-i]);
      }
      else if(t==AstAddNode.class) 
      { int[] bitsArr = astNodeWidth.bitsForComponents(node);
        for(j=i;j<i+cc-1;++j) subCircuits[j]=new ADD_2L_Lplus1(bitsArr[j-i]);
      }
    }
    System.err.println("Subcircuits: "+subCircuits.length);
    /*
    for(int i=0;i<subCircuits.length;++i)
      System.err.print(subCircuits[i]==null?0:1);
    System.err.println();
    */
    super.createSubCircuits();
  }

  private void hookCharInput(Wire[] subInputWires,int subInputIndex,
    int extInputIndex)
  {
    int i,sz = charTraits.bitsPerChar();
    for(i=0;i<sz;++i) 
      connect(inputWires,extInputIndex+i,subInputWires,subInputIndex+i);
  }
  private Hashtable<Wire,Wire> sinkToSource = new Hashtable<Wire,Wire>();
  private void connect(Wire[] from,int findex,Wire to[],int tindex)
  {
    /*
    if(sinkToSource.containsKey(to[tindex])) 
      throw new RuntimeException("Duplicate wire assignment");
    sinkToSource.put(to[tindex],from[findex]);
    */
    from[findex].connectTo(to,tindex);
    /*
    String fport="",tport="";
    if(from==inputWires) fport="extInput";
    if(to==outputWires) tport="canthappen";
    for(int i=0;i<subCircuits.length;++i)
    { if(from==subCircuits[i].outputWires) 
        fport=subCircuits[i]+" output";
      if(to==subCircuits[i].inputWires)
        tport=subCircuits[i]+" input";
    }
    System.err.println(fport+'['+findex+"] to "+tport+'['+tindex+']');
    */
  }
  private void fixWire(Wire[] to,int tindex,int v) 
  {
    to[tindex].fixWire(v); 
    /*
    String tport="";
    for(int i=0;i<subCircuits.length;++i)
      if(to==subCircuits[i].inputWires)
        tport = subCircuits[i]+" input";
    System.err.println(tport+'['+tindex+"] fixed "+v);
    */
  }

  protected void connectWires() throws Exception
  {
    for(Map.Entry<AstNode,Integer> mentry : props.circuitIndex.entrySet())
    { AstNode node = mentry.getKey();
      int i = mentry.getValue();
      Class t = node.getType();
      int cc = node.children().length;
      if(t==AstValueNode.class) continue;
      else if(t==AstNequNode.class)
      { AstNequNode d = (AstNequNode)node.getData();
        if(props.inputIndex.containsKey(d.getOperandA()))
          hookCharInput(subCircuits[i].inputWires,0,
              props.inputIndex.get(d.getOperandA()));
        if(props.inputIndex.containsKey(d.getOperandB()))
          hookCharInput(subCircuits[i].inputWires,charTraits.bitsPerChar(),
              props.inputIndex.get(d.getOperandB()));
      }else if(t==AstAddNode.class)
      { // we need to work on subCircuits [i,i+cc-1)
        Wire[] prev = subCircuits[props.circuitIndex.get(node.children()[0])]
          .outputWires;
        for(int j=i+cc-2;j>=i;--j)
        { int wires = subCircuits[j].inputWires.length/2;
          Wire[] side = subCircuits[props.circuitIndex.get(
              node.children()[j-i+1])].outputWires;
          for(int k=0;k<wires;++k) // each wire
          { if(k>=prev.length) fixWire(subCircuits[j].inputWires,2*k,0);
            else connect(prev,k,subCircuits[j].inputWires,2*k);
            if(k>=side.length) fixWire(subCircuits[j].inputWires,2*k+1,0);
            else connect(side,k,subCircuits[j].inputWires,2*k+1);
          }
          prev = subCircuits[j].outputWires;
        }
      }else if(t==AstMinNode.class)
      { // we need to work on subCircuits [i,i+cc-1)
        Wire[] prev = subCircuits[props.circuitIndex.get(node.children()[0])]
          .outputWires;
        for(int j=i+cc-2;j>=i;--j)
        { int wires = subCircuits[j].inputWires.length/2;
          Wire[] side = subCircuits[props.circuitIndex.get(
              node.children()[j-i+1])].outputWires;
          for(int k=0;k<wires;++k) // each wire
          { if(k>=prev.length) fixWire(subCircuits[j].inputWires,k,0);
            else connect(prev,k,subCircuits[j].inputWires,k);
            if(k>=side.length) fixWire(subCircuits[j].inputWires,k+wires,0);
            else connect(side,k,subCircuits[j].inputWires,k+wires);
          }
          prev = subCircuits[j].outputWires;
        }
      }
    }
  }

  protected void defineOutputWires()
  {
    int i,ci = outputComponent;
    for(i=0;i<outDegree;++i) 
    {
      outputWires[i] = subCircuits[ci].outputWires[i];
      /*
      System.err.println(subCircuits[ci]+" output["+i+
          "] to extOutput["+i+"]");
          */
    }
  }

  protected void fixInternalWires()
  { 
    for(Map.Entry<AstNode,Integer> mentry : props.circuitIndex.entrySet())
    { AstNode node = mentry.getKey();
      int i = mentry.getValue();
      Class t = node.getType();
      if(t==AstValueNode.class) 
      {
        int v = ((AstValueNode)node.getData()).getValue();
        int wd = subCircuits[i].outputWires.length;
        fixWires(subCircuits[i].inputWires,0,wd,0);
        fixWires(subCircuits[i].inputWires,wd,wd*2,v);
      }
      else if(t==AstNequNode.class)
      { AstNequNode d = (AstNequNode)node.getData();
        int wd = subCircuits[i].inputWires.length/2;
        if(!d.getOperandA().isSymbolic())
          fixWires(subCircuits[i].inputWires,0,wd,
              charTraits.encode(d.getOperandA().getChar()));
        if(!d.getOperandB().isSymbolic())
          fixWires(subCircuits[i].inputWires,wd,wd*2,
              charTraits.encode(d.getOperandB().getChar()));
      }
    }
  }
  
  public void fixWires(Wire[] input,int st,int en,int value)
  {
    for(int i=st;i<en;++i)
    {
      fixWire(input,i,value%2);
      value/=2;
    }
  }
  public void fixWires(Wire[] input,int st,int en,char ch)
  {
    fixWires(input,st,en,(int)ch);
  }

  /*
     Intended for creating input State object from input wire labels.  Maps
     from AST input nodes to indices in circuit input wires. Used after the
     circuit has already been generated and initialized.

     Return value must NOT be modified! Normally you would want to simply loop
     through the Hashtable in a for-each loop.
     */
  public Hashtable<AstCharRef,Integer> getInputs() 
  { 
    return props.inputIndex;
  }

  // takes in all the required parameters, and calls the constructor
  public static class Generator
  {
    // various construction parameters and defaults
    public AstCharTraits charTraits = new AstDefaultCharTraits();
    public AstNodeWidth astNodeWidth = new AstNodeWidth.Fixed(4);

    public AstCircuit generate(AstNode root)
      { return generate(root,root.toString()); } // use ref as name
    public AstCircuit generate(AstNode root,String circuitName)
    {
      // traverse circuit to get input count, output count and component count
      // and maps I/O to actual pin numbers of circuit
      AstProperties param = new AstProperties();
      explore(root,param);
      return new AstCircuit(root,param,charTraits,astNodeWidth,circuitName);
    }
    // get topologically sorted ordering
    private void explore(AstNode node, AstProperties params)
    {
      if(params.circuitIndex.containsKey(node)) return;

      if(node.getType()==AstNequNode.class)
      { AstNequNode d = (AstNequNode)node.getData();
        int wd = charTraits.bitsPerChar();
        if(d.getOperandA().isSymbolic()) params.newInput(d.getOperandA(),wd);
        if(d.getOperandB().isSymbolic()) params.newInput(d.getOperandB(),wd);
      }

      AstNode[] child = node.children();
      int sc=-1;
      if(child.length==0) sc=1;
      else 
      { assert child.length>=2;
        sc=child.length-1;
      }
      for(int i=0;i<child.length;++i)
        explore(child[i],params);

      params.newNode(node,sc);
    }
  }

  private static class AstProperties
  {
    public int inputBits, subCircuitCount;
    // Maps from AstNode to the first slot index of subCircuits[]
    //   that corresponds to the node. One node can expand to multiple
    //   subCircuits
    public Hashtable<AstNode,Integer> circuitIndex;
    // Maps from AstCharRef to the first slot index of inputWires[]
    //   that corresponds to the input. One input is often represented
    //   by multiple input wires
    public Hashtable<AstCharRef,Integer> inputIndex;

    public AstProperties() 
    {
      inputBits=subCircuitCount=0;
      circuitIndex = new Hashtable<AstNode,Integer>();
      inputIndex = new Hashtable<AstCharRef,Integer>();
    }

    void newNode(AstNode node,int subCount)
    {
      assert subCount>0;
      if(circuitIndex.containsKey(node)) return;
      circuitIndex.put(node,subCircuitCount);
      subCircuitCount+=subCount;
    }

    void newInput(AstCharRef chref,int bitCount) 
    { if(inputIndex.containsKey(chref)) return;
      inputIndex.put(chref,inputBits);
      inputBits+=bitCount;
    }
  }
}
