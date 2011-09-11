package test;

import ast.*;
import ast.apps.*;

public class SplitOpsTest
{
  public static void test(AstNode root)
  {
    SubtreeOpCount soc = new SplitOpsTest(true).exploreSubtree(root);
    SubtreeOpCount soc2 = new SplitOpsTest(false).exploreSubtree(root);
    System.err.println(soc2.oc.ab+" from "+
        "dumb: "+soc.oc.ab+"+"+soc.oc.a+"+"+soc.oc.b+" plain: "+
        ast.AstNodeCounter.count(root));
  }
  AstVisitedMap<OpStat> opStat = new AstVisitedMap<OpStat>();
  AstVisitedMap<GroupChildren> groupChildren 
    = new AstVisitedMap<GroupChildren>();

  // whether or not this depends on inputs from Alice or Bob
  private static class OpStat  {  boolean a,b; } // default init false
  // number of operations of each type, never memoized
  private static class OpCount 
  { int a,b,ab;    // default init to 0
    void add(OpCount oc) { a+=oc.a; b+=oc.b; ab+=oc.ab; }
  }

  // number of children in a group
  private static class GroupChildren
  {
    int childA,childB,childAB,child1; // default init = 0
    OpStat os = new OpStat();
    boolean allSecret() 
      { return childA==0 && childB==0 && child1==0 && childAB!=0; }
    GroupChildren copy()
    {
      GroupChildren rv = new GroupChildren();
      rv.childA=this.childA;
      rv.childB=this.childB;
      rv.childAB=this.childAB;
      rv.child1=this.child1;
      rv.os.a=this.os.a;
      rv.os.b=this.os.b;
      return rv;
    }
  }

  private static class SubtreeOpCount
  { OpCount oc = new OpCount();
    OpStat os = new OpStat();
  }
  private static class GroupOpCount
  { OpCount oc = new OpCount();
    GroupChildren gc = new GroupChildren();

    void add(GroupOpCount gop)
    { 
      oc.add(gop.oc);
      gc.childA+=gop.gc.childA;
      gc.childB+=gop.gc.childB;
      gc.child1+=gop.gc.child1;
      gc.childAB+=gop.gc.childAB;
      gc.os.a = gc.os.a || gop.gc.os.a;
      gc.os.b = gc.os.b || gop.gc.os.b;
    }
  }

  private boolean dumbCount;

  private SplitOpsTest(boolean dc) { dumbCount=dc; }
  SubtreeOpCount exploreSubtree(AstNode node)
  {
    SubtreeOpCount rv = new SubtreeOpCount();
    if(opStat.isVisited(node)) 
    { rv.os=opStat.valueAt(node);
      return rv;  // already counted, return 0 for counts
    }

    if(node.children().length==0)
    {
      if(node.getType()==AstValueNode.class) 
      { opStat.visit(node,rv.os); 
        return rv;
      }
      AstCharRef a,b;
      // refactor into a superclass or if/else thing
      if(node.getType()==AstNequNode.class)
      { AstNequNode d = (AstNequNode)node.getData();
        a = d.getOperandA(); b = d.getOperandB();
      }else if(node.getType()==AstSWSimilarityNode.class)
      { AstSWSimilarityNode d = (AstSWSimilarityNode)node.getData();
        a = d.getOperandA(); b = d.getOperandB();
      }else throw new AstReducer.UnknownNodeException(node);

      if(a.isSymbolic() && b.isSymbolic()) { rv.oc.ab++; rv.os.a=rv.os.b=true; }
      else if(a.isSymbolic()) { rv.oc.a++; rv.os.a=true; }
      else if(b.isSymbolic()) { rv.oc.b++; rv.os.b=true; }
      else assert false: "Reducer is not evaluating const expressions";
      opStat.visit(node,rv.os);
      return rv;
    }

    if(dumbCount) normalCount(node,rv);
    else exploreAssocGroup(node,rv);
    opStat.visit(node,rv.os);
    return rv;
  }

  String nodeStr(AstNode node)
  {
    java.io.ByteArrayOutputStream os = new java.io.ByteArrayOutputStream();
    AstPrinter.print(node,new java.io.PrintStream(os));
    return os.toString();
  }
  void normalCount(AstNode node, SubtreeOpCount rv)
  {
    AstNode[] child = node.children();
    int fulc=0;
    boolean justa=false,justb=false;
    for(int i=0;i<child.length;++i)
    { SubtreeOpCount soc = exploreSubtree(child[i]);
      rv.oc.add(soc.oc);
      rv.os.a|=soc.os.a;
      rv.os.b|=soc.os.b;
      if(dumbCount) fulc++;
      else
      { if(soc.os.a && soc.os.b) fulc++;
        else if(soc.os.a) justa=true;
        else if(soc.os.b) justb=true;
      }
    }
    if(fulc>0 || (justa&&justb))
    { if(justa) fulc++;
      if(justb) fulc++;
      rv.oc.ab+=fulc-1;
    }
  }

  void exploreAssocGroup(AstNode node,SubtreeOpCount rv)
  {
    GroupOpCount res = exploreAssocSubgroup(node);
    if(!res.gc.os.a) assert res.gc.childA==0;
    if(!res.gc.os.b) assert res.gc.childB==0;
    if(!res.gc.os.a && !res.gc.os.b) assert res.gc.childAB==0;

    rv.oc.add(res.oc);
    // this is true iff exploreAssocSubgroup returned through [opaqueLine]
    if(res.gc.childA==0 && res.gc.childB==0 && 
        res.gc.childAB==0 && res.gc.child1==0) return;
    int extra = 0;
    if(res.gc.childA!=0) 
    { extra++; 
      rv.oc.a+=res.gc.childA-1; 
      rv.os.a=true;
    }
    if(res.gc.childB!=0) 
    { extra++; 
      rv.oc.b+=res.gc.childB-1; 
      rv.os.b=true;
    }
    if(res.gc.child1!=0) 
    { if(res.gc.childA!=0) rv.oc.a++;
      else if(res.gc.childB!=0) rv.oc.b++;
      else extra++;
    }
    if(res.gc.childAB!=0 || (res.gc.childA!=0 && res.gc.childB!=0))
    { rv.oc.ab+=extra+res.gc.childAB-1;
      rv.os.a=rv.os.b=true;  // our output is a secure output
      return;
    }
    assert res.gc.childA!=0 || res.gc.childB!=0 
      : "Reducer not folding constants "+res.gc.child1+" "+nodeStr(node);
    return;
  }
  GroupOpCount exploreAssocSubgroup(AstNode node)
  {
    GroupOpCount rv = new GroupOpCount();
    if(groupChildren.isVisited(node)) 
    { rv.gc=groupChildren.valueAt(node).copy();
      if(rv.gc.childAB>0) rv.gc.childAB=1;
      return rv;
    }

    AstNode[] child = node.children();
    for(int i=0;i<child.length;++i)
    { if(node.getType()==child[i].getType())
//      { if(node.getType()==AstMaxNode.class) System.err.println("hh");
        rv.add(exploreAssocSubgroup(child[i]));
//      }
      else
      {
        SubtreeOpCount res = exploreSubtree(child[i]);
        rv.oc.add(res.oc);
        if(res.os.a && res.os.b) rv.gc.childAB++;
        else if(res.os.a) { rv.gc.childA++; }
        else if(res.os.b) { rv.gc.childB++; }
        else rv.gc.child1++;
        if(res.os.a) rv.gc.os.a=true;
        if(res.os.b) rv.gc.os.b=true;
      }
    }
    if(false && rv.gc.allSecret())  // collapse, no point going beyond this again
    { rv.oc.ab+=rv.gc.childAB-1;
      rv.gc.childAB=0;     // [opaqueLine]
    }
    groupChildren.visit(node,rv.gc);
    return rv;
  }
}
