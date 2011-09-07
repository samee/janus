package ast;

import java.util.HashMap;
import java.util.HashSet;

// Checks for the equality of the structure of AstNode, tries to speed it up
public class StructureMatcher
{
  HashSet<NodePair> equalities = new HashSet<NodePair>();
  public StructureMatcher() {}

  public int hashOf(AstNode node)
    { return node.getData().hashCode();  }

  public int equCacheSize() { return equalities.size(); }
  public void resetEquCache() { equalities.clear(); }
  public boolean checkEqual(AstNode a,AstNode b)
  {
    if(a==b) return true;
    if(hashOf(a)!=hashOf(b)) return false;
    if(a.getType()!=b.getType()) return false;
    AstNode[] ach = a.children(), bch = b.children();
    if(ach.length!=bch.length) return false;
    if(ach.length == 0) return a.getData().equals(b.getData());
    NodePair np = new NodePair(a,b);
    if(equalities.contains(np)) return true;
    for(int i=0;i<ach.length;++i) if(!checkEqual(ach[i],bch[i])) return false;
    equalities.add(np);
    return true;
  }
  
  // ref hashes *should* be safe for the purposes of the set
  private static class NodePair
  {
    public AstNode a,b;
    public NodePair(AstNode a,AstNode b) 
    { this.a=a; this.b=b; 
      if(a.hashCode()>b.hashCode()) { this.a=b; this.b=a; } // swap
    }
    public int hashCode() 
      { return HashUtil.combine(a.hashCode(),b.hashCode()); }
    public boolean equals(Object that)
    { if(this==that) return true;
      if(this.getClass()!=that.getClass()) return false;
      NodePair tthat = (NodePair)that;
      return this.a==tthat.a && this.b==tthat.b;
    }
  }
}
